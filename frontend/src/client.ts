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
                this.notifyConnected((message as Connected).room)
                break;
            case 'GameEvent':
                const event: GameEvent = (message as GameEventMessage).event;
                switch (event.eventType) {
                    case 'PlayerJoinedRoom':
                        this.notifyPlayerJoinRoom(event as PlayerJoinedRoom)
                        break;
                    case 'PlayerLeftRoom':
                        this.notifyPlayerLeftRoom(event as PlayerLeftRoom)
                        break;
                    case 'MatchStarted':
                        this.notifyMatchStarted(event as MatchStarted)
                        break;
                    case 'TrumpRevealed':
                        this.notifyTrumpRevealed(event as TrumpRevealed)
                        break;
                    case 'BoardCardsDealt':
                        this.notifyBoardCardsDealt(event as BoardCardsDealt)
                        break;
                    case 'CardPlayed':
                        this.notifyCardPlayed(event as CardPlayed)
                        break;
                    case 'CardsTaken':
                        this.notifyCardsTaken(event as CardsTaken)
                        break;
                    case 'PlayerConfirmed':
                        this.notifyPlayerConfirmed(event as PlayerConfirmed)
                        break;
                    case 'TimedOut':
                        this.notifyTimedOut(event as TimedOut)
                        break;
                    case 'TrickCompleted':
                        this.notifyTrickCompleted(event as TrickCompleted)
                        break;
                    case 'GameCompleted':
                        this.notifyGameCompleted(event as GameCompleted)
                        break;
                    case 'MatchCompleted':
                        this.notifyMatchCompleted(event as MatchCompleted)
                        break;
                    case 'GameAborted':
                        this.notifyGameAborted(event as GameAborted)
                        break;
                    case 'MatchAborted':
                        this.notifyMatchAborted(event as MatchAborted)
                        break;
                    case 'CardsDealt':
                        this.notifyCardsDealt(event as CardsDealt)
                        break;
                    case 'DeckShuffled':
                        this.notifyDeckShuffled(event as DeckShuffled)
                        break;
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
