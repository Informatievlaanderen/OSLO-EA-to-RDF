build:
	sudo docker build -f Dockerfile.build -t ea .

exec:
	sudo docker build -f Dockerfile.local -t eat .

JAVAFILES=$(shell find src  -type f -name *.java -print)

#JAVAFILES=$(wildcard src/main/java/*.java)

format: $(JAVAFILES)
	java -jar google-java-format-1.7-all-deps.jar --replace $^

