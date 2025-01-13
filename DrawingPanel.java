import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

class DrawingPanel extends JPanel {
    public DrawingPanel() {
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        synchronized (Client.getLines()) {
            for (LineData lineData : Client.getLines()) {
                g2d.setColor(lineData.color);
                g2d.setStroke(new BasicStroke(lineData.color == Color.WHITE ? 20 : 4));
                
                for (int i = 0; i < lineData.line.size() - 1; i++) {
                    Point p1 = lineData.line.get(i);
                    Point p2 = lineData.line.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
            
            // Dessin de la ligne en cours
            ArrayList<Point> currentLine = Client.getCurrentLine();
            if (currentLine.size() > 1) {
                g2d.setColor(Client.getCurrentColor());
                g2d.setStroke(new BasicStroke(Client.getCurrentColor() == Color.WHITE ? 20 : 4));
                
                for (int i = 0; i < currentLine.size() - 1; i++) {
                    Point p1 = currentLine.get(i);
                    Point p2 = currentLine.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }
}