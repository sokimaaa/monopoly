package com.sokima.monopoly
package domain.service

import domain.{Money, PlayerId, Position, PropertyId}

sealed trait GameEvent

object GameEvent {
  case class PlayerMoved(playerId: PlayerId, from: Position, to: Position, passedGo: Boolean)
      extends GameEvent

  case class PropertyPurchased(playerId: PlayerId, propertyId: PropertyId, price: Money)
      extends GameEvent

  case class PropertyAvailable(playerId: PlayerId, propertyId: PropertyId, price: Money)
      extends GameEvent

  case class RentPaid(
      fromPlayerId: PlayerId,
      toPlayerId: PlayerId,
      amount: Money,
      propertyName: String
  ) extends GameEvent

  case class TaxPaid(playerId: PlayerId, amount: Money) extends GameEvent

  case class PlayerBankrupt(playerId: PlayerId) extends GameEvent

  case class GameFinished(winnerId: PlayerId) extends GameEvent

  case class PlayerPassedGo(playerId: PlayerId, salary: Money) extends GameEvent
}
