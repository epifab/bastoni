import {Action} from "./action";

export type UserId = string

export interface User {
    id: UserId
    name: string
}

export interface Player extends User {
    points: number
}

export interface PlayerState {
    state: 'SittingOut' | 'SittingIn' | 'Waiting' | 'Acting' | 'EndOfGame' | 'EndOfMatch'
    player: User
}

export interface SittingOutPlayer extends PlayerState {
    state: 'SittingOut',
    player: User
}

export interface SittingInPlayer extends PlayerState {
    state: 'SittingIn',
    player: Player
}

export interface WaitingPlayer extends PlayerState {
    state: 'Waiting',
    player: Player
}

export interface ActingPlayer extends PlayerState {
    state: 'Acting',
    player: Player,
    action: Action,
    timeout: number
}

export interface EndOfGamePlayer extends PlayerState {
    state: 'EndOfGame',
    player: Player,
    points: number,
    winner: boolean
}

export interface EndOfMatchPlayer extends PlayerState {
    state: 'EndOfMatch',
    player: Player,
    winner: boolean
}
