/******************************************************************************
 * Mots.java
 * Représentation d'un mot et de sa difficulté dans le jeu
 *
 * Cette classe stocke :
 * - Le mot à deviner
 * - Son niveau de difficulté
 *****************************************************************************/

/**
 * Classe représentant un mot à deviner dans le jeu
 * Chaque mot est associé à un niveau de difficulté
 */
public class Mots {
    //==========================================================================
    // Variables membres
    //==========================================================================
    private final String mot;         // Le mot à deviner
    private final String difficulte;  // Niveau de difficulté (généralement "1" ou "2")

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Crée un nouveau mot avec sa difficulté
     * @param mot Le mot à deviner
     * @param difficulte Le niveau de difficulté du mot
     */
    public Mots(String mot, String difficulte) {
        this.mot = mot;
        this.difficulte = difficulte;
    }

    //==========================================================================
    // Getters
    //==========================================================================
    /**
     * Retourne le mot
     * @return Le mot à deviner
     */
    public String getMot() {
        return mot;
    }

    /**
     * Retourne la difficulté du mot
     * @return Le niveau de difficulté
     */
    public String getDifficulte() {
        return difficulte;
    }
}