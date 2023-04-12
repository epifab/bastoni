import {GameEventMessage, MessageIn, Connected} from "./model/messageIn";
import {MessageOut} from "./model/messageOut";
import {RoomSnapshot} from "./model/room";
import {
    BoardCardsDealt,
    CardPlayed, CardsDealt, CardsTaken, DeckShuffled, GameAborted, GameCompleted,
    GameEvent, MatchAborted, MatchCompleted,
    MatchStarted, PlayerConfirmed,
    PlayerJoinedRoom,
    PlayerLeftRoom, TimedOut, TrickCompleted,
    TrumpRevealed
} from "./model/event";

import messageInTi from "./model/messageIn-ti";
import eventTi from "./model/event-ti";
import {Checker, createCheckers} from "ts-interface-checker";

const {
    GameEventMessage,
    Connected
} = createCheckers(messageInTi)

const {
    PlayerJoinRoom,
    PlayerLeftRoom,
    MatchStarted,
    TrumpRevealed,
    BoardCardsDealt,
    CardPlayed,
    CardsTaken,
    PlayerConfirmed,
    TimedOut,
    TrickCompleted,
    GameCompleted,
    MatchCompleted,
    GameAborted,
    MatchAborted,
    CardsDealt,
    DeckShuffled,
} = createCheckers(eventTi)

function parseJson<T>(checker: Checker, message: any): T {
    checker.check(message)
    return message as T
}

export class Client {
    private default: ((event: any) => void) = (event) => console.log(event)

    private notifyConnected: ((snapshot: RoomSnapshot) => void) = this.default
    private notifyPlayerJoinRoom: ((event: PlayerJoinedRoom) => void) = this.default
    private notifyPlayerLeftRoom: ((event: PlayerLeftRoom) => void) = this.default
    private notifyMatchStarted: ((event: MatchStarted) => void) = this.default
    private notifyTrumpRevealed: ((event: TrumpRevealed) => void) = this.default
    private notifyBoardCardsDealt: ((event: BoardCardsDealt) => void) = this.default
    private notifyCardPlayed: ((event: CardPlayed) => void) = this.default
    private notifyCardsTaken: ((event: CardsTaken) => void) = this.default
    private notifyPlayerConfirmed: ((event: PlayerConfirmed) => void) = this.default
    private notifyTimedOut: ((event: TimedOut) => void) = this.default
    private notifyTrickCompleted: ((event: TrickCompleted) => void) = this.default
    private notifyGameCompleted: ((event: GameCompleted) => void) = this.default
    private notifyMatchCompleted: ((event: MatchCompleted) => void) = this.default
    private notifyGameAborted: ((event: GameAborted) => void) = this.default
    private notifyMatchAborted: ((event: MatchAborted) => void) = this.default
    private notifyCardsDealt: ((event: CardsDealt) => void) = this.default
    private notifyDeckShuffled: ((event: DeckShuffled) => void) = this.default

    onSnapshot(callback: ((snapshot: RoomSnapshot) => void)) {
        this.notifyConnected = callback
    }

    onPlayerJoinRoom(callback: ((event: PlayerJoinedRoom) => void)) {
        this.notifyPlayerJoinRoom = callback
    }

    onPlayerLeftRoom(callback: ((event: PlayerLeftRoom) => void)) {
        this.notifyPlayerLeftRoom = callback
    }

    onMatchStarted(callback: ((event: MatchStarted) => void)) {
        this.notifyMatchStarted = callback
    }

    onTrumpRevealed(callback: ((event: TrumpRevealed) => void)) {
        this.notifyTrumpRevealed = callback
    }

    onBoardCardsDealt(callback: ((event: BoardCardsDealt) => void)) {
        this.notifyBoardCardsDealt = callback
    }

    onCardPlayed(callback: ((event: CardPlayed) => void)) {
        this.notifyCardPlayed = callback
    }

    onCardsTaken(callback: ((event: CardsTaken) => void)) {
        this.notifyCardsTaken = callback
    }

    onPlayerConfirmed(callback: ((event: PlayerConfirmed) => void)) {
        this.notifyPlayerConfirmed = callback
    }

    onTimedOut(callback: ((event: TimedOut) => void)) {
        this.notifyTimedOut = callback
    }

