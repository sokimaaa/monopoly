package com.sokima.monopoly
package domain.service

import domain.{Bank, Game, Money, Player, PlayerId, Property, Square}

sealed trait Payee

object Payee {
  case object Bank extends Payee
  case class Player(playerId: PlayerId) extends Payee
}

case class PaymentResult(
    game: Game,
    events: List[GameEvent],
    releasedProperties: List[Property],
    updatedProperties: List[Property]
)

class PaymentService(propertyService: PropertyService) {

  def pay(
      game: Game,
      fromPlayerId: PlayerId,
      to: Payee,
      amount: Money
  ): Either[String, PaymentResult] =
    game.players.find(_.id == fromPlayerId) match {
      case None => Left("Payer not found")
      case Some(payer) =>
        val liquidation =
          if (payer.canAfford(amount)) LiquidationResult(game, payer, Nil)
          else attemptLiquidation(game, payer, amount)
        val gameAfterLiquidation = liquidation.game.updatePlayer(liquidation.player)

        to match {
          case Payee.Bank =>
            if (liquidation.player.canAfford(amount)) {
              val updatedPayer = liquidation.player.pay(amount)
              val updatedGame  = gameAfterLiquidation.updatePlayer(updatedPayer)
              Right(
                PaymentResult(
                  updatedGame,
                  Nil,
                  Nil,
                  liquidation.updatedProperties
                )
              )
            } else {
              val (bankruptGame, releasedProperties) =
                gameAfterLiquidation.markPlayerBankrupt(fromPlayerId)
              Right(
                PaymentResult(
                  bankruptGame,
                  List(GameEvent.PlayerBankrupt(fromPlayerId)),
                  releasedProperties,
                  liquidation.updatedProperties ++ releasedProperties
                )
              )
            }

          case Payee.Player(payeeId) =>
            gameAfterLiquidation.players.find(_.id == payeeId) match {
              case None => Left("Payee not found")
              case Some(payee) =>
                if (liquidation.player.canAfford(amount)) {
                  val updatedPayer  = liquidation.player.pay(amount)
                  val updatedPayee  = payee.receive(amount)
                  val updatedGame   = gameAfterLiquidation
                    .updatePlayer(updatedPayer)
                    .updatePlayer(updatedPayee)
                  Right(
                    PaymentResult(
                      updatedGame,
                      Nil,
                      Nil,
                      liquidation.updatedProperties
                    )
                  )
                } else {
                  val (bankruptGame, transferredProperties) =
                    gameAfterLiquidation.transferPropertiesToCreditor(fromPlayerId, payeeId)
                  Right(
                    PaymentResult(
                      bankruptGame,
                      List(GameEvent.PlayerBankrupt(fromPlayerId)),
                      transferredProperties,
                      liquidation.updatedProperties ++ transferredProperties
                    )
                  )
                }
            }
        }
    }

  private case class LiquidationResult(
      game: Game,
      player: Player,
      updatedProperties: List[Property]
  )

  private def attemptLiquidation(
      game: Game,
      player: Player,
      amount: Money
  ): LiquidationResult = {
    val afterSales = liquidateImprovements(game, player, amount, Nil)
    val afterMortgages =
      if (afterSales.player.canAfford(amount)) afterSales
      else liquidateMortgages(afterSales.game, afterSales.player, amount, afterSales.updatedProperties)
    afterMortgages
  }

  private def liquidateImprovements(
      game: Game,
      player: Player,
      amount: Money,
      updatedProperties: List[Property]
  ): LiquidationResult =
    if (player.canAfford(amount)) {
      LiquidationResult(game, player, updatedProperties)
    } else {
      sellableProperties(game, player, game.bank) match {
        case None => LiquidationResult(game, player, updatedProperties)
        case Some(property) =>
          val groupProperties = groupFor(game, property)
          val saleResult =
            if (property.hotel)
              propertyService.sellHotel(player, property, groupProperties, game.bank)
            else
              propertyService.sellHouse(player, property, groupProperties, game.bank)
          saleResult match {
            case Left(_) =>
              LiquidationResult(game, player, updatedProperties)
            case Right((updatedPlayer, updatedProperty, updatedBank)) =>
              val updatedGame = game
                .updatePlayer(updatedPlayer)
                .updateProperty(updatedProperty)
                .copy(bank = updatedBank)
              liquidateImprovements(
                updatedGame,
                updatedPlayer,
                amount,
                updatedProperties :+ updatedProperty
              )
          }
      }
    }

  private def liquidateMortgages(
      game: Game,
      player: Player,
      amount: Money,
      updatedProperties: List[Property]
  ): LiquidationResult =
    if (player.canAfford(amount)) {
      LiquidationResult(game, player, updatedProperties)
    } else {
      mortgageableProperties(game, player) match {
        case None => LiquidationResult(game, player, updatedProperties)
        case Some(property) =>
          propertyService.mortgageProperty(player, property) match {
            case Left(_) => LiquidationResult(game, player, updatedProperties)
            case Right((updatedPlayer, updatedProperty)) =>
              val updatedGame = game
                .updatePlayer(updatedPlayer)
                .updateProperty(updatedProperty)
              liquidateMortgages(
                updatedGame,
                updatedPlayer,
                amount,
                updatedProperties :+ updatedProperty
              )
          }
      }
    }

  private def sellableProperties(game: Game, player: Player, bank: Bank): Option[Property] = {
    val properties = ownedProperties(game, player.id)
      .filter(_.isStreet)
      .filter(_.improvementLevel > 0)
      .sortBy(p => (-p.improvementLevel, p.position.value))

    properties.collectFirst {
      case property if canSellProperty(game, property, bank) => property
    }
  }

  private def canSellProperty(game: Game, property: Property, bank: Bank): Boolean = {
    val groupProps = groupFor(game, property)
    val maxLevel = groupProps.map(_.improvementLevel).maxOption.getOrElse(0)
    val evenAllowed = property.improvementLevel == maxLevel
    val hotelAllowed = !property.hotel || bank.hasHouses(4)
    evenAllowed && hotelAllowed
  }

  private def mortgageableProperties(game: Game, player: Player): Option[Property] =
    ownedProperties(game, player.id)
      .filterNot(_.mortgaged)
      .filterNot(_.hasImprovements)
      .sortBy(_.position.value)
      .headOption

  private def ownedProperties(game: Game, ownerId: PlayerId): List[Property] =
    game.board.squares.collect {
      case Square.PropertySquare(property) if property.ownerId.contains(ownerId) => property
    }.toList

  private def groupFor(game: Game, property: Property): List[Property] =
    property.colorGroup match {
      case None => List(property)
      case Some(group) =>
        game.board.squares.collect {
          case Square.PropertySquare(p) if p.colorGroup.contains(group) => p
        }.toList
    }
}
