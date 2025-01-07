// La classe Joueur représente un joueur dans le jeu, avec un nom et des points accumulés au fur et à mesure de sa participation.

class Joueur {
    private String nom; // Nom du joueur
    private int points; // Points accumulés par le joueur

    // Constructeur qui initialise le joueur avec un nom et définit les points à 0.
    public Joueur(String nom) {
        this.nom = nom;  // Assigne le nom du joueur
        this.points = 0; // Initialise les points du joueur à 0
    }

    // Méthode pour obtenir le nom du joueur.
    public String getNom() {
        return nom;  // Retourne le nom du joueur
    }

    // Méthode pour obtenir le nombre de points du joueur.
    public int getPoints() {
        return points;  // Retourne le nombre actuel de points du joueur
    }

    // Méthode pour ajouter des points au joueur. Le nombre de points à ajouter est passé en paramètre.
    public void ajouterPoints(int points) {
        this.points += points;  // Ajoute le nombre de points spécifié aux points actuels du joueur
    }
}