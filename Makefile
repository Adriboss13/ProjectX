# Makefile pour compiler et nettoyer le projet Java

# Variables
JAVAC = javac
JAVA_FILES = *.java
CLASS_FILES = *.class

# Cible par défaut
.PHONY: all
all:
	$(JAVAC) $(JAVA_FILES)

# Cible pour nettoyer les fichiers compilés
.PHONY: clean
clean:
	rm -f $(CLASS_FILES)