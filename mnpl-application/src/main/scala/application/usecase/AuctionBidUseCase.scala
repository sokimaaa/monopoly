package com.sokima.monopoly
package application.usecase

import application.port.{EventPublisher, GameRepository, PropertyRepository}
import domain.{Auction, Game, GameId, Money, PlayerId, Property, Square}
import domain.service.{Payee, PaymentService}

class AuctionBidUseCase(
    gameRepository: GameRepository,
    propertyRepository: PropertyRepository,
    paymentService: PaymentService,
    eventPublisher: EventPublisher
) {

  def placeBid(gameId: GameId, bidderId: PlayerId, amount: Money): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      auction <- game.auction.toRight("No active auction")
      bidder <- game.players.find(_.id == bidderId).toRight("Bidder not found")
      _ <- Either.cond(!bidder.isBankrupt, (), "Bidder is bankrupt")
      _ <- Either.cond(bidder.canAfford(amount), (), "Insufficient funds")
      updatedAuction <- auction.placeBid(bidderId, amount)
      updatedGame = game.copy(auction = Some(updatedAuction))
      resolved <- resolveIfFinished(updatedGame, updatedAuction)
      _ <- gameRepository.update(resolved)
    } yield resolved

  def fold(gameId: GameId, bidderId: PlayerId): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      auction <- game.auction.toRight("No active auction")
      updatedAuction = auction.fold(bidderId)
      updatedGame = game.copy(auction = Some(updatedAuction))
      resolved <- resolveIfFinished(updatedGame, updatedAuction)
      _ <- gameRepository.update(resolved)
    } yield resolved

  private def resolveIfFinished(game: Game, auction: Auction): Either[String, Game] =
    auction.winner match {
      case None => Right(game)
      case Some(winnerId) =>
        for {
          property <- findProperty(game, auction.propertyId)
          payment <- paymentService.pay(game, winnerId, Payee.Bank, auction.highestBid)
          updatedProperty = property.assignOwner(winnerId)
          updatedGame <- updateWinner(payment.game, winnerId, updatedProperty)
          _ <- propertyRepository.update(updatedProperty)
          _ <- updateProperties(payment.updatedProperties, propertyRepository)
        } yield {
          payment.events.foreach(eventPublisher.publish)
          updatedGame
        }
    }

  private def findProperty(game: Game, propertyId: domain.PropertyId): Either[String, Property] =
    game.board.squares.collectFirst {
      case Square.PropertySquare(property) if property.id == propertyId => property
    }.toRight("Property not found")

  private def updateWinner(
      game: Game,
      winnerId: PlayerId,
      property: Property
  ): Either[String, Game] =
    game.players.find(_.id == winnerId) match {
      case None => Left("Winner not found")
      case Some(player) =>
        val updatedPlayer = player.addProperty(property.id)
        Right(
          game
            .updatePlayer(updatedPlayer)
            .updateProperty(property)
            .copy(auction = None)
        )
    }

  private def updateProperties(
      properties: List[Property],
      repository: PropertyRepository
  ): Either[String, Unit] =
    properties.foldLeft(Right(()): Either[String, Unit]) { (acc, property) =>
      acc.flatMap(_ => repository.update(property).map(_ => ()))
    }
}
