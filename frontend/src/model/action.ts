import {CardSuit} from "./card";

export interface BriscolaPlayContext {
    type: 'Briscola',
    trump: CardSuit
}

export interface TressettePlayContext {
    type: 'Tressette',
    trump?: CardSuit
}

export interface ScopaPlayContext {
    type: 'Scopa'
}

type PlayContext = BriscolaPlayContext | TressettePlayContext | ScopaPlayContext

export interface PlayCardAction {
    type: 'PlayCard',
    context: PlayContext
}

export interface ShuffleDeckAction {
    type: 'ShuffleDeck'
}

export interface ConfirmAction {
    type: 'Confirm'
}

export type Action = PlayCardAction | ShuffleDeckAction | ConfirmAction
