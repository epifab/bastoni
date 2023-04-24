import denari01 from './denari/01.svg'
import denari02 from './denari/02.svg'
import denari03 from './denari/03.svg'
import denari04 from './denari/04.svg'
import denari05 from './denari/05.svg'
import denari06 from './denari/06.svg'
import denari07 from './denari/07.svg'
import denari08 from './denari/08.svg'
import denari09 from './denari/09.svg'
import denari10 from './denari/10.svg'
import coppe01 from './coppe/01.svg'
import coppe02 from './coppe/02.svg'
import coppe03 from './coppe/03.svg'
import coppe04 from './coppe/04.svg'
import coppe05 from './coppe/05.svg'
import coppe06 from './coppe/06.svg'
import coppe07 from './coppe/07.svg'
import coppe08 from './coppe/08.svg'
import coppe09 from './coppe/09.svg'
import coppe10 from './coppe/10.svg'
import spade01 from './spade/01.svg'
import spade02 from './spade/02.svg'
import spade03 from './spade/03.svg'
import spade04 from './spade/04.svg'
import spade05 from './spade/05.svg'
import spade06 from './spade/06.svg'
import spade07 from './spade/07.svg'
import spade08 from './spade/08.svg'
import spade09 from './spade/09.svg'
import spade10 from './spade/10.svg'
import bastoni01 from './bastoni/01.svg'
import bastoni02 from './bastoni/02.svg'
import bastoni03 from './bastoni/03.svg'
import bastoni04 from './bastoni/04.svg'
import bastoni05 from './bastoni/05.svg'
import bastoni06 from './bastoni/06.svg'
import bastoni07 from './bastoni/07.svg'
import bastoni08 from './bastoni/08.svg'
import bastoni09 from './bastoni/09.svg'
import bastoni10 from './bastoni/10.svg'
import {CardRank, CardSuit} from "../../model/card";

const cardsRecord: Record<CardSuit, Record<CardRank, string>> = {
    [CardSuit.Denari]: {
        [CardRank.Asso]: denari01,
        [CardRank.Due]: denari02,
        [CardRank.Tre]: denari03,
        [CardRank.Quattro]: denari04,
        [CardRank.Cinque]: denari05,
        [CardRank.Sei]: denari06,
        [CardRank.Sette]: denari07,
        [CardRank.Fante]: denari08,
        [CardRank.Cavallo]: denari09,
        [CardRank.Re]: denari10,
    },
    [CardSuit.Coppe]: {
        [CardRank.Asso]: coppe01,
        [CardRank.Due]: coppe02,
        [CardRank.Tre]: coppe03,
        [CardRank.Quattro]: coppe04,
        [CardRank.Cinque]: coppe05,
        [CardRank.Sei]: coppe06,
        [CardRank.Sette]: coppe07,
        [CardRank.Fante]: coppe08,
        [CardRank.Cavallo]: coppe09,
        [CardRank.Re]: coppe10,
    },
    [CardSuit.Spade]: {
        [CardRank.Asso]: spade01,
        [CardRank.Due]: spade02,
        [CardRank.Tre]: spade03,
        [CardRank.Quattro]: spade04,
        [CardRank.Cinque]: spade05,
        [CardRank.Sei]: spade06,
        [CardRank.Sette]: spade07,
        [CardRank.Fante]: spade08,
        [CardRank.Cavallo]: spade09,
        [CardRank.Re]: spade10,
    },
    [CardSuit.Bastoni]: {
        [CardRank.Asso]: bastoni01,
        [CardRank.Due]: bastoni02,
        [CardRank.Tre]: bastoni03,
        [CardRank.Quattro]: bastoni04,
        [CardRank.Cinque]: bastoni05,
        [CardRank.Sei]: bastoni06,
        [CardRank.Sette]: bastoni07,
        [CardRank.Fante]: bastoni08,
        [CardRank.Cavallo]: bastoni09,
        [CardRank.Re]: bastoni10,
    }
}

export default cardsRecord;
