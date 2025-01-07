import java.util.List;

class Podium {
    public void afficher(List<Joueur> joueurs) {
        joueurs.sort((j1, j2) -> j2.getPoints() - j1.getPoints()); // Tri des joueurs par points
        System.out.println("Podium :");
        for (int i = 0; i < joueurs.size(); i++) {
            System.out.println((i + 1) + ". " + joueurs.get(i).getNom() + " - " + joueurs.get(i).getPoints() + " points");
        }
    }
}