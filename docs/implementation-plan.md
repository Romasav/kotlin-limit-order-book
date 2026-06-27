# Kotlin Limit Order Book - Implementation Plan

This project will be built gradually as a learning project. The goal is to keep each step small enough to understand, test, commit, and review before moving on.

## Current Status

- [x] Git repository initialized
- [x] GitHub repository created
- [ ] Kotlin/Gradle project skeleton created
- [ ] Core domain model implemented
- [ ] Limit order book implemented
- [ ] Matching engine implemented
- [ ] Tests added for core behavior
- [ ] CLI/replay runner added
- [ ] Simulator and benchmark tooling added

## Phase 0: Git and GitHub Setup

Goal: create a clean project history before writing application code.

- [x] Initialize local git repository
- [x] Add Kotlin/Gradle/IntelliJ `.gitignore`
- [x] Add starter `README.md`
- [x] Commit existing project brief and planning docs
- [x] Create GitHub repository
- [x] Configure GitHub `origin` remote
- [x] Push local `main` branch to GitHub

Suggested first commit:

```text
Initial project brief and implementation plan
```

## Phase 1: Kotlin Project Skeleton

Goal: create a minimal runnable Kotlin/JVM project with tests.

- [ ] Add Gradle wrapper
- [ ] Add `settings.gradle.kts`
- [ ] Add `build.gradle.kts`
- [ ] Configure Kotlin/JVM
- [ ] Configure test framework
- [ ] Add simple `main()` entry point
- [ ] Add first passing test
- [ ] Run tests locally

Learning focus:

- Kotlin project structure
- Gradle basics
- Running and testing from the command line

## Phase 2: Core Domain Model

Goal: define the language of the trading system before implementing behavior.

- [ ] Define `Symbol`
- [ ] Define `OrderId`
- [ ] Define `Side`
- [ ] Define `Price`
- [ ] Define `Quantity`
- [ ] Define order types: limit and market
- [ ] Define command types
- [ ] Define event types

Possible commands:

- [ ] `PlaceLimitOrder`
- [ ] `PlaceMarketOrder`
- [ ] `CancelOrder`
- [ ] `AmendOrder`

Possible events:

- [ ] `OrderAccepted`
- [ ] `OrderRejected`
- [ ] `TradeExecuted`
- [ ] `OrderRested`
- [ ] `OrderCancelled`
- [ ] `OrderAmended`

Learning focus:

- Value objects
- Sealed classes
- Immutable data modeling
- Separating commands from events

## Phase 3: Single-Symbol Limit Order Book

Goal: maintain resting buy and sell limit orders for one symbol.

- [ ] Store bids highest price first
- [ ] Store asks lowest price first
- [ ] Preserve FIFO order at the same price
- [ ] Add resting limit orders
- [ ] Query best bid
- [ ] Query best ask
- [ ] Produce book snapshot
- [ ] Test price priority
- [ ] Test time priority

Learning focus:

- Price-time priority
- Choosing data structures
- Deterministic ordering

## Phase 4: Limit Order Matching

Goal: match compatible limit orders and emit deterministic trade events.

- [ ] Match incoming buy orders against best asks
- [ ] Match incoming sell orders against best bids
- [ ] Support full fills
- [ ] Support partial fills
- [ ] Rest unfilled limit quantity
- [ ] Remove filled resting orders
- [ ] Emit `TradeExecuted` events
- [ ] Test crossing orders
- [ ] Test partial fills
- [ ] Test multiple fills from one incoming order

Learning focus:

- Matching engine mechanics
- State transitions
- Event-driven behavior

## Phase 5: Engine API and Event Flow

Goal: wrap the book with a clean command-processing API.

- [ ] Create `MatchingEngine`
- [ ] Accept commands as input
- [ ] Return events as output
- [ ] Keep behavior deterministic
- [ ] Reject invalid commands
- [ ] Add tests around command-to-event behavior

Learning focus:

- API boundaries
- Validation
- Testable design

## Phase 6: Market Orders, Cancel, and Amend

Goal: complete the basic exchange feature set.

- [ ] Implement market buy orders
- [ ] Implement market sell orders
- [ ] Implement order cancellation
- [ ] Implement simple order amendment
- [ ] Reject cancel for unknown order
- [ ] Reject amend for unknown order
- [ ] Test market order partial fill
- [ ] Test market order with insufficient liquidity
- [ ] Test cancellation removes order from book
- [ ] Test amendment updates priority correctly

Learning focus:

- Edge cases
- Book mutation
- Clear rejection behavior

## Phase 7: Multi-Symbol Engine

Goal: support multiple fake instruments such as `ACME`, `FOO`, and `BAR`.

- [ ] Maintain one order book per symbol
- [ ] Route commands by symbol
- [ ] Keep order ids unique
- [ ] Snapshot individual books
- [ ] Test that symbols do not interfere with each other

Learning focus:

- Composition
- Routing
- Isolated state

## Phase 8: Replay and CLI Runner

Goal: replay command files and inspect deterministic output.

- [ ] Define simple command file format
- [ ] Parse command files
- [ ] Execute commands in order
- [ ] Print emitted events
- [ ] Print final book state
- [ ] Add sample replay files
- [ ] Test replay determinism

Example command file:

```text
LIMIT BUY ACME 10 100
LIMIT SELL ACME 5 99
CANCEL order-1
```

Learning focus:

- File input
- Parsing
- Reproducible debugging

## Phase 9: Simulation and Benchmarking

Goal: generate workloads and measure basic performance.

- [ ] Generate random but deterministic command streams
- [ ] Seed the simulator for reproducibility
- [ ] Measure commands processed per second
- [ ] Measure basic latency
- [ ] Record benchmark results
- [ ] Identify one bottleneck
- [ ] Improve one bottleneck and compare results

Learning focus:

- Performance measurement
- Profiling mindset
- Reproducible benchmarks

## Phase 10: Optional Native Performance Experiment

Goal: experiment with a focused C++ subsystem only after the Kotlin version is solid.

- [ ] Choose a small subsystem to compare
- [ ] Implement a minimal C++ version
- [ ] Compare behavior against Kotlin tests
- [ ] Compare performance
- [ ] Document tradeoffs

Learning focus:

- Native performance
- Interop tradeoffs
- Avoiding premature optimization

## Working Style

Use small branches and small commits.

Suggested branch pattern:

```text
phase-0-git-setup
phase-1-kotlin-skeleton
phase-2-domain-model
phase-3-order-book
```

For each phase:

1. Create or switch to a focused branch.
2. Implement one small piece.
3. Add or update tests.
4. Run tests.
5. Commit with a clear message.
6. Update this plan.
