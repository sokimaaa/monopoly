# Cats Effect Adoption for HTTP API

## Context
We want to introduce Cats Effect for an HTTP API while keeping the hexagonal architecture intact. The domain stays pure and infrastructure details (HTTP server, persistence, runtime) remain in adapters. This proposal builds on Cats Effect fundamentals (IO, Resource) and fits the current multi-module sbt layout.

## Goals
- Use Cats Effect for safe, explicit side effects and resource lifecycle management.
- Keep `mnpl-domain` free of effect types and frameworks.
- Keep `mnpl-application` independent of HTTP libraries; only abstract ports.
- Provide a path to add an HTTP API without destabilizing existing CLI and future adapters.

## Cats Effect building blocks (from docs)
- `IO` as the concrete effect type for side effects and sequencing (`IO.println`, `for`-comprehension, `>>`).
- `Resource` for safe acquire/use/release of server and persistence resources.
  - `Resource.make(acquire)(release)` and `use` are the core API.
  - Resources release in reverse order and are non-interruptible.

## Proposed architecture
- **Domain (`mnpl-domain`)**: unchanged, pure data and logic.
- **Application (`mnpl-application`)**:
  - Ports stay abstract.
  - Use cases remain effect-free or introduce `F[_]` with minimal typeclass constraints.
- **Infrastructure**:
  - `mnpl-infra-http` (new): HTTP server adapter, request/response mapping, DTOs, codecs.
  - `mnpl-infra-persistence` (existing): move toward effectful repos with `F[_]` and `Resource`.
  - `mnpl-bootstrap`: composition root, wiring, and `IOApp`.

## Types and approaches
### Option A (minimal change): keep use cases pure, wrap in `IO` at the edge
- Use cases keep signatures like `Either[String, A]`.
- HTTP adapter lifts with `IO.fromEither` and maps to HTTP responses.
- Lowest risk and simplest incremental adoption.

### Option B (preferred): make use cases polymorphic in `F[_]`
- Use cases depend on `F[_]` plus typeclasses (`Sync`/`Async`) and ports returning `F`.
- Errors modeled as ADTs rather than `String`.
- Infrastructure decides concrete `IO`, keeps application framework-agnostic.

## Modules to add or adjust
- `mnpl-infra-http` (new)
  - HTTP routes/controllers.
  - DTOs and mapping to/from domain types.
  - Server `Resource`.
- `mnpl-bootstrap`
  - `IOApp` entry point for HTTP server.
  - Wires repositories, services, use cases, and routes.
- `mnpl-application`
  - If Option B: make ports return `F` (e.g., `def findById(id): F[Option[Game]]`).
  - Introduce error ADT in application layer.

## Patterns to use
- **Ports and adapters**: HTTP adapter only knows application interfaces.
- **Typeclass-based effects**: `F[_]: Sync`/`Async` for side effects and async boundaries.
- **Resource lifecycle**: Server, database pools, and event publishers as `Resource[F, A]`.
- **Error ADTs**: explicit domain/application errors; map to HTTP responses in adapters.
- **Pure domain**: domain services remain total functions or return `Either`.

## Example snippets (aligned to current domain model)

### Application error ADT
```scala
package com.sokima.monopoly.application

sealed trait AppError
object AppError {
  case object GameNotFound extends AppError
  case object GameFinished extends AppError
  case object CurrentPlayerBankrupt extends AppError
  case object DiceAlreadyRolled extends AppError
  case object InvalidPosition extends AppError
}
```

