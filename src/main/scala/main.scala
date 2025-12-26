package com.sokima.monopoly

// Domain Model - Core entities

case class Player(
                   id: String,
                   name: String,
                   position: Int,
                   balance: Int,
                   properties: Set[String],
                   inJail: Boolean = false,
                   jailTurns: Int = 0
                 )

sealed trait PropertyType
object PropertyType {
  case object Street extends PropertyType
  case object Railroad extends PropertyType
  case object Utility extends PropertyType
}

case class Property(
                     name: String,
                     position: Int,
                     price: Int,
                     rent: Int,
                     propertyType: PropertyType,
                     owner: Option[String] = None
                   )

sealed trait Square
object Square {
  case class PropertySquare(property: Property) extends Square
  case object Go extends Square
  case object Jail extends Square
  case object FreeParking extends Square
  case object GoToJail extends Square
  case class Tax(amount: Int) extends Square
  case class Chance(description: String) extends Square
  case class CommunityChest(description: String) extends Square
}

case class Board(squares: Vector[Square]) {
  def getSquare(position: Int): Square = squares(position % squares.length)
}

case class GameState(
                      board: Board,
                      players: List[Player],
                      currentPlayerIndex: Int,
                      properties: Map[String, Property],
                      gameOver: Boolean = false
                    ) {
  def currentPlayer: Player = players(currentPlayerIndex)

  def nextPlayer: GameState = {
    val nextIndex = (currentPlayerIndex + 1) % players.length
    copy(currentPlayerIndex = nextIndex)
  }

  def updatePlayer(player: Player): GameState = {
    val updatedPlayers = players.map(p => if (p.id == player.id) player else p)
    copy(players = updatedPlayers)
  }

  def updateProperty(property: Property): GameState = {
    copy(properties = properties + (property.name -> property))
  }
}

// Game Actions
sealed trait GameAction
object GameAction {
  case object RollDice extends GameAction
  case object BuyProperty extends GameAction
  case object EndTurn extends GameAction
  case object PayRent extends GameAction
  case object ViewStatus extends GameAction
  case object Quit extends GameAction
}

// Game Engine - Business Logic
object GameEngine {
  val SALARY_AMOUNT = 200
  val STARTING_BALANCE = 1500

  def rollDice(): (Int, Int) = {
    val d1 = scala.util.Random.nextInt(6) + 1
    val d2 = scala.util.Random.nextInt(6) + 1
    (d1, d2)
  }

  def movePlayer(player: Player, steps: Int, boardSize: Int): (Player, Boolean) = {
    val oldPos = player.position
    val newPos = (oldPos + steps) % boardSize
    val passedGo = newPos < oldPos
    val updatedBalance = if (passedGo) player.balance + SALARY_AMOUNT else player.balance

    (player.copy(position = newPos, balance = updatedBalance), passedGo)
  }

  def handleSquare(state: GameState, player: Player): (GameState, String) = {
    val square = state.board.getSquare(player.position)

    square match {
      case Square.PropertySquare(prop) =>
        state.properties.get(prop.name) match {
          case Some(property) if property.owner.isEmpty =>
            (state, s"${prop.name} is available for ${prop.price}. Buy? (y/n)")
          case Some(property) if property.owner.contains(player.id) =>
            (state, s"You own ${prop.name}.")
          case Some(property) =>
            val rent = property.rent
            val updatedPlayer = player.copy(balance = player.balance - rent)
            val owner = state.players.find(_.id == property.owner.get).get
            val updatedOwner = owner.copy(balance = owner.balance + rent)

            val newState = state
              .updatePlayer(updatedPlayer)
              .updatePlayer(updatedOwner)

            (newState, s"Paid ${rent} rent to ${owner.name} for ${prop.name}")
          case None => (state, "Error: Property not found")
        }

      case Square.Go =>
        (state, "Welcome to GO! Collect $200")

      case Square.FreeParking =>
        (state, "Free Parking - Relax!")

      case Square.GoToJail =>
        val jailedPlayer = player.copy(position = 10, inJail = true, jailTurns = 0)
        (state.updatePlayer(jailedPlayer), "Go to Jail! Do not pass GO.")

      case Square.Tax(amount) =>
        val updatedPlayer = player.copy(balance = player.balance - amount)
        (state.updatePlayer(updatedPlayer), s"Paid ${amount} in taxes")

      case _ =>
        (state, "Nothing happens here.")
    }
  }

  def buyProperty(state: GameState, player: Player): (GameState, String) = {
    val square = state.board.getSquare(player.position)

    square match {
      case Square.PropertySquare(prop) =>
        state.properties.get(prop.name) match {
          case Some(property) if property.owner.isEmpty && player.balance >= property.price =>
            val updatedPlayer = player.copy(
              balance = player.balance - property.price,
              properties = player.properties + property.name
            )
            val updatedProperty = property.copy(owner = Some(player.id))

            val newState = state
              .updatePlayer(updatedPlayer)
              .updateProperty(updatedProperty)

            (newState, s"Bought ${property.name} for ${property.price}")

          case Some(property) if property.owner.isEmpty =>
            (state, s"Insufficient funds. Need ${property.price}, have ${player.balance}")

          case _ =>
            (state, "Property not available for purchase")
        }

      case _ =>
        (state, "No property to buy here")
    }
  }

