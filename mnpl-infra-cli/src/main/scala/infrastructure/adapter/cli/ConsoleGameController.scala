package com.sokima.monopoly
package infrastructure.adapter.cli

import application.port.GameRepository
import application.usecase.*
import domain.{Game, GameId, Money, PlayerId, Square}

class ConsoleGameController(
    createGameUseCase: CreateGameUseCase,
    rollDiceUseCase: RollDiceUseCase,
    buyPropertyUseCase: BuyPropertyUseCase,
    declinePropertyUseCase: DeclinePropertyUseCase,
    auctionBidUseCase: AuctionBidUseCase,
    payJailFineUseCase: PayJailFineUseCase,
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
    game.auction.foreach { auction =>
      val propertyName = game.board.squares.collectFirst {
        case Square.PropertySquare(prop) if prop.id == auction.propertyId => prop.name
      }.getOrElse("Unknown Property")
      val highestBidder = auction.highestBidderId
        .flatMap(id => game.players.find(_.id == id).map(_.name))
        .getOrElse("None")
      println(
        s"Auction: $propertyName | Highest: $$${auction.highestBid.amount} by $highestBidder"
      )
    }
    println("-" * 50)
  }

  def displaySquareInfo(game: Game): Unit =
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
      case Square.Go(_)          => println("\n📍 GO")
      case Square.Jail(_)        => println("\n📍 Jail (Just Visiting)")
      case Square.FreeParking(_) => println("\n📍 Free Parking")
      case Square.Tax(_, amt)    => println(s"\n📍 Tax ($$${amt.amount})")
      case _                     => println("\n📍 Special Square")
    }

  def displayAllPlayers(game: Game): Unit = {
    println("\n=== PLAYERS ===")
    game.players.foreach { p =>
      val indicator = if (p.id == game.currentPlayer.id) ">>>" else "   "
      val status    = if (p.isBankrupt) "[BANKRUPT]" else ""
      println(
        s"$indicator ${p.name}: $$${p.balance.amount} | Props: ${p.ownedProperties.size} $status"
      )
    }
    println()
  }

  def promptAction(game: Game, turnActive: Boolean): String = {
    if (game.auction.isDefined) {
      println("\nAuction in progress: [a]bid | [f]old | [s]tatus | [q]uit")
    } else {
      val jailOption = if (game.currentPlayer.inJail && !turnActive) " | [p]ay jail fine" else ""
      println(s"\n[r]oll | [b]uy | [d]ecline | [e]nd turn | [s]tatus$jailOption | [q]uit")
    }
    print("> ")
    scala.io.StdIn.readLine().trim.toLowerCase
  }

  def gameLoop(gameId: GameId): Unit = {
    var running    = true
    var turnActive = false

    while (running)
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

          promptAction(game, turnActive) match {
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

            case "d" | "decline" if turnActive =>
              declinePropertyUseCase.execute(gameId) match {
                case Right(_) =>
                  println("🔨 Property declined. Auction started.")
                case Left(error) =>
                  println(s"❌ $error")
              }

            case "d" | "decline" =>
              println("❌ Roll dice first!")

            case "a" | "bid" if game.auction.isDefined =>
              val bidderId = promptBidderId(game)
              val amount   = Money(readInt("Enter bid amount:", defaultValue = 0))
              auctionBidUseCase.placeBid(gameId, bidderId, amount) match {
                case Right(_) =>
                  println(s"✅ Bid placed: $$${amount.amount}")
                case Left(error) =>
                  println(s"❌ $error")
              }

            case "f" | "fold" if game.auction.isDefined =>
              val bidderId = promptBidderId(game)
              auctionBidUseCase.fold(gameId, bidderId) match {
                case Right(_) =>
                  println("✅ Bidder folded.")
                case Left(error) =>
                  println(s"❌ $error")
              }

            case "p" | "pay" if game.currentPlayer.inJail && !turnActive =>
              payJailFineUseCase.execute(gameId) match {
                case Right(_) =>
                  println("✅ Jail fine paid.")
                case Left(error) =>
                  println(s"❌ $error")
              }

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

  private def promptBidderId(game: Game): PlayerId = {
    println("Enter bidder id:")
    game.players.filterNot(_.isBankrupt).foreach { p =>
      println(s"  ${p.id.value}: ${p.name} ($$${p.balance.amount})")
    }
    PlayerId(scala.io.StdIn.readLine().trim)
  }

  private def readInt(prompt: String, defaultValue: Int): Int = {
    println(prompt)
    scala.io.StdIn.readLine().trim.toIntOption.getOrElse(defaultValue)
  }
}
