import React from 'react';
import logo from './logo.svg';
import './App.css';
import {fetchAuthToken} from "./clients/authClient";
import {GameClientBuilder} from "./clients/gameClient";
import {RoomId} from "./model/room";
import {authenticateMessage, connectMessage, joinRoomMessage} from "./model/outboxMessage";

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

connect()

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <p>
          Edit <code>src/App.tsx</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      </header>
    </div>
  );
}

export default App;
