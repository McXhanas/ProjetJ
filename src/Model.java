import java.awt.Color;

public class Model {
	  public static final Color[] COULEURS = {
		        Color.YELLOW,
		        Color.GREEN,
		        Color.BLUE,
		        Color.MAGENTA,
		        Color.RED,
		        Color.ORANGE,
		        Color.WHITE,
		        Color.BLACK
		    };
	  public static int N_TENTATIVES = 10;
	  public static int DIFFICULTE = 4; 
	  public enum Etat {
		    GAGNE,
		    PERDU,
		    EN_COURS
		}
}
