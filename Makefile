FRONTEND_DIST=node_modules/@hexlet/java-flight-booking-frontend/dist
JAR=build/libs/app.jar

.PHONY: build contract

install:
	npm ci
	./gradlew compileJava

build:
	rm -rf src/main/resources/public/assets src/main/resources/public/index.html
	mkdir -p public
	rsync -av $(FRONTEND_DIST)/ src/main/resources/public/
	./gradlew bootJar

start:
	java -jar $(JAR)

contract:
	npx tsp compile contract

test:
	./gradlew test

clean:
	rm -rf ~/.gradle/caches/modules-2/files-2.1/ch.qos.logback
	./gradlew clean

all: clean install build start
