import java.io.IOException;

public class TestGestionnaireMots {
    public static void main(String[] args) {
        GestionnaireMots gestionnaireMots = new GestionnaireMots();

        try {
            // Charger les mots depuis le fichier
            gestionnaireMots.chargerMotsDepuisFichier("mots.txt");

            // Afficher tous les mots chargés
            for (Mots mot : gestionnaireMots.getMots()) {
                System.out.println(mot);
            }

            // Obtenir un mot aléatoire
            Mots motAleatoire = gestionnaireMots.obtenirMotAleatoire();
            System.out.println("Mot aléatoire : " + motAleatoire);

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des mots : " + e.getMessage());
        }
    }
}