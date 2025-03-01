# Define variables
JAVAC = javac
JAVA = java
MVN = mvn
JAR_FILE = target/ChallengeSBPO2025-1.0.jar
SOURCE_DIR = src/main/java/org/sbpo2025/challenge
LIBS = libs/commons-lang3-3.12.0.jar
OUT_DIR = out
DATASET_DIR = datasets/a
RESULT_DIR = results/a
INSTANCE_FILE = $(DATASET_DIR)/instance_0020.txt
RESULT_FILE = $(RESULT_DIR)/results_0020.txt

# Default target to run everything
all: build run

# Clean and build the project with Maven
build:
	$(MVN) clean install

# Compile Java files (optional if you want to do this manually before running)
compile:
	$(JAVAC) -d $(OUT_DIR) -cp $(LIBS) $(SOURCE_DIR)/*.java

# Run the Java program using the JAR file
run:
	$(JAVA) -jar $(JAR_FILE) $(INSTANCE_FILE) $(RESULT_FILE)

# Clean the out directory (optional)
clean:
	rm -rf $(OUT_DIR)
