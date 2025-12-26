package com.sokima.monopoly
package composition

import application.port.{BoardFactory, EventPublisher, GameRepository, PropertyRepository}
import application.usecase.{BuyPropertyUseCase, CreateGameUseCase, EndTurnUseCase, RollDiceUseCase}
import domain.Money
import domain.service.*
import infrastructure.adapter.cli.ConsoleGameController
import infrastructure.adapter.event.ConsoleEventPublisher
import infrastructure.adapter.persistence.{InMemoryGameRepository, InMemoryPropertyRepository}
import infrastructure.factory.ClassicBoardFactory

object DependencyContainer {

  // Configuration
  val gameConfig: GameConfig = GameConfig(
    startingBalance = Money(1500),
    goSalary = Money(200),
    boardSize = 40
  )

  // Factories
  val boardFactory: BoardFactory = new ClassicBoardFactory()

  // Infrastructure - Persistence
  val gameRepository: GameRepository = new InMemoryGameRepository()

  lazy val propertyRepository: PropertyRepository = {
    val board = boardFactory.createBoard()
    val properties = boardFactory.extractProperties(board)
    new InMemoryPropertyRepository(properties)
  }

  // Infrastructure - Event Publishing
  val eventPublisher: EventPublisher = new ConsoleEventPublisher()

  // Domain Services
  val diceRoller: DiceRoller = new RandomDiceRoller()
  val movementService: MovementService = new MovementService(gameConfig)
  val propertyService: PropertyService = new PropertyService()
  val squareActionService: SquareActionService = new SquareActionService(propertyService)

  // Use Cases
  val createGameUseCase: CreateGameUseCase = new CreateGameUseCase(
    gameRepository,
    propertyRepository,
    boardFactory,
    gameConfig
  )

  val rollDiceUseCase: RollDiceUseCase = new RollDiceUseCase(
    gameRepository,
    propertyRepository,
    movementService,
    squareActionService,
    propertyService,
    diceRoller,
    eventPublisher
  )

  val buyPropertyUseCase: BuyPropertyUseCase = new BuyPropertyUseCase(
    gameRepository,
    propertyRepository,
    propertyService,
    eventPublisher
  )

  val endTurnUseCase: EndTurnUseCase = new EndTurnUseCase(
    gameRepository,
    eventPublisher
  )

  // Presentation Layer
  val consoleController: ConsoleGameController = new ConsoleGameController(
    createGameUseCase,
    rollDiceUseCase,
    buyPropertyUseCase,
    endTurnUseCase,
    gameRepository
  )
}
