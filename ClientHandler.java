// Cette classe représente un gestionnaire pour chaque client connecté au serveur.
// Elle permet de recevoir des messages du client et de les transmettre à tous les autres clients connectés.

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    // Attributs privés pour gérer la connexion d'un client
    private final Socket socket; // Socket représentant la connexion avec le client
    private final PrintWriter out; // Flux de sortie pour envoyer des messages au client
    private BufferedReader in; // Flux d'entrée pour lire les messages du client

    // Constructeur pour initialiser le gestionnaire avec un socket et un flux de sortie
    public ClientHandler(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            // Initialisation du flux d'entrée pour lire les messages provenant du client
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message; // Variable pour stocker les messages reçus

            // Boucle principale : lire les messages envoyés par le client
            while ((message = in.readLine()) != null) {
                // Transmettre le message à tous les autres clients connectés
                for (PrintWriter client : Server.getClients()) { // Récupération de la liste des clients
                    if (client != out) { // Vérifie que le message n'est pas renvoyé à l'expéditeur
                        client.println(message); // Envoi du message
                    }
                }
            }
        } catch (IOException e) {
            // En cas d'erreur de communication, afficher une trace de l'exception
            e.printStackTrace();
        } finally {
            // Bloc exécuté à la fin (en sortie de boucle ou en cas d'exception)
            try {
                // Fermer la connexion avec le client
                socket.close();
                // Retirer le client de la liste des clients connectés
                Server.getClients().remove(out); // Mise à jour de la liste côté serveur
            } catch (IOException e) {
                // Gérer les erreurs potentielles lors de la fermeture du socket
                e.printStackTrace();
            }
        }
    }
}