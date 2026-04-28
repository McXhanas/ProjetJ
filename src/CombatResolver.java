public class CombatResolver {

    public RoundResult resolve(Card c1, Card c2, Player p1, Player p2) {
        int elementAdvantage = c1.getElement().advantageAgainst(c2.getElement());

        if (elementAdvantage == 1) {
            return new RoundResult(c1, c2, p1,
                c1.getElement() + " bat " + c2.getElement());
        }
        if (elementAdvantage == -1) {
            return new RoundResult(c1, c2, p2,
                c2.getElement() + " bat " + c1.getElement());
        }

        int cmp = Integer.compare(c1.getValue(), c2.getValue());
        if (cmp > 0) return new RoundResult(c1, c2, p1,
            "valeur " + c1.getValue() + " > " + c2.getValue());
        if (cmp < 0) return new RoundResult(c1, c2, p2,
            "valeur " + c2.getValue() + " > " + c1.getValue());

        return new RoundResult(c1, c2, null, "valeurs et éléments identiques");
    }
}