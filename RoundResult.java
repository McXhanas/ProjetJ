public class RoundResult {

    private final Card card1;
    private final Card card2;
    private final Player winner;
    private final String reason;

    public RoundResult(Card card1, Card card2, Player winner, String reason) {
        this.card1  = card1;
        this.card2  = card2;
        this.winner = winner;
        this.reason = reason;
    }

    public Card   getCard1()  { return card1; }
    public Card   getCard2()  { return card2; }
    public Player getWinner() { return winner; }
    public String getReason() { return reason; }
    public boolean isDraw()   { return winner == null; }

    @Override
    public String toString() {
        String result = isDraw()
            ? "Égalité !"
            : winner.getName() + " remporte ce round.";
        return String.format("%s  vs  %s  →  %s  (%s)", card1, card2, result, reason);
    }
}