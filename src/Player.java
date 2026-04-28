

public class Player {

    private final String name;
    private final Hand hand;
    private int score;

    public Player(String name) {
        this.name  = name;
        this.hand  = new Hand();
        this.score = 0;
    }

    public void drawCard(Deck deck) {
        hand.addCard(deck.draw());
    }

    public Card playCard(int index) {
        return hand.removeCard(index);
    }

    public void addPoint() { score++; }

    public String getName()  { return name; }
    public int    getScore() { return score; }
    public Hand   getHand()  { return hand; }

    @Override
    public String toString() {
        return String.format("%s (score : %d)", name, score);
    }
}