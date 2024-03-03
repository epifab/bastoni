import {Sprite} from "@pixi/react";
import CardLayout from "../view/CardLayout";
import {isVisible} from "bastoni/model/card";
import napoletane from "../view/cards/napoletane";
import piacentine from "../view/cards/piacentine";
import retro from "../view/cards/retro.tsx";
import {CardStyle} from "../view/CardStyle";
import {useEffect, useState} from "react";
import {Texture} from "pixi.js";
// import {DropShadowFilter} from '@pixi/filter-drop-shadow';

export default function Card(layout: CardLayout) {

    const [card, setCard] = useState<Texture | undefined>(undefined);
    const svgs = layout.style === CardStyle.Piacentine ? piacentine : napoletane;

    useEffect(() => {
        async function loadCard() {
            const uri = isVisible(layout.card) ? svgs[layout.card.suit][layout.card.rank] : retro;
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
