import React from 'react';
import './App.css';
import {fetchAuthToken} from "./clients/authClient";
import {GameClient, GameClientBuilder} from "./clients/gameClient";
import {Room, RoomId} from "./model/room";
import {authenticateMessage, connectMessage, joinTableMessage} from "./model/outboxMessage";
import Card from "./components/Card";
import {CardRank, CardSuit} from "./model/card";
import {CardStyle} from "./view/CardStyle";
import {Stage} from "@pixi/react";
import {PlayerState, UserId} from "./model/player";

async function connect() {
    const roomId: RoomId = 'ab24cf47-505a-4794-85a6-2866749eb4f5';
    const playerName = 'John Doe';
    const authToken = await fetchAuthToken(playerName);
    return new GameClientBuilder()
        .onReady((client) => client.send(authenticateMessage(authToken)))
        .onAuthenticated((user, client) => client.send(connectMessage))
        .onConnected((room, client) => client.send(joinTableMessage))
        .onPlayerJoinedTable((event) => console.log(`${event.user.name} joined the room`))
        .onPlayerLeftTable((event) => console.log(`${event.user.name} left the room`))
        .build(roomId);
}


function App() {

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
                rotation={2}
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
