package com.sokima.monopoly
package domain

sealed trait Card

object Card {
  case class MoveTo(position: Position, awardGo: Boolean = true) extends Card
  case class MoveBack(steps: Int) extends Card
  case class Gain(amount: Money) extends Card
  case class Pay(amount: Money) extends Card
  case object GoToJail extends Card
  case object GetOutOfJailFree extends Card
  case class Repairs(houseCost: Money, hotelCost: Money) extends Card
}
