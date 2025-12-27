package com.sokima.monopoly
package domain.service

import domain.{Player, Property}

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

  def payRent(
      tenant: Player,
      landlord: Player,
      property: Property
  ): (Player, Player, GameEvent.RentPaid) = {

    val updatedTenant   = tenant.pay(property.rent)
    val updatedLandlord = landlord.receive(property.rent)
    val event           = GameEvent.RentPaid(tenant.id, landlord.id, property.rent, property.name)

    (updatedTenant, updatedLandlord, event)
  }
}
