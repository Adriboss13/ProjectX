/******************************************************************************
 * ClientHandler.java
 * Gestionnaire de connexion client
 *
 * Cette classe gère :
 * - La connexion individuelle d'un client
 * - La communication bidirectionnelle
 * - Le traitement des messages asynchrones
 * - La gestion du cycle de vie de la connexion
 *****************************************************************************/

import java.io.*;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gère une connexion client individuelle
 * Implémente Runnable pour fonctionner dans un thread dédié
 */
public class ClientHandler implements Runnable {
    //==========================================================================
    // Variables membres
    //==========================================================================
    private final Socket socket;                        // Socket de connexion
    private final Serveur serveur;                     // Référence au serveur principal
    private final ExecutorService messageExecutor;      // Exécuteur pour messages asynchrones
    private final BufferedWriter out;                  // Flux de sortie
    private BufferedReader in;                         // Flux d'entrée
    private Joueur joueur;                            // Joueur associé
    private volatile boolean isActive = true;          // État de la connexion

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Initialise un nouveau gestionnaire de client
     * @param socket Socket de connexion du client
     * @param serveur Référence au serveur principal
     */
    public ClientHandler(Socket socket, Serveur serveur) {
        this.socket = socket;
        this.serveur = serveur;
        this.messageExecutor = Executors.newSingleThreadExecutor();
        try {
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //==========================================================================
    // Méthodes d'exécution principale
    //==========================================================================
    /**
     * Point d'entrée du thread
     * Gère la connexion et le traitement des messages
     */
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String nomJoueur = in.readLine();
            joueur = new Joueur(nomJoueur);
            System.out.println("Nouveau joueur connecté : " + nomJoueur);

            serveur.getPartie().ajouterJoueur(joueur);
            serveur.playerNameSet(nomJoueur); // Ajouter cette ligne pour notifier le serveur
            if (serveur.getClients().size() >= 3) {
                serveur.broadcast(nomJoueur + " a rejoint la partie!", this);
            }

            String message;
            while (isActive && (message = in.readLine()) != null) {
                final String finalMessage = message;
                if (message.startsWith("DRAW:")) {
                    serveur.broadcastDrawing(message.substring(5), this);
                } else {
                    CompletableFuture.runAsync(() -> processMessage(finalMessage));
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur avec le client " + (joueur != null ? joueur.getNom() : "inconnu") + ": " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * Traite les messages reçus du client
     * @param message Message à traiter
     */
    private void processMessage(String message) {
        // Ne logger que les messages non-DRAW
        if (!message.startsWith("DRAW:")) {
            System.out.println("Message reçu du client " + joueur.getNom() + " : " + message);
        }
        // Gérer les messages ici...
        if (message.startsWith("CHOSEN_WORD:")) {
            String chosenWord = message.substring(12);
            serveur.getPartie().setMotChoisi(chosenWord);
        }
        if (message.startsWith("CHAT:")) {
            String chatText = message.substring(5).trim();
            boolean trouve = serveur.getPartie().verifierMot(joueur, chatText);
            if (!trouve) {
                serveur.broadcast("CHAT:" + joueur.getNom() + ": " + chatText, null);
            }
        }
        if (message.startsWith("CLEAR:")) {
            serveur.broadcast("CLEAR:", null);
        }
    }

    //==========================================================================
    // Méthodes de gestion de la connexion
    //==========================================================================
    /**
     * Ferme proprement la connexion
     * Nettoie les ressources et notifie le serveur
     */
    private void closeConnection() {
        isActive = false;
        messageExecutor.shutdown();
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
        }
        serveur.removeClient(this);
    }

    //==========================================================================
    // Méthodes de communication
    //==========================================================================
    /**
     * Envoie un message de manière asynchrone
     * @param message Message à envoyer
     */
    public void envoyerMessageAsync(String message) {
        if (!isActive) return;
        messageExecutor.execute(() -> {
            try {
                synchronized(out) {
                    out.write(message + "\n");
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("Erreur envoi message: " + e.getMessage());
                closeConnection();
            }
        });
    }

    /**
     * Méthode de compatibilité pour l'envoi de messages
     * @param message Message à envoyer
     */
    public void envoyerMessage(String message) {
        envoyerMessageAsync(message);
    }

    //==========================================================================
    // Getters
    //==========================================================================
    /**
     * Récupère le joueur associé
     * @return Le joueur
     */
    public Joueur getJoueur() {
        return joueur;
    }

    /**
     * Vérifie si la connexion est active
     * @return true si la connexion est active
     */
    public boolean isActive() {
        return isActive;
    }
}