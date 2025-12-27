# NoSQL Deep Dive for Monopoly Persistence

## Context
We want NoSQL options for storing `Game` and `Property` now, and richer presentation data later (HTML fragments, icons, design metadata). This analysis surveys NoSQL categories and recommends a practical path for a Scala project with clean hexagonal boundaries.

## Goals
- Identify which NoSQL categories fit our data shape and access patterns.
- Highlight a concrete DB choice per relevant category, with pros/cons.
- Note Scala ecosystem best practices for integration.

## NoSQL categories and fit
### Document-oriented (MongoDB, Couchbase, Firestore)
**Fit:** Good. Game state and property metadata are naturally document-shaped, and HTML/design payloads fit well.

**Recommendation: MongoDB (self-hosted or Atlas)**
- **Why MongoDB:** Mature, popular in Scala/JVM via the official Java driver; good for nested documents and flexible schemas.
- **Why not Couchbase:** Strong option, but typically used when in-memory performance or N1QL is needed; smaller Scala community footprint.
- **Why not Firestore:** Great managed service and schema flexibility, but strong cloud lock-in and different operational model.

**Pros**
- Flexible schema for evolving property design payloads.
- Easy snapshot storage for game state.
- Works well with JSON-style DTOs.

**Cons**
- Referential integrity is app-owned.
- Transaction semantics and updates require careful design (esp. multi-document).

**Scala/JVM access (MongoDB Java driver dependency snippet from docs):**
```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>x.y.z</version>
</dependency>
```

**Managed alternative (Firestore Java quickstart from docs):**
```java
FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance();
Firestore db = firestoreOptions.getService();
System.out.println("Hello Firestore!");
db.close();
```

### Key-Value (Redis, etcd)
**Fit:** Useful as a secondary store (cache, session, fast lookups), not a durable primary store for game history.

**Recommendation: Redis**
- **Why Redis:** Battle-tested; very fast for hot game states, caching, and pub/sub.
- **Why not etcd:** More oriented to distributed coordination, not application data modeling.

**Pros**
- Ultra-low latency.
- Great for ephemeral state, rate limiting, or leaderboards.

**Cons**
- Primary persistence requires extra care (AOF/RDB) and data modeling is limited.

**Scala/JVM access (Jedis example from docs):**
```java
JedisPooled client = new JedisPooled("localhost", 6479);
```

### Wide-column / Column-oriented (Cassandra, ScyllaDB, HBase)
**Fit:** Low. Our access patterns are not huge, time-bucketed, or high-write scale. Data is relational within a small domain.

**Why not now**
- Complex data modeling for small-scale workloads.
- Consistency and query patterns require careful design upfront.

### Graph (Neo4j, JanusGraph)
**Fit:** Low. The game board graph is static and small, and queries are simple.

**Why not now**
- Adds operational overhead with little value.
- We do not need graph traversal queries at scale.

### Time-series (InfluxDB, TimescaleDB, QuestDB)
**Fit:** Low. We are not collecting metrics or temporal sensor data.

**Why not now**
- Over-optimized for timestamp-heavy writes and time-window queries.

### Event storage / Event sourcing (EventStoreDB, Kafka + snapshots)
**Fit:** Medium as a future option. The domain already emits events (`GameEvent`).

**Why not now**
- Event sourcing needs projection infrastructure and careful versioning.
- Not needed to meet current persistence requirements.

## Scala community practices (pragmatic guidance)
- Prefer the official Java client when Scala-native drivers are thin or dated.
- Keep mapping/serialization in `mnpl-infra-persistence`; domain stays pure.
- Persist `Game` as a snapshot + version field (optimistic concurrency).
- Store `Property` design payloads as JSON strings or a JSON object.
- Avoid direct serialization of domain types into DB records; define explicit DTOs.

## Suggested path for this project
- **Primary NoSQL option:** MongoDB for game snapshots + property metadata (HTML/icons).
- **Secondary cache:** Redis for hot game state or quick access in a multi-node setup.
- Keep the persistence adapter modular to allow SQL (Postgres + JSONB) later.

## Decision summary
- **Document DB:** MongoDB recommended; Firestore as managed alternative.
- **Key-Value:** Redis only as a cache/secondary store.
- **Other categories:** Not justified for current scale and query patterns.
