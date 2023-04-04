# Bastoni

[![Build Status](https://app.travis-ci.com/epifab/bastoni.svg?token=jwZ8R2sq9gVzq2syFmMJ&branch=main)](https://app.travis-ci.com/epifab/bastoni)

Classic italian card games service,
including a backend for tressette, briscola and scopa.

![Screenshot](screenshot.png)

## Run

Pre-requisites:

- sbt
- npm

```shell
./run
```

Browse to [localhost:9090](http://localhost:9090)


## Design

This project is based on ideas described by the following articles: 
- [Event driven design for gaming applications](https://www.epifab.solutions/posts/event-driven-design-for-gaming-applications)
- [Scala.js, the good the bad and the ugly](https://www.epifab.solutions/posts/scalajs-the-good-the-bad-and-the-ugly)

Games are implemented as state machines, and defined as a pure, [side-effect-free functions](./modules/domain/src/main/scala/bastoni/domain/logic/GameLogic.scala):

```scala
val play: (State, Event | Command) => (State, List[Event | Command])
```

The [GameService](./modules/domain/src/main/scala/bastoni/domain/logic/GameService.scala) acts as an orchestration layer
performing side effects including:
- storing / retrieving the state of each game
- consuming / publishing to a message bus

The frontend (the player) consumes events and publishes commands to the bus.

The following illustrates how players connect and join a room.

```mermaid
sequenceDiagram
    actor Player
    participant Message bus
    participant Service
    participant Repository
    
    Player->>Message bus: Connect command
    Message bus->>+Service: Connect command
    Service->>-Message bus: Snapshot
    Message bus->>Player: Snapshot
    Player->>+Message bus: JoinRoom command
    Message bus->>+Service: JoinRoom command
    Repository->>Service: fetch state
    Service->>Service: validate request
    Service->>Repository: update state
    Service->>-Message bus: PlayerJoinedRoom event
    Message bus->>Player: PlayerJoinedRoom event
```

Here is how two players (who previously connected and joined the same room)
can start a new match:

```mermaid
sequenceDiagram
    actor Player1
    actor Player2
    participant Message bus
    participant Service
    participant Repository
    participant Game
    
    Player1->>Message bus: StartMatch
    Message bus->>+Service: StartMatch
    Repository->>Service: fetch state
    Service->>+Game: (state, StartMatch)
    Game->>-Service: (new state, MatchStarted)
    Service->>Repository: update state
    Service->>-Message bus: MatchStarted
    
    Message bus->>+Service: MatchStarted
    Repository->>Service: fetch state
    Service->>+Game: (state, MatchStarted)
    Game->>-Service: (new state, Act)
    Service->>Repository: update state
    Service->>-Message bus: Act
    Message bus->>Player1: Act
    Message bus->>Player2: Act

    Player2->>Message bus: ShuffleDeck
    Message bus->>+Service: ShuffleDeck
    Repository->>Service: fetch state
    Service->>+Game: (state, ShuffleDeck)
    Game->>-Service: (state, DeckShuffled, Delayed(Continue))
    Service->>Repository: update state
    Service->>-Message bus: DeckShuffled
    Message bus->>Player1: DeckShuffled
    Message bus->>Player2: DeckShuffled
    Service->>Message bus: Continue (delayed)
    
    Message bus->>+Service: Continue
    Repository->>Service: fetch state
    Service->>+Game: (state, Continue)
    Game->>-Service: (new state, BoardCardsDealt, Delayed(Continue))
    Service->>Repository: update state
    Service->>-Message bus: BoardCardsDealt
    Message bus->>Player1: BoardCardsDealt
    Message bus->>Player2: BoardCardsDealt
    Service->>Message bus: Continue (delayed)
```

## Technical notes

This project is written in pure functional Scala (Scala 3) and compiles to both JVM and JavaScript.
The entire domain (including business logic for all games) can be run on a browser.

