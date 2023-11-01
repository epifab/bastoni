import {Sprite} from "@pixi/react";
import CardLayout from "../view/CardLayout";
import {isVisible} from "../model/card";
import napoletane from "../view/cards/napoletane/resources";
import piacentine from "../view/cards/piacentine/resources";
import back from "../view/cards/retro.svg";
import {Assets} from "pixi.js";
import {CardStyle} from "../view/CardStyle";
import {useEffect, useState} from "react";
import {DropShadowFilter} from '@pixi/filter-drop-shadow';

export default function Card(layout: CardLayout) {

    const [card, setCard] = useState(null);
    const svgs = layout.style === CardStyle.Piacentine ? piacentine : napoletane;

    useEffect(() => {
        async function loadCard() {
            const uri = isVisible(layout.card) ? svgs[layout.card.suit][layout.card.rank] : back;
            setCard(await Assets.load(uri));
        }

        card || loadCard();
    });

    return (
        card && <Sprite
            texture={card}
            x={layout.topLeft.x}
            y={layout.topLeft.y}
            scale={1}
            filters={[new DropShadowFilter()]}
            rotation={layout.rotation ?? 0}
            anchor={{x: 0, y: 0}}
        />
    )
}
