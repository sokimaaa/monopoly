package com.sokima.monopoly
package domain

case class GameId(value: String) extends AnyVal

case class Game(
    id: GameId,
    board: Board,
    players: List[Player],
    currentPlayerIndex: Int,
    status: GameStatus = GameStatus.Running,
    turnState: TurnState = TurnState()
) {
  def currentPlayer: Player = players(currentPlayerIndex)

  def nextPlayerIndex: Int = (currentPlayerIndex + 1) % players.length

  def isFinished: Boolean = status == GameStatus.Finished

  def updatePlayer(player: Player): Game = {
    val updatedPlayers = players.map(p => if (p.id == player.id) player else p)
    copy(players = updatedPlayers)
  }

  def advanceTurn: Game = copy(currentPlayerIndex = nextPlayerIndex).resetTurnState

  def advanceTurnSkippingBankrupt: Game =
    nextActivePlayerIndex match {
      case Some(index) => copy(currentPlayerIndex = index).resetTurnState
      case None        => endGame
    }

  def recordDiceRoll(roll: DiceRoll): Game = {
    val updatedDoublesCount =
      if (roll.isDoubles) turnState.doublesCount + 1 else 0

    copy(
      turnState = turnState.copy(
        diceRolled = true,
        doublesCount = updatedDoublesCount
      )
    )
  }

  def resetTurnState: Game = copy(turnState = TurnState())

  def endGame: Game = copy(status = GameStatus.Finished)

  def activePlayers: List[Player] = players.filterNot(_.isBankrupt)

  def checkGameOver: Game =
    if (activePlayers.length <= 1) endGame else this

  private def nextActivePlayerIndex: Option[Int] = {
    val total      = players.length
    val candidates = (1 to total).map(offset => (currentPlayerIndex + offset) % total)
    candidates.find(index => !players(index).isBankrupt)
  }
}
