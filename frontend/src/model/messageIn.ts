import {GameEvent} from "./event";
import {Room} from "./room";
import {User} from "./player";

export interface MessageIn {
    messageType: 'GameEvent' | 'Connected' | 'Ping'
}

export interface GameEventMessage extends MessageIn {
    messageType: 'GameEvent',
    event: GameEvent
}

export interface Connected extends MessageIn {
    messageType: 'Connected',
    user: User
    room: Room
}

export interface Ping extends MessageIn {
    messageType: 'Ping'
}
