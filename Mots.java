// Mots.java : Représente un mot à deviner
class Mots {
    private String mot;
    private String difficulte;

    public Mots(String mot, String difficulte) {
        this.mot = mot;
        this.difficulte = difficulte;
    }

    public String getMot() {
        return mot;
    }

    public String getDifficulte() {
        return difficulte;
    }
}