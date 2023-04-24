import {Sprite} from "@pixi/react";
import CardLayout from "../view/CardLayout";
import {isVisible} from "../model/card";
import napoletane from "../cards/napoletane/resources";
import piacentine from "../cards/piacentine/resources";
import back from "../cards/retro.svg";
import {Assets, Texture} from "pixi.js";
import {CardStyle} from "../view/CardStyle";
import {useEffect, useState} from "react";

export default function Card(layout: CardLayout) {

    const [card, setCard] = useState(null);
    const svgs = layout.style === CardStyle.Piacentine ? piacentine : napoletane;

    useEffect(() => {
        async function loadCard() {
            const uri = isVisible(layout.card) ? svgs[layout.card.suit][layout.card.rank] : back;
            console.log(uri);
            setCard(await Assets.load(uri));
        }

        card || loadCard();
    });

    return (
        card && <Sprite
            texture={card}
            x={layout.topLeft.x}
            y={layout.topLeft.y}
            scale={layout.width / 420}
        />
    )
}
