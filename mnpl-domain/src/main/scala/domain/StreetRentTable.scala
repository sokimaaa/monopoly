package com.sokima.monopoly
package domain

case class StreetRentTable(
    base: Money,
    oneHouse: Money,
    twoHouses: Money,
    threeHouses: Money,
    fourHouses: Money,
    hotel: Money
) {
  def rentFor(level: Int): Money =
    level match {
      case 0 => base
      case 1 => oneHouse
      case 2 => twoHouses
      case 3 => threeHouses
      case 4 => fourHouses
      case _ => hotel
    }
}
