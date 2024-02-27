import React, {useEffect, useState} from 'react';
import './App.css';
import {fetchAuthToken} from "bastoni/authClient";
import {GameClientBuilder} from "bastoni/gameClient";
import {authenticateMessage, connectMessage, joinTableMessage, OutboxMessage} from "bastoni/model/outboxMessage";
import Card from "./components/Card";
import {GameType} from "bastoni/model/gameType"
import {CardStyle} from "./view/CardStyle";
import {Stage} from "@pixi/react";
import {playAgainstComputer} from "bastoni"
import {Room, RoomId} from "bastoni/model/room"
import {CardSuit, CardRank} from "bastoni/model/card"
import {InboxMessage} from "bastoni/model/inboxMessage"

async function connect() {
  const roomId: RoomId = 'ab24cf47-505a-4794-85a6-2866749eb4f5';
  const playerName = 'John Doe';
  const authToken = await fetchAuthToken(playerName, 'localhost:9000', false);
  return new GameClientBuilder()
    .onReady((client) => client.send(authenticateMessage(authToken)))
    .onAuthenticated((user, client) => client.send(connectMessage))
    .onConnected((room, client) => client.send(joinTableMessage))
    .onPlayerJoinedTable((event) => console.log(`${event.user.name} joined the room`))
    .onPlayerLeftTable((event) => console.log(`${event.user.name} left the room`))
    .build(roomId);
}


export function App() {
  const [room, setRoom] = useState<Room | undefined>(undefined)
  const [controller, setController] = useState<(message: OutboxMessage) => void>((message: OutboxMessage) => {
  })

  useEffect(() => {
    const cleanup = playAgainstComputer(
      'Me',
      GameType.Briscola,
      (message: InboxMessage, room: Room) => {
        console.log(message)
        setRoom(room)
      },
      (sendMessage: (message: OutboxMessage) => void) => setController(sendMessage)
    )
    console.info("Game loaded")
    return () => {
      cleanup()
      console.info("Game unloaded")
    }
  }, []);


  // const [rotation, setRotation] = useState(0)
  //
  // useEffect(() => {
  //     setRotation(rotation + 0.0001)
  // })

  return (
    <Stage options={{antialias: true, autoDensity: false, resolution: 1}} width={window.innerWidth}
           height={window.innerHeight}>
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
    // <div className="App">
    //     <header className="App-header">
    //         <img src={logo} className="App-logo" alt="logo"/>
    //         <p>
    //             Edit <code>src/App.tsx</code> and save to reload.
    //         </p>
    //
    //     </header>
    // </div>
  );
}

export default App;
