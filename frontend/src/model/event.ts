import {User, UserId} from "./user";
import {Game} from "./game";
import {GameScore, MatchScore} from "./score";
import {Card, VisibleCard} from "./card";
import {Action} from "./action";

export interface PlayerJoinedRoom {
    eventType: 'PlayerJoinedRoom',
    user: User,
    seat: number
}

export interface PlayerLeftRoom {
    eventType: 'PlayerLeftRoom',
    user: User,
    seat: number
}

export interface MatchStarted {
    eventType: 'MatchStarted',
    gameType: Game,
    matchScores: MatchScore[]
}

export interface TrumpRevealed {
    eventType: 'TrumpRevealed',
    card: VisibleCard
}

export interface BoardCardsDealt {
    eventType: 'BoardCardsDealt',
    cards: VisibleCard[]
}

export interface CardPlayed {
    eventType: 'CardPlayed',
    playerId: UserId,
    card: VisibleCard
}

export interface CardsTaken {
    eventType: 'CardsTaken',
    playerId: UserId,
    card: VisibleCard,
    taken: VisibleCard[],
    scopa?: VisibleCard
}

export interface PlayerConfirmed {
    eventType: 'PlayerConfirmed',
    playerId: UserId
}

export interface TimedOut {
    eventType: 'TimedOut',
    playerId: UserId,
    action: Action
}

export interface TrickCompleted {
    eventType: 'TrickCompleted',
    winnerId: UserId
}

export interface GameCompleted {
    eventType: 'GameCompleted',
    scores: GameScore[],
    matchScores: MatchScore[]
}

export interface MatchCompleted {
    eventType: 'MatchCompleted',
    winnerIds: UserId[]
}

export interface GameAborted {
    eventType: 'GameAborted',
    reason: string
}

export interface MatchAborted {
    eventType: 'MatchAborted',
    reason: string
}

export interface CardsDealt {
    eventType: 'CardsDealt',
    cards: Card[]
}

export interface DeckShuffled {
    eventType: 'DeckShuffled',
    numberOfCards: number
}

export type GameEvent =
    PlayerJoinedRoom
    | PlayerLeftRoom
    | MatchStarted
    | TrumpRevealed
    | BoardCardsDealt
    | CardPlayed
    | CardsTaken
    | PlayerConfirmed
    | TimedOut
    | TrickCompleted
    | GameCompleted
    | MatchCompleted
    | GameAborted
    | MatchAborted
    | CardsDealt
    | DeckShuffled
