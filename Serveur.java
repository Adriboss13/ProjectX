/******************************************************************************
 * Serveur.java
 * Point central du jeu qui gère les connexions clients et le déroulement de la partie
 *
 * Ce serveur :
 * - Accepte les connexions des clients
 * - Gère le début de partie
 * - Coordonne les échanges entre les joueurs
 * - Maintient la synchronisation du jeu
 *****************************************************************************/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Serveur {
    //==========================================================================
    // Constantes
    //==========================================================================
    private static final int PORT = 12345;           // Port d'écoute du serveur
    private static final int MIN_PLAYERS = 3;        // Nombre minimum de joueurs pour démarrer
    private static final int MAX_PLAYERS = 10;       // Nombre maximum de joueurs autorisés
    private static final int BUFFER_SIZE = 8192;     // Taille du buffer réseau
    private static final int COUNTDOWN_SECONDS = 15;  // Temps de compte à rebours avant début de partie

    //==========================================================================
    // Variables membres
    //==========================================================================
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();  // Liste thread-safe des clients
    private final Partie partie;                     // Instance de la partie en cours
    private volatile boolean gameStarted = false;    // État de la partie
    private final Object lock = new Object();        // Verrou pour synchronisation
    private Timer currentTimer;                      // Timer de la manche en cours
    private final Set<String> connectedPlayers;      // Noms des joueurs connectés

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Initialise un nouveau serveur de jeu
     */
    public Serveur() {
        this.partie = new Partie(this, "Mots.txt");
        this.connectedPlayers = Collections.synchronizedSet(new HashSet<>());
    }

    //==========================================================================
    // Méthodes de gestion de partie
    //==========================================================================

    /**
     * Vérifie périodiquement si les conditions de démarrage sont remplies
     */
    private void checkGameStart() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final boolean[] countdownStarted = new boolean[1];  // Pour suivre si le compte à rebours a commencé
        countdownStarted[0] = false;

        scheduler.scheduleAtFixedRate(() -> {
            if (clients.size() >= MIN_PLAYERS && !gameStarted) {
                // Vérifier si tous les joueurs ont défini leur nom
                boolean allPlayersNamed = clients.size() == connectedPlayers.size();

                if (allPlayersNamed && !countdownStarted[0]) {
                    countdownStarted[0] = true;
                    startCountdown(scheduler);
                } else if (!allPlayersNamed) {
                    System.out.println("En attente des noms de tous les joueurs...");
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Lance le compte à rebours avant le début de la partie
     * @param scheduler L'exécuteur de tâches pour le compte à rebours
     */
    private void startCountdown(ScheduledExecutorService scheduler) {
        Runnable startGameTask = new Runnable() {
            int countdown = COUNTDOWN_SECONDS;  // Utilisation de la constante

            @Override
            public void run() {
                if (countdown > 0) {
                    broadcast("La partie commence dans " + countdown + " secondes!", null);
                    countdown--;
                } else {
                    broadcast("La partie commence maintenant!", null);
                    scheduler.shutdown();

                    new Thread(() -> {
                        List<ClientHandler> activeClients;
                        synchronized (clients) {
                            activeClients = new ArrayList<>(clients);
                        }
                        partie.demarrerPartie(activeClients);
                    }).start();
                }
            }
        };

        scheduler.scheduleAtFixedRate(startGameTask, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Démarre le serveur et attend les connexions
     */
    public void demarrer() {
        System.out.println("Démarrage du serveur sur le port " + PORT);

        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReceiveBufferSize(BUFFER_SIZE);
            serverSocket.bind(new InetSocketAddress(PORT));

            new Thread(this::checkGameStart).start();

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            executor.setKeepAliveTime(60L, TimeUnit.SECONDS);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setTcpNoDelay(true);
                clientSocket.setSendBufferSize(BUFFER_SIZE);
                clientSocket.setReceiveBufferSize(BUFFER_SIZE);

                if (gameStarted && clients.size() >= MAX_PLAYERS) {
                    try (BufferedWriter out = new BufferedWriter(
                            new OutputStreamWriter(clientSocket.getOutputStream()))) {
                        out.write("La partie est pleine (maximum " + MAX_PLAYERS + " joueurs)\n");
                        out.flush();
                    }
                    clientSocket.close();
                    continue;
                }

                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                executor.execute(clientHandler);

                if (!gameStarted) {
                    System.out.println("Nouveau client connecté. Total: " + clients.size());
                    broadcast("Un nouveau joueur a rejoint la partie. (" + clients.size() + " joueurs)", null);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
        }
    }

    //==========================================================================
    // Méthodes de gestion du timer
    //==========================================================================

    /**
     * Démarre un nouveau timer pour la manche en cours
     */
    public void startTimer() {
        if (currentTimer != null) {
            currentTimer.stopTimer();
        }
        currentTimer = new Timer(this);
        currentTimer.startTimer();
    }

    //==========================================================================
    // Méthodes de communication
    //==========================================================================

    /**
     * Diffuse un message à tous les clients sauf celui spécifié
     * @param message Le message à diffuser
     * @param exclude Le client à exclure (peut être null)
     */
    public void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler client : clients) {
            if (client != exclude && client.isActive()) {
                client.envoyerMessageAsync(message);
            }
        }
    }

    /**
     * Diffuse des données de dessin à tous les clients sauf celui spécifié
     * @param drawingData Les données de dessin à diffuser
     * @param exclude Le client à exclure
     */
    public void broadcastDrawing(String drawingData, ClientHandler exclude) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != exclude && client.isActive()) {
                    client.envoyerMessage("DRAW:" + drawingData);
                }
            }
        }
    }

    //==========================================================================
    // Getters & Setters
    //==========================================================================

    public Timer getCurrentTimer() { return currentTimer; }
    public Partie getPartie() { return partie; }
    public List<ClientHandler> getClients() {
        synchronized (clients) {
            return new ArrayList<>(clients);
        }
    }

    /**
     * Recherche un client par son joueur associé
     * @param joueur Le joueur à rechercher
     * @return Le ClientHandler associé ou null si non trouvé
     */
    public ClientHandler getClientHandler(Joueur joueur) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getJoueur().equals(joueur)) {
                    return client;
                }
            }
        }
        return null;
    }

    //==========================================================================
    // Méthodes de gestion des joueurs
    //==========================================================================

    /**
     * Enregistre le nom d'un nouveau joueur
     * @param playerName Le nom du joueur
     */
    public void playerNameSet(String playerName) {
        connectedPlayers.add(playerName);
    }

    /**
     * Supprime un client de la partie
     * @param client Le client à supprimer
     */
    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
            broadcast(client.getJoueur().getNom() + " a quitté la partie.", null);

            if (clients.size() < MIN_PLAYERS && gameStarted) {
                broadcast("Trop peu de joueurs pour continuer. Fin de la partie.", null);
                partie.setPartieEnCours(false);
            }
        }
    }

    //==========================================================================
    // Point d'entrée
    //==========================================================================

    /**
     * Point d'entrée principal du serveur
     */
    public static void main(String[] args) {
        new Serveur().demarrer();
    }
}