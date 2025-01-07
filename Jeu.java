// La classe Jeu contient le point d'entrée principal du programme (méthode main) et démarre le serveur du jeu.

public class Jeu {

    // Point d'entrée du programme, qui crée un objet Server et démarre le serveur.
    public static void main(String[] args) {
        Server serveur = new Server(); // Crée une instance de la classe Server pour gérer le serveur du jeu.
        serveur.demarrer(); // Appelle la méthode 'demarrer' sur l'objet serveur pour démarrer le serveur.
    }
}