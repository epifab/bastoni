import {CardSuit} from "./card";
import {GameType} from "./gameType";

export interface PlayContext {
    gameType: GameType
}

export interface BriscolaPlayContext extends PlayContext {
    gameType: 'Briscola',
    trump: CardSuit
}

export interface TressettePlayContext extends PlayContext {
    gameType: 'Tressette',
    trump?: CardSuit
}

export interface ScopaPlayContext extends PlayContext {
    gameType: 'Scopa'
}

export interface Action {
    type: 'PlayCard' | 'ShuffleDeck' | 'Confirm'
}

export interface PlayCardAction extends Action {
    type: 'PlayCard',
    context: PlayContext
}

export interface ShuffleDeckAction extends Action {
    type: 'ShuffleDeck'
}

export interface ConfirmAction extends Action {
    type: 'Confirm'
}
