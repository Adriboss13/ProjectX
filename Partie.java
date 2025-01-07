import java.util.ArrayList;
import java.util.List;

class Partie {
    private List<Joueur> joueurs = new ArrayList<>(); // Liste des joueurs
    private Mots motCourant; // Mot en cours de devinette
    private Joueur dessinateur; // Joueur en train de dessiner
    private int chrono; // Temps restant dans le tour
    private int tours; // Nombre de tours restants
    private GestionnaireMots gestionnaireDeMot; // Gestionnaire des mots

    // Crée une nouvelle partie avec des joueurs fictifs
    public void creerPartie() {
        System.out.println("Création de la partie.");
        joueurs.add(new Joueur("Alice"));
        joueurs.add(new Joueur("Bob"));
        joueurs.add(new Joueur("Charlie"));

        gestionnaireDeMot = new GestionnaireMots();
        gestionnaireDeMot.ajouterMot(new Mots("Chat", 1));
        gestionnaireDeMot.ajouterMot(new Mots("Maison", 2));
        gestionnaireDeMot.ajouterMot(new Mots("Soleil", 1));

        attribuerRoles();
        proposerMotAuDessinateur();
    }

    // Attribue les rôles (dessinateur et devineurs)
    public void attribuerRoles() {
        dessinateur = joueurs.get(0); // Simplification : le premier joueur devient dessinateur
        System.out.println(dessinateur.getNom() + " est le dessinateur.");
    }

    // Propose un mot au dessinateur
    public void proposerMotAuDessinateur() {
        motCourant = gestionnaireDeMot.obtenirMotAleatoire();
        System.out.println("Mot choisi pour le dessinateur : " + motCourant.getMot());
    }

    // Vérifie si le mot proposé par un joueur est correct
    public boolean verifierMot(String proposition) {
        return motCourant.getMot().equalsIgnoreCase(proposition);
    }

    // Attribue des points à un joueur
    public void attribuerPoints(Joueur joueur) {
        joueur.ajouterPoints(10);
        System.out.println("10 points attribués à " + joueur.getNom());
    }
}