// La classe MessageListener hérite de Thread et écoute les messages reçus depuis le serveur pour les traiter.

import java.io.*;
import java.util.Base64;
import javax.swing.SwingUtilities;

public class MessageListener extends Thread {
    private final Client client; // Instance de la classe Client, utilisée pour communiquer avec l'interface graphique.

    // Constructeur qui prend en paramètre une instance de Client, permettant à MessageListener d'interagir avec l'interface graphique.
    public MessageListener(Client client) {
        this.client = client; // Assigne l'instance de Client à la variable membre
    }

    // La méthode run() est exécutée lors du démarrage du thread, elle écoute les messages du serveur.
    @Override
    public void run() {
        try {
            String message;
            // Boucle infinie pour lire les messages reçus du serveur.
            while ((message = Client.getIn().readLine()) != null) {
                // Décode le message reçu en Base64 pour récupérer les données sous forme binaire.
                byte[] data = Base64.getDecoder().decode(message);
                // Crée un flux d'entrée à partir des données décodées pour lire l'objet transmis.
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                // Lit l'objet LineData qui a été sérialisé et reçu.
                LineData receivedLineData = (LineData) ois.readObject();
                // Synchronise l'accès à la liste de lignes pour éviter des conflits d'accès en multithreading.
                synchronized (Client.getLines()) {
                    Client.getLines().add(receivedLineData); // Ajoute la nouvelle ligne à la liste de lignes du client
                }
                // Utilise SwingUtilities.invokeLater pour effectuer l'appel à repaint() sur le thread de l'interface graphique
                // pour mettre à jour l'affichage sans bloquer l'interface.
                SwingUtilities.invokeLater(() -> client.repaint());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace(); // Affiche les erreurs s'il y en a
        }
    }
}