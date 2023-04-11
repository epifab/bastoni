import Konva from 'konva';
import { VisibleCard } from './model/card';

const card: VisibleCard = {
    rank: 'Asso',
    suit: 'Denari',
    ref: 1
}

const width = window.innerWidth;
const height = window.innerHeight;

const stage = new Konva.Stage({
    container: 'container',
    width: width,
    height: height,
});

const layer = new Konva.Layer();

const rect1 = new Konva.Rect({
    x: 20,
    y: 20,
    width: 100,
    height: 50,
    fill: 'green',
    stroke: 'black',
    strokeWidth: 4,
});
// add the shape to the layer
layer.add(rect1);

const rect2 = new Konva.Rect({
    x: 150,
    y: 40,
    width: 100,
    height: 50,
    fill: 'red',
    shadowBlur: 10,
    cornerRadius: 10,
});
layer.add(rect2);

const rect3 = new Konva.Rect({
    x: 50,
    y: 120,
    width: 100,
    height: 100,
    fill: 'blue',
    cornerRadius: [0, 10, 20, 30],
});
layer.add(rect3);

// add the layer to the stage
stage.add(layer);
