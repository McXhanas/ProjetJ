import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class GameView extends Frame {

    private static final int W = 900, H = 650;

    private final GameCanvas canvas;
    private final TextField  inputField;
    private final Button     confirmButton;

    private String           prompt    = "";
    private final List<String> log     = new ArrayList<>();
    private String[]         handLines = new String[0];

    private volatile String  userInput = null;
    private final Object     lock      = new Object();
    
    private List<Integer>    handIds     = new ArrayList<>();
    private int              selectedIdx = -1;
    private final Map<Integer, Image> imageCache = new HashMap<>();
    
    private static final String IMG_DIR = System.getProperty("user.dir") + "/images/";
    public GameView() {
        super("⚔ DuelCards ⚔");
        setSize(W, H);
        setResizable(false);
        setLayout(new BorderLayout());

        canvas = new GameCanvas();
        add(canvas, BorderLayout.CENTER);

        Panel south = new Panel(new BorderLayout());
        south.setBackground(new Color(30, 30, 50));

        inputField = new TextField();
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        inputField.setBackground(new Color(40, 40, 65));
        inputField.setForeground(Color.WHITE);

        confirmButton = new Button("Confirmer");
        confirmButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        confirmButton.setBackground(new Color(70, 130, 180));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setPreferredSize(new Dimension(110, 35));

        south.add(inputField,    BorderLayout.CENTER);
        south.add(confirmButton, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        inputField.addActionListener(e -> submitInput());
        confirmButton.addActionListener(e -> submitInput());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        setVisible(true);
        inputField.requestFocus();
    }

    // ── API Contrôleur ────────────────────────────────────────────────────────

    public void showWelcome() {
        addLog("╔══════════════════════════════╗");
        addLog("║     ⚔   DUEL  CARDS   ⚔      ║");
        addLog("╚══════════════════════════════╝");
    }

    public void showMatchup(String n1, String n2) {
        addLog("  " + n1 + "  vs  " + n2);
        addLog("");
    }

    public void showRoundHeader(int current, int total) {
        addLog("─── Round " + current + " / " + total + " ───────────────");
    }

    public void showRoundResult(RoundResult result) { addLog(result.toString()); }

    public void showScores(Player p1, Player p2) {
        addLog("  Scores →  " + p1.getName() + " : " + p1.getScore()
             + "  |  " + p2.getName() + " : " + p2.getScore());
        addLog("");
    }

    public void showFinalResult(Player p1, Player p2) {
        addLog("══════════════════════════════");
        addLog("        RÉSULTAT FINAL");
        addLog("══════════════════════════════");
        showScores(p1, p2);
        int s1 = p1.getScore(), s2 = p2.getScore();
        if      (s1 > s2) addLog("🏆  " + p1.getName() + " remporte la partie !");
        else if (s2 > s1) addLog("🏆  " + p2.getName() + " remporte la partie !");
        else              addLog("🤝  Égalité parfaite !");
        setPrompt("Partie terminée. Fermez la fenêtre.");
        inputField.setEnabled(false);
        confirmButton.setEnabled(false);
    }

    public void showHand(String playerName, Hand hand, List<Integer> pokedexIds) {
        List<String> lines = new ArrayList<>();
        lines.add("Main de " + playerName + " :");
        for (int i = 0; i < hand.size(); i++)
            lines.add("  [" + i + "] " + hand.getCard(i));
        handLines   = lines.toArray(new String[0]);
        handIds     = new ArrayList<>(pokedexIds);
        selectedIdx = -1;
        for (int id : pokedexIds) loadImage(id);
        canvas.repaint();
    }

    public void showError(String message) { addLog("  ⚠ " + message); }

    public String askPlayerName(int number) {
        setPrompt("Nom du Joueur " + number + " :");
        handLines = new String[0];
        return readInput();
    }

    public int askCardChoice(String playerName, int maxIndex) {
        setPrompt(playerName + ", choisissez une carte (0-" + maxIndex + ") :");
        String s = readInput();
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    public void close() {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setPrompt(String text) { prompt = text; canvas.repaint(); }

    private void addLog(String line) {
        log.add(line);
        if (log.size() > 200) log.remove(0);
        canvas.repaint();
    }

    private void submitInput() {
        synchronized (lock) {
            userInput = inputField.getText();
            inputField.setText("");
            lock.notifyAll();
        }
    }

    private String readInput() {
        synchronized (lock) {
            userInput = null;
            try { while (userInput == null) lock.wait(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return userInput;
        }
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    private class GameCanvas extends Canvas {

        GameCanvas() { setBackground(new Color(20, 20, 35)); }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            int cw = getWidth(), ch = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(15, 15, 30),
                                          0, ch, new Color(30, 30, 60)));
            g2.fillRect(0, 0, cw, ch);

            drawLogPanel(g2, 10, 10, cw / 2 - 20, ch - 20);
            drawHandPanel(g2, cw / 2 + 10, 10, cw / 2 - 20, ch - 20);
        }

        private void drawLogPanel(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(25, 25, 45, 220));
            g.fillRoundRect(x, y, w, h, 16, 16);
            g.setColor(new Color(80, 100, 160));
            g.drawRoundRect(x, y, w, h, 16, 16);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(100, 180, 255));
            g.drawString("Journal de partie", x + 12, y + 22);
            g.setColor(new Color(80, 100, 160));
            g.drawLine(x + 8, y + 28, x + w - 8, y + 28);

            // Clipping strict : rien ne déborde hors du panneau
            Shape oldClip = g.getClip();
            g.setClip(x + 6, y + 30, w - 12, h - 36);

            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            int maxTextW = w - 20;
            int lineH = 16, maxLines = (h - 40) / lineH;
            int start = Math.max(0, log.size() - maxLines);
            int yy = y + 44;
            for (int i = start; i < log.size(); i++) {
                g.setColor(colorForLine(log.get(i)));
                g.drawString(truncate(fm, log.get(i), maxTextW), x + 10, yy);
                yy += lineH;
            }

            g.setClip(oldClip); // restaure le clip d'origine
        }

        private void drawHandPanel(Graphics2D g, int x, int y, int w, int h) {
            // Fond
            g.setColor(new Color(25, 35, 50, 230));
            g.fillRoundRect(x, y, w, h, 16, 16);
            g.setColor(new Color(80, 140, 160));
            g.drawRoundRect(x, y, w, h, 16, 16);

            // Titre
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(100, 220, 200));
            g.drawString("Cartes en main", x + 12, y + 22);
            g.setColor(new Color(80, 140, 160));
            g.drawLine(x + 8, y + 28, x + w - 8, y + 28);

            // Texte des cartes (clippé)
            Shape oldClip = g.getClip();
            int textZoneH = h / 2 - 10;
            g.setClip(x + 6, y + 30, w - 12, textZoneH);

            g.setFont(new Font("Monospaced", Font.PLAIN, 13));
            FontMetrics fm = g.getFontMetrics();
            int maxTW = w - 24;
            int yy = y + 50;
            for (int i = 0; i < handLines.length; i++) {
                String line = handLines[i];
                if (i > 0 && (i - 1) == selectedIdx) {
                    g.setColor(new Color(255, 220, 80, 60));
                    g.fillRoundRect(x + 8, yy - 13, w - 16, 18, 6, 6);
                }
                g.setColor(colorForCard(line));
                g.drawString(truncate(fm, line, maxTW), x + 12, yy);
                yy += 20;
            }
            g.setClip(oldClip);

            // Images Pokémon
            int imgZoneY = y + h / 2;
            int imgZoneH = h / 2 - 10;

            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(new Color(160, 200, 255));
            g.drawString("Aperçu des cartes :", x + 12, imgZoneY + 14);

            int cols    = 5;
            int spacing = 8;
            int cellW   = (w - spacing * (cols + 1)) / cols;
            int cellH   = Math.min(cellW, (imgZoneH - 30) / 2);
            cellW       = cellH;

            for (int i = 0; i < handIds.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int ix  = x + spacing + col * (cellW + spacing);
                int iy  = imgZoneY + 22 + row * (cellH + spacing);

                boolean isSelected = (i == selectedIdx);
                g.setColor(isSelected
                    ? new Color(255, 220, 80, 80)
                    : new Color(40, 50, 70, 180));
                g.fillRoundRect(ix - 3, iy - 3, cellW + 6, cellH + 6, 8, 8);

                g.setColor(isSelected
                    ? new Color(255, 220, 80)
                    : new Color(80, 120, 160));
                g.drawRoundRect(ix - 3, iy - 3, cellW + 6, cellH + 6, 8, 8);

                Image img = (i < handIds.size()) ? loadImage(handIds.get(i)) : null;
                if (img != null) {
                    g.drawImage(img, ix, iy, cellW, cellH, null);
                } else {
                    g.setColor(new Color(60, 60, 90));
                    g.fillRoundRect(ix, iy, cellW, cellH, 6, 6);
                    g.setColor(new Color(150, 150, 180));
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    String pid = "#" + handIds.get(i);
                    g.drawString(pid, ix + cellW / 2 - 10, iy + cellH / 2 + 4);
                }

                g.setColor(new Color(200, 200, 230));
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString("[" + i + "]", ix + cellW / 2 - 7, iy + cellH + 12);
            }

            // Prompt
            if (!prompt.isEmpty()) {
                g.setColor(new Color(255, 220, 80));
                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                FontMetrics fmp = g.getFontMetrics();
                g.drawString(truncate(fmp, ">> " + prompt, w - 20), x + 12, y + h - 14);
            }
        }
        /** Tronque le texte avec "..." s'il dépasse maxWidth pixels. */
        private String truncate(FontMetrics fm, String text, int maxWidth) {
            if (fm.stringWidth(text) <= maxWidth) return text;
            String dots = "...";
            int ellW = fm.stringWidth(dots);
            int i = text.length();
            while (i > 0 && fm.stringWidth(text.substring(0, i)) + ellW > maxWidth) i--;
            return text.substring(0, i) + dots;
        }

        private Color colorForLine(String line) {
            if (line.contains("Scores"))   return new Color(150, 255, 180);
            if (line.contains("Round"))    return new Color(180, 160, 255);
            if (line.contains("remporte")) return new Color(100, 240, 180);
            if (line.contains("FINAL"))    return new Color(255, 200, 80);
            if (line.contains("galité"))   return new Color(200, 200, 255);
            return new Color(200, 210, 230);
        }

        private Color colorForCard(String line) {
            if (line.contains("FEU"))   return new Color(255, 120, 60);
            if (line.contains("EAU"))   return new Color(80, 180, 255);
            if (line.contains("TERRE")) return new Color(100, 200, 100);
            if (line.contains("AIR"))   return new Color(200, 200, 255);
            return new Color(220, 220, 240);
        }
    }
    private Image loadImage(int pokedexId) {
        if (imageCache.containsKey(pokedexId)) return imageCache.get(pokedexId);
        try {
            File f = new File(System.getProperty("user.dir") + "/images/" + pokedexId + ".png");
            System.out.println("Cherche image : " + f.getAbsolutePath() + " | existe : " + f.exists());
            if (f.exists()) {
                BufferedImage raw = ImageIO.read(f);
                Image scaled = raw.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                imageCache.put(pokedexId, scaled);
                return scaled;
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image " + pokedexId + " : " + e.getMessage());
        }
        imageCache.put(pokedexId, null);
        return null;
    }
    
}