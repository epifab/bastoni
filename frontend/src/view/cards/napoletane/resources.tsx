import {CardRank, CardSuit} from "bastoni/model/card";

const cardsRecord: Record<CardSuit, Record<CardRank, string>> = {
    [CardSuit.Denari]: {
        [CardRank.Asso]: './cards/napoletane/denari/01.svg',
        [CardRank.Due]: './cards/napoletane/denari/02.svg',
        [CardRank.Tre]: './cards/napoletane/denari/03.svg',
        [CardRank.Quattro]: './cards/napoletane/denari/04.svg',
        [CardRank.Cinque]: './cards/napoletane/denari/05.svg',
        [CardRank.Sei]: './cards/napoletane/denari/06.svg',
        [CardRank.Sette]: './cards/napoletane/denari/07.svg',
        [CardRank.Fante]: './cards/napoletane/denari/08.svg',
        [CardRank.Cavallo]: './cards/napoletane/denari/09.svg',
        [CardRank.Re]: './cards/napoletane/denari/10.svg',
    },
    [CardSuit.Coppe]: {
        [CardRank.Asso]: './cards/napoletane/coppe/01.svg',
        [CardRank.Due]: './cards/napoletane/coppe/02.svg',
        [CardRank.Tre]: './cards/napoletane/coppe/03.svg',
        [CardRank.Quattro]: './cards/napoletane/coppe/04.svg',
        [CardRank.Cinque]: './cards/napoletane/coppe/05.svg',
        [CardRank.Sei]: './cards/napoletane/coppe/06.svg',
        [CardRank.Sette]: './cards/napoletane/coppe/07.svg',
        [CardRank.Fante]: './cards/napoletane/coppe/08.svg',
        [CardRank.Cavallo]: './cards/napoletane/coppe/09.svg',
        [CardRank.Re]: './cards/napoletane/coppe/10.svg',
    },
    [CardSuit.Spade]: {
        [CardRank.Asso]: './cards/napoletane/spade/01.svg',
        [CardRank.Due]: './cards/napoletane/spade/02.svg',
        [CardRank.Tre]: './cards/napoletane/spade/03.svg',
        [CardRank.Quattro]: './cards/napoletane/spade/04.svg',
        [CardRank.Cinque]: './cards/napoletane/spade/05.svg',
        [CardRank.Sei]: './cards/napoletane/spade/06.svg',
        [CardRank.Sette]: './cards/napoletane/spade/07.svg',
        [CardRank.Fante]: './cards/napoletane/spade/08.svg',
        [CardRank.Cavallo]: './cards/napoletane/spade/09.svg',
        [CardRank.Re]: './cards/napoletane/spade/10.svg',
    },
    [CardSuit.Bastoni]: {
        [CardRank.Asso]: './cards/napoletane/bastoni.01.svg',
        [CardRank.Due]: './cards/napoletane/bastoni.02.svg',
        [CardRank.Tre]: './cards/napoletane/bastoni.03.svg',
        [CardRank.Quattro]: './cards/napoletane/bastoni.04.svg',
        [CardRank.Cinque]: './cards/napoletane/bastoni.05.svg',
        [CardRank.Sei]: './cards/napoletane/bastoni.06.svg',
        [CardRank.Sette]: './cards/napoletane/bastoni.07.svg',
        [CardRank.Fante]: './cards/napoletane/bastoni.08.svg',
        [CardRank.Cavallo]: './cards/napoletane/bastoni.09.svg',
        [CardRank.Re]: './cards/napoletane/bastoni.10.svg',
    }
}

export default cardsRecord;
