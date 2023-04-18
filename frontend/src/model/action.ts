import {CardSuit} from "./card";
import {GameType} from "./gameType";

export interface PlayContext {
    gameType: GameType
}

export interface BriscolaPlayContext extends PlayContext {
    gameType: GameType.Briscola,
    trump: CardSuit
}

export interface TressettePlayContext extends PlayContext {
    gameType: GameType.Tressette,
    trump?: CardSuit
}

export interface ScopaPlayContext extends PlayContext {
    gameType: GameType.Scopa
}

export enum ActionType {
    PlayCard = 'PlayCard',
    ShuffleDeck = 'ShuffleDeck',
    Confirm = 'Confirm'
}

export interface Action {
    type: ActionType
}

export interface PlayCardAction extends Action {
    type: ActionType.PlayCard,
    context: PlayContext
}

export interface ShuffleDeckAction extends Action {
    type: ActionType.ShuffleDeck
}

export interface ConfirmAction extends Action {
    type: ActionType.Confirm
}
