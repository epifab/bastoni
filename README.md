# Bastoni

[![Build Status](https://app.travis-ci.com/epifab/bastoni.svg?token=jwZ8R2sq9gVzq2syFmMJ&branch=main)](https://app.travis-ci.com/epifab/bastoni)

Gaming platform written in purely functional Scala, supporting italian classic card games: 
[tressette](https://en.wikipedia.org/wiki/Tressette), 
[briscola](https://en.wikipedia.org/wiki/Briscola) 
and [scopa](https://en.wikipedia.org/wiki/Scopa).

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

### Architecture

The architecture is fully event-driven and designed to specifically address resilience and scalability.

Remote players connect and communicate to the service exclusively through a Game controller.
The Game controller is in charge of forwarding remote players' messages to a message bus
and also consuming, transforming and forwarding messages from that same bus back to the remote player.

The game service (responsible for handling games logic) publishes to the same bus,
but consumes messages from a queue, allowing multiple instances to run simultaneously for scalability.

```mermaid
flowchart TD
    frontend1((Player))
    frontend2((Player))
    frontend3((Player))
    controller[Game controller]
    bus{{Message bus}}
    queue{{Message queue}}
    service1[Game service]
    service2[Game service]
    db[(Game repository)]

    frontend1---controller
    frontend2---controller
    frontend3---controller
    controller---bus
    bus-->queue
    service1-->bus
    service2-->bus
    queue-->service1
    queue-->service2
    service1---db
    service2---db
```

Internally, the game service implements games as state machines.  
Each game is defined as a pure, [side-effect-free function](./modules/domain/src/main/scala/bastoni/domain/logic/GameLogic.scala):

```scala
val play: (State, Event | Command) => (State, List[Event | Command])
```

All side effects are performed by the game service, including:
- storing / retrieving the state of each game
- consuming messages from the queue
- publishing messages to the bus

The following diagram illustrates how players connect and join a room.
Please note that the message queue is omitted for the sake of simplicity:

```mermaid
sequenceDiagram
    actor p AS Player 1
    participant controller AS Game controller
    participant bus AS Message bus
    participant service AS Game service
    participant repo AS Repository

    note over p, repo: The Player submits a Connect command to the controller
    p-)+controller: Connect
    controller->>-bus: Connect
    bus->>+service: Connect
    repo->>service: fetch state
    service->>-bus: PlayerConnected
    bus->>+controller: PlayerConnected
    controller->>-p: Connected(room)
    note over p, repo: The frontend will display the current view of the room<br/>The player submits a JoinTable command to the controller
    p-)+controller: JoinTable
    controller->>-bus: JoinTable
    bus->>+service: JoinTable
    repo->>service: fetch state
    service->>service: validate request
    service->>repo: update state
    service->>-bus: PlayerJoinedTable
    bus->>+controller: PlayerJoinedTable
    controller->>-p: GameEvent(PlayerJoinedTable)
    note over p, repo: The frontend will display Player 1 at the table
```

The following diagram illustrates how two players (in the same room) can start playing together.  
Again, some components have been omitted for simplicity:

```mermaid
sequenceDiagram
    actor p1 AS Player1
    actor p2 AS Player2
    participant bus AS Message bus
    participant service AS Game service
    participant repo AS Repository
    participant machine AS State machine
    
    note over p1, machine: Player1 submits a StartMatch command to the controller
    p1-)bus: StartMatch
    bus->>+service: StartMatch
    repo->>service: fetch state
    service->>+machine: play(state, StartMatch)
    machine->>-service: (new state, MatchStarted)
    service->>repo: update state
    service->>-bus: MatchStarted

    note over p1, machine: A MatchStarted event is broadcast to all players connected to the room as well as the Game service
    par
        bus->>p1: MatchStarted
    and 
        bus->>p2: MatchStarted
    and 
        bus->>+service: MatchStarted
    end
        
    repo->>service: fetch state
    service->>+machine: play(state, MatchStarted)
    machine->>-service: (new state, [Act])
    service->>repo: update state
    service->>-bus: Act

    note over p1, machine: An Act command (including the target player and the requested action)<br/> is issued by the Game service and broadcast to all players
    par
        bus->>p1: Act
    and
        bus->>p2: Act
    end

    note over p1, machine: Player2 submits a ShuffleDeck command to the controller
    p2->>bus: ShuffleDeck
    bus->>+service: ShuffleDeck
    repo->>service: fetch state
    service->>+machine: play(state, ShuffleDeck)
    machine->>-service: (state, [DeckShuffled, Delayed(Continue)])
    service->>repo: update state
    service->>bus: DeckShuffled
    service->>-service: schedule delayed submission of a Continue command

    note over p1, machine: A DeckShuffled event is broadcast
    par
        bus->>p1: DeckShuffled
    and
        bus->>p2: DeckShuffled
    and
        bus->>+service: DeckShuffled
    end
    repo->>service: fetch state
    service->>+machine: play(state, DeckShuffled)
    machine->>-service: (new state, [])
    service->>-repo: update state

    note over p1, machine: A Continue command that was previously scheduled is now issued
    service-)bus: Continue (received)
    bus->>+service: Continue
    repo->>service: fetch state
    service->>+machine: play(state, Continue)
    machine->>-service: (new state, BoardCardsDealt, Delayed(Continue))
    service->>repo: update state
    service->>bus: BoardCardsDealt
    note over p1, machine: A BoardCardsDealt event is issued and will be received by the various frontend to display the cards
    service->>-service: schedule delayed submission of a Continue command
```

### More in details

This project is _not_ (and might never become) production-ready.
It is indeed purely experimental, so feel free to draw inspiration from it, and use it at your own risk.

Please note that due to the experimental nature of this service,
all pieces of infrastructure (repository, message bus and message queue) have in-memory implementations only,
relying on cats effect and fs2.
