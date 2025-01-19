/******************************************************************************
 * Client.java
 * Interface graphique et gestion de la connexion client
 *
 * Cette classe gère :
 * - L'interface utilisateur du jeu
 * - La connexion au serveur
 * - Le dessin et l'envoi des traits
 * - Le chat et les interactions utilisateur
 *****************************************************************************/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class Client extends JFrame {
    //==========================================================================
    // Constantes de configuration
    //==========================================================================
    private static final String HOST = "localhost";    // Hôte par défaut
    private static final int PORT = 12345;            // Port par défaut
    private static final int BUFFER_SIZE = 8192;      // Taille du buffer réseau

    //==========================================================================
    // Composants réseau
    //==========================================================================
    private Socket socket;                            // Socket de connexion
    private BufferedReader in;                        // Flux d'entrée
    private BufferedWriter out;                       // Flux de sortie

    //==========================================================================
    // Composants de dessin
    //==========================================================================
    private static final List<LineData> lines = Collections.synchronizedList(new ArrayList<>());
    private static Color currentColor = Color.BLACK;
    private static final ArrayList<Point> currentLine = new ArrayList<>();
    private static int currentStrokeWidth = 2;        // Taille du trait
    private boolean canDraw = false;                  // Autorisation de dessiner

    //==========================================================================
    // Composants d'interface graphique
    //==========================================================================
    private JButton selectedColorButton = null;
    private final JTextArea chatArea;
    private final DrawingPanel drawingPanel;
    private final JTextField chatInput;
    private final JLabel timerLabel;
    private final JPanel wordChoicePanel;
    private final JLabel currentWordLabel;
    private final JPanel topPanel; // Ajout d'un champ pour manipuler topPanel
    private final JPanel colorPanel; // Modification : retirer l'initialisation ici
    private final JPanel gamePanel; // Nouveau panneau pour contenir tous les éléments de jeu

    //==========================================================================
    // État du jeu
    //==========================================================================
    private String playerRole = "";                   // Rôle du joueur
    private boolean hasFoundWord = false;             // Mot trouvé ou non
    private String motActuel = null;                  // Mot en cours
    private final Set<Integer> revealedIndices = new HashSet<>(); // Pour suivre les positions des lettres révélées

    //==========================================================================
    // Constructeur et initialisation
    //==========================================================================
    /**
     * Crée et initialise l'interface du client
     * @param host Adresse du serveur
     * @param port Port du serveur
     */
    public Client(String host, int port) {
        setTitle("Dessiner c'est Gagné");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout principal
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Créer le panneau d'attente
        JPanel waitingPanel = new JPanel(new GridBagLayout());
        JLabel waitingLabel = new JLabel("En attente de joueurs...");
        waitingLabel.setFont(new Font("Arial", Font.BOLD, 24));
        waitingPanel.add(waitingLabel);

        // Créer le panneau de jeu principal
        gamePanel = new JPanel(new BorderLayout(10, 10));

        // Panneau principal divisé en deux
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);

        // Panneau gauche (dessin + contrôles)
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));

        // Créer un panneau pour la partie supérieure avec une hauteur fixe
        topPanel = new JPanel(new BorderLayout());
        topPanel.setPreferredSize(new Dimension(800, 200));

        // Timer et mot courant
        JPanel infoBar = new JPanel(new BorderLayout());
        timerLabel = new JLabel("Temps: 60s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        currentWordLabel = new JLabel("En attente...", SwingConstants.CENTER);
        currentWordLabel.setFont(new Font("Arial", Font.BOLD, 20));
        infoBar.add(timerLabel, BorderLayout.EAST);
        infoBar.add(currentWordLabel, BorderLayout.CENTER);

        // Panneau de choix des mots (occupe tout l'espace restant en haut)
        wordChoicePanel = new JPanel();
        wordChoicePanel.setLayout(new BoxLayout(wordChoicePanel, BoxLayout.Y_AXIS));
        wordChoicePanel.setBackground(new Color(200, 200, 255));  // Couleur de fond plus visible
        wordChoicePanel.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        wordChoicePanel.setVisible(false); // Caché par défaut

        // Assembler la partie supérieure
        topPanel.add(infoBar, BorderLayout.NORTH);
        topPanel.add(wordChoicePanel, BorderLayout.CENTER);

        // Ajouter au panneau principal
        leftPanel.add(topPanel, BorderLayout.NORTH);

        // Panneau de dessin
        drawingPanel = new DrawingPanel();
        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        leftPanel.add(drawingPanel, BorderLayout.CENTER);

        // Initialiser colorPanel avant de l'utiliser
        colorPanel = new JPanel(new FlowLayout());
        createColorPanel(); // Cette méthode va maintenant remplir le panel au lieu d'en créer un nouveau

        // Palette de couleurs
        leftPanel.add(colorPanel, BorderLayout.SOUTH); // Utilisation de la variable d'instance déjà initialisée
        leftPanel.add(colorPanel, BorderLayout.SOUTH);

        // Panneau droit (chat)
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));

        // Zone de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(300, 600));

        // Zone de saisie du chat
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendMessage());

        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(chatInput, BorderLayout.SOUTH);

        // Ajout des panneaux au splitPane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        // Ajouter tous les composants au gamePanel au lieu du frame directement
        gamePanel.add(splitPane, BorderLayout.CENTER);

        // Commencer avec seulement le panneau d'attente visible
        add(waitingPanel, BorderLayout.CENTER);
        gamePanel.setVisible(false);

        // Configuration des événements de la souris
        setupMouseListeners();

        // Connexion au serveur
        connectToServer(host, port);
    }

    //==========================================================================
    // Méthodes de gestion du dessin
    //==========================================================================
    /**
     * Configure les écouteurs de souris pour le dessin
     */
    private void setupMouseListeners() {
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (canDraw) {
                    currentLine.clear();
                    currentLine.add(e.getPoint());
                    sendDrawingData();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (canDraw) {
                    lines.add(new LineData(new ArrayList<>(currentLine), currentColor, currentStrokeWidth));
                    currentLine.clear();
                    sendDrawingData();
                }
            }
        });

        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (canDraw) {
                    currentLine.add(e.getPoint());
                    sendDrawingData();
                    drawingPanel.repaint();
                }
            }
        });
    }

    /**
     * Crée le panneau de couleurs et outils
     */
    private void createColorPanel() {
        Color[] colors = {
            Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN,
            Color.BLUE, Color.MAGENTA, Color.PINK,Color.BLACK, Color.WHITE
        };

        boolean first = true;
        for (Color color : colors) {
            JButton colorButton = new JButton();
            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setBackground(color);
            colorButton.setOpaque(true);
            colorButton.setContentAreaFilled(true);
            colorButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));

            colorButton.addActionListener(e -> {
                if (selectedColorButton != null) {
                    selectedColorButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                }
                selectedColorButton = colorButton;
                colorButton.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

                // Définir la taille de trait en fonction de la couleur
                if (color.equals(Color.WHITE)) {
                    currentStrokeWidth = 40; // Gomme plus grande
                } else {
                    currentStrokeWidth = 4; // Trait normal
                }
                currentColor = color;
            });

            if (color.equals(Color.BLACK)) { // Sélectionner le noir par défaut
                selectedColorButton = colorButton;
                colorButton.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                currentColor = color;
            } else if (first) {
                selectedColorButton = colorButton;
                colorButton.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                currentColor = color;
                first = false;
            }

            colorPanel.add(colorButton);
        }

        // Bouton pour augmenter la taille
        JButton plusButton = new JButton("+");
        plusButton.setPreferredSize(new Dimension(30, 30));
        plusButton.setOpaque(true);
        plusButton.setContentAreaFilled(true);
        plusButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        plusButton.addActionListener(e -> {
            if (!currentColor.equals(Color.WHITE)) {
                currentStrokeWidth += 2;
            }
        });
        colorPanel.add(plusButton);

        // Bouton pour diminuer la taille
        JButton minusButton = new JButton("-");
        minusButton.setPreferredSize(new Dimension(30, 30));
        minusButton.setOpaque(true);
        minusButton.setContentAreaFilled(true);
        minusButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        minusButton.addActionListener(e -> {
            if (!currentColor.equals(Color.WHITE) && currentStrokeWidth > 2) {
                currentStrokeWidth -= 2;
            }
        });
        colorPanel.add(minusButton);

        // Bouton pour tout effacer
        JButton clearButton = new JButton("🗑️");
        clearButton.setPreferredSize(new Dimension(30, 30));
        clearButton.setOpaque(true);
        clearButton.setContentAreaFilled(true);
        clearButton.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        clearButton.addActionListener(e -> {
            lines.clear();
            drawingPanel.repaint();
            try {
                synchronized(out) {
                    out.write("CLEAR:\n");
                    out.flush();
                }
            } catch (IOException ex) {
                System.err.println("Erreur lors de l'envoi de la commande clear: " + ex.getMessage());
            }
        });
        colorPanel.add(clearButton);

        colorPanel.revalidate();
        colorPanel.repaint();
    }

    /**
     * Envoie les données de dessin au serveur
     */
    private void sendDrawingData() {
        if (!canDraw) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            // Inclure la taille du trait lors de la création du LineData
            LineData lineData = new LineData(new ArrayList<>(currentLine), currentColor, currentStrokeWidth);
            oos.writeObject(lineData);
            oos.flush();

            synchronized(out) {
                out.write("DRAW:" + Base64.getEncoder().encodeToString(baos.toByteArray()) + "\n");
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'envoi des données de dessin: " + e.getMessage());
        }
    }

    //==========================================================================
    // Méthodes de communication réseau
    //==========================================================================
    /**
     * Établit la connexion avec le serveur
     */
    private void connectToServer(String host, int port) {
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true); // Désactiver l'algorithme de Nagle
            socket.setSendBufferSize(BUFFER_SIZE);
            socket.setReceiveBufferSize(BUFFER_SIZE);
            socket.connect(new InetSocketAddress(host, port));

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String playerName = JOptionPane.showInputDialog(this,
                "Entrez votre nom:", "Bienvenue", JOptionPane.QUESTION_MESSAGE);

            if (playerName == null || playerName.trim().isEmpty()) {
                System.exit(0);
            }

            out.write(playerName + "\n");
            out.flush();

            // Thread de réception des messages
            new Thread(this::receiveMessages).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Impossible de se connecter au serveur: " + e.getMessage(),
                "Erreur de connexion",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Gère la réception des messages du serveur
     */
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> processMessage(finalMessage));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "Connexion perdue avec le serveur: " + e.getMessage(),
                    "Erreur réseau",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });
        }
    }

    /**
     * Envoie un message de chat
     */
    private void sendMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            try {
                synchronized(out) {
                    out.write("CHAT:" + message + "\n");
                    out.flush();
                }
                chatInput.setText("");
            } catch (IOException e) {
                System.err.println("Erreur d'envoi: " + e.getMessage());
            }
        }
    }

    //==========================================================================
    // Méthodes de traitement des messages
    //==========================================================================
    /**
     * Traite les messages reçus du serveur
     */
    private void processMessage(String message) {
        // Ne logger que les messages non-DRAW
        if (!message.startsWith("DRAW:")) {
            System.out.println("DEBUG - Message reçu : " + message);
        }

        if (message.startsWith("La partie commence dans")) {
            // Remplacer le panneau d'attente par le panneau de jeu
            SwingUtilities.invokeLater(() -> {
                getContentPane().removeAll();
                add(gamePanel, BorderLayout.CENTER);
                gamePanel.setVisible(true);
                revalidate();
                repaint();
            });
        }

        if (message.startsWith("ROLE:")) {
            playerRole = message.substring(5);
            canDraw = false;
            hasFoundWord = false;
            motActuel = null;
            revealedIndices.clear(); // Réinitialiser les lettres révélées

            if (playerRole.equals("dessinateur")) {
                chatArea.append("Vous êtes le dessinateur pour ce tour!\n");
                currentWordLabel.setText("En attente du choix du mot...");
                colorPanel.setVisible(true);
                // Ne pas manipuler wordChoicePanel ici, il sera géré par CHOIX_MOTS
                System.out.println("DEBUG - Rôle dessinateur assigné");
            } else {
                chatArea.append("Vous devez deviner le mot!\n");
                currentWordLabel.setText(formatMotCache(null));
                wordChoicePanel.setVisible(false);
                topPanel.setPreferredSize(new Dimension(800, 50)); // Réduire la taille pour les devineurs
                colorPanel.setVisible(false);
            }
            topPanel.revalidate();
            topPanel.repaint();
        }
        else if (message.startsWith("CHOIX_MOTS:")) {
            String[] words = message.substring(11).split(",");
            System.out.println("DEBUG - Mots reçus: " + Arrays.toString(words));

            if (playerRole.equals("dessinateur")) {
                topPanel.setPreferredSize(new Dimension(800, 200)); // Agrandir pour le choix des mots
                SwingUtilities.invokeLater(() -> {
                    try {
                        wordChoicePanel.removeAll();
                        wordChoicePanel.setLayout(new BoxLayout(wordChoicePanel, BoxLayout.Y_AXIS));

                        // Panneau pour le titre avec fond
                        JPanel titlePanel = new JPanel();
                        titlePanel.setOpaque(true);
                        titlePanel.setBackground(new Color(200, 200, 255));
                        JLabel titleLabel = new JLabel("Choisissez votre mot !");
                        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
                        titleLabel.setForeground(new Color(50, 50, 255));
                        titlePanel.add(titleLabel);

                        wordChoicePanel.add(titlePanel);
                        wordChoicePanel.add(Box.createVerticalStrut(20));

                        // Panneau pour les boutons
                        JPanel buttonsPanel = new JPanel();
                        buttonsPanel.setLayout(new GridLayout(words.length, 1, 0, 10));
                        buttonsPanel.setOpaque(false);

                        for (String word : words) {
                            JButton button = new JButton(word);
                            button.setFont(new Font("Arial", Font.BOLD, 20));
                            button.setBackground(new Color(230, 230, 255));
                            button.setPreferredSize(new Dimension(300, 50));
                            button.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(100, 100, 255), 2),
                                BorderFactory.createEmptyBorder(10, 20, 10, 20)
                            ));

                            button.addActionListener(e -> {
                                try {
                                    synchronized(out) {
                                        out.write("CHOSEN_WORD:" + word + "\n");
                                        out.flush();
                                    }
                                    wordChoicePanel.setVisible(false);
                                    currentWordLabel.setText("Mot à dessiner : " + word);
                                } catch (IOException ex) {
                                    System.err.println("Erreur lors de l'envoi du mot choisi: " + ex.getMessage());
                                }
                            });

                            buttonsPanel.add(button);
                        }

                        wordChoicePanel.add(buttonsPanel);
                        wordChoicePanel.setVisible(true);
                        wordChoicePanel.revalidate();
                        wordChoicePanel.repaint();
                        topPanel.revalidate();   // Nouvelle ligne
                        topPanel.repaint();      // Nouvelle ligne

                        // Debug
                        System.out.println("DEBUG - Affichage des mots pour le dessinateur");
                        printComponentHierarchy(wordChoicePanel, "");
                    } catch (Exception e) {
                        System.err.println("ERROR - Exception lors de l'affichage des choix : " + e.getMessage());
                        System.err.println("ERROR - Exception lors de l'affichage des choix : " + e.getMessage());
                    }
                });
            }
            topPanel.revalidate();
            topPanel.repaint();
        }
        else if (message.startsWith("NOUVEAU_DESSINATEUR:")) {
            String dessinateur = message.substring(19);
            chatArea.append(">> " + dessinateur + " est le nouveau dessinateur!\n");
            lines.clear();
            drawingPanel.repaint();
        } else if (message.startsWith("CHOSEN_WORD_CONFIRMED:")) {
            if (playerRole.equals("dessinateur")) {
                canDraw = true;
                String mot = message.substring(22); // Corrigé de 20 à 22
                if (mot.startsWith("D: ")) {
                    mot = mot.substring(3);
                }
                motActuel = mot;
                currentWordLabel.setText("Mot à dessiner : " + mot);
                wordChoicePanel.setVisible(false);
                topPanel.setPreferredSize(new Dimension(800, 50)); // Réduire la taille pour le dessinateur
            } else {
                String mot = message.substring(22);
                if (mot.startsWith("D: ")) {
                    mot = mot.substring(3);
                }
                motActuel = mot;
                currentWordLabel.setText(hasFoundWord ? "Mot: " + mot : formatMotCache(mot));
            }
            topPanel.revalidate();
            topPanel.repaint();
        } else if (message.startsWith("TEMPS:")) {
            timerLabel.setText("Temps: " + message.substring(6) + "s");
        } else if (message.startsWith("CLEAR:")) {
            lines.clear();
            drawingPanel.repaint();
        } else if (message.startsWith("DRAW:")) {
            try {
                byte[] data = Base64.getDecoder().decode(message.substring(5));
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                LineData receivedLine = (LineData) ois.readObject();
                lines.add(receivedLine);
                drawingPanel.repaint();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur lors de la réception des données de dessin: " + e.getMessage());
            }
        } else if (message.startsWith("CHAT:")) {
            chatArea.append(message.substring(5) + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
        // Nouveau traitement pour les messages de bonne réponse
        else if (message.startsWith("GUESS_CORRECT:")) {
            String contenu = message.substring(14);
            if (playerRole.equals("dessinateur")) {
                chatArea.append("[Dessinateur] " + contenu + "\n");
            } else {
                chatArea.append("[Succès] " + contenu + "\n");
                if (contenu.contains("Vous avez trouvé le mot")) {
                    hasFoundWord = true;
                    if (motActuel != null) {
                        currentWordLabel.setText("Mot: " + motActuel);
                    }
                }
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
        else if (message.startsWith("REVEAL_LETTER:")) {
            revealNewLetter();
        }
        else if (message.startsWith("NOTIFICATION:")) {
            String notification = message.substring(13);
            chatArea.append("[Notification] " + notification + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
        else {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    //==========================================================================
    // Méthodes utilitaires
    //==========================================================================
    /**
     * Formate l'affichage du mot caché
     */
    private String formatMotCache(String mot) {
        if (mot == null) return "En attente du choix du mot...";
        if (playerRole.equals("dessinateur") || hasFoundWord) {
            return "Mot: " + mot;
        }

        StringBuilder sb = new StringBuilder("Mot: ");
        for (int i = 0; i < mot.length(); i++) {
            if (mot.charAt(i) == ' ') {
                sb.append("     "); // 5 espaces pour un espace dans le mot
            } else if (mot.charAt(i) == '-') {
                sb.append("- "); // Les tirets sont toujours visibles
            } else if (revealedIndices.contains(i)) {
                sb.append(mot.charAt(i)).append(" "); // Révéler la lettre à cette position
            } else {
                sb.append("_ ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Révèle une nouvelle lettre du mot
     */
    private void revealNewLetter() {
        if (motActuel == null || playerRole.equals("dessinateur") || hasFoundWord) return;

        // Créer une liste des indices disponibles (pas d'espace, pas de tiret, pas déjà révélé)
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < motActuel.length(); i++) {
            if (motActuel.charAt(i) != ' ' &&
                motActuel.charAt(i) != '-' &&
                !revealedIndices.contains(i)) {
                availableIndices.add(i);
            }
        }

        if (!availableIndices.isEmpty()) {
            // Choisir un indice aléatoire parmi ceux disponibles
            int randomIndex = availableIndices.get(new Random().nextInt(availableIndices.size()));
            revealedIndices.add(randomIndex);

            // Mettre à jour l'affichage
            currentWordLabel.setText(formatMotCache(motActuel));
            chatArea.append("[Indice] Une nouvelle lettre a été révélée!\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    /**
     * Utilitaire de débogage pour l'interface
     */
    private void printComponentHierarchy(Container container, String indent) {
        System.out.println(indent + container.getClass().getSimpleName() +
                          " visible=" + container.isVisible() +
                          " size=" + container.getSize());
        for (Component component : container.getComponents()) {
            if (component instanceof Container innerContainer) {
                printComponentHierarchy(innerContainer, indent + "  ");
            } else {
                if (component != null) {
                    System.out.println(indent + "  " + component.getClass().getSimpleName() +
                                     " visible=" + component.isVisible() +
                                     " size=" + component.getSize());
                }
            }
        }
    }

    //==========================================================================
    // Getters statiques
    //==========================================================================
    public static List<LineData> getLines() { return lines; }
    public static ArrayList<Point> getCurrentLine() { return currentLine; }
    public static Color getCurrentColor() { return currentColor; }
    public static int getCurrentStrokeWidth() { return currentStrokeWidth; }

    //==========================================================================
    // Point d'entrée
    //==========================================================================
    /**
     * Point d'entrée principal
     * Gère les arguments de ligne de commande pour host/port
     */
    public static void main(String[] args) {
        String host = HOST;
        int port = PORT;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Port invalide, utilisation du port par défaut " + PORT);
                port = PORT;
            }
        }

        final String finalHost = host;
        final int finalPort = port;

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                JOptionPane.showMessageDialog(null, "Erreur lors de la configuration de l'apparence: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
            new Client(finalHost, finalPort).setVisible(true);
        });
    }
}