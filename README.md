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

Phase 6 complete: market orders, cancellation, and amendment.

The single-symbol matching engine now supports limit and market buy/sell orders, cancellation, and amendment. Market orders consume all available opposite-side liquidity and emit a cancellation event for any unfilled quantity instead of resting it. Quantity reductions at the same price retain time priority, while quantity increases and price changes re-enter the book with new time priority. Commands emit deterministic acceptance, trade, resting, cancellation, amendment, and rejection events, and the final book state is available as a snapshot.

## Running Locally

```bash
./gradlew run
./gradlew test
```
