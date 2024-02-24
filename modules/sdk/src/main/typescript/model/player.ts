import {Action} from "./action";

export type UserId = string

export interface User {
    id: UserId
    name: string
}

export interface Player extends User {
    points: number
}

export enum PlayerState {
    SittingOut = 'SittingOut',
    Playing = 'Playing',
    Waiting = 'Waiting',
    Acting = 'Acting',
    EndOfGame = 'EndOfGame',
    EndOfMatch = 'EndOfMatch'
}

export interface SittingOutPlayer {
    state: PlayerState.SittingOut,
    player: User
}

export interface PlayingPlayer {
    state: PlayerState.Playing,
    player: Player
}

export interface WaitingPlayer {
    state: PlayerState.Waiting,
    player: Player
}

export interface ActingPlayer {
    state: PlayerState.Acting,
    player: Player,
    action: Action,
    timeout: number
}

export interface EndOfGamePlayer {
    state: PlayerState.EndOfGame,
    player: Player,
    points: number,
    winner: boolean
}

export interface EndOfMatchPlayer {
    state: PlayerState.EndOfMatch,
    player: Player,
    winner: boolean
}

export type PlayerContext =
    SittingOutPlayer
    | PlayingPlayer
    | WaitingPlayer
    | ActingPlayer
    | EndOfGamePlayer
    | EndOfMatchPlayer

export function isActing(player: PlayerContext): player is ActingPlayer {
    return player.state == PlayerState.Acting
}