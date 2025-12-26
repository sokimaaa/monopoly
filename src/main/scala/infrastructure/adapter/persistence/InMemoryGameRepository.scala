package com.sokima.monopoly
package infrastructure.adapter.persistence

import application.port.GameRepository
import domain.{Game, GameId}

import scala.collection.mutable

class InMemoryGameRepository extends GameRepository {
  private val storage = mutable.Map[GameId, Game]()

  def save(game: Game): Either[String, Game] = {
    storage.put(game.id, game)
    Right(game)
  }

  def findById(gameId: GameId): Option[Game] = storage.get(gameId)

  def update(game: Game): Either[String, Game] = {
    if (storage.contains(game.id)) {
      storage.update(game.id, game)
      Right(game)
    } else {
      Left("Game not found")
    }
  }

  def delete(gameId: GameId): Unit = storage.remove(gameId)
}
