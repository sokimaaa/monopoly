package com.sokima.monopoly
package application.usecase

import domain.{GameId, PlayerId}

sealed trait GameQuery

object GameQuery {
  case class GetGameState(gameId: GameId) extends GameQuery

  case class GetPlayerStatus(gameId: GameId, playerId: PlayerId) extends GameQuery

  case class GetAvailableProperties(gameId: GameId) extends GameQuery
}
