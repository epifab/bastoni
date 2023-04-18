import {GameEvent} from "./event";
import {Room} from "./room";
import {User} from "./player";

export enum InboxMessageType {
    Authenticated = 'Authenticated',
    Connected = 'Connected',
    Disconnected = 'Disconnected',
    GameEvent = 'GameEvent',
    Ping = 'Ping'
}

export interface InboxMessage {
    messageType: InboxMessageType
}

export interface Authenticated extends InboxMessage {
    messageType: InboxMessageType.Authenticated,
    user: User
}

export interface Connected extends InboxMessage {
    messageType: InboxMessageType.Connected,
    room: Room
}

export interface Disconnected extends InboxMessage {
    messageType: InboxMessageType.Disconnected,
    reason: string
}

export interface GameEventMessage extends InboxMessage {
    messageType: InboxMessageType.GameEvent,
    event: GameEvent
}

export interface Ping extends InboxMessage {
    messageType: InboxMessageType.Ping
}
