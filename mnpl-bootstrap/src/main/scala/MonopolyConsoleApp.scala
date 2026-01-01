package com.sokima.monopoly

object MonopolyConsoleApp extends App {

  import composition.DependencyContainer.*

  tuiController.displayWelcome()

  tuiController.startNewGame() match {
    case Right(gameId) =>
      tuiController.gameLoop(gameId)
    case Left(_) =>
      ()
  }
}
