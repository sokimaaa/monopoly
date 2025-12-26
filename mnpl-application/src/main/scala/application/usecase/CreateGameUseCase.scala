package com.sokima.monopoly
package application.usecase

import application.port.{BoardFactory, GameRepository, PropertyRepository}
import domain.service.GameConfig
import domain.*

class CreateGameUseCase(
                         gameRepository: GameRepository,
                         propertyRepository: PropertyRepository,
                         boardFactory: BoardFactory,
                         config: GameConfig
                       ) {

  def execute(playerNames: List[String]): Either[String, Game] = {
    if (playerNames.length < 2 || playerNames.length > 4) {
      Left("Game requires 2-4 players")
    } else {
      val gameId = GameId(java.util.UUID.randomUUID().toString)
      val board = boardFactory.createBoard()

      val players = playerNames.zipWithIndex.map { case (name, idx) =>
        Player(
          id = PlayerId(s"player_$idx"),
          name = name,
          position = Position(0),
          balance = config.startingBalance,
          ownedProperties = Set.empty
        )
      }

      val game = Game(
        id = gameId,
        board = board,
        players = players,
        currentPlayerIndex = 0,
        rules = config.rules
      )

      gameRepository.save(game)
    }
  }
}
