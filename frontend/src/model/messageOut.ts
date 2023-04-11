import {VisibleCard} from './card';
import {Game} from './game';

export interface ConnectMessage {
    messageType: 'Connect'
}

export const connectMessage: ConnectMessage = {
    messageType: 'Connect'
}

export interface JoinRoomMessage {
    messageType: 'JoinRoom'
}

export const joinRoomMessage: JoinRoomMessage = {
    messageType: 'JoinRoom'
}

export interface LeaveRoomMessage {
    messageType: 'LeaveRoom'
}

export const leaveRoomMessage: LeaveRoomMessage = {
    messageType: 'LeaveRoom'
}

export interface StartMatchMessage {
    messageType: 'StartMatch',
    gameType: Game
}

export function startMatchMessage(gameType: Game): StartMatchMessage {
    return {
        messageType: 'StartMatch',
        gameType
    }
}

export interface ShuffleDeckMessage {
    messageType: 'ShuffleDeck'
}

export const shuffleDeckMessage: ShuffleDeckMessage = {
    messageType: 'ShuffleDeck'
}

export interface OkMessage {
    messageType: 'Ok'
}

export const okMessage: OkMessage = {
    messageType: 'Ok'
}

export interface PlayCardMessage {
    messageType: 'PlayCard'
    card: VisibleCard
}

export function playCardMessage(card: VisibleCard): PlayCardMessage {
    return {
        messageType: 'PlayCard',
        card
    }
}

export interface TakeCardsMessage {
    messageType: 'TakeCards'
    card: VisibleCard,
    take: VisibleCard[]
}

export function takeCardsMessage(card: VisibleCard, take: VisibleCard[]): TakeCardsMessage {
    return {
        messageType: 'TakeCards',
        card,
        take
    }
}

export interface Pong {
    messageType: 'Pong'
}

export const pongMessage: Pong = {
    messageType: 'Pong'
}

export type MessageOut =
    ConnectMessage
    | JoinRoomMessage
    | LeaveRoomMessage
    | StartMatchMessage
    | ShuffleDeckMessage
    | OkMessage
    | PlayCardMessage
    | TakeCardsMessage
    | Pong