  def checkBankruptcy(state: GameState): GameState = {
    val activePlayers = state.players.filter(_.balance >= 0)
    if (activePlayers.length == 1) {
      state.copy(gameOver = true)
    } else {
      state
    }
  }
}

// Board Setup
object BoardSetup {
  def createClassicBoard(): Board = {
    val squares = Vector(
      Square.Go,
      Square.PropertySquare(Property("Mediterranean Avenue", 1, 60, 2, PropertyType.Street)),
      Square.CommunityChest("Community Chest"),
      Square.PropertySquare(Property("Baltic Avenue", 3, 60, 4, PropertyType.Street)),
      Square.Tax(200),
      Square.PropertySquare(Property("Reading Railroad", 5, 200, 25, PropertyType.Railroad)),
      Square.PropertySquare(Property("Oriental Avenue", 6, 100, 6, PropertyType.Street)),
      Square.Chance("Chance"),
      Square.PropertySquare(Property("Vermont Avenue", 8, 100, 6, PropertyType.Street)),
      Square.PropertySquare(Property("Connecticut Avenue", 9, 120, 8, PropertyType.Street)),
      Square.Jail,
      Square.PropertySquare(Property("St. Charles Place", 11, 140, 10, PropertyType.Street)),
      Square.PropertySquare(Property("Electric Company", 12, 150, 15, PropertyType.Utility)),
      Square.PropertySquare(Property("States Avenue", 13, 140, 10, PropertyType.Street)),
      Square.PropertySquare(Property("Virginia Avenue", 14, 160, 12, PropertyType.Street)),
      Square.PropertySquare(Property("Pennsylvania Railroad", 15, 200, 25, PropertyType.Railroad)),
      Square.PropertySquare(Property("St. James Place", 16, 180, 14, PropertyType.Street)),
      Square.CommunityChest("Community Chest"),
      Square.PropertySquare(Property("Tennessee Avenue", 18, 180, 14, PropertyType.Street)),
      Square.PropertySquare(Property("New York Avenue", 19, 200, 16, PropertyType.Street)),
      Square.FreeParking,
      Square.PropertySquare(Property("Kentucky Avenue", 21, 220, 18, PropertyType.Street)),
      Square.Chance("Chance"),
      Square.PropertySquare(Property("Indiana Avenue", 23, 220, 18, PropertyType.Street)),
      Square.PropertySquare(Property("Illinois Avenue", 24, 240, 20, PropertyType.Street)),
      Square.PropertySquare(Property("B&O Railroad", 25, 200, 25, PropertyType.Railroad)),
      Square.PropertySquare(Property("Atlantic Avenue", 26, 260, 22, PropertyType.Street)),
      Square.PropertySquare(Property("Ventnor Avenue", 27, 260, 22, PropertyType.Street)),
      Square.PropertySquare(Property("Water Works", 28, 150, 15, PropertyType.Utility)),
      Square.PropertySquare(Property("Marvin Gardens", 29, 280, 24, PropertyType.Street)),
      Square.GoToJail,
      Square.PropertySquare(Property("Pacific Avenue", 31, 300, 26, PropertyType.Street)),
      Square.PropertySquare(Property("North Carolina Avenue", 32, 300, 26, PropertyType.Street)),
      Square.CommunityChest("Community Chest"),
      Square.PropertySquare(Property("Pennsylvania Avenue", 34, 320, 28, PropertyType.Street)),
      Square.PropertySquare(Property("Short Line", 35, 200, 25, PropertyType.Railroad)),
      Square.Chance("Chance"),
      Square.PropertySquare(Property("Park Place", 37, 350, 35, PropertyType.Street)),
      Square.Tax(100),
      Square.PropertySquare(Property("Boardwalk", 39, 400, 50, PropertyType.Street))
    )

    Board(squares)
  }

  def extractProperties(board: Board): Map[String, Property] = {
    board.squares.collect {
      case Square.PropertySquare(prop) => prop.name -> prop
    }.toMap
  }

  def initializeGame(playerNames: List[String]): GameState = {
    val board = createClassicBoard()
    val players = playerNames.zipWithIndex.map { case (name, idx) =>
      Player(
        id = s"player_$idx",
        name = name,
        position = 0,
        balance = GameEngine.STARTING_BALANCE,
        properties = Set.empty
      )
    }

    GameState(
      board = board,
      players = players,
      currentPlayerIndex = 0,
      properties = extractProperties(board)
    )
  }
}

// Console UI
object ConsoleUI {
  def displayWelcome(): Unit = {
    println("=" * 50)
    println("          MONOPOLY - CONSOLE EDITION")
    println("=" * 50)
    println()
  }

