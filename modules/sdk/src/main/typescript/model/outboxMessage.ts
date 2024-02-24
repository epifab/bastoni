import {VisibleCard} from './card';
import {GameType} from './gameType';

export enum OutboxMessageType {
    Authenticate = 'Authenticate',
    Connect = 'Connect',
    JoinTable = 'JoinTable',
    LeaveTable = 'LeaveTable',
    StartMatch = 'StartMatch',
    ShuffleDeck = 'ShuffleDeck',
    Ok = 'Ok',
    PlayCard = 'PlayCard',
    TakeCards = 'TakeCards',
    Pong = 'Pong'
}

export interface OutboxMessage {
    messageType: OutboxMessageType
}

export interface AuthenticateMessage extends OutboxMessage {
    messageType: OutboxMessageType.Authenticate,
    authToken: string
}

export function authenticateMessage(authToken: string): AuthenticateMessage {
    return {
        messageType: OutboxMessageType.Authenticate,
        authToken
    }
}

export interface ConnectMessage extends OutboxMessage {
    messageType: OutboxMessageType.Connect
}

export const connectMessage: ConnectMessage = {
    messageType: OutboxMessageType.Connect
}

export interface JoinTableMessage extends OutboxMessage {
    messageType: OutboxMessageType.JoinTable
}

export const joinTableMessage: JoinTableMessage = {
    messageType: OutboxMessageType.JoinTable
}

export interface LeaveTableMessage extends OutboxMessage {
    messageType: OutboxMessageType.LeaveTable
}

export const leaveTableMessage: LeaveTableMessage = {
    messageType: OutboxMessageType.LeaveTable
}

export interface StartMatchMessage extends OutboxMessage {
    messageType: OutboxMessageType.StartMatch,
    gameType: GameType
}

export function startMatchMessage(gameType: GameType): StartMatchMessage {
    return {
        messageType: OutboxMessageType.StartMatch,
        gameType
    }
}

export interface ShuffleDeckMessage extends OutboxMessage {
    messageType: OutboxMessageType.ShuffleDeck
}

export const shuffleDeckMessage: ShuffleDeckMessage = {
    messageType: OutboxMessageType.ShuffleDeck
}

export interface OkMessage extends OutboxMessage {
    messageType: OutboxMessageType.Ok
}

export const okMessage: OkMessage = {
    messageType: OutboxMessageType.Ok
}

export interface PlayCardMessage extends OutboxMessage {
    messageType: OutboxMessageType.PlayCard
    card: VisibleCard
}

export function playCardMessage(card: VisibleCard): PlayCardMessage {
    return {
        messageType: OutboxMessageType.PlayCard,
        card
    }
}

export interface TakeCardsMessage extends OutboxMessage {
    messageType: OutboxMessageType.TakeCards
    card: VisibleCard,
    take: VisibleCard[]
}

export function takeCardsMessage(card: VisibleCard, take: VisibleCard[]): TakeCardsMessage {
    return {
        messageType: OutboxMessageType.TakeCards,
        card,
        take
    }
}

export interface Pong extends OutboxMessage {
    messageType: OutboxMessageType.Pong
}

export const pongMessage: Pong = {
    messageType: OutboxMessageType.Pong
}
