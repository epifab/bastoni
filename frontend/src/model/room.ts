import {PlayerState, UserId} from "./player";
import {Card} from "./card";
import {GameType} from "./gameType";
import {GameScore, MatchScore} from "./score";

export interface Seat {
    index: number,
    hand: Card[],
    taken: Card[],
    takenBy?: PlayerState
}

interface BoardCard {
    card: Card,
    playedBy?: UserId
}

interface MatchInfo {
    gameType: GameType,
    matchScore: MatchScore[],
    gameScore?: GameScore[]
}

export type RoomId = string

export interface Room {
    me: UserId,
    seats: Seat[],
    deck: Card[],
    board: BoardCard[],
    matchInfo: MatchInfo | null | undefined,
    dealerIndex: number | null | undefined,
    players: { [id: UserId]: PlayerState }
}
