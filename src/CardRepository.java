import java.io.*;
import java.util.*;

public class CardRepository {

    private static final int CARDS_TO_PICK = 20;
    private final String filePath;
    private final List<Integer> selectedLineNumbers = new ArrayList<>();

    public CardRepository(String filePath) {
        this.filePath = filePath;
    }

    public List<Card> loadRandom() throws IOException {

        // 1. Lire toutes les lignes valides
        List<int[]>    lineNumbers = new ArrayList<>();
        List<String[]> allEntries  = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String raw;
            int lineNum = 0;
            while ((raw = reader.readLine()) != null) {
                lineNum++;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(";");
                if (parts.length != 4) {
                    System.err.println("Ligne " + lineNum + " ignorée : " + line);
                    continue;
                }
                lineNumbers.add(new int[]{lineNum, allEntries.size()});
                allEntries.add(parts);
            }
        }

        if (allEntries.size() < CARDS_TO_PICK)
            throw new IOException("Pas assez de cartes : " + allEntries.size()
                + " disponibles, " + CARDS_TO_PICK + " requis.");

        // 2. Tirer 20 indices aléatoires sans répétition
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < allEntries.size(); i++) indices.add(i);
        Collections.shuffle(indices);
        List<Integer> picked = indices.subList(0, CARDS_TO_PICK);

        // 3. Instancier cartes + mémoriser numéros de lignes
        selectedLineNumbers.clear();
        List<Card> cards = new ArrayList<>();
        Map<Integer, Integer> indexToLine = new HashMap<>();
        for (int[] pair : lineNumbers) indexToLine.put(pair[1], pair[0]);

        for (int idx : picked) {
            String[] parts = allEntries.get(idx);
            try {
                int     pokedexId = Integer.parseInt(parts[0].trim());
                String  name      = parts[1].trim();
                int     value     = Integer.parseInt(parts[2].trim());
                Element element   = Element.valueOf(parts[3].trim().toUpperCase());
                cards.add(new Card(pokedexId, name, value, element));
                selectedLineNumbers.add(indexToLine.get(idx));
            } catch (Exception e) {
                System.err.println("Entrée ignorée : " + Arrays.toString(parts));
            }
        }

        System.out.println("Cartes chargées : " + cards.size()
            + " | Lignes : " + selectedLineNumbers);
        return cards;
    }

    /** Numéros de lignes (1-indexé) des cartes sélectionnées. */
    public List<Integer> getSelectedLineNumbers() {
        return Collections.unmodifiableList(selectedLineNumbers);
    }
}