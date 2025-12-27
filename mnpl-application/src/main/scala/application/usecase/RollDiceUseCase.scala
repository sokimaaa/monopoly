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
    propertyService: PropertyService,
    diceRoller: DiceRoller,
    eventPublisher: EventPublisher
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
      (finalGame, additionalEvents) = result // todo: hack used, need to think of better way

      _ <- gameRepository.update(finalGame)
    } yield {
      val allEvents = moveEvent :: additionalEvents
      allEvents.foreach(eventPublisher.publish)
      (finalGame, roll, allEvents)
    }

  private def handleSquareAction(
      game: Game,
      square: Square
  ): Either[String, (Game, List[GameEvent])] =
    squareActionService.determineAction(square, game) match {
      case SquareAction.NoAction =>
        Right((game, Nil))

      case SquareAction.PayRentAction(property, landlordId) =>
        val tenant   = game.currentPlayer
        val landlord = game.players.find(_.id == landlordId).get

        val (updatedTenant, updatedLandlord, rentEvent) =
          propertyService.payRent(tenant, landlord, property)

        val updatedGame = game
          .updatePlayer(updatedTenant)
          .updatePlayer(updatedLandlord)

        Right((updatedGame, List(rentEvent)))

      case SquareAction.PayTaxAction(amount) =>
        val player        = game.currentPlayer
        val updatedPlayer = player.pay(amount)
        val taxEvent      = GameEvent.TaxPaid(player.id, amount)

        Right((game.updatePlayer(updatedPlayer), List(taxEvent)))

      case _ =>
        Right((game, Nil))
    }
}
