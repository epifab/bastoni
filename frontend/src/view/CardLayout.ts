import {Card} from "bastoni/model/card";
import Point from "./Point";
import {CardStyle} from "./CardStyle";
import Angle from "./Angle";

export default interface CardLayout {
    card: Card,
    width: number,
    topLeft: Point,
    rotation?: Angle,
    style?: CardStyle,
}
