import {Connected, GameEventMessage, MessageIn} from "./model/messageIn";
import {MessageOut, pongMessage} from "./model/messageOut";
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

interface ClientEventListeners {
    onConnected: ((snapshot: Room) => void),
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

const defaultListeners = {
    onConnected: defaultHandler,
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
    private listeners: ClientEventListeners = defaultListeners

    constructor(ws: WebSocket) {
        this.ws = ws;

        this.ws.onmessage = (event) => {
            this.onMessageIn(decodeJson<MessageIn>('MessageIn', JSON.parse(event.data)));
        };
        this.ws.onerror = (event) => {
            console.log(JSON.stringify(event.type));
        };
        this.ws.onclose = (event) => {
            console.log(JSON.stringify(`Connection closed: ${event.reason}`));
        };
    }

    send(message: MessageOut): GameClient {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
            return this;
        }
        else {
            throw new Error(`Not connected, status is: ${this.ws?.readyState ?? 'unknown'}`)
        }
    }

    onSnapshot(callback: ((snapshot: Room) => void)): GameClient {
        this.listeners.onConnected = callback;
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

    private onMessageIn: (message: MessageIn) => void = (message) => {
        switch (message.messageType) {
            case 'Connected':
                return this.listeners.onConnected(decodeJson<Connected>('Connected', message).room)
            case 'Ping':
                return this.send(pongMessage);
            case 'GameEvent':
                const event: GameEvent = decodeJson<GameEventMessage>('GameEventMessage', message).event;
                switch (event.eventType) {
                    case 'PlayerJoinedRoom':
                        return this.listeners.onPlayerJoinedRoom(decodeJson<PlayerJoinedRoom>('PlayerJoinedRoom', event));
                    case 'PlayerLeftRoom':
                        return this.listeners.onPlayerLeftRoom(decodeJson<PlayerLeftRoom>('PlayerLeftRoom', event));
                    case 'MatchStarted':
                        return this.listeners.onMatchStarted(decodeJson<MatchStarted>('MatchStarted', event));
                    case 'TrumpRevealed':
                        return this.listeners.onTrumpRevealed(decodeJson<TrumpRevealed>('TrumpRevealed', event));
                    case 'BoardCardsDealt':
                        return this.listeners.onBoardCardsDealt(decodeJson<BoardCardsDealt>('BoardCardsDealt', event));
                    case 'CardPlayed':
                        return this.listeners.onCardPlayed(decodeJson<CardPlayed>('CardPlayed', event));
                    case 'CardsTaken':
                        return this.listeners.onCardsTaken(decodeJson<CardsTaken>('CardsTaken', event));
                    case 'PlayerConfirmed':
                        return this.listeners.onPlayerConfirmed(decodeJson<PlayerConfirmed>('PlayerConfirmed', event));
                    case 'TimedOut':
                        return this.listeners.onTimedOut(decodeJson<TimedOut>('TimedOut', event));
                    case 'TrickCompleted':
                        return this.listeners.onTrickCompleted(decodeJson<TrickCompleted>('TrickCompleted', event));
                    case 'GameCompleted':
                        return this.listeners.onGameCompleted(decodeJson<GameCompleted>('GameCompleted', event));
                    case 'MatchCompleted':
                        return this.listeners.onMatchCompleted(decodeJson<MatchCompleted>('MatchCompleted', event));
                    case 'GameAborted':
                        return this.listeners.onGameAborted(decodeJson<GameAborted>('GameAborted', event));
                    case 'MatchAborted':
                        return this.listeners.onMatchAborted(decodeJson<MatchAborted>('MatchAborted', event));
                    case 'CardsDealt':
                        return this.listeners.onCardsDealt(decodeJson<CardsDealt>('CardsDealt', event));
                    case 'DeckShuffled':
                        return this.listeners.onDeckShuffled(decodeJson<DeckShuffled>('DeckShuffled', event));
                }
        }
    }
}

export function connect(roomId: RoomId, onConnect: (client: GameClient) => void, host: string = 'localhost:9090', secure: boolean = false) {
    const baseUrl = `${secure ? "wss" : "ws"}://${host}`
    let ws= new WebSocket(`${baseUrl}/play/${roomId}`);

    ws.onopen = () => {
        console.log("connected");
        onConnect(new GameClient(ws));
    };
}
