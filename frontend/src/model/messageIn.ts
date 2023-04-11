import {GameEvent} from "./event";

export interface GameEventMessage {
    messageType: 'GameEvent',
    event: GameEvent
}
