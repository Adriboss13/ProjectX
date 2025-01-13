import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Serveur {
    private static final int PORT = 12345;
    private List<ClientHandler> clients = new ArrayList<>();
    private Partie partie;
    private final Object lock = new Object(); // Verrou pour la synchronisation

    public static void main(String[] args) {
        new Serveur().demarrer();
    }

    public void demarrer() {
        System.out.println("Serveur en cours de démarrage...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur démarré sur le port " + PORT);

            // Création de la partie avec le serveur et le chemin du fichier Mots.txt
            partie = new Partie(this, "Mots.txt");

            // Création du CountDownLatch pour attendre que tous les joueurs soient prêts
            CountDownLatch latch = new CountDownLatch(3); // Attendre 3 joueurs prêts

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nouveau joueur connecté.");
                ClientHandler clientHandler = new ClientHandler(socket, this, latch); // Passer le latch ici
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String message, ClientHandler excludeClient) {
        synchronized(clients) {
            for (ClientHandler client : clients) {
                if (client != excludeClient) {
                    client.envoyerMessage(message);
                }
            }
        }
    }

    public Partie getPartie() {
        return partie;
    }

    // Méthode de diffusion des dessins
    public void broadcastDrawing(String drawingData, ClientHandler excludeClient) {
        synchronized(clients) {
            for (ClientHandler client : clients) {
                if (client != excludeClient) {
                    client.envoyerMessage("DRAW:" + drawingData);
                }
            }
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private Joueur joueur;
        private Serveur serveur; // Référence au serveur ajoutée ici
        private CountDownLatch latch; // Latch pour synchroniser l'attente des joueurs

        // Modification du constructeur pour accepter une référence à Serveur et le CountDownLatch
        public ClientHandler(Socket socket, Serveur serveur, CountDownLatch latch) {
            this.socket = socket;
            this.serveur = serveur;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("Bienvenue ! Entrez votre nom :");
                String nom = in.readLine();
                joueur = new Joueur(nom);

                // Ajout du joueur à la partie de manière synchronisée
                synchronized(lock) {
                    serveur.getPartie().ajouterJoueur(joueur);
                }

                out.println("Vous êtes connecté, en attente des autres joueurs...");
                latch.countDown();
                latch.await();

                synchronized(lock) {
                    if (!serveur.getPartie().isPartieEnCours()) {
                        serveur.getPartie().setPartieEnCours(true);
                        out.println("Tous les joueurs sont prêts ! La partie commence.");
                        serveur.broadcast("La partie commence !", null);
                        serveur.getPartie().demarrerPartie(serveur.clients);
                    }
                }

                // Réception des messages
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("DRAW:")) {
                        serveur.broadcastDrawing(message.substring(5), this);
                    } else {
                        // Vérification des propositions de mots
                        synchronized(lock) {
                            if (serveur.getPartie().verifierMot(joueur, message)) {
                                out.println("Bravo, vous avez trouvé le mot !");
                                serveur.broadcast(joueur.getNom() + " a trouvé le mot : " + message, this);
                            } else {
                                serveur.broadcast(joueur.getNom() + " a proposé : " + message, this);
                            }
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized(clients) {
                    serveur.clients.remove(this);
                }
            }
        }

        public void envoyerMessage(String message) {
            out.println(message);
        }

        public Joueur getJoueur() {
            return joueur;
        }
    }
}
