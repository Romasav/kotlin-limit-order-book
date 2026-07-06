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

Phase 3 complete: single-symbol limit order book.

The project now has a minimal Kotlin/JVM Gradle setup, validated domain value objects and commands/events, plus a single-symbol limit order book with price-time priority and focused tests.

## Running Locally

```bash
./gradlew run
./gradlew test
```
