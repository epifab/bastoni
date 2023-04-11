import {UserId} from "./user";
import {VisibleCard} from "./card";

export interface MatchScore {
    playerIds: UserId[],
    points: number
}

export interface BriscolaGameScoreItem {
    card: VisibleCard,
    points: number
}

export interface ScopaGameScoreItemCarte {
    type: 'Carte',
    count: number
}

export interface ScopaGameScoreItemDenari {
    type: 'Denari',
    count: number
}

export interface ScopaGameScoreItemPrimiera {
    type: 'Primiera',
    cards: VisibleCard[],
    count: number
}


export interface ScopaGameScoreItemSettebello {
    type: 'Settebello'
}

export interface ScopaGameScoreItemScope {
    type: 'Scope',
    count: number
}

export type ScopaGameScoreItem =
    ScopaGameScoreItemCarte
    | ScopaGameScoreItemDenari
    | ScopaGameScoreItemPrimiera
    | ScopaGameScoreItemSettebello
    | ScopaGameScoreItemScope

interface TressetteGameScoreItemCarta {
    type: 'Carta',
    thirds: 1 | 3
}

interface TressetteGameScoreItemRete {
    type: 'Rete'
}

export type TressetteGameScoreItem = TressetteGameScoreItemCarta | TressetteGameScoreItemRete

export interface GameScore {
    playerIds: UserId[],
    points: number,
    details: (BriscolaGameScoreItem | ScopaGameScoreItem | TressetteGameScoreItem)[]
}
