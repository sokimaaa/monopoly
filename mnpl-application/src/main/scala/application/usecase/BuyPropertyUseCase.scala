package com.sokima.monopoly
package application.usecase

import application.port.{EventPublisher, GameRepository, PropertyRepository}
import domain.service.{GameEvent, PropertyService}
import domain.{Game, GameId, Property, Square}

class BuyPropertyUseCase(
                          gameRepository: GameRepository,
                          propertyRepository: PropertyRepository,
                          propertyService: PropertyService,
                          eventPublisher: EventPublisher
                        ) {

  def execute(gameId: GameId): Either[String, (Game, Property, GameEvent.PropertyPurchased)] = {
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      _ <- Either.cond(game.rules.allowPropertyPurchases, (), "Property purchases are disabled in this game")
      player = game.currentPlayer

      square <- game.board.getSquare(player.position).toRight("Invalid position")
      property <- extractProperty(square)

      result <- propertyService.purchaseProperty(player, property)
      (updatedPlayer, updatedProperty, purchaseEvent) = result

      updatedGame = game.updatePlayer(updatedPlayer)

      _ <- propertyRepository.update(updatedProperty)
      _ <- gameRepository.update(updatedGame)
    } yield {
      eventPublisher.publish(purchaseEvent)
      (updatedGame, updatedProperty, purchaseEvent)
    }
  }

  private def extractProperty(square: Square): Either[String, Property] = square match {
    case Square.PropertySquare(property) => Right(property)
    case _ => Left("No property at current position")
  }
}
