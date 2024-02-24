import {OutboxMessage} from "./model/outboxMessage";
import {InboxMessage} from "./model/inboxMessage";

export function playAgainstComputer(
  playerName: string,
  gameType: string,
  callback: (message: InboxMessage) => void,
  onInt: (command: OutboxMessage) => void
): void
