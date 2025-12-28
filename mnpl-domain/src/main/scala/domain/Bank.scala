package com.sokima.monopoly
package domain

case class Bank(
    housesAvailable: Int,
    hotelsAvailable: Int
) {
  def hasHouses(count: Int = 1): Boolean = housesAvailable >= count

  def hasHotels(count: Int = 1): Boolean = hotelsAvailable >= count

  def takeHouse: Bank = copy(housesAvailable = housesAvailable - 1)

  def returnHouse: Bank = copy(housesAvailable = housesAvailable + 1)

  def takeHotel: Bank = copy(hotelsAvailable = hotelsAvailable - 1)

  def returnHotel: Bank = copy(hotelsAvailable = hotelsAvailable + 1)

  def takeHouses(count: Int): Bank = copy(housesAvailable = housesAvailable - count)

  def returnHouses(count: Int): Bank = copy(housesAvailable = housesAvailable + count)

  def returnHotels(count: Int): Bank = copy(hotelsAvailable = hotelsAvailable + count)
}
