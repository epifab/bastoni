import {Room} from "bastoni/model/room"
import {CardSuit, CardRank} from "bastoni/model/card"
import {Stage, Text} from "@pixi/react";
import Card from "./Card.tsx";
import {CardStyle} from "../view/CardStyle.ts";
import {TextStyle} from "pixi.js";

interface RoomProps {
  room: Room
}

export function GameRoom({room}: RoomProps) {
  return (
    <Stage options={{antialias: true, autoDensity: false, resolution: 1}}
           width={window.innerWidth}
           height={window.innerHeight}
    >
      <Text
        text={room.players[room.me]?.name}
        style={new TextStyle({fill: 0xFFFFFF, fontSize: 40})}
      />

      <Card
        card={{
          rank: CardRank.Tre,
          suit: CardSuit.Bastoni,
          ref: 3
        }}
        width={200}
        topLeft={{x: 0, y: 0}}
        style={CardStyle.Napoletane}
      />
      <Card
        card={{
          rank: CardRank.Asso,
          suit: CardSuit.Spade,
          ref: 3
        }}
        width={200}
        topLeft={{x: 900, y: 500}}
        style={CardStyle.Napoletane}
        rotation={{deg: 2}}
      />
      <Card
        card={{ref: 2}}
        width={200}
        topLeft={{x: 100, y: 200}}
        style={CardStyle.Napoletane}
      />
    </Stage>
  )
}