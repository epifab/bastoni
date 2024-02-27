import {Room} from "bastoni/model/room";
import CardLayout from "./CardLayout";
import {CardStyle} from "./CardStyle";
import {isActing, PlayerState} from "bastoni/model/player";

export default class RoomLayout {
    deck: CardLayout[]
    cardStyle: CardStyle = CardStyle.Napoletane

    constructor(room: Room) {
        this.deck = room.deck.map(card => {
            return {
                card,
                width: 0,
                topLeft: {
                    x: 0,
                    y: 0
                },
                rotation: {
                    deg: 0
                },
                style: this.cardStyle
            }
        })
        room.seats.map(seat => {
            if (seat.occupant) {
                if (isActing(seat.occupant)) {
                    
                }
            }
        })
    }
}