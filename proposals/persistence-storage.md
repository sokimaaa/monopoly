# Persistence Storage Proposal (SQL + NoSQL)

## Context
We need persistence for `Game` and `Property` in the current iteration and expect richer UI assets later (HTML fragments, icons, design metadata per property). The design must keep domain/application independent and fit the hexagonal boundary (ports in `mnpl-application`, adapters in `mnpl-infra-persistence`).

## Goals
- Persist and reload `Game` and `Property` without leaking storage concerns into the domain.
- Support storing rich property presentation data (HTML fragments, icons, layout metadata).
- Keep data model evolvable as rules and UI expand.
- Provide both SQL and NoSQL paths with clear tradeoffs.

## Storage options overview
### Option A: SQL (PostgreSQL recommended)
Use relational tables for game state and properties, with JSONB for HTML/design payloads.

**Pros**
- Strong consistency and transactions for game state updates.
- Natural relational modeling for players, properties, and ownership.
- JSONB column is flexible for HTML/design metadata.
- Easier analytics and reporting queries.

**Cons**
- Schema migrations required for evolution.
- Nested document updates can be verbose vs NoSQL.

### Option B: NoSQL (MongoDB)
Store games and properties as documents, embedding arrays for players/ownership.

**Pros**
- Flexible schema for UI design payloads and future variants.
- Can store full game snapshots without heavy joins.
- Easier to evolve document structure incrementally.

**Cons**
- Harder to enforce referential integrity.
- Transactions are possible but more complex operationally.
- Querying across collections requires careful indexing and duplication.

## Recommended direction
Start with PostgreSQL for correctness and consistency in game state, using JSONB fields for property design payloads. Keep NoSQL as a future option for user-generated content or if design metadata becomes a large, evolving document.

## Proposed data model
### SQL schema (PostgreSQL)
Tables are normalized for game state and properties.

**games**
- `id` (text, PK)
- `current_player_index` (int)
- `status` (text)
- `turn_dice_rolled` (bool)
- `turn_doubles_count` (int)

**players**
- `id` (text, PK)
- `game_id` (text, FK -> games.id)
- `name` (text)
- `position` (int)
- `balance` (int)
- `bankrupt` (bool)
- `owned_properties` (text[]) or a join table

**properties**
- `id` (text, PK)
- `name` (text)
- `position` (int)
- `price` (int)
- `rent` (int)
- `property_type` (text)
- `owner_id` (text, nullable)
- `design_payload` (jsonb)  // HTML, icon, layout, metadata

### NoSQL document model (MongoDB)
Store game and property documents. Ownership can be embedded or referenced.

**games collection**
```json
{
  "id": "game-123",
  "currentPlayerIndex": 0,
  "status": "Running",
  "turnState": { "diceRolled": false, "doublesCount": 0 },
  "players": [
    {
      "id": "player_0",
      "name": "Alice",
      "position": 7,
      "balance": 1400,
      "bankrupt": false,
      "ownedProperties": ["p1", "p2"]
    }
  ]
}
```

**properties collection**
```json
{
  "id": "p1",
  "name": "Boardwalk",
  "position": 39,
  "price": 400,
  "rent": 50,
  "propertyType": "Street",
  "ownerId": "player_0",
  "designPayload": {
    "html": "<div class='property'>...</div>",
    "icon": { "type": "svg", "value": "<svg>...</svg>" }
  }
}
```

## Integration with hexagonal architecture
- Keep the ports in `mnpl-application` (`GameRepository`, `PropertyRepository`).
- Implement adapters in `mnpl-infra-persistence`:
  - `SlickGameRepository`, `SlickPropertyRepository`
  - or `MongoGameRepository`, `MongoPropertyRepository`
- Serialization/mapping remains in infrastructure to keep domain pure.

## Scala integration examples
### SQL with Slick (PostgreSQL)
Dependencies (from Slick docs):
```scala
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "$project.version$",
  "org.slf4j" % "slf4j-nop" % "1.7.26",
  "com.typesafe.slick" %% "slick-hikaricp" % "$project.version$"
)
```

Config (from Slick docs):
```hocon
appdb = {
  url = "jdbc:postgresql://localhost:5432/monopoly"
  driver = org.postgresql.Driver
  connectionPool = HikariCP
}
```

Minimal Slick mapping and usage:
```scala
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext

final case class GameRow(id: String, currentPlayerIndex: Int, status: String)

class Games(tag: Tag) extends Table[GameRow](tag, "games") {
  def id = column[String]("id", O.PrimaryKey)
  def currentPlayerIndex = column[Int]("current_player_index")
  def status = column[String]("status")
  def * = (id, currentPlayerIndex, status).mapTo[GameRow]
}

class SlickGameRepository(db: Database)(using ExecutionContext) {
  private val games = TableQuery[Games]

  def insert(row: GameRow) =
    db.run(games += row)

  def findById(id: String) =
    db.run(games.filter(_.id === id).result.headOption)
}
```

### NoSQL with MongoDB Java driver (Scala interop)
MongoDB manual examples show using `MongoClients.create(...)` and accessing collections in Java. The same API is callable from Scala:
```scala
import com.mongodb.client.MongoClients
import org.bson.Document

val client = MongoClients.create("mongodb://localhost:27017")
val db = client.getDatabase("monopoly")
val games = db.getCollection("games")

val gameDoc = new Document()
  .append("id", "game-123")
  .append("currentPlayerIndex", Int.box(0))
  .append("status", "Running")

games.insertOne(gameDoc)
```

## Decision matrix (short)
- If consistency and transactional updates are priority: **PostgreSQL + Slick**.
- If flexible UI payload evolution is priority: **MongoDB** or **PostgreSQL JSONB**.

## Next steps
1. Choose primary DB (recommend PostgreSQL).
2. Add `mnpl-infra-persistence` adapter for Game/Property repositories.
3. Define migration strategy (Flyway/Liquibase) if SQL is chosen.
4. Decide JSON storage format for property design payloads (HTML string vs structured JSON).
