package com.sokima.monopoly
package domain.service

import domain.*

class PropertyService {

  def purchaseProperty(
      player: Player,
      property: Property
  ): Either[String, (Player, Property, GameEvent.PropertyPurchased)] =
    if (property.isOwned) {
      Left(s"Property ${property.name} is already owned")
    } else if (!player.canAfford(property.price)) {
      Left(s"Insufficient funds. Need ${property.price.amount}, have ${player.balance.amount}")
    } else {
      val updatedPlayer   = player.acquireProperty(property.id, property.price)
      val updatedProperty = property.assignOwner(player.id)
      val event           = GameEvent.PropertyPurchased(player.id, property.id, property.price)

      Right((updatedPlayer, updatedProperty, event))
    }

  def calculateRent(game: Game, property: Property, roll: DiceRoll): Either[String, Money] =
    property.ownerId match {
      case None => Right(Money(0))
      case Some(ownerId) =>
        if (property.mortgaged) Right(Money(0))
        else
          property.propertyType match {
            case PropertyType.Street =>
              val table = property.rentTable.toRight("Missing rent table for street")
              table.map { rentTable =>
                val baseRent = rentTable.rentFor(property.improvementLevel)
                val monopolyBonus =
                  property.improvementLevel == 0 &&
                    property.colorGroup.exists(group => hasMonopoly(game, ownerId, group))
                if (monopolyBonus) Money(baseRent.amount * 2) else baseRent
              }
            case PropertyType.Railroad =>
              val count = countOwnedByType(game, ownerId, PropertyType.Railroad)
              Right(railroadRent(count))
            case PropertyType.Utility =>
              val count = countOwnedByType(game, ownerId, PropertyType.Utility)
              val multiplier = if (count >= 2) 10 else 4
              Right(Money(roll.total * multiplier))
          }
    }

  def hasMonopoly(game: Game, ownerId: PlayerId, group: ColorGroup): Boolean = {
    val groupProps = game.board.squares.collect {
      case Square.PropertySquare(property)
          if property.propertyType == PropertyType.Street && property.colorGroup.contains(group) =>
        property
    }
    groupProps.nonEmpty &&
    groupProps.forall(p => p.ownerId.contains(ownerId)) &&
    groupProps.forall(p => !p.mortgaged)
  }

  def buildHouse(
      player: Player,
      property: Property,
      groupProperties: List[Property],
      bank: Bank
  ): Either[String, (Player, Property, Bank)] =
    for {
      _ <- ensureStreet(property)
      _ <- ensureOwner(player, property)
      _ <- ensureBuildEligibility(player.id, groupProperties)
      _ <- Either.cond(!property.hotel && property.houses < 4, (), "Cannot build more houses here")
      _ <- Either.cond(bank.hasHouses(), (), "Bank has no houses available")
      _ <- Either.cond(canBuildEvenly(property, groupProperties), (), "Must build evenly")
      cost <- property.houseCost.toRight("Missing house cost")
      _ <- Either.cond(player.canAfford(cost), (), "Insufficient funds to build a house")
    } yield {
      val updatedPlayer = player.pay(cost)
      val updatedProperty = property.withHouse
      (updatedPlayer, updatedProperty, bank.takeHouse)
    }

  def buildHotel(
      player: Player,
      property: Property,
      groupProperties: List[Property],
      bank: Bank
  ): Either[String, (Player, Property, Bank)] =
    for {
      _ <- ensureStreet(property)
      _ <- ensureOwner(player, property)
      _ <- ensureBuildEligibility(player.id, groupProperties)
      _ <- Either.cond(property.houses == 4 && !property.hotel, (), "Need 4 houses to build a hotel")
      _ <- Either.cond(bank.hasHotels(), (), "Bank has no hotels available")
      _ <- Either.cond(
        groupProperties.forall(_.improvementLevel >= 4),
        (),
        "Hotels must be built evenly"
      )
      cost <- property.houseCost.toRight("Missing house cost")
      _ <- Either.cond(player.canAfford(cost), (), "Insufficient funds to build a hotel")
    } yield {
      val updatedPlayer = player.pay(cost)
      val updatedProperty = property.withHotel
      val updatedBank = bank.takeHotel.returnHouses(4)
      (updatedPlayer, updatedProperty, updatedBank)
    }

