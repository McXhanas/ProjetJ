import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {

    private static final int MAX_SIZE = 5;
    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        if (cards.size() >= MAX_SIZE)
            throw new IllegalStateException("La main est pleine (max " + MAX_SIZE + " cartes).");
        cards.add(card);
    }

    public Card removeCard(int index) {
        if (index < 0 || index >= cards.size())
            throw new IndexOutOfBoundsException("Index invalide : " + index);
        return cards.remove(index);
    }

    public Card getCard(int index) { return cards.get(index); }
    public int  size()             { return cards.size(); }
    public boolean isEmpty()       { return cards.isEmpty(); }

    public List<Card> getCards()   { return Collections.unmodifiableList(cards); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++)
            sb.append(String.format("  [%d] %s%n", i, cards.get(i)));
        return sb.toString();
    }
}