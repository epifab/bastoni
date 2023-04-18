import {
    Authenticated,
    Connected,
    Disconnected,
    GameEventMessage,
    InboxMessage,
    InboxMessageType
} from "./model/inboxMessage";
import {OutboxMessage, pongMessage} from "./model/outboxMessage";
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
    PlayerJoinedRoom,
    PlayerLeftRoom,
    TimedOut,
    TrickCompleted,
    TrumpRevealed
} from "./model/event";
import decodeJson from "./modelDecoder";
import {User} from "./model/player";

interface ClientEventListeners {
    onReady: (() => void),
    onAuthenticated: ((user: User) => void),
    onConnected: ((snapshot: Room) => void),
    onDisconnected: ((reason: string) => void),
    onPlayerJoinedRoom: ((event: PlayerJoinedRoom) => void),
    onPlayerLeftRoom: ((event: PlayerLeftRoom) => void),
    onMatchStarted: ((event: MatchStarted) => void),
    onTrumpRevealed: ((event: TrumpRevealed) => void),
    onBoardCardsDealt: ((event: BoardCardsDealt) => void),
    onCardPlayed: ((event: CardPlayed) => void),
    onCardsTaken: ((event: CardsTaken) => void),
    onPlayerConfirmed: ((event: PlayerConfirmed) => void),
    onTimedOut: ((event: TimedOut) => void),
    onTrickCompleted: ((event: TrickCompleted) => void),
    onGameCompleted: ((event: GameCompleted) => void),
    onMatchCompleted: ((event: MatchCompleted) => void),
    onGameAborted: ((event: GameAborted) => void),
    onMatchAborted: ((event: MatchAborted) => void),
    onCardsDealt: ((event: CardsDealt) => void),
    onDeckShuffled: ((event: DeckShuffled) => void)
}

function defaultHandler(event: any): void {
    console.log(event);
}

const defaultListeners: ClientEventListeners = {
    onReady: () => console.log('Connected to the remote server'),
    onAuthenticated: defaultHandler,
    onConnected: defaultHandler,
    onDisconnected: defaultHandler,
    onPlayerJoinedRoom: defaultHandler,
    onPlayerLeftRoom: defaultHandler,
    onMatchStarted: defaultHandler,
    onTrumpRevealed: defaultHandler,
    onBoardCardsDealt: defaultHandler,
    onCardPlayed: defaultHandler,
    onCardsTaken: defaultHandler,
    onPlayerConfirmed: defaultHandler,
    onTimedOut: defaultHandler,
    onTrickCompleted: defaultHandler,
    onGameCompleted: defaultHandler,
    onMatchCompleted: defaultHandler,
    onGameAborted: defaultHandler,
    onMatchAborted: defaultHandler,
    onCardsDealt: defaultHandler,
    onDeckShuffled: defaultHandler,
}

export class GameClient {
    private readonly ws: WebSocket
    private readonly listeners: ClientEventListeners = defaultListeners
    private authenticated?: User

    constructor(ws: WebSocket) {
        this.ws = ws;

        this.ws.onopen = (event) => {
            this.listeners.onReady();
        };
        this.ws.onmessage = (event) => {
            this.onMessageReceived(decodeJson<InboxMessage>('InboxMessage', JSON.parse(event.data)));
        };
        this.ws.onerror = (event) => {
            console.error(JSON.stringify(event.type));
        };
        this.ws.onclose = (event) => {
            console.log(`Connection closed`);
        };
    }

    ready(): boolean {
        return this.authenticated !== undefined;
    }

