// La classe GestionnaireMots permet de gérer une liste de mots et d'effectuer des opérations telles que l'ajout de mots,
// le chargement des mots depuis un fichier et l'obtention d'un mot aléatoire. Elle stocke ces mots dans une liste.

import java.io.*;
import java.util.*;

public class GestionnaireMots {
    private List<Mots> mots; // Liste pour stocker les mots

    // Constructeur qui initialise la liste de mots.
    public GestionnaireMots() {
        this.mots = new ArrayList<>(); // Initialise la liste vide de mots
    }

    // Ajouter un mot à la liste des mots
    public void ajouterMot(Mots mot) {
        mots.add(mot); // Ajoute le mot passé en paramètre à la liste
    }

    // Charger les mots depuis un fichier texte.
    // Chaque ligne du fichier doit être au format "mot,difficulté".
    public void chargerMotsDepuisFichier(String cheminFichier) throws IOException {
        // Utilisation d'un BufferedReader pour lire le fichier ligne par ligne
        try (BufferedReader reader = new BufferedReader(new FileReader(cheminFichier))) {
            String ligne; // Variable pour stocker chaque ligne lue du fichier
            while ((ligne = reader.readLine()) != null) { // Continue tant qu'il y a des lignes à lire
                // Sépare la ligne en deux parties : le mot et la difficulté
                String[] parts = ligne.split(",");
                if (parts.length == 2) { // Vérifie que la ligne est bien formatée
                    String motTexte = parts[0].trim(); // Récupère le mot (en supprimant les espaces inutiles)
                    try {
                        int difficulte = Integer.parseInt(parts[1].trim()); // Convertit la difficulté en entier
                        // Si la difficulté est valide, on ajoute le mot à la liste
                        ajouterMot(new Mots(motTexte, difficulte));
                    } catch (NumberFormatException e) {
                        // En cas d'erreur de format pour la difficulté, on affiche un message d'erreur
                        System.err.println("Difficulté invalide pour le mot : " + ligne);
                    }
                } else {
                    // Si la ligne ne contient pas exactement deux parties (mot et difficulté), on affiche un message d'erreur
                    System.err.println("Ligne mal formatée : " + ligne);
                }
            }
        }
    }

    // Retourne un mot aléatoire depuis la liste des mots
    public Mots obtenirMotAleatoire() {
        // Vérifie si la liste est vide
        if (mots.isEmpty()) {
            throw new IllegalStateException("Aucun mot disponible."); // Lève une exception si la liste est vide
        }
        Random random = new Random(); // Crée un générateur de nombres aléatoires
        return mots.get(random.nextInt(mots.size())); // Retourne un mot choisi aléatoirement dans la liste
    }

    // Retourne la liste des mots, utilisée pour l'affichage ou les tests
    public List<Mots> getMots() {
        return mots; // Renvoie la liste des mots
    }
}
