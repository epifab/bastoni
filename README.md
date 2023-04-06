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
    actor Player
    participant Game controller
    participant Message bus
    participant Game service
    participant Repository

    note over Player, Repository: The Player submits a Connect command to the controller
    Player-)+Game controller: Connect
    Game controller->>-Message bus: Connect
    Message bus->>+Game service: Connect
    Repository->>Game service: fetch state
    Game service->>-Message bus: PlayerConnected
    Message bus->>+Game controller: PlayerConnected
    Game controller->>-Player: RoomSnapshot
    note over Player, Repository: The frontend will display the current view of the room<br/>The player submits a JoinRoom command to the controller
    Player-)+Game controller: JoinRoom
    Game controller->>-Message bus: JoinRoom
    Message bus->>+Game service: JoinRoom
    Repository->>Game service: fetch state
    Game service->>Game service: validate request
    Game service->>Repository: update state
    Game service->>-Message bus: PlayerJoinedRoom
    Message bus->>+Game controller: PlayerJoinedRoom
    Game controller->>-Player: GameEvent(PlayerJoinedRoom)
    note over Player, Repository: The frontend will display the current player in the room
```

The following diagram illustrates how two players (in the same room) can start playing together.  
Again, some components have been omitted for simplicity:

```mermaid
sequenceDiagram
    actor Player1
    actor Player2
    participant Message bus
    participant Game service
    participant Repository
    participant Game
    
    note over Player1, Game: Player1 submits a StartMatch command to the controller
    Player1-)Message bus: StartMatch
    Message bus->>+Game service: StartMatch
    Repository->>Game service: fetch state
    Game service->>+Game: play(state, StartMatch)
    Game->>-Game service: (new state, MatchStarted)
    Game service->>Repository: update state
    Game service->>-Message bus: MatchStarted

    note over Player1, Game: A MatchStarted event is broadcast to all players connected to the room as well as the Game service
    par
        Message bus->>Player1: MatchStarted
    and 
        Message bus->>Player2: MatchStarted
    and 
        Message bus->>+Game service: MatchStarted
    end
        
    Repository->>Game service: fetch state
    Game service->>+Game: play(state, MatchStarted)
    Game->>-Game service: (new state, [Act])
    Game service->>Repository: update state
    Game service->>-Message bus: Act

    note over Player1, Game: An Act command (including the target player and the requested action)<br/> is issued by the Game service and broadcast to all players
    par
        Message bus->>Player1: Act
    and
        Message bus->>Player2: Act
    end

    note over Player1, Game: Player2 submits a ShuffleDeck command to the controller
    Player2->>Message bus: ShuffleDeck
    Message bus->>+Game service: ShuffleDeck
    Repository->>Game service: fetch state
    Game service->>+Game: play(state, ShuffleDeck)
    Game->>-Game service: (state, [DeckShuffled, Delayed(Continue)])
    Game service->>Repository: update state
    Game service->>Message bus: DeckShuffled
    Game service->>-Game service: schedule delayed submission of a Continue command

    note over Player1, Game: A DeckShuffled event is broadcast
    par
        Message bus->>Player1: DeckShuffled
    and
        Message bus->>Player2: DeckShuffled
    and
        Message bus->>+Game service: DeckShuffled
    end
    Repository->>Game service: fetch state
    Game service->>+Game: play(state, DeckShuffled)
    Game->>-Game service: (new state, [])
    Game service->>Repository: update state
    deactivate Game service

    note over Player1, Game: A Continue command that was previously scheduled is now issued
    Game service-)Message bus: Continue (received)
    Message bus->>+Game service: Continue
    Repository->>Game service: fetch state
    Game service->>+Game: play(state, Continue)
    Game->>-Game service: (new state, BoardCardsDealt, Delayed(Continue))
    Game service->>Repository: update state
    Game service->>Message bus: BoardCardsDealt
    note over Player1, Game: A BoardCardsDealt event is issued and will be received by the various frontend to display the cards
    Game service->>-Game service: schedule delayed submission of a Continue command
```

### More in details

This project is _not_ (and might never become) production-ready.
It is indeed purely experimental, so feel free to draw inspiration from it, and use it at your own risk.

Please note that due to the experimental nature of this service,
all pieces of infrastructure (repository, message bus and message queue) have in-memory implementations only,
relying on cats effect and fs2.
