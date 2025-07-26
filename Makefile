.PHONY: build test run lint fmt
build: ; ./gradlew clean build

test:  ; ./gradlew test

run:   ; ./gradlew :bot-gateway:run

lint:  ; ./gradlew ktlintCheck detekt

fmt:   ; ./gradlew ktlintFormat
