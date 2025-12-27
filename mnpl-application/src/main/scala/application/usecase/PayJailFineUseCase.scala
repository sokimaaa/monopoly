package com.sokima.monopoly
package application.usecase

import application.port.{EventPublisher, GameRepository, PropertyRepository}
import domain.{Game, GameId, Money, Property}
import domain.service.{Payee, PaymentService}

class PayJailFineUseCase(
    gameRepository: GameRepository,
    propertyRepository: PropertyRepository,
    paymentService: PaymentService,
    eventPublisher: EventPublisher
) {

  private val jailFine = Money(50)

  def execute(gameId: GameId): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      _ <- Either.cond(game.currentPlayer.inJail, (), "Player is not in jail")
      _ <- Either.cond(!game.turnState.diceRolled, (), "Dice already rolled this turn")
      result <- paymentService.pay(game, game.currentPlayer.id, Payee.Bank, jailFine)
      updatedGame = result.game.players.find(_.id == game.currentPlayer.id) match {
        case Some(player) if player.isBankrupt => result.game
        case Some(player)                      => result.game.updatePlayer(player.releaseFromJail)
        case None                              => result.game
      }
      _ <- updateProperties(result.releasedProperties)
      _ <- gameRepository.update(updatedGame)
    } yield {
      result.events.foreach(eventPublisher.publish)
      updatedGame
    }

  private def updateProperties(properties: List[Property]): Either[String, Unit] =
    properties.foldLeft(Right(()): Either[String, Unit]) { (acc, property) =>
      acc.flatMap(_ => propertyRepository.update(property).map(_ => ()))
    }
}
