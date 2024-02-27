import {Sprite} from "@pixi/react";
import CardLayout from "../view/CardLayout";
import {isVisible} from "bastoni/model/card";
import napoletane from "../view/cards/napoletane/resources";
import piacentine from "../view/cards/piacentine/resources";
import {CardStyle} from "../view/CardStyle";
import {useEffect, useState} from "react";
import {Texture} from "pixi.js";
// import {DropShadowFilter} from '@pixi/filter-drop-shadow';

export default function Card(layout: CardLayout) {

    const [card, setCard] = useState<Texture>(null);
    const svgs = layout.style === CardStyle.Piacentine ? piacentine : napoletane;

    useEffect(() => {
        async function loadCard() {
            const uri = isVisible(layout.card) ? svgs[layout.card.suit][layout.card.rank] : './cards/retro.svg';
            setCard(await Texture.fromURL(uri));
        }

        card || loadCard();
    });

    return (
        card && <Sprite
            texture={card}
            x={layout.topLeft.x}
            y={layout.topLeft.y}
            scale={1}
            filters={[]}
            rotation={layout.rotation?.deg ?? 0}
            anchor={{x: 0, y: 0}}
        />
    )
}
