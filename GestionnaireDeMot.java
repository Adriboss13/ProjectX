/******************************************************************************
 * GestionnaireDeMot.java
 * Gestion du dictionnaire de mots pour le jeu
 *
 * Cette classe gère :
 * - Le chargement des mots depuis un fichier
 * - La sélection aléatoire des mots
 * - Le stockage des mots et leurs difficultés
 *****************************************************************************/

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Classe gérant le dictionnaire de mots et leur sélection
 */
class GestionnaireDeMot {
    //==========================================================================
    // Variables membres
    //==========================================================================
    private final List<Mots> motsDisponibles;     // Liste des mots disponibles pour le jeu

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Initialise le gestionnaire et charge les mots depuis le fichier
     * @param cheminFichier Chemin vers le fichier contenant les mots
     */
    public GestionnaireDeMot(String cheminFichier) {
        motsDisponibles = new ArrayList<>();
        chargerMotsDepuisFichier(cheminFichier);
    }

    //==========================================================================
    // Méthodes privées
    //==========================================================================
    /**
     * Charge les mots depuis le fichier spécifié
     * Format attendu : "mot,difficulté" par ligne
     * @param cheminFichier Chemin du fichier à charger
     */
    private void chargerMotsDepuisFichier(String cheminFichier) {
        try (BufferedReader br = new BufferedReader(new FileReader(cheminFichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.split(",");
                if (parts.length == 2) {
                    String mot = parts[0].trim();
                    String difficulte = parts[1].trim();
                    motsDisponibles.add(new Mots(mot, difficulte));
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des mots depuis le fichier: " + e.getMessage());
        }
    }

    //==========================================================================
    // Méthodes publiques
    //==========================================================================
    /**
     * Sélectionne un mot aléatoire dans la liste
     * @return Un mot aléatoire ou null si la liste est vide
     */
    public Mots choisirMotAleatoire() {
        if (motsDisponibles.isEmpty()) return null;
        return motsDisponibles.get(new Random().nextInt(motsDisponibles.size()));
    }

    /**
     * Retourne la liste complète des mots disponibles
     * @return Liste des mots disponibles
     */
    public List<Mots> getMotsDisponibles() {
        return motsDisponibles;
    }
}
