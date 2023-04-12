import {GameEvent} from "./event";
import {RoomSnapshot} from "./room";
import {User} from "./player";

export interface MessageIn {
    messageType: 'GameEvent' | 'Connected'
}

export interface GameEventMessage extends MessageIn {
    messageType: 'GameEvent',
    event: GameEvent
}

export interface Connected extends MessageIn {
    messageType: 'Connected',
    user: User
    room: RoomSnapshot
}

