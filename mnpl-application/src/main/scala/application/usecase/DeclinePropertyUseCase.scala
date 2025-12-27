package com.sokima.monopoly
package application.usecase

import application.port.{EventPublisher, GameRepository}
import domain.{Game, GameId, PlayerId, Property, Square}
import domain.service.GameEvent

class DeclinePropertyUseCase(
    gameRepository: GameRepository,
    eventPublisher: EventPublisher
) {

  def execute(gameId: GameId): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      player = game.currentPlayer
      square <- game.board.getSquare(player.position).toRight("Invalid position")
      property <- extractProperty(square)
      propertyForAuction = ownerBankrupt(property, game) match {
        case true  => property.clearOwner
        case false => property
      }
      _ <- Either.cond(!propertyForAuction.isOwned, (), "Property already owned")
      bidders = game.players.filterNot(_.isBankrupt).map(_.id).toSet
      updatedGame <- game.startAuction(propertyForAuction.id, bidders)
      _ <- gameRepository.update(updatedGame)
    } yield {
      eventPublisher.publish(
        GameEvent.PropertyAvailable(player.id, propertyForAuction.id, propertyForAuction.price)
      )
      updatedGame
    }

  private def extractProperty(square: Square): Either[String, Property] = square match {
    case Square.PropertySquare(property) => Right(property)
    case _                               => Left("No property at current position")
  }

  private def ownerBankrupt(property: Property, game: Game): Boolean =
    property.ownerId.flatMap(id => game.players.find(_.id == id)).exists(_.isBankrupt)
}
