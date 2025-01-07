// La classe DrawingPanel étend JPanel et permet de dessiner sur le panneau en utilisant les données reçues par le client.
// Elle gère le rendu graphique des lignes (dessins) et de la "gomme" (effacement), avec un fond blanc par défaut.

import java.awt.*;
import javax.swing.*;

public class DrawingPanel extends JPanel {

    // Constructeur de la classe, qui définit le fond du panneau à blanc.
    public DrawingPanel() {
        setBackground(Color.WHITE); // Définit le fond en blanc
    }

    // Méthode qui est appelée pour peindre le composant. Elle gère l'affichage de toutes les lignes dessinées.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Appelle la méthode de la classe parente pour s'assurer que tout est bien nettoyé avant de dessiner.
        Graphics2D g2d = (Graphics2D) g; // Convertit l'objet Graphics en Graphics2D pour des options de rendu plus avancées.

        // Active l'anticrénelage pour un rendu plus fluide des lignes.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Synchronisation avec les données partagées pour éviter les conflits lors de l'accès aux lignes.
        synchronized (Client.getLines()) {

            // Dessine toutes les lignes stockées dans le client.
            for (LineData lineData : Client.lines) {
                g2d.setColor(lineData.color); // Définit la couleur de la ligne.
                g2d.setStroke(new BasicStroke(lineData.color == Color.WHITE ? 10 : 4)); // Définit l'épaisseur de la ligne (plus épaisse si la couleur est blanche pour simuler une gomme).

                // Dessine chaque segment de la ligne en reliant les points consécutifs.
                for (int i = 0; i < lineData.line.size() - 1; i++) {
                    Point p1 = lineData.line.get(i); // Premier point du segment.
                    Point p2 = lineData.line.get(i + 1); // Deuxième point du segment.
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y); // Dessine une ligne entre les deux points.
                }
            }

            // Dessine la ligne en cours, celle qui est activement dessinée par le client.
            g2d.setColor(Client.getCurrentColor()); // Définit la couleur de la ligne courante.
            g2d.setStroke(new BasicStroke(Client.getCurrentColor() == Color.WHITE ? 10 : 4)); // Définit l'épaisseur de la ligne courante (même règle pour la gomme).

            // Dessine les segments de la ligne courante.
            for (int i = 0; i < Client.currentLine.size() - 1; i++) {
                Point p1 = Client.currentLine.get(i); // Premier point du segment de la ligne courante.
                Point p2 = Client.currentLine.get(i + 1); // Deuxième point du segment de la ligne courante.
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y); // Dessine une ligne entre les deux points.
            }
        }
    }
}
