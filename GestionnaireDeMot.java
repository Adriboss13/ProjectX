import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class GestionnaireDeMot {
    private List<Mots> motsDisponibles = new ArrayList<>();

    public GestionnaireDeMot(String cheminFichier) {
        chargerMotsDepuisFichier(cheminFichier);
    }

    // Charger les mots depuis le fichier
    private void chargerMotsDepuisFichier(String cheminFichier) {
        try (BufferedReader br = new BufferedReader(new FileReader(cheminFichier))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                // Supposons que chaque ligne est de la forme : "mot,difficulte"
                String[] parts = ligne.split(",");
                if (parts.length == 2) {
                    String mot = parts[0].trim();
                    String difficulte = parts[1].trim();
                    motsDisponibles.add(new Mots(mot, difficulte));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Mots choisirMotAleatoire() {
        if (motsDisponibles.isEmpty()) return null;
        return motsDisponibles.get(new Random().nextInt(motsDisponibles.size()));
    }
}
