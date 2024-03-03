import {CardRank, CardSuit} from "bastoni/model/card";

const cardsRecord: Record<CardSuit, Record<CardRank, string>> = {
    [CardSuit.Denari]: {
        [CardRank.Asso]: './cards/piacentine/denari/01.svg',
        [CardRank.Due]: './cards/piacentine/denari/02.svg',
        [CardRank.Tre]: './cards/piacentine/denari/03.svg',
        [CardRank.Quattro]: './cards/piacentine/denari/04.svg',
        [CardRank.Cinque]: './cards/piacentine/denari/05.svg',
        [CardRank.Sei]: './cards/piacentine/denari/06.svg',
        [CardRank.Sette]: './cards/piacentine/denari/07.svg',
        [CardRank.Fante]: './cards/piacentine/denari/08.svg',
        [CardRank.Cavallo]: './cards/piacentine/denari/09.svg',
        [CardRank.Re]: './cards/piacentine/denari/10.svg',
    },
    [CardSuit.Coppe]: {
        [CardRank.Asso]: './cards/piacentine/coppe/01.svg',
        [CardRank.Due]: './cards/piacentine/coppe/02.svg',
        [CardRank.Tre]: './cards/piacentine/coppe/03.svg',
        [CardRank.Quattro]: './cards/piacentine/coppe/04.svg',
        [CardRank.Cinque]: './cards/piacentine/coppe/05.svg',
        [CardRank.Sei]: './cards/piacentine/coppe/06.svg',
        [CardRank.Sette]: './cards/piacentine/coppe/07.svg',
        [CardRank.Fante]: './cards/piacentine/coppe/08.svg',
        [CardRank.Cavallo]: './cards/piacentine/coppe/09.svg',
        [CardRank.Re]: './cards/piacentine/coppe/10.svg',
    },
    [CardSuit.Spade]: {
        [CardRank.Asso]: './cards/piacentine/spade/01.svg',
        [CardRank.Due]: './cards/piacentine/spade/02.svg',
        [CardRank.Tre]: './cards/piacentine/spade/03.svg',
        [CardRank.Quattro]: './cards/piacentine/spade/04.svg',
        [CardRank.Cinque]: './cards/piacentine/spade/05.svg',
        [CardRank.Sei]: './cards/piacentine/spade/06.svg',
        [CardRank.Sette]: './cards/piacentine/spade/07.svg',
        [CardRank.Fante]: './cards/piacentine/spade/08.svg',
        [CardRank.Cavallo]: './cards/piacentine/spade/09.svg',
        [CardRank.Re]: './cards/piacentine/spade/10.svg',
    },
    [CardSuit.Bastoni]: {
        [CardRank.Asso]: './cards/piacentine/bastoni/01.svg',
        [CardRank.Due]: './cards/piacentine/bastoni/02.svg',
        [CardRank.Tre]: './cards/piacentine/bastoni/03.svg',
        [CardRank.Quattro]: './cards/piacentine/bastoni/04.svg',
        [CardRank.Cinque]: './cards/piacentine/bastoni/05.svg',
        [CardRank.Sei]: './cards/piacentine/bastoni/06.svg',
        [CardRank.Sette]: './cards/piacentine/bastoni/07.svg',
        [CardRank.Fante]: './cards/piacentine/bastoni/08.svg',
        [CardRank.Cavallo]: './cards/piacentine/bastoni/09.svg',
        [CardRank.Re]: './cards/piacentine/bastoni/10.svg',
    }
}

export default cardsRecord;
