import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameController {

    private static final int CARDS_PER_PLAYER = 5;

    private final Player player1;
    private final Player player2;
    private final Deck deck;
    private final CombatResolver resolver;
    private final GameView view;
    private int currentRound = 1;

    public GameController(GameView view)  {
        this.view     = view;
        this.resolver = new CombatResolver();

        String name1 = view.askPlayerName(1);
        String name2 = view.askPlayerName(2);

        this.player1 = new Player(name1);
        this.player2 = new Player(name2);
        try {
            this.deck = new Deck("cards.txt");
        } catch (java.io.IOException e) {
            throw new RuntimeException("Impossible de charger cards.txt : " + e.getMessage(), e);
        }
    }

    public void run() {
        view.showWelcome();
        view.showMatchup(player1.getName(), player2.getName());
        dealCards();

        while (!isOver()) {
            view.showRoundHeader(currentRound, CARDS_PER_PLAYER);
            playRound();
            currentRound++;
        }

        view.showFinalResult(player1, player2);
        view.close();
    }

    private void dealCards() {
        for (int i = 0; i < CARDS_PER_PLAYER; i++) {
            player1.drawCard(deck);
            player2.drawCard(deck);
        }
    }

    private void playRound() {
        Card c1 = pickCard(player1);
        Card c2 = pickCard(player2);
        RoundResult result = resolver.resolve(c1, c2, player1, player2);
        if (!result.isDraw()) result.getWinner().addPoint();
        view.showRoundResult(result);
        view.showScores(player1, player2);
    }

    private Card pickCard(Player player) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < player.getHand().size(); i++)
            ids.add(player.getHand().getCard(i).getPokedexId());

        view.showHand(player.getName(), player.getHand(), ids);
        int maxIndex = player.getHand().size() - 1;
        int index = -1;

        while (index < 0 || index > maxIndex) {
            index = view.askCardChoice(player.getName(), maxIndex);
            if (index < 0 || index > maxIndex)
                view.showError("Index invalide, choisissez entre 0 et " + maxIndex + ".");
        }
        return player.playCard(index);
    }

    private boolean isOver() { return currentRound > CARDS_PER_PLAYER; }
}