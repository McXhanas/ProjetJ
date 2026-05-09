import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class GameView extends Frame {

    private static final int W = 1200, H = 720;

    private final GameCanvas canvas;
    private final TextField  inputField;
    private final Button     confirmButton;
    private final Label      promptLabel;

    private final List<String[]> logEntries = new ArrayList<>();

    private Hand          currentHand       = null;
    private List<Integer> handIds           = new ArrayList<>();
    private int           selectedIdx       = -1;
    private String        currentPlayerName = "";

    private int    duelId1 = -1, duelId2 = -1;
    private String duelName1 = "", duelName2 = "";
    private String duelElem1 = "", duelElem2 = "";
    private int    duelVal1  = 0,  duelVal2  = 0;
    private String duelWinner = "";

    private final Map<Integer, Image> imgSmall = new HashMap<>();
    private final Map<Integer, Image> imgLarge = new HashMap<>();

    private volatile String userInput = null;
    private final Object    lock      = new Object();

    public GameView() {
        super("⚔  DuelCards  ⚔");
        setSize(W, H);
        setResizable(true);
        setLayout(new BorderLayout(0, 0));

        canvas = new GameCanvas();
        add(canvas, BorderLayout.CENTER);

        Panel south = new Panel(new BorderLayout(6, 0));
        south.setBackground(new Color(15, 15, 30));

        promptLabel = new Label("  ");
        promptLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        promptLabel.setForeground(new Color(255, 220, 80));
        promptLabel.setBackground(new Color(15, 15, 30));
        promptLabel.setPreferredSize(new Dimension(320, 36));

        inputField = new TextField();
        inputField.setFont(new Font("Monospaced", Font.PLAIN, 15));
        inputField.setBackground(new Color(30, 30, 55));
        inputField.setForeground(Color.WHITE);

        confirmButton = new Button("  Jouer  ");
        confirmButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        confirmButton.setBackground(new Color(50, 110, 190));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setPreferredSize(new Dimension(100, 36));

        Panel inputRow = new Panel(new BorderLayout(4, 0));
        inputRow.setBackground(new Color(15, 15, 30));
        inputRow.add(inputField,    BorderLayout.CENTER);
        inputRow.add(confirmButton, BorderLayout.EAST);

        south.add(promptLabel, BorderLayout.WEST);
        south.add(inputRow,    BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        inputField.addActionListener(e -> submitInput());
        confirmButton.addActionListener(e -> submitInput());
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        setVisible(true);
        inputField.requestFocus();
    }

    public void showWelcome() {
        addLog("title", "⚔   DUEL CARDS   ⚔");
        addLog("info",  "Bienvenue ! Les cartes ont ete distribuees.");
    }

    public void showMatchup(String n1, String n2) {
        addLog("title", n1 + "  vs  " + n2);
    }

    public void showRoundHeader(int current, int total) {
        addLog("round", "── Round " + current + " / " + total + " ──");
    }

    public void showRoundResult(RoundResult result) {
        addLog("result", result.toString());
    }

    public void showScores(Player p1, Player p2) {
        addLog("score", p1.getName() + " : " + p1.getScore()
            + "   |   " + p2.getName() + " : " + p2.getScore());
    }

    public void showFinalResult(Player p1, Player p2) {
        addLog("title", "RESULTAT FINAL");
        showScores(p1, p2);
        int s1 = p1.getScore(), s2 = p2.getScore();
        if      (s1 > s2) addLog("winner", "🏆  " + p1.getName() + " remporte la partie !");
        else if (s2 > s1) addLog("winner", "🏆  " + p2.getName() + " remporte la partie !");
        else              addLog("draw",   "🤝  Egalite parfaite !");
        setPrompt("Partie terminee — fermez la fenetre.");
        inputField.setEnabled(false);
        confirmButton.setEnabled(false);
    }

    public void showHand(String playerName, Hand hand, List<Integer> pokedexIds) {
        currentPlayerName = playerName;
        currentHand       = hand;
        handIds           = new ArrayList<>(pokedexIds);
        selectedIdx       = -1;
        for (int id : pokedexIds) loadSmall(id);
        canvas.repaint();
    }

    public void showDuel(String name1, int id1, String elem1, int val1,
                         String name2, int id2, String elem2, int val2,
                         String winner) {
        duelName1 = name1; duelId1 = id1; duelElem1 = elem1; duelVal1 = val1;
        duelName2 = name2; duelId2 = id2; duelElem2 = elem2; duelVal2 = val2;
        duelWinner = winner;
        loadLarge(id1);
        loadLarge(id2);
        canvas.repaint();
    }

    public void showError(String msg) { addLog("error", "⚠  " + msg); }

    public String askPlayerName(int number) {
        setPrompt("Nom du Joueur " + number + " :");
        currentHand = null;
        handIds     = new ArrayList<>();
        return readInput();
    }

    public int askCardChoice(String playerName, int maxIndex) {
        setPrompt(playerName + "  —  carte ( 0 – " + maxIndex + " ) :");
        String s = readInput();
        try {
            int idx = Integer.parseInt(s.trim());
            selectedIdx = idx;
            canvas.repaint();
            return idx;
        } catch (NumberFormatException e) { return -1; }
    }

    public void close() {}

    private void addLog(String type, String text) {
        logEntries.add(new String[]{type, text});
        if (logEntries.size() > 300) logEntries.remove(0);
        canvas.repaint();
    }

    private void setPrompt(String text) {
        promptLabel.setText("  ▶  " + text);
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
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            return userInput;
        }
    }

    private Image loadSmall(int id) {
        if (imgSmall.containsKey(id)) return imgSmall.get(id);
        try {
            File f = new File(System.getProperty("user.dir") + "/images/" + id + ".png");
            if (f.exists()) {
                imgSmall.put(id, ImageIO.read(f).getScaledInstance(72, 72, Image.SCALE_SMOOTH));
                return imgSmall.get(id);
            }
        } catch (Exception ignored) {}
        imgSmall.put(id, null);
        return null;
    }

    private Image loadLarge(int id) {
        if (imgLarge.containsKey(id)) return imgLarge.get(id);
        try {
            File f = new File(System.getProperty("user.dir") + "/images/" + id + ".png");
            if (f.exists()) {
                imgLarge.put(id, ImageIO.read(f).getScaledInstance(150, 150, Image.SCALE_SMOOTH));
                return imgLarge.get(id);
            }
        } catch (Exception ignored) {}
        imgLarge.put(id, null);
        return null;
    }

    private class GameCanvas extends Canvas {

        GameCanvas() { setBackground(new Color(12, 12, 24)); }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int cw = getWidth(), ch = getHeight();

            g2.setPaint(new GradientPaint(0, 0, new Color(12, 12, 28), 0, ch, new Color(22, 22, 48)));
            g2.fillRect(0, 0, cw, ch);

            g2.setColor(new Color(255, 255, 255, 35));
            Random rnd = new Random(99);
            for (int i = 0; i < 100; i++)
                g2.fillOval(rnd.nextInt(cw), rnd.nextInt(ch), rnd.nextInt(2)+1, rnd.nextInt(2)+1);

            int pad  = 8;
            int logW  = (int)(cw * 0.26);
            int duelW = (int)(cw * 0.40);
            int handW = cw - logW - duelW - pad * 4;

            drawLogPanel (g2, pad,                         pad, logW,  ch - pad * 2);
            drawDuelPanel(g2, pad + logW + pad,            pad, duelW, ch - pad * 2);
            drawHandPanel(g2, pad + logW + pad + duelW + pad, pad, handW, ch - pad * 2);
        }

        private void drawLogPanel(Graphics2D g, int x, int y, int w, int h) {
            panel(g, x, y, w, h, new Color(18, 20, 42), new Color(55, 75, 135));
            title(g, x, y, w, "Journal", new Color(110, 165, 255));

            Shape oc = g.getClip();
            g.setClip(x + 5, y + 32, w - 10, h - 38);
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            FontMetrics fm = g.getFontMetrics();
            int lh = 16, max = (h - 42) / lh;
            int start = Math.max(0, logEntries.size() - max);
            int yy = y + 46;
            for (int i = start; i < logEntries.size(); i++) {
                String[] e = logEntries.get(i);
                g.setColor(logColor(e[0]));
                g.drawString(cut(fm, e[1], w - 16), x + 8, yy);
                yy += lh;
            }
            g.setClip(oc);
        }

        private void drawDuelPanel(Graphics2D g, int x, int y, int w, int h) {
            panel(g, x, y, w, h, new Color(16, 18, 38), new Color(90, 55, 150));
            title(g, x, y, w, "Zone de Duel", new Color(190, 140, 255));

            if (duelId1 < 0) {
                g.setFont(new Font("SansSerif", Font.ITALIC, 13));
                g.setColor(new Color(100, 100, 150));
                String msg = "En attente du premier duel...";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, x + (w - fm.stringWidth(msg)) / 2, y + h / 2);
                return;
            }

            int cy = y + h / 2 - 20;
            int iSize = 150, margin = 24;

            duelCard(g, x + margin, cy - iSize / 2 - 25, iSize,
                duelId1, duelName1, duelElem1, duelVal1, duelWinner.equals(duelName1));

            duelCard(g, x + w - margin - iSize, cy - iSize / 2 - 25, iSize,
                duelId2, duelName2, duelElem2, duelVal2, duelWinner.equals(duelName2));

            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            g.setColor(new Color(230, 60, 60, 210));
            FontMetrics fv = g.getFontMetrics();
            g.drawString("VS", x + w / 2 - fv.stringWidth("VS") / 2, cy + 10);

            if (!duelWinner.isEmpty()) {
                String res = duelWinner.equals("draw") ? "Egalite !" : duelWinner + " gagne !";
                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                g.setColor(duelWinner.equals("draw") ? new Color(220, 220, 100) : new Color(80, 255, 140));
                FontMetrics fr = g.getFontMetrics();
                g.drawString(res, x + w / 2 - fr.stringWidth(res) / 2, cy + iSize / 2 + 30);
            }
        }

        private void duelCard(Graphics2D g, int x, int y, int size,
                               int id, String name, String elem, int val, boolean winner) {
            Color ec = elemColor(elem);
            if (winner) {
                g.setColor(new Color(255, 210, 0, 50));
                g.fillRoundRect(x - 10, y - 10, size + 20, size + 65, 18, 18);
                g.setColor(new Color(255, 210, 0, 160));
                g.setStroke(new BasicStroke(2.5f));
                g.drawRoundRect(x - 10, y - 10, size + 20, size + 65, 18, 18);
                g.setStroke(new BasicStroke(1f));
            }
            g.setColor(new Color(ec.getRed()/5, ec.getGreen()/5, ec.getBlue()/5, 210));
            g.fillRoundRect(x, y, size, size + 55, 12, 12);
            g.setColor(ec);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, size, size + 55, 12, 12);
            g.setStroke(new BasicStroke(1f));

            Image img = imgLarge.get(id);
            if (img != null) {
                g.drawImage(img, x, y, size, size, null);
            } else {
                g.setColor(new Color(40, 40, 65));
                g.fillRect(x, y, size, size);
                g.setColor(new Color(130, 130, 160));
                g.setFont(new Font("Monospaced", Font.BOLD, 13));
                g.drawString("#" + id, x + size / 2 - 14, y + size / 2 + 5);
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(Color.WHITE);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(cut(fm, name, size - 4), x + 4, y + size + 16);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.setColor(ec);
            g.drawString(elem, x + 4, y + size + 30);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(255, 215, 0));
            String vs = "★ " + val;
            g.drawString(vs, x + size - fm.stringWidth(vs) - 4, y + size + 44);
        }

        private void drawHandPanel(Graphics2D g, int x, int y, int w, int h) {
            panel(g, x, y, w, h, new Color(16, 26, 36), new Color(45, 110, 130));
            String t = currentHand != null ? "Main de " + currentPlayerName : "En attente...";
            title(g, x, y, w, t, new Color(90, 210, 190));

            if (currentHand == null || currentHand.size() == 0) return;

            int n = currentHand.size(), pad = 8;
            int cardH   = (h - 42 - pad * (n + 1)) / n;
            int imgSize = Math.min(cardH - 10, 75);

            for (int i = 0; i < n; i++) {
                var card  = currentHand.getCard(i);
                int id    = i < handIds.size() ? handIds.get(i) : -1;
                boolean sel = (i == selectedIdx);
                Color   ec  = elemColor(card.getElement().name());

                int cy = y + 40 + pad + i * (cardH + pad);
                int cx = x + pad;
                int cw = w - pad * 2;

                Color bg = new Color(ec.getRed()/6+15, ec.getGreen()/6+15, ec.getBlue()/6+15, 210);
                g.setColor(bg);
                g.fillRoundRect(cx, cy, cw, cardH, 10, 10);
                g.setColor(sel ? new Color(255, 220, 80) : ec);
                g.setStroke(new BasicStroke(sel ? 2.5f : 1.2f));
                g.drawRoundRect(cx, cy, cw, cardH, 10, 10);
                g.setStroke(new BasicStroke(1f));

                int imgX = cx + 5, imgY = cy + (cardH - imgSize) / 2;
                Image img = id >= 0 ? loadSmall(id) : null;
                if (img != null) {
                    g.drawImage(img, imgX, imgY, imgSize, imgSize, null);
                } else {
                    g.setColor(new Color(40, 40, 65));
                    g.fillRoundRect(imgX, imgY, imgSize, imgSize, 6, 6);
                }

                int tx = imgX + imgSize + 8, midY = cy + cardH / 2;

                g.setFont(new Font("SansSerif", Font.BOLD, 15));
                g.setColor(sel ? new Color(255, 220, 80) : new Color(170, 170, 215));
                g.drawString("[" + i + "]", tx, midY - 10);

                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(cut(fm, card.getName(), cw - imgSize - 50), tx, midY + 6);

                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.setColor(ec);
                g.drawString(card.getElement().name(), tx, midY + 20);

                g.setFont(new Font("SansSerif", Font.BOLD, 13));
                g.setColor(new Color(255, 215, 0));
                g.drawString("★ " + card.getValue(), cx + cw - 38, midY + 6);
            }
        }

        private void panel(Graphics2D g, int x, int y, int w, int h, Color bg, Color border) {
            g.setColor(new Color(0, 0, 0, 55));
            g.fillRoundRect(x+3, y+3, w, h, 16, 16);
            g.setColor(bg);
            g.fillRoundRect(x, y, w, h, 16, 16);
            g.setColor(border);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, w, h, 16, 16);
            g.setStroke(new BasicStroke(1f));
        }

        private void title(Graphics2D g, int x, int y, int w, String text, Color color) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
            g.fillRoundRect(x+1, y+1, w-2, 30, 16, 16);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + 20);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g.drawLine(x+8, y+30, x+w-8, y+30);
        }

        private String cut(FontMetrics fm, String text, int max) {
            if (fm.stringWidth(text) <= max) return text;
            String d = "..."; int ew = fm.stringWidth(d), i = text.length();
            while (i > 0 && fm.stringWidth(text.substring(0, i)) + ew > max) i--;
            return text.substring(0, i) + d;
        }

        private Color logColor(String type) {
            switch (type) {
                case "title":  return new Color(255, 200, 80);
                case "round":  return new Color(170, 145, 255);
                case "result": return new Color(90, 215, 175);
                case "score":  return new Color(140, 255, 140);
                case "winner": return new Color(255, 215, 0);
                case "draw":   return new Color(195, 195, 255);
                case "error":  return new Color(255, 95, 75);
                default:       return new Color(185, 195, 220);
            }
        }

        private Color elemColor(String elem) {
            if (elem == null) return new Color(140, 140, 175);
            switch (elem.toUpperCase()) {
                case "FEU":   return new Color(255, 95, 45);
                case "EAU":   return new Color(55, 155, 255);
                case "TERRE": return new Color(75, 195, 75);
                case "AIR":   return new Color(175, 155, 255);
                default:      return new Color(140, 140, 175);
            }
        }
    }
}