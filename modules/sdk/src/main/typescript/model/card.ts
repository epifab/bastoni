export enum CardRank {
    Asso = 'Asso',
    Due = 'Due',
    Tre = 'Tre',
    Quattro = 'Quattro',
    Cinque = 'Cinque',
    Sei = 'Sei',
    Sette = 'Sette',
    Fante = 'Fante',
    Cavallo = 'Cavallo',
    Re = 'Re',
}

export enum CardSuit {
    Denari = 'Denari',
    Coppe = 'Coppe',
    Spade = 'Spade',
    Bastoni = 'Bastoni',
}

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

export function isVisible(card: Card): card is VisibleCard {
    return (card as VisibleCard).rank !== undefined;
}
