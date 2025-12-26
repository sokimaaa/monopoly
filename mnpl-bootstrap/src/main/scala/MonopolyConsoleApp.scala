package com.sokima.monopoly

object MonopolyConsoleApp extends App {

  import composition.DependencyContainer.*

  consoleController.displayWelcome()

  consoleController.startNewGame() match {
    case Right(gameId) =>
      println(s"\n✅ Game created: ${gameId.value}")
      println("Starting game...\n")
      consoleController.gameLoop(gameId)

    case Left(error) =>
      println(s"❌ Failed to create game: $error")
  }

  println("\n👋 Thanks for playing!")
}