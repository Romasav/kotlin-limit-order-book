# Kotlin Limit Order Book and Exchange Simulator

## Project Overview

This project is a small electronic exchange simulator built in Kotlin. Its goal is to model the core infrastructure behind a simple financial market: accepting buy and sell orders, maintaining an order book, matching compatible orders, and producing deterministic events that describe what happened.

The project is not intended to predict prices, trade real money, or implement a profitable strategy. Instead, it focuses on building the software systems that power trading infrastructure: correctness, deterministic behaviour, clean APIs, event-driven design, testing, replayability, and performance measurement.

At its core, the system simulates how buyers and sellers interact in a market. Buyers submit orders saying how much they are willing to pay, sellers submit orders saying how much they are willing to accept, and the matching engine decides when trades should occur.

## What This Project Should Do

The system should allow users or simulations to submit trading commands for fake financial instruments such as `ACME`, `FOO`, or `BAR`.

It should support the following high-level actions:

- Place buy and sell limit orders
- Place buy and sell market orders
- Cancel existing orders
- Amend existing orders
- Match compatible buy and sell orders
- Track partially filled orders
- Maintain the current state of the order book
- Emit events such as order accepted, trade executed, order cancelled, and book updated
- Replay a sequence of previous commands deterministically
- Run generated simulations and benchmarks

The main purpose is to build a realistic but manageable model of a matching engine and limit order book.

## Core Concepts

### Order

An order is a request to buy or sell a quantity of a financial instrument.

Examples:

```text
BUY 10 ACME @ 100
SELL 5 ACME @ 101
```

A buy order expresses demand.  
A sell order expresses supply.

### Limit Order

A limit order includes a price constraint.

A buy limit order means:

```text
Buy this quantity, but do not pay more than this price.
```

A sell limit order means:

```text
Sell this quantity, but do not accept less than this price.
```

If a limit order cannot be matched immediately, it may rest in the order book.

### Market Order

A market order does not specify a price.

It means:

```text
Buy or sell immediately at the best available price.
```

Market orders consume available liquidity from the opposite side of the book.

### Order Book

The order book stores unmatched limit orders.

It has two sides:

```text
Bids: buy orders
Asks: sell orders
```

The best bid is the highest current buy price.  
The best ask is the lowest current sell price.  
The spread is the difference between the best ask and best bid.

### Matching Engine

The matching engine is the core component of the system.

It receives incoming commands, checks the current order book, determines whether trades should occur, updates the book, and emits events describing the result.

### Price-Time Priority

Orders should be matched using price-time priority.

This means:

1. Better prices are matched first.
2. If two orders have the same price, the order that arrived earlier is matched first.

For buy orders, a higher price has priority.  
For sell orders, a lower price has priority.

## Example Scenario

Suppose the order book is empty.

A user submits:

```text
BUY 10 ACME @ 100
```

There are no sellers, so the order rests in the book.

Current book:

```text
Bids:
100 -> 10

Asks:
empty
```

Then another user submits:

```text
SELL 5 ACME @ 99
```

The seller is willing to sell at 99, and the buyer is willing to pay up to 100, so the orders match.

The engine emits a trade event:

```text
TRADE ACME 5 @ 100
```

The buy order is partially filled. It originally wanted 10 shares, but only 5 were matched.

Updated book:

```text
Bids:
100 -> 5

Asks:
empty
```

## Expected System Behaviour

The system should be deterministic.

Given the same input commands in the same order, it should always produce the same output events and final order book state.

This is important because deterministic behaviour makes the engine easier to test, debug, benchmark, and replay.

The system should clearly separate:

- Incoming commands
- Matching logic
- Order book state
- Output events
- Simulation/replay tools
- Benchmarking tools

## Non-Goals

This project is not trying to:

- Trade real money
- Connect to a real brokerage or exchange
- Predict market prices
- Build a profitable trading strategy
- Implement advanced finance theory
- Use machine learning as the core feature
- Model every detail of a real-world exchange

The focus is on software engineering, market mechanics, correctness, and performance.

## Target Final Scope

The final project should include:

- A Kotlin/JVM matching engine
- A limit order book for one or more symbols
- Support for limit orders
- Support for market orders
- Support for order cancellation
- Support for simple order amendments
- Price-time-priority matching
- Partial fills
- Deterministic event output
- Command replay from a file
- A command-line interface or simple runner
- Unit tests for core matching behaviour
- A generated workload simulator
- Benchmark results showing throughput and latency
- Optional native C++ performance experiment for a focused subsystem

## Portfolio Goal

The goal of this project is to demonstrate the ability to build a small but realistic trading infrastructure component.

A strong finished version should show:

- Understanding of basic market mechanics
- Clean software design
- Correct handling of edge cases
- Deterministic event-driven architecture
- Strong testing discipline
- Performance awareness
- Ability to profile and improve bottlenecks
- Clear documentation and examples

The project should communicate the following engineering story:

```text
I built a deterministic matching engine and limit order book in Kotlin to understand electronic trading infrastructure from first principles. The system supports realistic order-matching behaviour, event replay, testing, simulation, and benchmarking, with optional native performance experiments for latency-sensitive components.
```