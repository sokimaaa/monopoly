package com.sokima.monopoly
package domain

sealed trait Square {
  def position: Position
}

object Square {
  case class PropertySquare(property: Property) extends Square {
    def position: Position = property.position
  }

  case class Go(position: Position) extends Square

  case class Jail(position: Position) extends Square

  case class FreeParking(position: Position) extends Square

  case class GoToJail(position: Position) extends Square

  case class Tax(position: Position, amount: Money) extends Square

  case class PercentTax(position: Position, percent: Int) extends Square

  case class Chance(position: Position) extends Square

  case class CommunityChest(position: Position) extends Square
}
