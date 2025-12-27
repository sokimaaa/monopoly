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

  def updateProperty(property: Property): Game =
    copy(board = board.updateProperty(property))

  def markPlayerBankrupt(playerId: PlayerId): (Game, List[Property]) =
    players.find(_.id == playerId) match {
      case None => (this, Nil)
      case Some(player) =>
        val propertyIds = player.ownedProperties
        val (updatedBoard, releasedProperties) = board.releaseProperties(propertyIds)
        val updatedPlayer = player.clearProperties.markBankrupt
        val updatedPlayers = players.map(p => if (p.id == playerId) updatedPlayer else p)
        (copy(players = updatedPlayers, board = updatedBoard), releasedProperties)
    }

  def transferPropertiesToCreditor(
      debtorId: PlayerId,
      creditorId: PlayerId
  ): (Game, List[Property]) = {
    val debtorOpt   = players.find(_.id == debtorId)
    val creditorOpt = players.find(_.id == creditorId)
    (debtorOpt, creditorOpt) match {
      case (Some(debtor), Some(creditor)) =>
        val propertyIds = debtor.ownedProperties
        var transferred: List[Property] = Nil
        val updatedSquares = board.squares.map {
          case Square.PropertySquare(property) if propertyIds.contains(property.id) =>
            val updated = property.assignOwner(creditorId)
            transferred = updated :: transferred
            Square.PropertySquare(updated)
          case other => other
        }
        val updatedBoard = board.copy(squares = updatedSquares)
        val updatedDebtor = debtor.clearProperties.markBankrupt
        val updatedCreditor = creditor.copy(
          ownedProperties = creditor.ownedProperties ++ propertyIds
        )
        val updatedPlayers = players.map {
          case p if p.id == debtorId   => updatedDebtor
          case p if p.id == creditorId => updatedCreditor
          case p                       => p
        }
        (copy(players = updatedPlayers, board = updatedBoard), transferred.reverse)
      case _ =>
        (this, Nil)
    }
  }

  def advanceTurn: Game = copy(currentPlayerIndex = nextPlayerIndex).resetTurnState

  def advanceTurnSkippingBankrupt: Game =
    nextActivePlayerIndex match {
      case Some(index) => copy(currentPlayerIndex = index).resetTurnState
      case None        => endGame
    }

  def recordDiceRoll(roll: DiceRoll): Game =
    recordDiceRoll(roll, extraRoll = false)

  def recordDiceRoll(roll: DiceRoll, extraRoll: Boolean): Game = {
    val updatedDoublesCount =
      if (roll.isDoubles) turnState.doublesCount + 1 else 0

    copy(
      turnState = turnState.copy(
        diceRolled = true,
        doublesCount = updatedDoublesCount,
        extraRoll = extraRoll
      )
    )
  }

  def resetTurnState: Game = copy(turnState = TurnState())

  def endGame: Game = copy(status = GameStatus.Finished)

  def activePlayers: List[Player] = players.filterNot(_.isBankrupt)

  def checkGameOver: Game =
    if (activePlayers.length <= 1) endGame else this

  def allowExtraRoll: Game =
    copy(turnState = turnState.copy(diceRolled = false, extraRoll = false))

  private def nextActivePlayerIndex: Option[Int] = {
    val total      = players.length
    val candidates = (1 to total).map(offset => (currentPlayerIndex + offset) % total)
    candidates.find(index => !players(index).isBankrupt)
  }
}
