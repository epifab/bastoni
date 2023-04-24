import {Card} from "../model/card";
import Point from "./Point";
import Size from "./Size";
import {CardStyle} from "./CardStyle";

export default interface CardLayout {
    card: Card,
    style?: CardStyle,
    width: number,
    topLeft: Point
}
