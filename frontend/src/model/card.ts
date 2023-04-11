export type CardRank = 'Asso' | 'Due' | 'Tre' | 'Quattro' | 'Cinque' | 'Sei' | 'Sette' | 'Fante' | 'Cavallo' | 'Re'
export type CardSuit = 'Denari' | 'Coppe' | 'Spade' | 'Bastoni'
export type CardId = number

export interface VisibleCard {
    rank: CardRank,
    suit: CardSuit,
    ref: CardId
}

export interface HiddenCard {
    ref: CardId
}

export type Card = VisibleCard | HiddenCard
