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

  def execute(gameId: GameId): Either[String, (Game, DiceRoll, List[GameEvent])] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      _    <- Either.cond(!game.isFinished, (), "Game is finished")
      _    <- Either.cond(!game.currentPlayer.isBankrupt, (), "Current player is bankrupt")
      _    <- Either.cond(!game.turnState.diceRolled, (), "Dice already rolled this turn")

      roll                     = diceRoller.roll()
      (movedPlayer, moveEvent) = movementService.movePlayer(game.currentPlayer, roll, game.board)

      gameAfterMove = game
        .recordDiceRoll(roll)
        .updatePlayer(movedPlayer)
      square <- gameAfterMove.board.getSquare(movedPlayer.position).toRight("Invalid position")

      result <- handleSquareAction(gameAfterMove, square)
      finalizedGame = result.game.checkGameOver
      finishEvent = if (finalizedGame.isFinished) {
        finalizedGame.activePlayers.headOption.map(w => GameEvent.GameFinished(w.id))
      } else {
        None
      }
      _ <- updateProperties(result.releasedProperties)
      _ <- gameRepository.update(finalizedGame)
    } yield {
      val allEvents = moveEvent :: (result.events ++ finishEvent.toList)
      allEvents.foreach(eventPublisher.publish)
      (finalizedGame, roll, allEvents)
    }

  private def handleSquareAction(
      game: Game,
      square: Square
  ): Either[String, ActionResult] =
    squareActionService.determineAction(square, game) match {
      case SquareAction.NoAction =>
        Right(ActionResult(game, Nil, Nil))

      case SquareAction.PropertyAvailable(property) =>
        val event = GameEvent.PropertyAvailable(game.currentPlayer.id, property.id, property.price)
        Right(ActionResult(game, List(event), Nil))

      case SquareAction.PayRentAction(property, landlordId) =>
        val tenant   = game.currentPlayer
        val payment = paymentService.pay(game, tenant.id, Payee.Player(landlordId), property.rent)
        payment.map { result =>
          val rentEvent =
            GameEvent.RentPaid(tenant.id, landlordId, property.rent, property.name)
          ActionResult(result.game, rentEvent :: result.events, result.releasedProperties)
        }

      case SquareAction.PayTaxAction(amount) =>
        val player        = game.currentPlayer
        val payment = paymentService.pay(game, player.id, Payee.Bank, amount)
        payment.map { result =>
          val taxEvent = GameEvent.TaxPaid(player.id, amount)
          ActionResult(result.game, taxEvent :: result.events, result.releasedProperties)
        }

      case _ =>
        Right(ActionResult(game, Nil, Nil))
    }

  private def updateProperties(properties: List[Property]): Either[String, Unit] =
    properties.foldLeft(Right(()): Either[String, Unit]) { (acc, property) =>
      acc.flatMap(_ => propertyRepository.update(property).map(_ => ()))
    }

  private case class ActionResult(
      game: Game,
      events: List[GameEvent],
      releasedProperties: List[Property]
  )
}
