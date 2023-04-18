import {Application, Sprite, Assets} from 'pixi.js';
import {gameClient} from "./gameClient";
import {authenticateMessage, connectMessage, joinRoomMessage} from "./model/outboxMessage";
import {AuthClient, fetchAuthToken} from './authClient'

const me = 'John Doe'
const room = '8757b48a-7c75-4d9c-a110-db1f12d46871'

connect();
pixijs();

async function connect() {
    const authToken = await fetchAuthToken(me);
    const client = gameClient(room);
    client
        .onReady(() => client.send(authenticateMessage(authToken)))
        .onAuthenticated((user) => client.send(connectMessage))
        .onConnected((room) => client.send(joinRoomMessage))
        .onPlayerJoinedTable((event) => console.log(`${event.user.name} joined the room`))
        .onPlayerLeftTable((event) => console.log(`${event.user.name} left the room`));
}

async function pixijs() {
    // The application will create a renderer using WebGL, if possible,
    // with a fallback to a canvas render. It will also setup the ticker
    // and the root stage PIXI.Container
    const app = new Application();

    // The application will create a canvas element for you that you
    // can then insert into the DOM
    document.body.appendChild(app.view as Node);

    // load the texture we need
    const texture = await Assets.load('/static/carte/retro.jpg');

    // This creates a texture from a 'bunny.png' image
    const bunny = new Sprite(texture);

    // Setup the position of the bunny
    bunny.x = app.renderer.width / 2;
    bunny.y = app.renderer.height / 2;

    // Rotate around the center
    bunny.anchor.x = 0.5;
    bunny.anchor.y = 0.5;

    // Add the bunny to the scene we are building
    app.stage.addChild(bunny);

    // Listen for frame updates
    app.ticker.add(() => {
        // each frame we spin the bunny around a bit
        bunny.rotation += 0.01;
    });

}


// const width = window.innerWidth;
// const height = window.innerHeight;
//
// const stage = new Konva.Stage({
//     container: 'container',
//     width: width,
//     height: height,
// });
//
// const layer = new Konva.Layer();
//
// const rect1 = new Konva.Rect({
//     x: 20,
//     y: 20,
//     width: 100,
//     height: 50,
//     fill: 'green',
//     stroke: 'black',
//     strokeWidth: 4,
// });
// // add the shape to the layer
// layer.add(rect1);
//
// const rect2 = new Konva.Rect({
//     x: 150,
//     y: 40,
//     width: 100,
//     height: 50,
//     fill: 'red',
//     shadowBlur: 10,
//     cornerRadius: 10,
// });
// layer.add(rect2);
//
// const rect3 = new Konva.Rect({
//     x: 50,
//     y: 120,
//     width: 100,
//     height: 100,
//     fill: 'blue',
//     cornerRadius: [0, 10, 20, 30],
// });
// layer.add(rect3);
//
// // add the layer to the stage
// stage.add(layer);
