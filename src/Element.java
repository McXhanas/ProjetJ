
public enum Element {
    FEU("🔥"),
    EAU("💧"),
    TERRE("🌿"),
    AIR("💨");

    private final String symbol;

    Element(String symbol) {
        this.symbol = symbol;
    }

    public int advantageAgainst(Element other) {
        if (this == other) return 0;
        return switch (this) {
            case FEU   -> (other == TERRE) ?  1 : -1;
            case TERRE -> (other == EAU)   ?  1 : -1;
            case EAU   -> (other == AIR)   ?  1 : -1;
            case AIR   -> (other == FEU)   ?  1 : -1;
        };
    }

    public String getSymbol() { return symbol; }

    @Override
    public String toString() { return name() + " " + symbol; }
}