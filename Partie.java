/******************************************************************************
 * Partie.java
 * Gestion d'une partie de jeu
 *
 * Cette classe gère :
 * - Le déroulement des manches
 * - La sélection et vérification des mots
 * - Le système de points
 * - La gestion des joueurs
 *****************************************************************************/

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Partie {
    //==========================================================================
    // Constantes
    //==========================================================================
    private static final int DUREE_MANCHE = 60;     // Durée d'une manche en secondes
    private static final int NB_MOTS_CHOIX = 2;     // Nombre de mots proposés au dessinateur

    //==========================================================================
    // Variables membres
    //==========================================================================
    private final List<Joueur> joueurs = Collections.synchronizedList(new ArrayList<>()); // Liste des joueurs
    private final GestionnaireDeMot gestionnaireDeMot;     // Gestionnaire des mots à deviner
    private final Serveur serveur;                         // Référence au serveur
    private final Object lockPartie = new Object();                       // Verrou de synchronisation
    private final Set<String> motsUtilises = new HashSet<>();                // Mots déjà utilisés
    private final List<Joueur> devineursQuiOntTrouve = new ArrayList<>();     // Ordre des joueurs ayant trouvé

    private int tourActuel = 0;                                // Tour de jeu actuel
    private Joueur dessinateur;                           // Joueur dessinateur actuel
    private Mots motCourant;                              // Mot à deviner actuel
    private volatile boolean partieEnCours = false;               // État de la partie
    private Timer currentTimer;  // Remplacer timerManche par currentTimer
    private int tempsRestant;                             // Temps restant

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Initialise une nouvelle partie
     * @param serveur Référence au serveur principal
     * @param cheminFichier Chemin vers le fichier des mots
     */
    public Partie(Serveur serveur, String cheminFichier) {
        this.serveur = serveur;
        this.gestionnaireDeMot = new GestionnaireDeMot(cheminFichier);
    }

    //==========================================================================
    // Méthodes de gestion de partie
    //==========================================================================

    /**
     * Démarre une nouvelle partie
     * @param clients Liste des clients connectés
     */
    public void demarrerPartie(List<ClientHandler> clients) {
        System.out.println("DemarrerPartie: Démarrage de la partie");
        synchronized (lockPartie) {
            if (!partieEnCours) {
                partieEnCours = true;
                tourActuel = 0;

                // Nettoyer l'interface pour tous les clients
                serveur.broadcast("CLEAR:", null);

                // Démarrer la première manche
                lancerManche(clients);
            }
        }
    }

    /**
     * Lance une nouvelle manche
     * @param clients Liste des clients actifs
     */
    private void lancerManche(List<ClientHandler> clients) {
        synchronized (lockPartie) {
            if (!partieEnCours) return;

            dessinateur = joueurs.get(tourActuel % joueurs.size());
            tempsRestant = DUREE_MANCHE;
            System.out.println("Nouveau dessinateur : " + dessinateur.getNom()); // Log pour le débogage

            serveur.broadcast("NOUVEAU_DESSINATEUR:" + dessinateur.getNom(), null);

            List<Mots> choixMots = selectionnerMotsProposition();
            System.out.println("Mots sélectionnés : " + choixMots.stream()
                .map(Mots::getMot)
                .collect(Collectors.joining(", "))); // Log pour le débogage

            ClientHandler dessinateurHandler = clients.stream()
                .filter(c -> c.getJoueur().equals(dessinateur))
                .findFirst()
                .orElse(null);

            if (dessinateurHandler != null) {
                String motsMessage = "CHOIX_MOTS:" + choixMots.stream()
                    .map(Mots::getMot)
                    .collect(Collectors.joining(","));
                System.out.println("Envoi des mots au dessinateur : " + motsMessage); // Log pour le débogage

                for (ClientHandler client : clients) {
                    if (client.getJoueur().equals(dessinateur)) {
                        client.envoyerMessageAsync("ROLE:dessinateur");
                    } else {
                        client.envoyerMessageAsync("ROLE:devineur");
                    }
                }
                dessinateurHandler.envoyerMessageAsync(motsMessage);
            } else {
                System.err.println("Dessinateur non trouvé dans la liste des clients"); // Log pour le débogage
            }
            devineursQuiOntTrouve.clear();
        }
    }

    /**
     * Termine la manche en cours et prépare la suivante
     */
    public void terminerManche() {
        // Arrêter le timer immédiatement
        if (currentTimer != null) {
            currentTimer.stopTimer();
        }
        // Arrêter aussi le timer du serveur
        serveur.getCurrentTimer().stopTimer();

        // Pause pour laisser le temps de voir le résultat
        serveur.broadcast("FIN_MANCHE:Le mot était : " + motCourant.getMot(), null);
        afficherPodium();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (lockPartie) {
            tourActuel++;
            // S'assurer que le timer est bien arrêté avant de lancer la nouvelle manche
            if (tourActuel < joueurs.size() * 3 && partieEnCours) {
                if (currentTimer != null) {
                    currentTimer.stopTimer();
                }
                if (serveur.getCurrentTimer() != null) {
                    serveur.getCurrentTimer().stopTimer();
                }
                lancerManche(serveur.getClients());
            } else {
                afficherPodium();
                partieEnCours = false;
            }
        }
    }

    //==========================================================================
    // Méthodes de gestion des mots
    //==========================================================================

    /**
     * Sélectionne les mots à proposer au dessinateur
     * @return Liste des mots proposés
     */
    private List<Mots> selectionnerMotsProposition() {
        List<Mots> propositions = new ArrayList<>();
        while (propositions.size() < NB_MOTS_CHOIX) {
            Mots mot = gestionnaireDeMot.choisirMotAleatoire();
            if (mot != null && !motsUtilises.contains(mot.getMot())) {
                propositions.add(mot);
            }
        }
        System.out.println("Nombre de mots disponibles : " + gestionnaireDeMot.getMotsDisponibles().size());
        return propositions;
    }

    /**
     * Vérifie si un mot proposé correspond au mot à deviner
     * @param joueur Joueur qui propose le mot
     * @param proposition Mot proposé
     * @return true si le mot est correct
     */
    public boolean verifierMot(Joueur joueur, String proposition) {
        synchronized(lockPartie) {
            String motNormalise = enleverAccents(motCourant.getMot()).toLowerCase();
            String propositionNormalisee = enleverAccents(proposition).toLowerCase();

            if (motNormalise.equals(propositionNormalisee) && !joueur.equals(dessinateur)) {
                // Calcul des points en fonction de l'ordre
                int ordre = devineursQuiOntTrouve.size() + 1;
                int points = calculerPoints(tempsRestant, ordre);
                joueur.ajouterPoints(points);
                // Le dessinateur ne gagne pas de points
                serveur.broadcast(joueur.getNom() + " a trouvé le mot en " + ordre + "e position et gagne " + points + " points!", null);
                devineursQuiOntTrouve.add(joueur);

                // Envoi de messages spécifiques
                ClientHandler handlerJoueur = serveur.getClientHandler(joueur);
                ClientHandler handlerDessinateur = serveur.getClientHandler(dessinateur);

                if (handlerJoueur != null) {
                    handlerJoueur.envoyerMessageAsync("GUESS_CORRECT:Vous avez trouvé le mot '" + motCourant.getMot() + "'!");
                }

                if (handlerDessinateur != null) {
                    handlerDessinateur.envoyerMessageAsync("GUESS_CORRECT:" + joueur.getNom() + " a trouvé le mot '" + motCourant.getMot() + "'!");
                }

                serveur.broadcast("NOTIFICATION:" + joueur.getNom() + " a trouvé le mot!", handlerJoueur);

                if (devineursQuiOntTrouve.size() == joueurs.size() - 1) {
                    // S'assurer que le timer est arrêté avant de terminer la manche
                    if (currentTimer != null) {
                        currentTimer.stopTimer();
                    }
                    if (serveur.getCurrentTimer() != null) {
                        serveur.getCurrentTimer().stopTimer();
                    }
                    terminerManche();
                }
                return true;
            } else if (!joueur.equals(dessinateur) && estPresqueLeMot(motNormalise, propositionNormalisee)) {
                // Envoyer un message uniquement au joueur qui a presque trouvé
                ClientHandler handlerJoueur = serveur.getClientHandler(joueur);
                if (handlerJoueur != null) {
                    handlerJoueur.envoyerMessageAsync("CHAT:[Indice] C'est presque ça !");
                }
                return false;
            }
            return false;
        }
    }

    /**
     * Compare deux mots pour voir s'ils sont proches
     * @param mot1 Premier mot
     * @param mot2 Second mot
     * @return true si les mots sont presque identiques
     */
    private boolean estPresqueLeMot(String mot1, String mot2) {
        // Si la différence de longueur est supérieure à 1, ce n'est pas "presque" le même mot
        if (Math.abs(mot1.length() - mot2.length()) > 1) {
            return false;
        }

        // Si les mots ont la même longueur, on compte les différences
        if (mot1.length() == mot2.length()) {
            int differences = 0;
            for (int i = 0; i < mot1.length(); i++) {
                if (mot1.charAt(i) != mot2.charAt(i)) {
                    differences++;
                }
            }
            return differences == 1;
        }

        // Si la longueur diffère de 1, on vérifie si on peut obtenir l'un à partir de l'autre
        String plusLong = mot1.length() > mot2.length() ? mot1 : mot2;
        String plusCourt = mot1.length() > mot2.length() ? mot2 : mot1;

        int indexLong = 0, indexCourt = 0;
        boolean differenceTrouvee = false;

        while (indexLong < plusLong.length() && indexCourt < plusCourt.length()) {
            if (plusLong.charAt(indexLong) != plusCourt.charAt(indexCourt)) {
                if (differenceTrouvee) {
                    return false;
                }
                differenceTrouvee = true;
                indexLong++;
                continue;
            }
            indexLong++;
            indexCourt++;
        }

        return true;
    }

    //==========================================================================
    // Méthodes utilitaires
    //==========================================================================

    /**
     * Supprime les accents d'une chaîne
     */
    private String enleverAccents(String texte) {
        String normalisé = Normalizer.normalize(texte, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalisé).replaceAll("");
    }

    /**
     * Calcule les points en fonction du temps et de l'ordre
     */
    private int calculerPoints(int tempsRestant, int ordre) {
        int basePoints = motCourant.getDifficulte().equals("1") ? 10 : 20;
        // Points supplémentaires diminuent avec l'ordre
        int bonus = Math.max(0, (joueurs.size() - ordre + 1) * 5);
        return basePoints + (tempsRestant / 2) + bonus;
    }

    /**
     * Démarre le timer de la manche en cours
     * Cette méthode est appelée lorsqu'un mot est choisi
     */
    private void demarrerTimer() {
        serveur.startTimer();
    }

    /**
     * Affiche le classement des joueurs
     */
    private void afficherPodium() {
        List<Joueur> podium = new ArrayList<>(joueurs);
        podium.sort((j1, j2) -> j2.getPoints() - j1.getPoints());

        StringBuilder classement = new StringBuilder("PODIUM:\n");
        for (int i = 0; i < podium.size(); i++) {
            Joueur j = podium.get(i);
            classement.append((i + 1)).append(". ")
                     .append(j.getNom()).append(" - ")
                     .append(j.getPoints()).append(" points\n");
        }
        serveur.broadcast(classement.toString(), null);
    }

    //==========================================================================
    // Getters & Setters
    //==========================================================================

    /**
     * Définit le mot choisi par le dessinateur
     */
    public void setMotChoisi(String motChoisi) {
        synchronized(lockPartie) {
            for (Mots mot : gestionnaireDeMot.getMotsDisponibles()) {
                if (mot.getMot().equalsIgnoreCase(motChoisi)) {
                    motCourant = mot;
                    motsUtilises.add(motChoisi); // Ajouter le mot aux mots utilisés
                    // Démarrer le timer de la manche
                    demarrerTimer();
                    serveur.broadcast("CHOSEN_WORD_CONFIRMED:" + motChoisi, null); // Ajout
                    break;
                }
            }
        }
    }

    /**
     * Modifie l'état de la partie
     */
    public void setPartieEnCours(boolean partieEnCours) {
        this.partieEnCours = partieEnCours;
        if (!partieEnCours && serveur.getCurrentTimer() != null) {
            serveur.getCurrentTimer().stopTimer();  // S'assurer que le timer est arrêté quand la partie se termine
        }
    }

    /**
     * Ajoute un joueur à la partie
     */
    public void ajouterJoueur(Joueur joueur) {
        joueurs.add(joueur);
    }
}