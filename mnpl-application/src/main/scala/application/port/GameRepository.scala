package com.sokima.monopoly
package application.port

import domain.{Game, GameId}

trait GameRepository {
  def save(game: Game): Either[String, Game]

  def findById(gameId: GameId): Option[Game]

  def update(game: Game): Either[String, Game]

  def delete(gameId: GameId): Unit
}
