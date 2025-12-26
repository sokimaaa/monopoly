package com.sokima.monopoly
package domain.service

import domain.{Board, DiceRoll, Player}

class MovementService(config: GameConfig) {

  def movePlayer(player: Player, roll: DiceRoll, board: Board): (Player, GameEvent.PlayerMoved) = {
    val oldPosition = player.position
    val newPosition = oldPosition.advance(roll.total, board.size)
    val passedGo = oldPosition.passedGo(newPosition)

    val movedPlayer = player.moveTo(newPosition, passedGo, config.goSalary)
    val event = GameEvent.PlayerMoved(player.id, oldPosition, newPosition, passedGo)

    (movedPlayer, event)
  }
}