### Option B: effect-polymorphic use case
```scala
package com.sokima.monopoly.application.usecase

import cats.effect.Sync
import cats.syntax.all._
import com.sokima.monopoly.application.AppError
import com.sokima.monopoly.application.port._
import com.sokima.monopoly.domain._
import com.sokima.monopoly.domain.service._

import cats.data.EitherT

final class RollDiceUseCaseF[F[_]: Sync](
    gameRepository: GameRepositoryF[F],
    propertyRepository: PropertyRepositoryF[F],
    movementService: MovementService,
    squareActionService: SquareActionService,
    propertyService: PropertyService,
    diceRoller: DiceRoller,
    eventPublisher: EventPublisherF[F]
) {
  def execute(gameId: GameId): F[Either[AppError, (Game, DiceRoll, List[GameEvent])]] =
    (
      for {
        game <- EitherT.fromOptionF(gameRepository.findById(gameId), AppError.GameNotFound)
        _    <- EitherT.cond[F]( !game.isFinished, (), AppError.GameFinished)
        _    <- EitherT.cond[F]( !game.currentPlayer.isBankrupt, (), AppError.CurrentPlayerBankrupt)
        _    <- EitherT.cond[F]( !game.turnState.diceRolled, (), AppError.DiceAlreadyRolled)
      roll = diceRoller.roll()
      (movedPlayer, moveEvent) = movementService.movePlayer(game.currentPlayer, roll, game.board)
      gameAfterMove = game.recordDiceRoll(roll).updatePlayer(movedPlayer)
        square <- EitherT.fromOption[F](gameAfterMove.board.getSquare(movedPlayer.position), AppError.InvalidPosition)
      result = handleSquareAction(gameAfterMove, square)
      (finalGame, additionalEvents) = result
        _ <- EitherT.liftF(gameRepository.update(finalGame))
        _ <- EitherT.liftF((moveEvent :: additionalEvents).traverse_(eventPublisher.publish))
      } yield (finalGame, roll, moveEvent :: additionalEvents)
    ).value

  private def handleSquareAction(game: Game, square: Square): (Game, List[GameEvent]) =
    squareActionService.determineAction(square, game) match {
      case SquareAction.NoAction =>
        (game, Nil)
      case SquareAction.PayRentAction(property, landlordId) =>
        val tenant   = game.currentPlayer
        val landlord = game.players.find(_.id == landlordId).get
        val (updatedTenant, updatedLandlord, rentEvent) =
          propertyService.payRent(tenant, landlord, property)
        (game.updatePlayer(updatedTenant).updatePlayer(updatedLandlord), List(rentEvent))
      case SquareAction.PayTaxAction(amount) =>
        val player        = game.currentPlayer
        val updatedPlayer = player.pay(amount)
        val taxEvent      = GameEvent.TaxPaid(player.id, amount)
        (game.updatePlayer(updatedPlayer), List(taxEvent))
      case _ =>
        (game, Nil)
    }
}
```

### HTTP adapter sketch (IO + Resource)
```scala
package com.sokima.monopoly.infra.http

import cats.effect.{IO, Resource}
import com.sokima.monopoly.application.usecase.RollDiceUseCase
import com.sokima.monopoly.domain.GameId

final class GameRoutes(rollDice: RollDiceUseCase) {
  def rollDiceEndpoint(gameId: String): IO[String] =
    IO.fromEither(rollDice.execute(GameId(gameId)).left.map(new Exception(_)))
      .map { case (game, roll, events) => s"${game.id.value} rolled ${roll.total}" }
}

object HttpServer {
  def resource(routes: GameRoutes): Resource[IO, Unit] =
    Resource.make(IO.println("start http"))(_ => IO.println("stop http"))
}
```

### Composition root (IOApp)
```scala
package com.sokima.monopoly.bootstrap

import cats.effect.{IO, IOApp}
import com.sokima.monopoly.infra.http.{GameRoutes, HttpServer}

object MonopolyHttpApp extends IOApp.Simple {
  def run: IO[Unit] =
    HttpServer.resource(new GameRoutes(/* wired use case */)).use(_ => IO.never)
}
```

## Graceful adoption plan
1. Add `cats-effect` dependency to `mnpl-bootstrap` and `mnpl-infra-http`.
2. Start with Option A to avoid risky refactors.
3. Introduce `mnpl-infra-http` with a single endpoint (`rollDice`).
4. Migrate application ports to `F[_]` (Option B) once HTTP path is stable.
5. Move persistence and event publishing to `Resource`-backed `F` implementations.

## Notes
- Prefer `Resource` for server and database lifecycles, and `IO.blocking` for blocking calls.
- Keep the domain and application pure and stable; only adapters should depend on HTTP.
