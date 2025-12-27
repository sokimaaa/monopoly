package com.sokima.monopoly
package domain

case class Property(
    id: PropertyId,
    name: String,
    position: Position,
    price: Money,
    rent: Money,
    propertyType: PropertyType,
    ownerId: Option[PlayerId] = None
) {
  def isOwned: Boolean = ownerId.isDefined

  def isOwnedBy(playerId: PlayerId): Boolean = ownerId.contains(playerId)

  def assignOwner(playerId: PlayerId): Property = copy(ownerId = Some(playerId))

  def clearOwner: Property = copy(ownerId = None)
}
