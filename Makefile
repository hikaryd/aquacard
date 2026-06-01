# AquaCard — обёртка над Gradle для частых задач.
# Переопределяемые переменные:  make build JAVA_HOME=/path  ANDROID_HOME=/path
#
# JAVA_HOME / ANDROID_HOME берутся из окружения, если заданы (?=),
# иначе используются дефолты ниже. Экспортируются в Gradle.

JAVA_HOME    ?= /opt/homebrew/opt/openjdk@17
ANDROID_HOME ?= $(HOME)/Library/Android/sdk
export JAVA_HOME
export ANDROID_HOME

GRADLE      := ./gradlew
GRADLE_ARGS ?=
APK         := app/build/outputs/apk/debug/app-debug.apk

.DEFAULT_GOAL := help

## help: показать список целей (по умолчанию)
.PHONY: help
help:
	@echo "AquaCard — доступные команды:"
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/## /  make /'

## build: собрать debug APK -> app/build/outputs/apk/debug/app-debug.apk
.PHONY: build
build:
	$(GRADLE) :app:assembleDebug $(GRADLE_ARGS)
	@echo "APK: $(APK)"

## test: прогнать JVM юнит-тесты
.PHONY: test
test:
	$(GRADLE) :app:testDebugUnitTest $(GRADLE_ARGS)

## check: тесты + Android Lint
.PHONY: check
check:
	$(GRADLE) :app:testDebugUnitTest :app:lintDebug $(GRADLE_ARGS)

## lint: только Android Lint (отчёт в app/build/reports/)
.PHONY: lint
lint:
	$(GRADLE) :app:lintDebug $(GRADLE_ARGS)

## verify: полная проверка — тесты + сборка APK (как в CI)
.PHONY: verify
verify:
	$(GRADLE) :app:testDebugUnitTest :app:assembleDebug --console=plain $(GRADLE_ARGS)

## install: установить debug APK на подключённое устройство (adb)
.PHONY: install
install:
	$(GRADLE) :app:installDebug $(GRADLE_ARGS)

## release: собрать release APK (требует настроенной подписи)
.PHONY: release
release:
	$(GRADLE) :app:assembleRelease $(GRADLE_ARGS)

## clean: очистить артефакты сборки (app/build, build)
.PHONY: clean
clean:
	$(GRADLE) clean $(GRADLE_ARGS)

# ──────────────── iOS (требуется macOS + Xcode) ────────────────
IOS_PROJECT := iosApp/AquaCard.xcodeproj
IOS_SCHEME  := AquaCard
IOS_SIM     ?= platform=iOS Simulator,name=iPhone 17

## ios-build: собрать iOS-приложение под симулятор
.PHONY: ios-build
ios-build:
	xcodebuild -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) \
		-destination '$(IOS_SIM)' build CODE_SIGNING_ALLOWED=NO

## ios-test: прогнать XCTest юнит-тесты под симулятор
.PHONY: ios-test
ios-test:
	xcodebuild test -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) \
		-destination '$(IOS_SIM)' CODE_SIGNING_ALLOWED=NO

## ipa: собрать неподписанную .ipa для SideStore -> dist/AquaCard.ipa
.PHONY: ipa
ipa:
	xcodebuild -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) \
		-configuration Release -sdk iphoneos -derivedDataPath build/ios \
		CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" build
	@rm -rf Payload dist && mkdir -p Payload dist
	@cp -R "$$(find build/ios -name 'AquaCard.app' -type d | head -1)" Payload/
	@zip -qr dist/AquaCard.ipa Payload && rm -rf Payload
	@echo "IPA: dist/AquaCard.ipa (неподписанная — ставить через SideStore)"

## tasks: показать все доступные Gradle-задачи
.PHONY: tasks
tasks:
	$(GRADLE) :app:tasks --all $(GRADLE_ARGS)

## doctor: проверить окружение сборки (JDK17 + Android SDK)
.PHONY: doctor
doctor:
	@echo "JAVA_HOME    = $(JAVA_HOME)"; \
	test -d "$(JAVA_HOME)" && echo "  ✓ JDK найден" || echo "  ✗ JDK НЕ найден"; \
	echo "ANDROID_HOME = $(ANDROID_HOME)"; \
	test -d "$(ANDROID_HOME)" && echo "  ✓ Android SDK найден" || echo "  ✗ Android SDK НЕ найден"
