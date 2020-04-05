build:
	docker images ea --format "{{.ID}}" > ea.old
	docker build -f Dockerfile.build -t ea .
	docker rmi `cat ea.old`

exec:
	docker images eat --format "{{.ID}}" > eat.old
	docker build -f Dockerfile.local -t eat .
	docker rmi `cat eat.old`

run: 
	docker run --rm -it --name eatest -v ${CURDIR}:/data eat bash

apply:
	java -jar /app/ea-2-rdf.jar jsonld -c /data/test/eap-mapping.json -n assocafdalen > /data/test/assocafdalen.report 

JAVAFILES=$(shell find src  -type f -name *.java -print)

#JAVAFILES=$(wildcard src/main/java/*.java)

format: $(JAVAFILES)
	java -jar google-java-format-1.7-all-deps.jar --replace $^

