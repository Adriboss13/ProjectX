/******************************************************************************
 * Joueur.java
 * Représentation d'un joueur dans le jeu
 *
 * Cette classe gère :
 * - L'identité du joueur
 * - Son score dans la partie
 *****************************************************************************/

public class Joueur {
    //==========================================================================
    // Variables membres
    //==========================================================================
    private final String nom;     // Nom du joueur (immuable)
    private int points;          // Score du joueur

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Crée un nouveau joueur
     * @param nom Le nom choisi par le joueur
     */
    public Joueur(String nom) {
        this.nom = nom;
        this.points = 0;
    }

    //==========================================================================
    // Getters & Setters
    //==========================================================================
    /**
     * Obtient le nom du joueur
     * @return Le nom du joueur
     */
    public String getNom() {
        return nom;
    }

    /**
     * Obtient le score actuel du joueur
     * @return Le nombre de points
     */
    public int getPoints() {
        return points;
    }

    /**
     * Ajoute des points au score du joueur
     * @param points Nombre de points à ajouter
     */
    public void ajouterPoints(int points) {
        this.points += points;
    }
}