  def displayGameState(state: GameState): Unit = {
    val player = state.currentPlayer
    println("\n" + "-" * 50)
    println(s"Turn: ${player.name}")
    println(s"Position: ${player.position} - Balance: $$${player.balance}")
    println(s"Properties owned: ${player.properties.size}")
    if (player.properties.nonEmpty) {
      println(s"  ${player.properties.mkString(", ")}")
    }
    println("-" * 50)
  }

  def displayAllPlayers(state: GameState): Unit = {
    println("\n=== ALL PLAYERS ===")
    state.players.foreach { p =>
      val indicator = if (p.id == state.currentPlayer.id) ">>>" else "   "
      println(s"$indicator ${p.name}: $$${p.balance} | Properties: ${p.properties.size} | Position: ${p.position}")
    }
    println()
  }

  def displaySquareInfo(state: GameState, player: Player): Unit = {
    val square = state.board.getSquare(player.position)
    square match {
      case Square.PropertySquare(prop) =>
        val property = state.properties(prop.name)
        property.owner match {
          case None =>
            println(s"\n📍 ${prop.name}")
            println(s"   Price: $$${prop.price} | Rent: $$${prop.rent}")
            println(s"   Type: ${prop.propertyType}")
          case Some(ownerId) =>
            val owner = state.players.find(_.id == ownerId).get
            println(s"\n📍 ${prop.name}")
            println(s"   Owner: ${owner.name} | Rent: $$${prop.rent}")
        }
      case Square.Go => println("\n📍 GO - Collect $200")
      case Square.Jail => println("\n📍 Jail (Just Visiting)")
      case Square.FreeParking => println("\n📍 Free Parking")
      case Square.GoToJail => println("\n📍 Go to Jail")
      case Square.Tax(amt) => println(s"\n📍 Tax - Pay $$$amt")
      case Square.Chance(_) => println("\n📍 Chance")
      case Square.CommunityChest(_) => println("\n📍 Community Chest")
    }
  }

  def promptAction(): String = {
    println("\nActions: [r]oll | [b]uy | [s]tatus | [q]uit")
    print("> ")
    scala.io.StdIn.readLine().trim.toLowerCase
  }

  def displayMessage(msg: String): Unit = {
    println(s"\n💬 $msg")
  }

  def displayWinner(winner: Player): Unit = {
    println("\n" + "=" * 50)
    println("              GAME OVER!")
    println("=" * 50)
    println(s"\n🏆 ${winner.name} WINS! 🏆")
    println(s"   Final Balance: $$${winner.balance}")
    println(s"   Properties: ${winner.properties.size}")
    println()
  }
}

// Main Game Loop
object MonopolyGame extends App {
  ConsoleUI.displayWelcome()

  println("Enter number of players (2-4):")
  val numPlayers = scala.io.StdIn.readLine().toInt.max(2).min(4)

  val playerNames = (1 to numPlayers).map { i =>
    println(s"Enter name for Player $i:")
    scala.io.StdIn.readLine()
  }.toList

  var gameState = BoardSetup.initializeGame(playerNames)
  var running = true
  var turnActive = false

  while (running && !gameState.gameOver) {
    ConsoleUI.displayGameState(gameState)
    ConsoleUI.displaySquareInfo(gameState, gameState.currentPlayer)

    val action = ConsoleUI.promptAction()

    action match {
      case "r" | "roll" =>
        if (!turnActive) {
          val (d1, d2) = GameEngine.rollDice()
          ConsoleUI.displayMessage(s"Rolled: $d1 + $d2 = ${d1 + d2}")

          val (movedPlayer, passedGo) = GameEngine.movePlayer(
            gameState.currentPlayer,
            d1 + d2,
            gameState.board.squares.length
          )

          if (passedGo) {
            ConsoleUI.displayMessage(s"Passed GO! Collected $$${GameEngine.SALARY_AMOUNT}")
          }

          gameState = gameState.updatePlayer(movedPlayer)

          val (newState, message) = GameEngine.handleSquare(gameState, movedPlayer)
          gameState = newState
          ConsoleUI.displayMessage(message)

          turnActive = true
        } else {
          ConsoleUI.displayMessage("Already rolled this turn! End turn first.")
        }

      case "b" | "buy" =>
        if (turnActive) {
          val (newState, message) = GameEngine.buyProperty(gameState, gameState.currentPlayer)
          gameState = newState
          ConsoleUI.displayMessage(message)

          gameState = gameState.nextPlayer
          turnActive = false
        } else {
          ConsoleUI.displayMessage("Roll dice first!")
        }

      case "s" | "status" =>
        ConsoleUI.displayAllPlayers(gameState)

      case "q" | "quit" =>
        running = false
        println("\nThanks for playing!")

      case _ =>
        ConsoleUI.displayMessage("Invalid action")
    }

    gameState = GameEngine.checkBankruptcy(gameState)
  }

  if (gameState.gameOver) {
    val winner = gameState.players.maxBy(_.balance)
    ConsoleUI.displayWinner(winner)
  }
}
