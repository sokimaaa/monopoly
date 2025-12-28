package com.sokima.monopoly
package application.usecase

import application.port.{GameRepository, PropertyRepository}
import domain.*
import domain.service.PropertyService

class PropertyImprovementUseCase(
    gameRepository: GameRepository,
    propertyRepository: PropertyRepository,
    propertyService: PropertyService
) {

  def buildHouse(gameId: GameId, propertyId: PropertyId): Either[String, Game] =
    updateImprovement(gameId, propertyId)(propertyService.buildHouse)

  def buildHotel(gameId: GameId, propertyId: PropertyId): Either[String, Game] =
    updateImprovement(gameId, propertyId)(propertyService.buildHotel)

  def sellHouse(gameId: GameId, propertyId: PropertyId): Either[String, Game] =
    updateImprovement(gameId, propertyId)(propertyService.sellHouse)

  def sellHotel(gameId: GameId, propertyId: PropertyId): Either[String, Game] =
    updateImprovement(gameId, propertyId)(propertyService.sellHotel)

  def mortgage(gameId: GameId, propertyId: PropertyId): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      player = game.currentPlayer
      property <- findProperty(game, propertyId)
      result <- propertyService.mortgageProperty(player, property)
      (updatedPlayer, updatedProperty) = result
      updatedGame = game
        .updatePlayer(updatedPlayer)
        .updateProperty(updatedProperty)
      _ <- propertyRepository.update(updatedProperty)
      _ <- gameRepository.update(updatedGame)
    } yield updatedGame

  def unmortgage(gameId: GameId, propertyId: PropertyId): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      player = game.currentPlayer
      property <- findProperty(game, propertyId)
      result <- propertyService.unmortgageProperty(player, property)
      (updatedPlayer, updatedProperty) = result
      updatedGame = game
        .updatePlayer(updatedPlayer)
        .updateProperty(updatedProperty)
      _ <- propertyRepository.update(updatedProperty)
      _ <- gameRepository.update(updatedGame)
    } yield updatedGame

  private def updateImprovement(
      gameId: GameId,
      propertyId: PropertyId
  )(
      action: (Player, Property, List[Property], Bank) => Either[String, (Player, Property, Bank)]
  ): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      player = game.currentPlayer
      property <- findProperty(game, propertyId)
      groupProperties = groupFor(game, property)
      result <- action(player, property, groupProperties, game.bank)
      (updatedPlayer, updatedProperty, updatedBank) = result
      updatedGame = game
        .updatePlayer(updatedPlayer)
        .updateProperty(updatedProperty)
        .copy(bank = updatedBank)
      _ <- propertyRepository.update(updatedProperty)
      _ <- gameRepository.update(updatedGame)
    } yield updatedGame

  private def findProperty(game: Game, propertyId: PropertyId): Either[String, Property] =
    game.board.squares.collectFirst {
      case Square.PropertySquare(property) if property.id == propertyId => property
    }.toRight("Property not found")

  private def groupFor(game: Game, property: Property): List[Property] =
    property.colorGroup match {
      case None => List(property)
      case Some(group) =>
        game.board.squares.collect {
          case Square.PropertySquare(p) if p.colorGroup.contains(group) => p
        }.toList
    }
}
