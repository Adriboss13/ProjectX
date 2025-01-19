/******************************************************************************
 * DrawingPanel.java
 * Panneau de dessin pour le jeu
 *
 * Cette classe gère :
 * - L'affichage des dessins
 * - Le rendu des lignes
 * - L'anti-aliasing pour une meilleure qualité
 *****************************************************************************/

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * Panneau personnalisé pour le dessin
 * Hérite de JPanel pour fournir une zone de dessin
 */
class DrawingPanel extends JPanel {
    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Initialise le panneau de dessin
     * Configure le fond en blanc
     */
    public DrawingPanel() {
        setBackground(Color.WHITE);
    }

    //==========================================================================
    // Méthodes de rendu
    //==========================================================================
    /**
     * Méthode de dessin principale
     * Gère le rendu de toutes les lignes avec anti-aliasing
     * @param g Le contexte graphique
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (Client.getLines()) {
            // Dessiner les lignes existantes
            for (LineData lineData : Client.getLines()) {
                drawLine(g2, lineData);
            }

            // Dessiner la ligne en cours
            if (!Client.getCurrentLine().isEmpty()) {
                drawLine(g2, new LineData(Client.getCurrentLine(),
                                        Client.getCurrentColor(),
                                        Client.getCurrentStrokeWidth()));
            }
        }
        g2.dispose();
    }

    /**
     * Dessine une ligne individuelle
     * @param g2d Le contexte graphique 2D
     * @param lineData Les données de la ligne à dessiner
     */
    private void drawLine(Graphics2D g2d, LineData lineData) {
        g2d.setColor(lineData.getColor());
        g2d.setStroke(new BasicStroke(lineData.getStrokeWidth()));

        ArrayList<Point> points = lineData.getLine(); // Utiliser le getter correctement défini
        for (int i = 0; i < points.size() - 1; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get(i + 1);
            g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }
}