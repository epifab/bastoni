import {useEffect, useState} from 'react';
import './App.css';
import {OutboxMessage, pongMessage} from "bastoni/model/outboxMessage";
import {GameRoom} from "./components/GameRoom";
import {GameType} from "bastoni/model/gameType"
import {playAgainstComputer} from "bastoni"
import {Room} from "bastoni/model/room"
import {InboxMessage} from "bastoni/model/inboxMessage"

// async function connect() {
//   const roomId: RoomId = 'ab24cf47-505a-4794-85a6-2866749eb4f5';
//   const playerName = 'John Doe';
//   const authToken = await fetchAuthToken(playerName, 'localhost:9000', false);
//   return new GameClientBuilder()
//     .onReady((client) => client.send(authenticateMessage(authToken)))
//     .onAuthenticated((user, client) => client.send(connectMessage))
//     .onConnected((room, client) => client.send(joinTableMessage))
//     .onPlayerJoinedTable((event) => console.log(`${event.user.name} joined the room`))
//     .onPlayerLeftTable((event) => console.log(`${event.user.name} left the room`))
//     .build(roomId);
// }


export function App() {
  const [room, setRoom] = useState<Room | undefined>(undefined)

  useEffect(() => {
    const cleanup: () => void = playAgainstComputer(
      'Me',
      GameType.Briscola,
      (message: InboxMessage, room?: Room) => {
        console.log(message)
        console.log(room)
        setRoom(room)
      },
      (sendMessage: (message: OutboxMessage) => void) => sendMessage(pongMessage)
    )
    console.info("Game loaded")
    return () => {
      console.info("Game unloaded")
      cleanup()
    }
  }, []);


  // const [rotation, setRotation] = useState(0)
  //
  // useEffect(() => {
  //     setRotation(rotation + 0.0001)
  // })

  return (
    <>
    {room ? <GameRoom room={room} /> : <p>loading</p>}
    </>
  );
}

export default App;
