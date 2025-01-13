// Joueur.java : Représente un joueur
class Joueur {
    private String nom;
    private int points;

    public Joueur(String nom) {
        this.nom = nom;
        this.points = 0;
    }

    public String getNom() {
        return nom;
    }

    public int getPoints() {
        return points;
    }

    public void ajouterPoints(int points) {
        this.points += points;
    }
}