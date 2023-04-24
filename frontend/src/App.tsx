import React from 'react';
import logo from './logo.svg';
import './App.css';
import {fetchAuthToken} from "./clients/authClient";
import {GameClientBuilder} from "./clients/gameClient";
import {RoomId} from "./model/room";
import {authenticateMessage, connectMessage, joinRoomMessage} from "./model/outboxMessage";
import Card from "./components/Card";
import {CardRank, CardSuit} from "./model/card";
import {CardStyle} from "./view/CardStyle";
import {Stage} from "@pixi/react";

async function connect() {
    const roomId: RoomId = 'ab24cf47-505a-4794-85a6-2866749eb4f5';
    const playerName = 'John Doe';
    const authToken = await fetchAuthToken(playerName);
    return new GameClientBuilder()
        .onReady((client) => client.send(authenticateMessage(authToken)))
        .onAuthenticated((user, client) => client.send(connectMessage))
        .onConnected((room, client) => client.send(joinRoomMessage))
        .onPlayerJoinedTable((event) => console.log(`${event.user.name} joined the room`))
        .onPlayerLeftTable((event) => console.log(`${event.user.name} left the room`))
        .build(roomId);
}

// connect()

function App() {
    return (
        <Stage width={window.innerWidth} height={window.innerHeight}>
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
                    rank: CardRank.Quattro,
                    suit: CardSuit.Denari,
                    ref: 3
                }}
                width={200}
                topLeft={{x: 200, y: 10}}
                style={CardStyle.Napoletane}
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
