package com.sokima.monopoly
package domain

case class Player(
    id: PlayerId,
    name: String,
    position: Position,
    balance: Money,
    ownedProperties: Set[PropertyId],
    inJail: Boolean = false,
    jailTurns: Int = 0
) {
  def isBankrupt: Boolean = balance.amount < 0

  def canAfford(price: Money): Boolean = balance >= price

  def moveTo(newPosition: Position, passedGo: Boolean, salary: Money): Player = {
    val updatedBalance = if (passedGo) balance + salary else balance
    copy(position = newPosition, balance = updatedBalance)
  }

  def pay(amount: Money): Player = copy(balance = balance - amount)

  def receive(amount: Money): Player = copy(balance = balance + amount)

  def acquireProperty(propertyId: PropertyId, price: Money): Player =
    copy(
      balance = balance - price,
      ownedProperties = ownedProperties + propertyId
    )
}
