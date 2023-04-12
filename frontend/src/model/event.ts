import {User, UserId} from "./player";
import {GameType} from "./gameType";
import {GameScore, MatchScore} from "./score";
import {Card, VisibleCard} from "./card";
import {Action} from "./action";

export interface GameEvent {
    eventType:
        'PlayerJoinedRoom'
        | 'PlayerLeftRoom'
        | 'MatchStarted'
        | 'TrumpRevealed'
        | 'BoardCardsDealt'
        | 'CardPlayed'
        | 'CardsTaken'
        | 'PlayerConfirmed'
        | 'TimedOut'
        | 'TrickCompleted'
        | 'GameCompleted'
        | 'MatchCompleted'
        | 'GameAborted'
        | 'MatchAborted'
        | 'CardsDealt'
        | 'DeckShuffled'
}

export interface PlayerJoinedRoom extends GameEvent {
    eventType: 'PlayerJoinedRoom',
    user: User,
    seat: number
}

export interface PlayerLeftRoom extends GameEvent {
    eventType: 'PlayerLeftRoom',
    user: User,
    seat: number
}

export interface MatchStarted extends GameEvent {
    eventType: 'MatchStarted',
    gameType: GameType,
    matchScores: MatchScore[]
}

export interface TrumpRevealed extends GameEvent {
    eventType: 'TrumpRevealed',
    card: VisibleCard
}

export interface BoardCardsDealt extends GameEvent{
    eventType: 'BoardCardsDealt',
    cards: VisibleCard[]
}

export interface CardPlayed extends GameEvent {
    eventType: 'CardPlayed',
    playerId: UserId,
    card: VisibleCard
}

export interface CardsTaken extends GameEvent {
    eventType: 'CardsTaken',
    playerId: UserId,
    card: VisibleCard,
    taken: VisibleCard[],
    scopa?: VisibleCard
}

export interface PlayerConfirmed extends GameEvent {
    eventType: 'PlayerConfirmed',
    playerId: UserId
}

export interface TimedOut extends GameEvent {
    eventType: 'TimedOut',
    playerId: UserId,
    action: Action
}

export interface TrickCompleted extends GameEvent {
    eventType: 'TrickCompleted',
    winnerId: UserId
}

export interface GameCompleted extends GameEvent {
    eventType: 'GameCompleted',
    scores: GameScore[],
    matchScores: MatchScore[]
}

export interface MatchCompleted extends GameEvent {
    eventType: 'MatchCompleted',
    winnerIds: UserId[]
}

export interface GameAborted extends GameEvent {
    eventType: 'GameAborted',
    reason: string
}

export interface MatchAborted extends GameEvent {
    eventType: 'MatchAborted',
    reason: string
}

export interface CardsDealt extends GameEvent {
    eventType: 'CardsDealt',
    cards: Card[]
}

export interface DeckShuffled extends GameEvent {
    eventType: 'DeckShuffled',
    numberOfCards: number
}
