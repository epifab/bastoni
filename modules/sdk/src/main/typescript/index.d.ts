import {OutboxMessage} from "./model/outboxMessage";
import {InboxMessage} from "./model/inboxMessage";
import {Room} from "./model/room";
import {GameType} from "./model/gameType";

export function playAgainstComputer(
  playerName: string,
  gameType: GameType,
  onMesage: (message: InboxMessage, room?: Room) => void,
  onInt: (sendMessage: (message: OutboxMessage) => void) => void
): () => void
