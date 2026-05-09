

import java.io.IOException;
import java.util.*;

public class Deck {

    private final Deque<Card>    cards = new ArrayDeque<>();
    private final CardRepository repository;

    public Deck(String filePath) throws IOException {
        this.repository = new CardRepository(filePath);
        List<Card> loaded = repository.loadRandom();
        cards.addAll(loaded);
        shuffle();
    }

    public void shuffle() {
        List<Card> list = new ArrayList<>(cards);
        Collections.shuffle(list);
        cards.clear();
        cards.addAll(list);
    }

    public Card draw() {
        if (isEmpty()) throw new IllegalStateException("Le deck est vide.");
        return cards.poll();
    }

    /** Numéros de lignes dans cards.txt des cartes de ce deck. */
    public List<Integer> getSelectedLineNumbers() {
        return repository.getSelectedLineNumbers();
    }

    public boolean isEmpty() { return cards.isEmpty(); }
    public int     size()    { return cards.size(); }
}