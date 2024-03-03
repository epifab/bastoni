import {
    Authenticated,
    Connected,
    Disconnected,
    GameEventMessage,
    InboxMessage,
    InboxMessageType
} from "./model/inboxMessage";
import {
    authenticateMessage,
    connectMessage,
    joinTableMessage,
    okMessage,
    OutboxMessage,
    playCardMessage,
    pongMessage,
    shuffleDeckMessage,
    startMatchMessage,
    takeCardsMessage
} from "./model/outboxMessage";
import {Room, RoomId} from "./model/room";
import {
    BoardCardsDealt,
    CardPlayed,
    CardsDealt,
    CardsTaken,
    DeckShuffled,
    GameAborted,
    GameCompleted,
    GameEvent,
    GameEventType,
    MatchAborted,
    MatchCompleted,
    MatchStarted,
    PlayerConfirmed,
    PlayerJoinedTable,
    PlayerLeftTable,
    TimedOut,
    TrickCompleted,
    TrumpRevealed
} from "./model/event";
import decodeJson from "./modelDecoder";
import {User} from "./model/player";
import {GameType} from "./model/gameType";
import {VisibleCard} from "./model/card";

function defaultHandler(event: any): void {
    console.log(event);
}

export class GameClient {
    private readonly ws: WebSocket

    constructor(ws: WebSocket) {
        this.ws = ws;
    }

    send(message: OutboxMessage): void {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        } else {
            throw new Error(`Not connected, status is: ${this.ws?.readyState ?? 'unknown'}`)
        }
    }

    connect(): void {
        this.send(connectMessage)
    }

    authenticate(authToken: string): void {
        this.send(authenticateMessage(authToken))
    }

    joinTable(): void {
        this.send(joinTableMessage)
    }

    startMatch(gameType: GameType): void {
        this.send(startMatchMessage(gameType))
    }

    shuffleDeck(): void {
        this.send(shuffleDeckMessage)
    }

    ok(): void {
        this.send(okMessage)
    }

    playCard(card: VisibleCard): void {
        this.send(playCardMessage(card))
    }

    takeCards(card: VisibleCard, take: VisibleCard[]): void {
        this.send(takeCardsMessage(card, take))
    }

    pong(): void {
        this.send(pongMessage)
    }
}

export class GameClientBuilder {
    ready: ((gameClient: GameClient) => void) = () => defaultHandler
    authenticated: ((user: User, gameClient: GameClient) => void) = defaultHandler
    connected: ((snapshot: Room, gameClient: GameClient) => void) = defaultHandler
    disconnected: ((reason: string, gameClient: GameClient) => void) = defaultHandler
    playerJoinedTable: ((event: PlayerJoinedTable, gameClient: GameClient) => void) = defaultHandler
    playerLeftTable: ((event: PlayerLeftTable, gameClient: GameClient) => void) = defaultHandler
    matchStarted: ((event: MatchStarted, gameClient: GameClient) => void) = defaultHandler
    trumpRevealed: ((event: TrumpRevealed, gameClient: GameClient) => void) = defaultHandler
    boardCardsDealt: ((event: BoardCardsDealt, gameClient: GameClient) => void) = defaultHandler
    cardPlayed: ((event: CardPlayed, gameClient: GameClient) => void) = defaultHandler
    cardsTaken: ((event: CardsTaken, gameClient: GameClient) => void) = defaultHandler
    playerConfirmed: ((event: PlayerConfirmed, gameClient: GameClient) => void) = defaultHandler
    timedOut: ((event: TimedOut, gameClient: GameClient) => void) = defaultHandler
    trickCompleted: ((event: TrickCompleted, gameClient: GameClient) => void) = defaultHandler
    gameCompleted: ((event: GameCompleted, gameClient: GameClient) => void) = defaultHandler
    matchCompleted: ((event: MatchCompleted, gameClient: GameClient) => void) = defaultHandler
    gameAborted: ((event: GameAborted, gameClient: GameClient) => void) = defaultHandler
    matchAborted: ((event: MatchAborted, gameClient: GameClient) => void) = defaultHandler
    cardsDealt: ((event: CardsDealt, gameClient: GameClient) => void) = defaultHandler
    deckShuffled: ((event: DeckShuffled, gameClient: GameClient) => void) = defaultHandler

    onReady(callback: ((client: GameClient) => void)): GameClientBuilder {
        this.ready = callback;
        return this;
    }

    onAuthenticated(callback: ((user: User, gameClient: GameClient) => void)): GameClientBuilder {
        this.authenticated = callback;
        return this;
    }

    onConnected(callback: ((snapshot: Room, gameClient: GameClient) => void)): GameClientBuilder {
        this.connected = callback;
        return this;
    }

    onDisconnected(callback: ((reason: string, gameClient: GameClient) => void)): GameClientBuilder {
        this.disconnected = callback;
        return this;
    }

    onPlayerJoinedTable(callback: ((event: PlayerJoinedTable, gameClient: GameClient) => void)): GameClientBuilder {
        this.playerJoinedTable = callback;
        return this;
    }

    onPlayerLeftTable(callback: ((event: PlayerLeftTable, gameClient: GameClient) => void)): GameClientBuilder {
        this.playerLeftTable = callback;
        return this;
    }

    onMatchStarted(callback: ((event: MatchStarted, gameClient: GameClient) => void)): GameClientBuilder {
        this.matchStarted = callback;
        return this;
    }

    onTrumpRevealed(callback: ((event: TrumpRevealed, gameClient: GameClient) => void)): GameClientBuilder {
        this.trumpRevealed = callback;
        return this;
    }

    onBoardCardsDealt(callback: ((event: BoardCardsDealt, gameClient: GameClient) => void)): GameClientBuilder {
        this.boardCardsDealt = callback;
        return this;
    }

