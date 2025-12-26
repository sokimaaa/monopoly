package com.sokima.monopoly
package infrastructure.adapter.event

import application.port.EventPublisher
import domain.service.GameEvent

class ConsoleEventPublisher extends EventPublisher {
  def publish(event: GameEvent): Unit = {
    event match {
      case GameEvent.PlayerMoved(_, _, to, passedGo) =>
        if (passedGo) println("💰 Passed GO! Collected salary")
        println(s"📍 Moved to position ${to.value}")

      case GameEvent.PropertyPurchased(_, propertyId, price) =>
        println(s"✅ Purchased property for $$${price.amount}")

      case GameEvent.RentPaid(_, _, amount, propertyName) =>
        println(s"💸 Paid $$${amount.amount} rent for $propertyName")

      case GameEvent.TaxPaid(_, amount) =>
        println(s"🏛️  Paid $$${amount.amount} in taxes")

      case GameEvent.GameFinished(winnerId) =>
        println(s"🏆 Game finished! Winner: ${winnerId.value}")

      case _ => ()
    }
  }
}