# Repository Guidelines

## Business feature implementation

In case, I am asked to implement a new business feature, you will follow the steps below:
- Work on domain and application layers only.
- Analyze if we need to extend existing code : 
  - If yes then extend it gracefully, following existing patterns.
  - If no then implement new code in domain and application layers.
- Implement next iteration of functionality. 
- Create checklist of what were implemented before (green) and what was implemented by you in current iteration (red). 
- If feature already exist - don't need to make changes. 
- The code must follow acceptance criteria (if it already fits much more - no need to write disabling logic, integrate it in current iteration).
- If you need to add dependencies do it on bootstrap layer, it is allowed if new dependencies appear.
- Avoid making changes on infrastructure layers, unless it is strictly necessary for feature implementation. Additionally explain what you did and show as black in final checklist.

## Project Structure & Module Organization
- Multi-module sbt build. Each module is in `mnpl-*` with Scala sources under `src/main/scala`.
- Core domain logic lives in `mnpl-domain` (entities, value objects, domain services).
- Application use cases and ports live in `mnpl-application`.
- Infrastructure adapters live in `mnpl-infra-cli`, `mnpl-infra-event`, `mnpl-infra-factory`, and `mnpl-infra-persistence`.
- Composition and the console entry point are in `mnpl-bootstrap`, including `MonopolyConsoleApp.scala`.

## Build, Test, and Development Commands
- `sbt compile` builds all modules.
- `sbt test` runs tests (currently none are defined).
- `sbt "project mnpl-bootstrap" run` launches the console app from `mnpl-bootstrap`.
- `sbt "project mnpl-domain" compile` builds a specific module.

## Coding Style & Naming Conventions
- Language: Scala 3. Use idiomatic Scala naming (Types in `PascalCase`, values/methods in `camelCase`).
- Packages follow the `com.sokima.monopoly` prefix (see `build.sbt`).
- Keep hexagonal boundaries clear: domain should not depend on infrastructure.
- Follow SOLID principles, clean architecture practices, use Functional Programming patterns.

## Testing Guidelines
- Add tests under `src/test/scala` per module when introduced.
- Name test classes with a `*Spec` suffix (e.g., `GameSpec.scala`) once a framework is chosen.

## Commit & Pull Request Guidelines
- Commit messages follow the pattern `[MNPL-###] Short description` (see `git log`).
- PRs should include: a concise summary, any linked issue key (e.g., `MNPL-007`), and console screenshots or sample output when behavior changes.

## Configuration Tips
- sbt settings are in `build.sbt` and `project/plugins.sbt`. Keep module dependencies aligned with the architecture.

## Scala Coding Approaches and Principles

### Default mindset
- Scala is multi-paradigm; prefer FP-first, use OOP mainly for modularity and composition.
- Optimize for clarity, correctness, maintainability; avoid cleverness.

### Functional core
- Prefer immutability: use `val`, immutable data structures; keep mutation local and not publicly exposed.
- Write small, pure functions; avoid hidden side effects in core logic.
- Push side effects (I/O, logging, external calls) to system boundaries.
- Prefer higher-order functions and combinators (`map`, `flatMap`, `fold`, `filter`) over loops.
- Build logic by composing small functions (pipelines).

### Idiomatic Scala
- Avoid `null`; represent optionality with `Option`.
- Never use `Option.get`; handle with pattern matching or combinators (`fold`, `getOrElse`, `map/flatMap`).
- Prefer pattern matching over long `if/else` chains; ensure exhaustiveness for closed domains.
- Avoid `return`; use expression-oriented style.
- Use string interpolation (`s"..."`) over concatenation.

### Types as design tools
- Use case classes for data; use ADTs for closed sets of states:
    - Scala 2: `sealed trait` + `case class/object`
    - Scala 3: prefer `enum` when appropriate
- Make illegal states unrepresentable:
    - encode constraints with types (`Option`, distinct domain types, refined/value/opaque types where relevant)
- Type inference for locals; add explicit types for public APIs and important boundaries.

### Abstraction and modularity (OOP done well)
- Prefer composition over inheritance; avoid deep class hierarchies.
- Use traits to define interfaces/behaviors; keep implementations separate.
- Separate concerns: keep business logic independent from infra (DB/HTTP/etc).

### Context / type classes / extension
- Use implicits/context parameters primarily for well-known patterns (type classes, context passing).
    - Scala 3: prefer `given/using` for clarity.
- Use extension methods to enrich types (Scala 3 `extension`, Scala 2 implicit classes); avoid opaque magic.

### Error handling
- Don’t throw exceptions for recoverable errors:
    - use `Either` / `Try` (or project-standard error ADTs).
- Use for-comprehensions to sequence `Option` / `Either` / `Try` / effect values.
- If validating multiple fields, prefer accumulating errors (if project supports it).

### Concurrency (backend)
- For async, use `Future` or the project’s effect type.
- Avoid blocking on main execution pools; isolate blocking work to dedicated pools or safe wrappers.
- Prefer immutable messages/data across threads; ensure resource safety (acquire/release reliably).

