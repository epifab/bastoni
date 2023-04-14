import {Checker, createCheckers} from "ts-interface-checker";
import actionTi from "./model/action-ti";
import cardTi from "./model/card-ti";
import eventTi from "./model/event-ti";
import gameTypeTi from "./model/gameType-ti";
import messageInTi from "./model/messageIn-ti";
import messageOutTi from "./model/messageOut-ti";
import playerTi from "./model/player-ti";
import roomTi from "./model/room-ti";
import scoreTi from "./model/score-ti";
import userTi from "./model/user-ti";

const checkers = createCheckers(
    actionTi,
    cardTi,
    eventTi,
    gameTypeTi,
    messageInTi,
    messageOutTi,
    playerTi,
    roomTi,
    scoreTi,
    userTi
)

export default function decodeJson<T>(typeName: string, json: any): T {
    if (checkers[typeName] === undefined) {
        throw new Error(`Type ${typeName} is not part of the model`);
    }
    checkers[typeName].check(json);
    return json as T;
}
