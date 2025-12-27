# Repository Guidelines

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
- Language: Scala 3. Use standard 2-space indentation and idiomatic Scala naming (Types in `PascalCase`, values/methods in `camelCase`).
- Packages follow the `com.sokima.monopoly` prefix (see `build.sbt`).
- Keep hexagonal boundaries clear: domain should not depend on infrastructure.
- Follow SOLID principles and clean architecture practices.

## Testing Guidelines
- No test framework is configured yet. Add tests under `src/test/scala` per module when introduced.
- Name test classes with a `*Spec` suffix (e.g., `GameSpec.scala`) once a framework is chosen.

## Commit & Pull Request Guidelines
- Commit messages follow the pattern `[MNPL-###] Short description` (see `git log`).
- PRs should include: a concise summary, any linked issue key (e.g., `MNPL-007`), and console screenshots or sample output when behavior changes.

## Configuration Tips
- sbt settings are in `build.sbt` and `project/plugins.sbt`. Keep module dependencies aligned with the architecture.
