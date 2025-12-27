package com.sokima.monopoly
package domain.service

import domain.{Board, DiceRoll, Player, Position}

class MovementService(config: GameConfig) {

  def movePlayer(player: Player, roll: DiceRoll, board: Board): (Player, GameEvent.PlayerMoved) = {
    val oldPosition = player.position
    val newPosition = oldPosition.advance(roll.total, board.size)
    val passedGo    = oldPosition.passedGo(newPosition)

    val movedPlayer = player.moveTo(newPosition, passedGo, config.goSalary)
    val event       = GameEvent.PlayerMoved(player.id, oldPosition, newPosition, passedGo)

    (movedPlayer, event)
  }

  def movePlayerTo(
      player: Player,
      target: Position,
      board: Board,
      awardGo: Boolean
  ): (Player, GameEvent.PlayerMoved) = {
    val oldPosition = player.position
    val newPosition = Position(target.value % board.size)
    val passedGo    = awardGo && oldPosition.passedGo(newPosition)

    val movedPlayer = player.moveTo(newPosition, passedGo, config.goSalary)
    val event       = GameEvent.PlayerMoved(player.id, oldPosition, newPosition, passedGo)

    (movedPlayer, event)
  }

  def movePlayerBack(
      player: Player,
      steps: Int,
      board: Board
  ): (Player, GameEvent.PlayerMoved) = {
    val oldPosition = player.position
    val newPosition = Position((oldPosition.value - steps + board.size) % board.size)
    val movedPlayer = player.moveTo(newPosition, passedGo = false, config.goSalary)
    val event       = GameEvent.PlayerMoved(player.id, oldPosition, newPosition, passedGo = false)

    (movedPlayer, event)
  }
}
