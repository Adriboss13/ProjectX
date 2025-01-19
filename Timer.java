/******************************************************************************
 * Timer.java
 * Gestion du chronomètre pour chaque manche de jeu
 *
 * Cette classe gère le décompte du temps pour chaque manche, envoie des notifications
 * aux joueurs et déclenche la révélation progressive des lettres.
 *****************************************************************************/

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {
    //==========================================================================
    // Constantes
    //==========================================================================
    private static final int DUREE_MANCHE = 60; // Durée d'une manche en secondes

    //==========================================================================
    // Variables membres
    //==========================================================================
    private final Serveur serveur;               // Référence vers le serveur principal
    private ScheduledExecutorService scheduler;   // Gestionnaire des tâches planifiées
    private int tempsRestant;                    // Temps restant pour la manche en cours

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Crée un nouveau timer pour une manche
     * @param serveur Référence vers le serveur principal
     */
    public Timer(Serveur serveur) {
        this.serveur = serveur;
        this.tempsRestant = DUREE_MANCHE;
    }

    //==========================================================================
    // Méthodes publiques
    //==========================================================================
    /**
     * Démarre le décompte du temps
     * Envoie des notifications régulières et gère la révélation des lettres
     */
    public void startTimer() {
        scheduler = Executors.newScheduledThreadPool(1);
        System.out.println("Chronomètre démarré : " + tempsRestant + " secondes restantes.");

        scheduler.scheduleAtFixedRate(() -> {
            if (tempsRestant > 0) {
                tempsRestant--;
                serveur.broadcast("TEMPS:" + tempsRestant, null);

                // Révéler des lettres à des moments spécifiques
                if (tempsRestant == (DUREE_MANCHE * 2/3)) {
                    serveur.broadcast("REVEAL_LETTER:1", null);
                } else if (tempsRestant == (DUREE_MANCHE * 1/3)) {
                    serveur.broadcast("REVEAL_LETTER:2", null);
                }

                // Notifications spéciales pour les dernières secondes
                if (tempsRestant <= 10) {
                    serveur.broadcast("Il ne reste plus que " + tempsRestant + " secondes !", null);
                }
            } else {
                stopTimer();
                serveur.broadcast("TEMPS_ECOULE:Le temps est écoulé !", null);
                serveur.getPartie().terminerManche();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Arrête le timer proprement
     * S'assure que toutes les tâches sont terminées
     */
    public void stopTimer() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Retourne le temps restant actuel
     * @return temps restant en secondes
     */
    public int getTempsRestant() {
        return tempsRestant;
    }
}