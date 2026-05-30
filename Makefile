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
