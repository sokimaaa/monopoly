package com.sokima.monopoly
package application.usecase

import application.port.{EventPublisher, GameRepository, PropertyRepository}
import domain.*
import domain.service.*

class RollDiceUseCase(
    gameRepository: GameRepository,
    propertyRepository: PropertyRepository,
    movementService: MovementService,
    squareActionService: SquareActionService,
    diceRoller: DiceRoller,
    eventPublisher: EventPublisher,
    paymentService: PaymentService
) {

  private val jailFine = Money(50)

  def execute(gameId: GameId): Either[String, (Game, DiceRoll, List[GameEvent])] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      _    <- Either.cond(!game.isFinished, (), "Game is finished")
      _    <- Either.cond(!game.currentPlayer.isBankrupt, (), "Current player is bankrupt")
      _    <- Either.cond(!game.turnState.diceRolled, (), "Dice already rolled this turn")

      result <- handleTurn(game)
      finalizedGame = result.game.checkGameOver
      finishEvent = if (finalizedGame.isFinished) {
        finalizedGame.activePlayers.headOption.map(w => GameEvent.GameFinished(w.id))
      } else {
        None
      }
      _ <- updateProperties(result.releasedProperties)
      _ <- gameRepository.update(finalizedGame)
    } yield {
      val allEvents = result.events ++ finishEvent.toList
      allEvents.foreach(eventPublisher.publish)
      (finalizedGame, result.roll, allEvents)
    }

  private def handleTurn(game: Game): Either[String, ActionResult] =
    if (game.currentPlayer.inJail) handleJailTurn(game) else handleRegularTurn(game)

  private def handleRegularTurn(game: Game): Either[String, ActionResult] = {
    val roll = diceRoller.roll()
    val updatedDoublesCount =
      if (roll.isDoubles) game.turnState.doublesCount + 1 else 0
    val gameAfterRoll = game.recordDiceRoll(roll, extraRoll = roll.isDoubles)

    if (updatedDoublesCount >= 3) {
      sendToJail(gameAfterRoll, roll)
    } else {
      val (movedPlayer, moveEvent) = movementService.movePlayer(game.currentPlayer, roll, game.board)
      val gameAfterMove = gameAfterRoll.updatePlayer(movedPlayer)
      for {
        square <- gameAfterMove.board.getSquare(movedPlayer.position).toRight("Invalid position")
        result <- handleSquareAction(gameAfterMove, square, roll, moveEvent)
      } yield result
    }
  }

  private def handleJailTurn(game: Game): Either[String, ActionResult] = {
    val roll   = diceRoller.roll()
    val player = game.currentPlayer

    if (roll.isDoubles) {
      val released = player.releaseFromJail
      val (movedPlayer, moveEvent) = movementService.movePlayer(released, roll, game.board)
      val gameAfterMove = game
        .recordDiceRoll(roll, extraRoll = false)
        .updatePlayer(movedPlayer)
      for {
        square <- gameAfterMove.board.getSquare(movedPlayer.position).toRight("Invalid position")
        result <- handleSquareAction(gameAfterMove, square, roll, moveEvent)
      } yield result
    } else {
      val attemptedPlayer = player.recordJailAttempt
      if (attemptedPlayer.jailTurns >= 3) {
        val gameAfterAttempt = game
          .recordDiceRoll(roll, extraRoll = false)
          .updatePlayer(attemptedPlayer)
        val payment = paymentService.pay(gameAfterAttempt, player.id, Payee.Bank, jailFine)
        payment.flatMap { result =>
          val updatedPlayer = result.game.players.find(_.id == player.id)
          updatedPlayer match {
            case Some(p) if p.isBankrupt =>
              Right(ActionResult(result.game, result.events, result.releasedProperties, roll))
            case Some(p) =>
              val released = p.releaseFromJail
              val (movedPlayer, moveEvent) = movementService.movePlayer(released, roll, result.game.board)
              val gameAfterMove = result.game.updatePlayer(movedPlayer)
              for {
                square <- gameAfterMove.board.getSquare(movedPlayer.position).toRight("Invalid position")
                actionResult <- handleSquareAction(gameAfterMove, square, roll, moveEvent)
              } yield actionResult.copy(
                events = actionResult.events ++ result.events,
                releasedProperties = actionResult.releasedProperties ++ result.releasedProperties
              )
            case None =>
              Left("Player not found")
          }
        }
      } else {
        val gameAfterAttempt = game
          .recordDiceRoll(roll, extraRoll = false)
          .updatePlayer(attemptedPlayer)
        Right(ActionResult(gameAfterAttempt, Nil, Nil, roll))
      }
    }
  }

  private def handleSquareAction(
      game: Game,
      square: Square,
      roll: DiceRoll,
      moveEvent: GameEvent.PlayerMoved
  ): Either[String, ActionResult] =
    squareActionService.determineAction(square, game) match {
      case SquareAction.NoAction =>
        Right(ActionResult(game, List(moveEvent), Nil, roll))

      case SquareAction.PropertyAvailable(property) =>
        val event = GameEvent.PropertyAvailable(game.currentPlayer.id, property.id, property.price)
        Right(ActionResult(game, List(moveEvent, event), Nil, roll))

      case SquareAction.PayRentAction(property, landlordId) =>
        val tenant   = game.currentPlayer
        val payment = paymentService.pay(game, tenant.id, Payee.Player(landlordId), property.rent)
        payment.map { result =>
          val rentEvent =
            GameEvent.RentPaid(tenant.id, landlordId, property.rent, property.name)
          val updatedGame = disableExtraRollIfBankrupt(result.game, tenant.id)
          ActionResult(
            updatedGame,
            moveEvent :: (rentEvent :: result.events),
            result.releasedProperties,
            roll
          )
        }

      case SquareAction.PayTaxAction(amount) =>
        val player  = game.currentPlayer
        val payment = paymentService.pay(game, player.id, Payee.Bank, amount)
        payment.map { result =>
          val taxEvent = GameEvent.TaxPaid(player.id, amount)
          val updatedGame = disableExtraRollIfBankrupt(result.game, player.id)
          ActionResult(
            updatedGame,
            moveEvent :: (taxEvent :: result.events),
            result.releasedProperties,
            roll
          )
        }

      case SquareAction.GoToJailAction =>
        sendToJail(game.copy(turnState = game.turnState.copy(extraRoll = false)), roll)
          .map(result => result.copy(events = List(moveEvent)))

      case _ =>
        Right(ActionResult(game, List(moveEvent), Nil, roll))
    }

  private def sendToJail(game: Game, roll: DiceRoll): Either[String, ActionResult] =
    for {
      jailPosition <- game.board.jailPosition.toRight("Jail position not found")
    } yield {
      val jailedPlayer = game.currentPlayer.sendToJail(jailPosition)
      val updatedGame = game
        .updatePlayer(jailedPlayer)
        .copy(turnState = game.turnState.copy(extraRoll = false))
      ActionResult(updatedGame, Nil, Nil, roll)
    }

  private def updateProperties(properties: List[Property]): Either[String, Unit] =
    properties.foldLeft(Right(()): Either[String, Unit]) { (acc, property) =>
      acc.flatMap(_ => propertyRepository.update(property).map(_ => ()))
    }

  private def disableExtraRollIfBankrupt(game: Game, playerId: PlayerId): Game =
    game.players.find(_.id == playerId) match {
      case Some(player) if player.isBankrupt =>
        game.copy(turnState = game.turnState.copy(extraRoll = false))
      case _ => game
    }

  private case class ActionResult(
      game: Game,
      events: List[GameEvent],
      releasedProperties: List[Property],
      roll: DiceRoll
  )
}
