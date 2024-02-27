import {OutboxMessage} from "./model/outboxMessage";
import {InboxMessage} from "./model/inboxMessage";
import {Room, RoomId} from "./model/room";
import {GameType} from "./model/gameType";

export function playAgainstComputer(
  playerName: string,
  gameType: GameType,
  onMesage: (message: InboxMessage, room: Room) => void,
  onInt: (sendMessage: (message: OutboxMessage) => void) => void
): () => void

export function playRemote(
  host: string,
  playerName: string,
  roomId: RoomId,
  roomSize: number,
  gameType: GameType,
  onMesage: (message: InboxMessage, room: Room) => void,
  onInt: (sendMessage: (message: OutboxMessage) => void) => void
): () => void