    onCardPlayed(callback: ((event: CardPlayed, gameClient: GameClient) => void)): GameClientBuilder {
        this.cardPlayed = callback;
        return this;
    }

    onCardsTaken(callback: ((event: CardsTaken, gameClient: GameClient) => void)): GameClientBuilder {
        this.cardsTaken = callback;
        return this;
    }

    onPlayerConfirmed(callback: ((event: PlayerConfirmed, gameClient: GameClient) => void)): GameClientBuilder {
        this.playerConfirmed = callback;
        return this;
    }

    onTimedOut(callback: ((event: TimedOut, gameClient: GameClient) => void)): GameClientBuilder {
        this.timedOut = callback;
        return this;
    }

    onTrickCompleted(callback: ((event: TrickCompleted, gameClient: GameClient) => void)): GameClientBuilder {
        this.trickCompleted = callback;
        return this;
    }

    onGameCompleted(callback: ((event: GameCompleted, gameClient: GameClient) => void)): GameClientBuilder {
        this.gameCompleted = callback;
        return this;
    }

    onMatchCompleted(callback: ((event: MatchCompleted, gameClient: GameClient) => void)): GameClientBuilder {
        this.matchCompleted = callback;
        return this;
    }

    onGameAborted(callback: ((event: GameAborted, gameClient: GameClient) => void)): GameClientBuilder {
        this.gameAborted = callback;
        return this;
    }

    onMatchAborted(callback: ((event: MatchAborted, gameClient: GameClient) => void)): GameClientBuilder {
        this.matchAborted = callback;
        return this;
    }

    onCardsDealt(callback: ((event: CardsDealt, gameClient: GameClient) => void)): GameClientBuilder {
        this.cardsDealt = callback;
        return this;
    }

    onDeckShuffled(callback: ((event: DeckShuffled, gameClient: GameClient) => void)): GameClientBuilder {
        this.deckShuffled = callback;
        return this;
    }

    build(roomId: RoomId, host: string = 'localhost:9090', secure: boolean = false): GameClient {
        const baseUrl = `${secure ? "wss" : "ws"}://${host}`
        let ws = new WebSocket(`${baseUrl}/play/${roomId}`);
        const client = new GameClient(ws);

        const messageReceived: (message: InboxMessage) => void = (message) => {
            switch (message.messageType) {
                case InboxMessageType.Authenticated:
                    return this.authenticated(decodeJson<Authenticated>('Authenticated', message).user, client);
                case InboxMessageType.Connected:
                    return this.connected(decodeJson<Connected>('Connected', message).room, client);
                case InboxMessageType.Disconnected:
                    return this.disconnected(decodeJson<Disconnected>('Disconnected', message).reason, client);
                case InboxMessageType.Ping:
                    return client.send(pongMessage);
                case InboxMessageType.GameEvent:
                    const event: GameEvent = decodeJson<GameEventMessage>('GameEventMessage', message).event;
                    switch (event.eventType) {
                        case GameEventType.PlayerJoinedTable:
                            return this.playerJoinedTable(decodeJson<PlayerJoinedTable>('PlayerJoinedTable', event), client);
                        case GameEventType.PlayerLeftTable:
                            return this.playerLeftTable(decodeJson<PlayerLeftTable>('PlayerLeftTable', event), client);
                        case GameEventType.MatchStarted:
                            return this.matchStarted(decodeJson<MatchStarted>('MatchStarted', event), client);
                        case GameEventType.TrumpRevealed:
                            return this.trumpRevealed(decodeJson<TrumpRevealed>('TrumpRevealed', event), client);
                        case GameEventType.BoardCardsDealt:
                            return this.boardCardsDealt(decodeJson<BoardCardsDealt>('BoardCardsDealt', event), client);
                        case GameEventType.CardPlayed:
                            return this.cardPlayed(decodeJson<CardPlayed>('CardPlayed', event), client);
                        case GameEventType.CardsTaken:
                            return this.cardsTaken(decodeJson<CardsTaken>('CardsTaken', event), client);
                        case GameEventType.PlayerConfirmed:
                            return this.playerConfirmed(decodeJson<PlayerConfirmed>('PlayerConfirmed', event), client);
                        case GameEventType.TimedOut:
                            return this.timedOut(decodeJson<TimedOut>('TimedOut', event), client);
                        case GameEventType.TrickCompleted:
                            return this.trickCompleted(decodeJson<TrickCompleted>('TrickCompleted', event), client);
                        case GameEventType.GameCompleted:
                            return this.gameCompleted(decodeJson<GameCompleted>('GameCompleted', event), client);
                        case GameEventType.MatchCompleted:
                            return this.matchCompleted(decodeJson<MatchCompleted>('MatchCompleted', event), client);
                        case GameEventType.GameAborted:
                            return this.gameAborted(decodeJson<GameAborted>('GameAborted', event), client);
                        case GameEventType.MatchAborted:
                            return this.matchAborted(decodeJson<MatchAborted>('MatchAborted', event), client);
                        case GameEventType.CardsDealt:
                            return this.cardsDealt(decodeJson<CardsDealt>('CardsDealt', event), client);
                        case GameEventType.DeckShuffled:
                            return this.deckShuffled(decodeJson<DeckShuffled>('DeckShuffled', event), client);
                    }
            }
        }

        ws.onopen = () => {
            this.ready(client);
        };
        ws.onmessage = (event) => {
            messageReceived(decodeJson<InboxMessage>('InboxMessage', JSON.parse(event.data)));
        };
        ws.onerror = (event) => {
            console.error(JSON.stringify(event.type));
        };
        ws.onclose = () => {
            console.log(`Connection closed`);
        }

        return client;
    }
}
