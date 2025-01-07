# Variables de configuration
JAVAC=javac
JAVA=java
FILES=Chrono.java Client.java ClientHandler.java Dessinateur.java Devineur.java DrawingPanel.java \
      GestionnaireMots.java Jeu.java Joueur.java LineData.java MessageListener.java Mots.java \
      mots.txt Partie.java Podium.java Server.java TestGestionnaireMots.java
CLASSFILES=$(FILES:.java=.class)

# Règle par défaut (compile tout)
all: $(CLASSFILES)

# Règle pour compiler les fichiers .java en .class
%.class: %.java
	$(JAVAC) $<

# Règle pour exécuter le serveur
run_server: all
	$(JAVA) Server

# Règle pour exécuter le client
run_client: all
	$(JAVA) Client

# Règle pour exécuter les tests
run_tests: all
	$(JAVA) TestGestionnaireMots

# Nettoyer les fichiers générés
clean:
	rm -f *.class

# Règle pour vérifier si tout est compilé et propre
check: $(CLASSFILES)
	@echo "Compilation terminée avec succès !"

# Règle pour afficher les fichiers sources
sources:
	@echo $(FILES)

.PHONY: all clean run_server run_client run_tests check sources
