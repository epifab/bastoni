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
    frontend1[Player]
    frontend2[Player]
    frontend3[Player]
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
Please note that the Game controller and the message queue are omitted
for the sake of simplicity:

```mermaid
sequenceDiagram
    actor Player
    participant Message bus
    participant Service
    participant Repository
    
    Player->>Message bus: Connect command
    Message bus->>+Service: Connect command
    Service->>-Message bus: PlayerConnected event
    Message bus->>Player: PlayerConnected event
    Player->>Message bus: JoinRoom command
    Message bus->>+Service: JoinRoom command
    Repository->>Service: fetch state
    Service->>Service: validate request
    Service->>Repository: update state
    Service->>-Message bus: PlayerJoinedRoom event
    Message bus->>Player: PlayerJoinedRoom event
```

The following diagram illustrates how two players (in the same room) can start playing together.  
Again, some components have been omitted for simplicity:

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
    Service->>+Game: play(state, StartMatch)
    Game->>-Service: (new state, MatchStarted)
    Service->>Repository: update state
    Service->>-Message bus: MatchStarted
    
    par
        Message bus->>Player1: MatchStarted
    and 
        Message bus->>Player2: MatchStarted
    and 
        Message bus->>+Service: MatchStarted
    end
        
    Repository->>Service: fetch state
    Service->>+Game: play(state, MatchStarted)
    Game->>-Service: (new state, [Act])
    Service->>Repository: update state
    Service->>-Message bus: Act

    par
        Message bus->>Player1: Act
    and
        Message bus->>Player2: Act
    end

    Player2->>Message bus: ShuffleDeck
    Message bus->>+Service: ShuffleDeck
    Repository->>Service: fetch state
    Service->>+Game: play(state, ShuffleDeck)
    Game->>-Service: (state, [DeckShuffled, Delayed(Continue)])
    Service->>Repository: update state
    Service->>Message bus: DeckShuffled
    Service--)-Message bus: Continue (delayed)
    
    par
        Message bus->>Player1: DeckShuffled
    and
        Message bus->>Player2: DeckShuffled
    and
        Message bus->>+Service: DeckShuffled
    end
    Repository->>Service: fetch state
    Service->>+Game: play(state, DeckShuffled)
    Game->>-Service: (new state, [])
    Service->>Repository: update state
    deactivate Service

    Message bus->>+Service: Continue
    Repository->>Service: fetch state
    Service->>+Game: play(state, Continue)
    Game->>-Service: (new state, BoardCardsDealt, Delayed(Continue))
    Service->>Repository: update state
    Service->>Message bus: BoardCardsDealt
    Service--)-Message bus: Continue (delayed)
```

### More in details

This project is _not_ (and might never become) production-ready.
It is indeed purely experimental, so feel free to draw inspiration from it, and use it at your own risk.

Please note that due to the experimental nature of this service,
all pieces of infrastructure (repository, message bus and message queue) have in-memory implementations only,
relying on cats effect and fs2.
