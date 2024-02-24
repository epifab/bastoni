import {User, UserId} from "./player";
import {GameType} from "./gameType";
import {GameScore, MatchScore} from "./score";
import {Card, VisibleCard} from "./card";
import {Action} from "./action";

export enum GameEventType {
    PlayerJoinedTable = 'PlayerJoinedTable',
    PlayerLeftTable = 'PlayerLeftTable',
    MatchStarted = 'MatchStarted',
    TrumpRevealed = 'TrumpRevealed',
    BoardCardsDealt = 'BoardCardsDealt',
    CardPlayed = 'CardPlayed',
    CardsTaken = 'CardsTaken',
    PlayerConfirmed = 'PlayerConfirmed',
    TimedOut = 'TimedOut',
    TrickCompleted = 'TrickCompleted',
    GameCompleted = 'GameCompleted',
    MatchCompleted = 'MatchCompleted',
    GameAborted = 'GameAborted',
    MatchAborted = 'MatchAborted',
    CardsDealt = 'CardsDealt',
    DeckShuffled = 'DeckShuffled',
}

export interface GameEvent {
    eventType: GameEventType
}

export interface PlayerJoinedTable extends GameEvent {
    eventType: GameEventType.PlayerJoinedTable,
    user: User,
    seat: number
}

export interface PlayerLeftTable extends GameEvent {
    eventType: GameEventType.PlayerLeftTable,
    user: User,
    seat: number
}

export interface MatchStarted extends GameEvent {
    eventType: GameEventType.MatchStarted,
    gameType: GameType,
    matchScores: MatchScore[]
}

export interface TrumpRevealed extends GameEvent {
    eventType: GameEventType.TrumpRevealed,
    card: VisibleCard
}

export interface BoardCardsDealt extends GameEvent{
    eventType: GameEventType.BoardCardsDealt,
    cards: VisibleCard[]
}

export interface CardPlayed extends GameEvent {
    eventType: GameEventType.CardPlayed,
    playerId: UserId,
    card: VisibleCard
}

export interface CardsTaken extends GameEvent {
    eventType: GameEventType.CardsTaken,
    playerId: UserId,
    card: VisibleCard,
    taken: VisibleCard[],
    scopa?: VisibleCard
}

export interface PlayerConfirmed extends GameEvent {
    eventType: GameEventType.PlayerConfirmed,
    playerId: UserId
}

export interface TimedOut extends GameEvent {
    eventType: GameEventType.TimedOut,
    playerId: UserId,
    action: Action
}

export interface TrickCompleted extends GameEvent {
    eventType: GameEventType.TrickCompleted,
    winnerId: UserId
}

export interface GameCompleted extends GameEvent {
    eventType: GameEventType.GameCompleted,
    scores: GameScore[],
    matchScores: MatchScore[]
}

export interface MatchCompleted extends GameEvent {
    eventType: GameEventType.MatchCompleted,
    winnerIds: UserId[]
}

export interface GameAborted extends GameEvent {
    eventType: GameEventType.GameAborted,
    reason: string
}

export interface MatchAborted extends GameEvent {
    eventType: GameEventType.MatchAborted,
    reason: string
}

export interface CardsDealt extends GameEvent {
    eventType: GameEventType.CardsDealt,
    cards: Card[]
}

export interface DeckShuffled extends GameEvent {
    eventType: GameEventType.DeckShuffled,
    numberOfCards: number
}
