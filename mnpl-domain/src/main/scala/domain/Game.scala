package com.sokima.monopoly
package domain

case class GameId(value: String) extends AnyVal

case class Game(
                 id: GameId,
                 board: Board,
                 players: List[Player],
                 currentPlayerIndex: Int,
                 isFinished: Boolean = false
               ) {
  def currentPlayer: Player = players(currentPlayerIndex)

  def nextPlayerIndex: Int = (currentPlayerIndex + 1) % players.length

  def updatePlayer(player: Player): Game = {
    val updatedPlayers = players.map(p => if (p.id == player.id) player else p)
    copy(players = updatedPlayers)
  }

  def advanceTurn: Game = copy(currentPlayerIndex = nextPlayerIndex)

  def endGame: Game = copy(isFinished = true)

  def activePlayers: List[Player] = players.filterNot(_.isBankrupt)

  def checkGameOver: Game =
    if (activePlayers.length <= 1) endGame else this
}
