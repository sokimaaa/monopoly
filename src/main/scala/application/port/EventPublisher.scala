package com.sokima.monopoly
package application.port

import domain.service.GameEvent

trait EventPublisher {
  def publish(event: GameEvent): Unit
}
