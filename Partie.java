import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Partie {
    private final List<Joueur> joueurs = Collections.synchronizedList(new ArrayList<>());
    private GestionnaireDeMot gestionnaireDeMot;
    private int tourActuel = 0;
    private Joueur dessinateur;
    private Mots motCourant;
    private Serveur serveur;
    private volatile boolean partieEnCours = false;
    private final Object lockPartie = new Object();

    public Partie(Serveur serveur, String cheminFichier) {
        this.serveur = serveur;
        gestionnaireDeMot = new GestionnaireDeMot(cheminFichier);
    }

    public void demarrerPartie(List<Serveur.ClientHandler> clients) {
        System.out.println("La partie commence !");
        synchronized(lockPartie) {
            while (tourActuel < joueurs.size() * 3) {
                lancerTour(clients);
                tourActuel++;
            }
            afficherPodium();
        }
    }

    public void ajouterJoueur(Joueur joueur) {
        synchronized(joueurs) {
            joueurs.add(joueur);
        }
    }

    private void lancerTour(List<Serveur.ClientHandler> clients) {
        synchronized(lockPartie) {
            dessinateur = joueurs.get(tourActuel % joueurs.size());
            motCourant = gestionnaireDeMot.choisirMotAleatoire();

            synchronized(clients) {
                for (Serveur.ClientHandler client : clients) {
                    if (client.getJoueur().equals(dessinateur)) {
                        client.envoyerMessage("Vous êtes le dessinateur. Mot : " + motCourant.getMot());
                    } else {
                        client.envoyerMessage("Vous êtes un devineur. Observez et devinez le dessin !");
                    }
                }
            }
            serveur.broadcast("Le mot à deviner est : " + motCourant.getMot(), null);
        }
    }

    public boolean verifierMot(Joueur joueur, String proposition) {
        synchronized(lockPartie) {
            if (motCourant.getMot().equalsIgnoreCase(proposition)) {
                joueur.ajouterPoints(motCourant.getDifficulte().equals("Facile") ? 10 : 20);
                dessinateur.ajouterPoints(motCourant.getDifficulte().equals("Facile") ? 3 : 7);
                serveur.broadcast(joueur.getNom() + " a trouvé le mot : " + motCourant.getMot(), null);
                return true;
            }
            return false;
        }
    }

    private void afficherPodium() {
        synchronized(joueurs) {
            List<Joueur> podium = new ArrayList<>(joueurs);
            podium.sort((j1, j2) -> j2.getPoints() - j1.getPoints());
            
            System.out.println("Podium final :");
            for (int i = 0; i < podium.size(); i++) {
                System.out.println((i + 1) + ". " + podium.get(i).getNom() + " - " + podium.get(i).getPoints() + " points");
            }
        }
    }

    public boolean isPartieEnCours() {
        return partieEnCours;
    }

    public void setPartieEnCours(boolean partieEnCours) {
        this.partieEnCours = partieEnCours;
    }
}