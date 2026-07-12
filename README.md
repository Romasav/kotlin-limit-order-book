# Kotlin Limit Order Book

This is a learning project for building a small electronic exchange simulator in Kotlin.

The goal is to model the core mechanics behind a simple financial market:

- accepting buy and sell orders
- maintaining a limit order book
- matching compatible orders
- emitting deterministic events
- replaying command streams
- measuring basic performance

The project is not intended to trade real money or predict prices. It is focused on software engineering, market mechanics, correctness, testing, replayability, and performance awareness.

## Project Docs

- [Project brief](docs/project-brief.md)
- [Implementation plan](docs/implementation-plan.md)

## Current Stage

Phase 5 complete: matching engine API and event flow.

The project now has a minimal Kotlin/JVM Gradle setup, validated domain value objects and commands/events, plus a single-symbol matching engine. It validates commands, prevents duplicate order ids, preserves price-time priority, emits deterministic acceptance, trade, resting, and rejection events, and exposes the final book state as a snapshot.

## Running Locally

```bash
./gradlew run
./gradlew test
```