  def sellHouse(
      player: Player,
      property: Property,
      groupProperties: List[Property],
      bank: Bank
  ): Either[String, (Player, Property, Bank)] =
    for {
      _ <- ensureStreet(property)
      _ <- ensureOwner(player, property)
      _ <- Either.cond(property.houses > 0 && !property.hotel, (), "No houses to sell")
      _ <- Either.cond(canSellEvenly(property, groupProperties), (), "Must sell evenly")
      cost <- property.houseCost.toRight("Missing house cost")
    } yield {
      val proceeds = Money(cost.amount / 2)
      val updatedPlayer = player.receive(proceeds)
      val updatedProperty = property.withoutHouse
      (updatedPlayer, updatedProperty, bank.returnHouse)
    }

  def sellHotel(
      player: Player,
      property: Property,
      groupProperties: List[Property],
      bank: Bank
  ): Either[String, (Player, Property, Bank)] =
    for {
      _ <- ensureStreet(property)
      _ <- ensureOwner(player, property)
      _ <- Either.cond(property.hotel, (), "No hotel to sell")
      _ <- Either.cond(canSellEvenly(property, groupProperties), (), "Must sell evenly")
      _ <- Either.cond(bank.hasHouses(4), (), "Bank lacks houses to break hotel")
      cost <- property.houseCost.toRight("Missing house cost")
    } yield {
      val proceeds = Money(cost.amount / 2)
      val updatedPlayer = player.receive(proceeds)
      val updatedProperty = property.withoutHotelToHouses
      val updatedBank = bank.returnHotel.takeHouses(4)
      (updatedPlayer, updatedProperty, updatedBank)
    }

  def mortgageProperty(player: Player, property: Property): Either[String, (Player, Property)] =
    for {
      _ <- ensureOwner(player, property)
      _ <- Either.cond(!property.mortgaged, (), "Property already mortgaged")
      _ <- Either.cond(!property.hasImprovements, (), "Sell improvements before mortgaging")
    } yield {
      val updatedPlayer = player.receive(property.mortgageValue)
      val updatedProperty = property.mortgage
      (updatedPlayer, updatedProperty)
    }

  def unmortgageProperty(player: Player, property: Property): Either[String, (Player, Property)] =
    for {
      _ <- ensureOwner(player, property)
      _ <- Either.cond(property.mortgaged, (), "Property is not mortgaged")
      payoff = mortgagePayoff(property)
      _ <- Either.cond(player.canAfford(payoff), (), "Insufficient funds to unmortgage")
    } yield {
      val updatedPlayer = player.pay(payoff)
      val updatedProperty = property.unmortgage
      (updatedPlayer, updatedProperty)
    }

  def mortgagePayoff(property: Property): Money =
    Money((property.mortgageValue.amount * 11) / 10)

  def countImprovements(properties: List[Property]): (Int, Int) = {
    val houses = properties.map(_.houses).sum
    val hotels = properties.count(_.hotel)
    (houses, hotels)
  }

  private def ensureStreet(property: Property): Either[String, Unit] =
    Either.cond(property.propertyType == PropertyType.Street, (), "Not a street property")

  private def ensureOwner(player: Player, property: Property): Either[String, Unit] =
    Either.cond(property.ownerId.contains(player.id), (), "Player does not own this property")

  private def ensureBuildEligibility(
      ownerId: PlayerId,
      groupProperties: List[Property]
  ): Either[String, Unit] =
    groupProperties.headOption match {
      case None => Left("Property group not found")
      case Some(property) =>
        property.colorGroup match {
          case None => Left("Property group not found")
          case Some(group) =>
            val hasMonopoly = groupProperties.forall(p => p.ownerId.contains(ownerId)) &&
              groupProperties.forall(p => !p.mortgaged) &&
              groupProperties.forall(_.colorGroup.contains(group))
            Either.cond(hasMonopoly, (), "Monopoly required to build")
        }
    }

  private def canBuildEvenly(property: Property, groupProperties: List[Property]): Boolean = {
    val minLevel = groupProperties.map(_.improvementLevel).minOption.getOrElse(0)
    property.improvementLevel == minLevel
  }

  private def canSellEvenly(property: Property, groupProperties: List[Property]): Boolean = {
    val maxLevel = groupProperties.map(_.improvementLevel).maxOption.getOrElse(0)
    property.improvementLevel == maxLevel
  }

  private def countOwnedByType(game: Game, ownerId: PlayerId, propertyType: PropertyType): Int =
    game.board.squares.collect {
      case Square.PropertySquare(property)
          if property.propertyType == propertyType && property.ownerId.contains(ownerId) =>
        property
    }.size

  private def railroadRent(count: Int): Money =
    count match {
      case 1 => Money(25)
      case 2 => Money(50)
      case 3 => Money(100)
      case _ => Money(200)
    }
}
