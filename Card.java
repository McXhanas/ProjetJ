public class Card {

    private final int     pokedexId;
    private final String  name;
    private final int     value;
    private final Element element;

    public Card(int pokedexId, String name, int value, Element element) {
        if (value < 1 || value > 10)
            throw new IllegalArgumentException("La valeur doit être entre 1 et 10.");
        this.pokedexId = pokedexId;
        this.name      = name;
        this.value     = value;
        this.element   = element;
    }

    public int     getPokedexId() { return pokedexId; }
    public String  getName()      { return name; }
    public int     getValue()     { return value; }
    public Element getElement()   { return element; }

    @Override
    public String toString() {
        return String.format("[%s | %d | %s]", name, value, element);
    }
}