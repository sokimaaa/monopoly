package com.sokima.monopoly
package application.usecase

import domain.{GameId, PlayerId}

sealed trait GameCommand

object GameCommand {
  case class CreateGame(playerNames: List[String]) extends GameCommand

  case class RollDice(gameId: GameId) extends GameCommand

  case class BuyProperty(gameId: GameId, playerId: PlayerId) extends GameCommand

  case class EndTurn(gameId: GameId) extends GameCommand
}