### Code style and organization
- Organize by packages/features; usually one main type per file (+ companion).
- Use companion objects for constructors/factories/constants; keep related instances near the type.
- Write Scaladoc for public APIs; comments explain “why”, not “what”.
- Use consistent formatting (e.g., Scalafmt); keep code readable and unsurprising.
- Prefer straightforward code over custom operators / overly terse tricks.

### Performance guidelines (only when needed)
- Pick correct algorithms/data structures; know collection complexity tradeoffs.
- Use `@tailrec` for tail-recursive functions when recursion is used.
- Avoid unnecessary intermediate collections in large pipelines (iterators/views/lazy where appropriate).
- Profile before micro-optimizing; keep optimizations encapsulated.

### Testing and quality
- Unit-test pure functions heavily; use property-based testing where useful.
- For side effects, abstract boundaries so logic can be tested with stubs/fakes.
- Use formatting + linting + CI; enforce “no null / no Option.get / no unnecessary var”.
- Prefer pragmatic designs; avoid over-abstract FP architectures unless complexity demands it.


## Hexagonal Architecture (Ports & Adapters) — Scala guidelines

### Goal
Keep **Domain + Use Cases** independent from frameworks (HTTP, DB, Kafka, AWS, Akka, Cats, etc.). Everything external is an **Adapter** plugged via **Ports**.

### Layering rules (non-negotiable)
1. **Domain** (pure)
    - Entities/Value Objects, ADTs, domain invariants, domain services (pure).
    - No I/O, no logging, no JSON, no DB types, no framework imports.
2. **Application** (use cases)
    - Orchestrates domain logic to achieve a business goal.
    - Talks ONLY through **ports** (traits) for persistence, time, UUID, messaging, etc.
    - Contains transaction boundaries, policies, auth decisions, idempotency, retries (policy), but not raw framework code.
3. **Adapters / Infrastructure**
    - Implement ports: DB repositories, HTTP clients, Kafka producers/consumers, external APIs.
    - Convert external models ↔ domain models (mapping layer).
4. **Bootstrap / Main**
    - Wiring only: build real implementations, config, dependency injection, start server/streams.

Dependency direction: **Adapters depend on Application/Domain**, never the reverse.

### Ports design (Scala style)
- Ports are **small traits** with business-shaped operations.
    - Prefer `trait UserRepo { def find(id): F[Option[User]] }`
    - Avoid “generic CRUD” unless it matches domain language.
- Keep port methods returning **domain types**, not DB/HTTP types.
- Model failures explicitly: `Either[DomainError, A]` or project-standard error ADT.
- Prefer idempotent ports where possible (messaging/outbox).

### Use case style (ensures clean hex)
- One use case = one entry point (Command/Query).
- Use case does:
    1) validate input (or parse boundary DTO)
    2) load needed domain state via ports
    3) call pure domain logic
    4) persist changes via ports
    5) publish events via port (often via Outbox)
- No JSON, no HTTP headers, no SQL in use case code.

### Adapters rules (keep them dumb)
- Adapters translate + call:
    - HTTP adapter: decode request → call use case → encode response
    - DB adapter: domain ↔ row mapping, SQL/queries, transaction primitives
    - Messaging adapter: deserialize → call use case → ack/commit policy
- Keep mapping explicit (a dedicated `mapping` package).
- Do not leak framework exceptions past adapter boundary; map to domain/app errors.

## “Smooth” patterns to make hex easy

### 1) Command/Query separation (CQRS-lite)
- Commands: change state, return minimal info.
- Queries: read-only, may use separate read ports/optimized views.

### 2) Algebra + Interpreter (Tagless Final optional)
- Ports as algebras (traits), adapters as interpreters.
- Lets you unit test use cases with in-memory interpreters.

### 3) Transaction boundary at Application layer
- Use a `TransactionPort` (or `Transactor`) so use cases can run atomically:
    - `def inTx[A](fa: F[A]): F[A]`
- DB adapter implements it; domain never knows transactions exist.

### 4) Outbox pattern for reliable messaging
- Use case writes state + “event to publish” in same transaction.
- Separate publisher adapter drains outbox and publishes to Kafka.

### 5) Anti-Corruption Layer (ACL) for external systems
- For each external API, create:
    - external DTOs + client adapter
    - mapping to a **local domain model**
- Never let external model types into domain/application.

### 6) “Clock/UUID” ports
- `ClockPort`, `IdGenPort` prevent hidden nondeterminism; tests become trivial.

## Testing strategy (fits hex naturally)
- Domain: pure unit tests + property tests for invariants.
- Application/use cases: tests with **in-memory ports** (no DB/HTTP).
- Adapter tests: integration tests per adapter (DB container, HTTP stub, Kafka container).
- End-to-end: thin smoke tests only (don’t duplicate coverage).

## Framework coding guidelines
When integrating third-party libraries or frameworks into the hexagonal architecture, 
follow these guidelines to ensure clean separation of concerns and maintainability, and avoid ai hallucinations.
- general workflow with #context7:
  1. resolve a libraryId you are going to use (e.g. cats-effect, akka-http, doobie, etc.)
  2. use #context7 for fetching latest documentation, library information, and usage examples.
  3. follow best practices for integrating the framework with hex architecture based on received docs.
  4. start coding task
