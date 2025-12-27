package com.sokima.monopoly
package domain

case class Player(
    id: PlayerId,
    name: String,
    position: Position,
    balance: Money,
    ownedProperties: Set[PropertyId],
    bankrupt: Boolean = false,
    inJail: Boolean = false,
    jailTurns: Int = 0
) {
  def isBankrupt: Boolean = bankrupt

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

  def clearProperties: Player = copy(ownedProperties = Set.empty)

  def markBankrupt: Player = copy(bankrupt = true, balance = Money(0), inJail = false, jailTurns = 0)

  def sendToJail(jailPosition: Position): Player =
    copy(position = jailPosition, inJail = true, jailTurns = 0)

  def releaseFromJail: Player = copy(inJail = false, jailTurns = 0)

  def recordJailAttempt: Player = copy(jailTurns = jailTurns + 1)
}