    send(message: OutboxMessage): GameClient {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
            return this;
        }
        else {
            throw new Error(`Not connected, status is: ${this.ws?.readyState ?? 'unknown'}`)
        }
    }

    onReady(callback: () => void): GameClient {
        this.listeners.onReady = callback;
        return this;
    }

    onAuthenticated(callback: ((user: User) => void)): GameClient {
        this.listeners.onAuthenticated = callback;
        return this;
    }

    onConnected(callback: ((snapshot: Room) => void)): GameClient {
        this.listeners.onConnected = callback;
        return this;
    }

    onDisconnected(callback: ((reason: string) => void)): GameClient {
        this.listeners.onDisconnected = callback;
        return this;
    }

    onPlayerJoinedRoom(callback: ((event: PlayerJoinedRoom) => void)): GameClient {
        this.listeners.onPlayerJoinedRoom = callback;
        return this;
    }

    onPlayerLeftRoom(callback: ((event: PlayerLeftRoom) => void)): GameClient {
        this.listeners.onPlayerLeftRoom = callback;
        return this;
    }

    onMatchStarted(callback: ((event: MatchStarted) => void)): GameClient {
        this.listeners.onMatchStarted = callback;
        return this;
    }

    onTrumpRevealed(callback: ((event: TrumpRevealed) => void)): GameClient {
        this.listeners.onTrumpRevealed = callback;
        return this;
    }

    onBoardCardsDealt(callback: ((event: BoardCardsDealt) => void)): GameClient {
        this.listeners.onBoardCardsDealt = callback;
        return this;
    }

    onCardPlayed(callback: ((event: CardPlayed) => void)): GameClient {
        this.listeners.onCardPlayed = callback;
        return this;
    }

    onCardsTaken(callback: ((event: CardsTaken) => void)): GameClient {
        this.listeners.onCardsTaken = callback;
        return this;
    }

    onPlayerConfirmed(callback: ((event: PlayerConfirmed) => void)): GameClient {
        this.listeners.onPlayerConfirmed = callback;
        return this;
    }

    onTimedOut(callback: ((event: TimedOut) => void)): GameClient {
        this.listeners.onTimedOut = callback;
        return this;
    }

    onTrickCompleted(callback: ((event: TrickCompleted) => void)): GameClient {
        this.listeners.onTrickCompleted = callback;
        return this;
    }

    onGameCompleted(callback: ((event: GameCompleted) => void)): GameClient {
        this.listeners.onGameCompleted = callback;
        return this;
    }

    onMatchCompleted(callback: ((event: MatchCompleted) => void)): GameClient {
        this.listeners.onMatchCompleted = callback;
        return this;
    }

    onGameAborted(callback: ((event: GameAborted) => void)): GameClient {
        this.listeners.onGameAborted = callback;
        return this;
    }

    onMatchAborted(callback: ((event: MatchAborted) => void)): GameClient {
        this.listeners.onMatchAborted = callback;
        return this;
    }

    onCardsDealt(callback: ((event: CardsDealt) => void)): GameClient {
        this.listeners.onCardsDealt = callback;
        return this;
    }

    onDeckShuffled(callback: ((event: DeckShuffled) => void)): GameClient {
        this.listeners.onDeckShuffled = callback;
        return this;
    }

    private onMessageReceived: (message: InboxMessage) => void = (message) => {
        switch (message.messageType) {
            case InboxMessageType.Authenticated:
                return this.listeners.onAuthenticated(decodeJson<Authenticated>('Authenticated', message).user);
            case InboxMessageType.Connected:
                return this.listeners.onConnected(decodeJson<Connected>('Connected', message).room);
            case InboxMessageType.Disconnected:
                return this.listeners.onDisconnected(decodeJson<Disconnected>('Disconnected', message).reason);
            case InboxMessageType.Ping:
                return this.send(pongMessage);
            case InboxMessageType.GameEvent:
                const event: GameEvent = decodeJson<GameEventMessage>('GameEventMessage', message).event;
                switch (event.eventType) {
                    case GameEventType.PlayerJoinedRoom:
                        return this.listeners.onPlayerJoinedRoom(decodeJson<PlayerJoinedRoom>('PlayerJoinedRoom', event));
                    case GameEventType.PlayerLeftRoom:
                        return this.listeners.onPlayerLeftRoom(decodeJson<PlayerLeftRoom>('PlayerLeftRoom', event));
                    case GameEventType.MatchStarted:
                        return this.listeners.onMatchStarted(decodeJson<MatchStarted>('MatchStarted', event));
                    case GameEventType.TrumpRevealed:
                        return this.listeners.onTrumpRevealed(decodeJson<TrumpRevealed>('TrumpRevealed', event));
                    case GameEventType.BoardCardsDealt:
                        return this.listeners.onBoardCardsDealt(decodeJson<BoardCardsDealt>('BoardCardsDealt', event));
                    case GameEventType.CardPlayed:
                        return this.listeners.onCardPlayed(decodeJson<CardPlayed>('CardPlayed', event));
                    case GameEventType.CardsTaken:
                        return this.listeners.onCardsTaken(decodeJson<CardsTaken>('CardsTaken', event));
                    case GameEventType.PlayerConfirmed:
                        return this.listeners.onPlayerConfirmed(decodeJson<PlayerConfirmed>('PlayerConfirmed', event));
                    case GameEventType.TimedOut:
                        return this.listeners.onTimedOut(decodeJson<TimedOut>('TimedOut', event));
                    case GameEventType.TrickCompleted:
                        return this.listeners.onTrickCompleted(decodeJson<TrickCompleted>('TrickCompleted', event));
                    case GameEventType.GameCompleted:
                        return this.listeners.onGameCompleted(decodeJson<GameCompleted>('GameCompleted', event));
                    case GameEventType.MatchCompleted:
                        return this.listeners.onMatchCompleted(decodeJson<MatchCompleted>('MatchCompleted', event));
                    case GameEventType.GameAborted:
                        return this.listeners.onGameAborted(decodeJson<GameAborted>('GameAborted', event));
                    case GameEventType.MatchAborted:
                        return this.listeners.onMatchAborted(decodeJson<MatchAborted>('MatchAborted', event));
                    case GameEventType.CardsDealt:
                        return this.listeners.onCardsDealt(decodeJson<CardsDealt>('CardsDealt', event));
                    case GameEventType.DeckShuffled:
                        return this.listeners.onDeckShuffled(decodeJson<DeckShuffled>('DeckShuffled', event));
                }
        }
    }
}

export function gameClient(roomId: RoomId, host: string = 'localhost:9090', secure: boolean = false) {
    const baseUrl = `${secure ? "wss" : "ws"}://${host}`
    let ws= new WebSocket(`${baseUrl}/play/${roomId}`);
    return new GameClient(ws);
}
