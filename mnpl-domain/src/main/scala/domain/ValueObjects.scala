package com.sokima.monopoly
package domain

case class Money(amount: Int) extends AnyVal {
  def +(other: Money): Money = Money(amount + other.amount)

  def -(other: Money): Money = Money(amount - other.amount)

  def >=(other: Money): Boolean = amount >= other.amount

  def <(other: Money): Boolean = amount < other.amount
}

case class PlayerId(value: String) extends AnyVal

case class Position(value: Int) extends AnyVal {
  def advance(steps: Int, boardSize: Int): Position =
    Position((value + steps) % boardSize)

  def passedGo(newPosition: Position): Boolean =
    newPosition.value < value
}

case class PropertyId(value: String) extends AnyVal

case class DiceRoll(die1: Int, die2: Int) {
  def total: Int         = die1 + die2
  def isDoubles: Boolean = die1 == die2
}
