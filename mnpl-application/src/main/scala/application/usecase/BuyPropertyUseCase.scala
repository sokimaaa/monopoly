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

  def execute(gameId: GameId): Either[String, (Game, Property, GameEvent.PropertyPurchased)] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      player = game.currentPlayer

      square   <- game.board.getSquare(player.position).toRight("Invalid position")
      property <- extractProperty(square)
      propertyForPurchase = ownerBankrupt(property, game) match {
        case true  => property.clearOwner
        case false => property
      }

      result <- propertyService.purchaseProperty(player, propertyForPurchase)
      (updatedPlayer, updatedProperty, purchaseEvent) = result

      updatedGame = game
        .updatePlayer(updatedPlayer)
        .updateProperty(updatedProperty)

      _ <- propertyRepository.update(updatedProperty)
      _ <- gameRepository.update(updatedGame)
    } yield {
      eventPublisher.publish(purchaseEvent)
      (updatedGame, updatedProperty, purchaseEvent)
    }

  private def extractProperty(square: Square): Either[String, Property] = square match {
    case Square.PropertySquare(property) => Right(property)
    case _                               => Left("No property at current position")
  }

  private def ownerBankrupt(property: Property, game: Game): Boolean =
    property.ownerId.flatMap(id => game.players.find(_.id == id)).exists(_.isBankrupt)
}
