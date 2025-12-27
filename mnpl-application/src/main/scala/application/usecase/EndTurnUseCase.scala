package com.sokima.monopoly
package application.usecase

import application.port.{EventPublisher, GameRepository}
import domain.service.GameEvent
import domain.{Game, GameId}

class EndTurnUseCase(
    gameRepository: GameRepository,
    eventPublisher: EventPublisher
) {

  def execute(gameId: GameId): Either[String, Game] =
    for {
      game <- gameRepository.findById(gameId).toRight("Game not found")
      nextGame = if (game.turnState.extraRoll) {
        game.allowExtraRoll
      } else {
        game
          .advanceTurnSkippingBankrupt
          .checkGameOver
      }

      _ <- gameRepository.update(nextGame)
    } yield {
      if (nextGame.isFinished && !game.turnState.extraRoll) {
        val winner = nextGame.activePlayers.headOption
        winner.foreach(w => eventPublisher.publish(GameEvent.GameFinished(w.id)))
      }
      nextGame
    }
}