    onTrickCompleted(callback: ((event: TrickCompleted) => void)) {
        this.notifyTrickCompleted = callback
    }

    onGameCompleted(callback: ((event: GameCompleted) => void)) {
        this.notifyGameCompleted = callback
    }

    onMatchCompleted(callback: ((event: MatchCompleted) => void)) {
        this.notifyMatchCompleted = callback
    }

    onGameAborted(callback: ((event: GameAborted) => void)) {
        this.notifyGameAborted = callback
    }

    onMatchAborted(callback: ((event: MatchAborted) => void)) {
        this.notifyMatchAborted = callback
    }

    onCardsDealt(callback: ((event: CardsDealt) => void)) {
        this.notifyCardsDealt = callback
    }

    onDeckShuffled(callback: ((event: DeckShuffled) => void)) {
        this.notifyDeckShuffled = callback
    }

    inbox: (message: MessageIn) => void = (message) => {
        switch (message.messageType) {
            case 'Connected':
                return this.notifyConnected(parseJson<Connected>(Connected, message).room)
            case 'GameEvent':
                const event: GameEvent = parseJson<GameEvent>(createCheckers(messageInTi).GameEvent, message);
                switch (event.eventType) {
                    case 'PlayerJoinedRoom':
                        return this.notifyPlayerJoinRoom(parseJson<PlayerJoinedRoom>(PlayerJoinRoom, event));
                    case 'PlayerLeftRoom':
                        return this.notifyPlayerLeftRoom(parseJson<PlayerLeftRoom>(PlayerLeftRoom, event));
                    case 'MatchStarted':
                        return this.notifyMatchStarted(parseJson<MatchStarted>(MatchStarted, event));
                    case 'TrumpRevealed':
                        return this.notifyTrumpRevealed(parseJson<TrumpRevealed>(TrumpRevealed, event));
                    case 'BoardCardsDealt':
                        return this.notifyBoardCardsDealt(parseJson<BoardCardsDealt>(BoardCardsDealt, event));
                    case 'CardPlayed':
                        return this.notifyCardPlayed(parseJson<CardPlayed>(CardPlayed, event));
                    case 'CardsTaken':
                        return this.notifyCardsTaken(parseJson<CardsTaken>(CardsTaken, event));
                    case 'PlayerConfirmed':
                        return this.notifyPlayerConfirmed(parseJson<PlayerConfirmed>(PlayerConfirmed, event));
                    case 'TimedOut':
                        return this.notifyTimedOut(parseJson<TimedOut>(TimedOut, event));
                    case 'TrickCompleted':
                        return this.notifyTrickCompleted(parseJson<TrickCompleted>(TrickCompleted, event));
                    case 'GameCompleted':
                        return this.notifyGameCompleted(parseJson<GameCompleted>(GameCompleted, event));
                    case 'MatchCompleted':
                        return this.notifyMatchCompleted(parseJson<MatchCompleted>(MatchCompleted, event));
                    case 'GameAborted':
                        return this.notifyGameAborted(parseJson<GameAborted>(GameAborted, event));
                    case 'MatchAborted':
                        return this.notifyMatchAborted(parseJson<MatchAborted>(MatchAborted, event));
                    case 'CardsDealt':
                        return this.notifyCardsDealt(parseJson<CardsDealt>(CardsDealt, event));
                    case 'DeckShuffled':
                        return this.notifyDeckShuffled(parseJson<DeckShuffled>(DeckShuffled, event));
                }
                break;
        }
    }
    ws: WebSocket

    constructor(secure: boolean) {
        const protocol = secure ? "wss" : "ws"
        const url = `${protocol}://localhost:9090`

        this.ws = new WebSocket(url)

        this.ws.onopen = () => {
            console.log("connected");
        };
        this.ws.onmessage = (event) => {
            if (event.data && event.data.messageType) {
                this.inbox(event.data as MessageIn);
            }
            else {
                console.error("Unprocessable message", event.data)
            }
        };
        this.ws.onerror = (event) => {
            console.log(JSON.stringify(event.type));
        };
        this.ws.onclose = (event) => {
            console.log(JSON.stringify(`Connection closed: ${event.reason}`));
        };
    }

    send(message: MessageOut): void {
        this.ws.send(JSON.stringify(message))
    }
}
