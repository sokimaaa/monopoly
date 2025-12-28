package com.sokima.monopoly
package domain

case class Property(
    id: PropertyId,
    name: String,
    position: Position,
    price: Money,
    rent: Money,
    propertyType: PropertyType,
    colorGroup: Option[ColorGroup] = None,
    rentTable: Option[StreetRentTable] = None,
    houseCost: Option[Money] = None,
    houses: Int = 0,
    hotel: Boolean = false,
    mortgaged: Boolean = false,
    ownerId: Option[PlayerId] = None
) {
  def isOwned: Boolean = ownerId.isDefined

  def isOwnedBy(playerId: PlayerId): Boolean = ownerId.contains(playerId)

  def assignOwner(playerId: PlayerId): Property = copy(ownerId = Some(playerId))

  def clearOwner: Property =
    copy(ownerId = None, houses = 0, hotel = false, mortgaged = false)

  def improvementLevel: Int = if (hotel) 5 else houses

  def hasImprovements: Boolean = hotel || houses > 0

  def mortgageValue: Money = Money(price.amount / 2)

  def isStreet: Boolean = propertyType == PropertyType.Street

  def withHouse: Property = copy(houses = houses + 1)

  def withoutHouse: Property = copy(houses = math.max(0, houses - 1))

  def withHotel: Property = copy(hotel = true, houses = 0)

  def withoutHotelToHouses: Property = copy(hotel = false, houses = 4)

  def clearImprovements: Property = copy(houses = 0, hotel = false)

  def mortgage: Property = copy(mortgaged = true)

  def unmortgage: Property = copy(mortgaged = false)
}
