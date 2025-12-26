package com.sokima.monopoly
package infrastructure.adapter.cli

import application.port.GameRepository
import application.usecase.*
import domain.{Game, GameId, Square}

class ConsoleGameController(
                             createGameUseCase: CreateGameUseCase,
                             rollDiceUseCase: RollDiceUseCase,
                             buyPropertyUseCase: BuyPropertyUseCase,
                             endTurnUseCase: EndTurnUseCase,
                             gameRepository: GameRepository
                           ) {

  def displayWelcome(): Unit = {
    println("=" * 50)
    println("     MONOPOLY - HEXAGONAL ARCHITECTURE")
    println("=" * 50)
    println()
  }

  def startNewGame(): Either[String, GameId] = {
    println("Enter number of players (2-4):")
    val numPlayers = scala.io.StdIn.readLine().toInt.max(2).min(4)

    val playerNames = (1 to numPlayers).map { i =>
      println(s"Enter name for Player $i:")
      scala.io.StdIn.readLine()
    }.toList

    createGameUseCase.execute(playerNames).map(_.id)
  }

  def displayGameState(game: Game): Unit = {
    val player = game.currentPlayer
    println("\n" + "-" * 50)
    println(s"Turn: ${player.name} (${player.id.value})")
    println(s"Position: ${player.position.value} | Balance: $$${player.balance.amount}")
    println(s"Properties: ${player.ownedProperties.size}")
    println("-" * 50)
  }

  def displaySquareInfo(game: Game): Unit = {
    game.board.getSquare(game.currentPlayer.position).foreach {
      case Square.PropertySquare(prop) =>
        println(s"\n📍 ${prop.name}")
        prop.ownerId match {
          case None =>
            println(s"   💰 Price: $$${prop.price.amount} | Rent: $$${prop.rent.amount}")
          case Some(ownerId) =>
            val owner = game.players.find(_.id == ownerId).get
            println(s"   👤 Owner: ${owner.name} | Rent: $$${prop.rent.amount}")
        }
      case Square.Go(_) => println("\n📍 GO")
      case Square.Jail(_) => println("\n📍 Jail (Just Visiting)")
      case Square.FreeParking(_) => println("\n📍 Free Parking")
      case Square.Tax(_, amt) => println(s"\n📍 Tax ($$${amt.amount})")
      case _ => println("\n📍 Special Square")
    }
  }

  def displayAllPlayers(game: Game): Unit = {
    println("\n=== PLAYERS ===")
    game.players.foreach { p =>
      val indicator = if (p.id == game.currentPlayer.id) ">>>" else "   "
      val status = if (p.isBankrupt) "[BANKRUPT]" else ""
      println(s"$indicator ${p.name}: $$${p.balance.amount} | Props: ${p.ownedProperties.size} $status")
    }
    println()
  }

  def promptAction(): String = {
    println("\n[r]oll | [b]uy | [e]nd turn | [s]tatus | [q]uit")
    print("> ")
    scala.io.StdIn.readLine().trim.toLowerCase
  }

  def gameLoop(gameId: GameId): Unit = {
    var running = true
    var turnActive = false

    while (running) {
      gameRepository.findById(gameId) match {
        case None =>
          println("Game not found!")
          running = false

        case Some(game) if game.isFinished =>
          val winner = game.activePlayers.headOption
          winner.foreach { w =>
            println("\n" + "=" * 50)
            println(s"🏆 ${w.name} WINS! 🏆")
            println(s"Final Balance: $$${w.balance.amount}")
            println("=" * 50)
          }
          running = false

        case Some(game) =>
          displayGameState(game)
          displaySquareInfo(game)

          promptAction() match {
            case "r" | "roll" if !turnActive =>
              rollDiceUseCase.execute(gameId) match {
                case Right((_, roll, _)) =>
                  println(s"\n🎲 Rolled: ${roll.die1} + ${roll.die2} = ${roll.total}")
                  turnActive = true
                case Left(error) =>
                  println(s"❌ $error")
              }

            case "r" | "roll" =>
              println("❌ Already rolled! End turn first.")

            case "b" | "buy" if turnActive =>
              buyPropertyUseCase.execute(gameId) match {
                case Right(_) =>
                  endTurnUseCase.execute(gameId)
                  turnActive = false
                case Left(error) =>
                  println(s"❌ $error")
              }

            case "b" | "buy" =>
              println("❌ Roll dice first!")

            case "e" | "end" if turnActive || game.currentPlayer.isBankrupt =>
              endTurnUseCase.execute(gameId)
              turnActive = false

            case "s" | "status" =>
              displayAllPlayers(game)

            case "q" | "quit" =>
              running = false

            case _ =>
              println("❌ Invalid command")
          }
      }
    }
  }
}
