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
INSTANCE ?= 20
INSTANCE_FILE = $(DATASET_DIR)/instance_$(shell printf "%04d" $(INSTANCE)).txt
RESULT_FILE = $(RESULT_DIR)/results_$(shell printf "%04d" $(INSTANCE)).txt

all: build run

build:
	$(MVN) clean install

compile:
	$(JAVAC) -d $(OUT_DIR) -cp $(LIBS) $(SOURCE_DIR)/*.java

run:
	mkdir -p $(RESULT_DIR)
	$(JAVA) -jar $(JAR_FILE) $(INSTANCE_FILE) $(RESULT_FILE)
	@if [ -f $(RESULT_FILE) ]; then python3 checker.py $(INSTANCE_FILE) $(RESULT_FILE); \
	else echo "Result file does not exist: $(RESULT_FILE)"; fi
	python3 update_best_solution.py $(INSTANCE_FILE) $(RESULT_FILE)

clean:
	rm -rf $(OUT_DIR)
