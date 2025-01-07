// La classe Mots représente un mot à deviner, avec sa difficulté et un indicateur de son utilisation.
class Mots {
    private final String mot; // Le mot à deviner
    private boolean dejaUtilise; // Indique si le mot a déjà été utilisé
    private final int difficulte; // Difficulté du mot

    // Constructeur pour initialiser le mot, la difficulté et son état d'utilisation
    public Mots(String mot, int difficulte) {
        this.mot = mot; // Assigne le mot à l'attribut mot
        this.difficulte = difficulte; // Assigne la difficulté du mot
        this.dejaUtilise = false; // Initialise l'état "dejaUtilise" à false, le mot n'a pas encore été utilisé
    }

    // Retourne le mot à deviner
    public String getMot() {
        return mot; // Renvoie l'attribut mot
    }

    // Retourne la difficulté du mot
    public int getDifficulte() {
        return difficulte; // Renvoie l'attribut difficulte
    }

    // Vérifie si le mot a déjà été utilisé
    public boolean isDejaUtilise() {
        return dejaUtilise; // Renvoie true si le mot a été utilisé, sinon false
    }

    // Marque le mot comme étant utilisé
    public void marquerCommeUtilise() {
        dejaUtilise = true; // Modifie l'état "dejaUtilise" à true
    }

    // Représentation textuelle de l'objet, utile pour l'affichage ou les logs
    @Override
    public String toString() {
        return "Mot{" +
                "texte='" + mot + '\'' + // Affiche le mot
                ", difficulte=" + difficulte + // Affiche la difficulté du mot
                '}';
    }
}