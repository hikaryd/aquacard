# AquaCard — Amusement IC & Aime NFC Emulator for AquaDX

AquaCard — приложение для карт Amusement IC и Sega Aime, работающее с приватным сервером AquaDX / AquaNet.

- **Android** (Kotlin, Jetpack Compose, Room) — генерация и хранение карт, беспроводная NFC-эмуляция (HCE-F) и просмотр профиля maimai.
- **iOS** (SwiftUI) — просмотр профиля maimai с AquaDX: рейтинг и статистика, Best 35/15, недавние партии и детальный результат по треку. Эмуляции карт на iOS нет (нет доступа к HCE-F FeliCa).

Готовые сборки `.apk` и `.ipa` — на вкладке [Releases](../../releases). Установка iOS-сборки идёт через [SideStore](https://sidestore.io), см. раздел [Установка](#установка).

## Возможности

### NFC-эмуляция карты (Android)

- Сервис `CardEmulationService` на базе `HostNfcFService` обрабатывает Polling и отвечает на Request Response (команда `0x04` → ответ `0x05`), передавая динамический NFCID2 (IDm).
- System Code — `4000` (как требует Android для HCE-F).

### Коды карты (Android)

- **Sega Access Code:** IDm разбирается как 64-битное знаковое целое с паддингом до 20 цифр. Результат совпадает с `AimeDB.doFelicaLookupV2` в AquaDX.
- **Konami Card ID (eAmusement):** Triple-DES со сдвигом байт-ключа, распаковкой в 5-битные группы Base32 и контрольной суммой.

Алгоритмы покрыты юнит-тестами с эталонными тест-векторами (`CryptoTest.kt`, `CardFormatTest.kt`).

### Профиль игрока

- Загрузка статистики maimai по username из публичного API AquaDX (`GET /api/v2/game/mai2/...`), без авторизации.
- Привязка карты на Android — `POST /api/v2/card/link`.

## Совместимость устройств

Для беспроводной эмуляции телефон должен аппаратно поддерживать NFC FeliCa (HCE-F).

- **Совместимы:** корейские Samsung серии «N» (Note 8/9 и др.), японские Xperia, Sharp Aquos и другие телефоны с лицензированным на уровне ОС стеком Sony FeliCa.
- **Несовместимы:** Google Pixel, глобальные OnePlus, Xiaomi (нет FeliCa в NFC-чипе). На таких устройствах эмуляция недоступна, но генератор карт работает: можно скопировать Access Code и привязать карту на сайте AquaNet, чтобы играть без телефона.

iOS-приложение карты не эмулирует независимо от устройства — это просмотрщик профиля.

## Структура проекта

```text
.
├── app/                      # Android-модуль
│   └── src/
│       ├── main/java/net/aquadx/aquacard/
│       │   ├── nfc/          # HCE-F (CardEmulationService, HceController)
│       │   ├── crypto/       # AccessCode, CardFormat
│       │   ├── data/         # Room + Retrofit-клиент AquaDX
│       │   └── ui/           # Compose-экраны и тема
│       └── test/             # JVM-тесты (крипто, формат, профиль)
├── iosApp/                   # iOS-модуль (SwiftUI)
│   ├── AquaCard/             # Профиль, детализация треков, тема, сетевой клиент
│   ├── AquaCardTests/        # XCTest
│   └── AquaCard.xcodeproj/
├── .github/workflows/        # CI: сборка apk+ipa и публикация в Releases
├── gradle/                   # Version Catalog + wrapper
├── docs/                     # Спецификации API и карт
├── build.gradle.kts
└── settings.gradle.kts
```

## Сборка

### Android

```bash
# JDK 17 + Android SDK (ANDROID_HOME или local.properties)
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:testDebugUnitTest      # JVM-тесты
```

Либо через Android Studio: открыть корень проекта, дождаться синхронизации Gradle, запустить `app`.

APK появляется в `app/build/outputs/apk/debug/app-debug.apk`.

### iOS (нужен macOS + Xcode)

```bash
open iosApp/AquaCard.xcodeproj   # открыть в Xcode
make ios-build                   # сборка под симулятор
make ios-test                    # XCTest
make ipa                         # неподписанная .ipa -> dist/
```

Для запуска на своём устройстве: в таргете AquaCard → Signing & Capabilities включить Automatically manage signing и выбрать свою команду (Personal Team бесплатного Apple ID).

### Makefile

```bash
make doctor    # проверить окружение (JDK17 + Android SDK)
make build     # debug APK
make test      # JVM-тесты
make verify    # тесты + сборка APK (как в CI)
make help      # список целей
```

## Установка

Пакеты собираются в CI по тегу `vX.Y.Z` и публикуются на вкладке [Releases](../../releases).

### Android (.apk)

1. Скачать `AquaCard-vX.Y.Z.apk` из Releases.
2. Открыть файл на телефоне, разрешить установку из неизвестных источников.
3. Установить.

### iOS (.ipa) через SideStore

`.ipa` из Releases не подписана и ставится через [SideStore](https://sidestore.io): он переподписывает её вашим Apple ID прямо на устройстве, бесплатно и без подключённого Mac.

1. Установить SideStore по инструкции с [sidestore.io](https://sidestore.io) (первичная настройка — через компьютер, дальше переподпись идёт по Wi-Fi).
2. Скачать `AquaCard-vX.Y.Z.ipa` из Releases на устройство.
3. В SideStore: My Apps → «+» → выбрать `.ipa`.
4. SideStore сам продлевает подпись до истечения 7-дневного лимита бесплатного сертификата, поэтому приложение остаётся рабочим без ручной переустановки.

Альтернатива — [AltStore](https://altstore.io) (переподпись требует включённого компьютера в той же сети). На iOS ≤ 16.6.1 подходит [TrollStore](https://ios.cfw.guide/installing-trollstore/) — постоянная подпись без лимита.

## Документация

В каталоге [`docs/`](docs/):

- [`aquadx-api-spec.md`](docs/aquadx-api-spec.md) — контракт веб-API AquaDX (профиль maimai / CHUNITHM).
- [`AquaDX-Card-App-Spec.md`](docs/AquaDX-Card-App-Spec.md) — формат карт, алгоритмы кодов и протокол HCE-F.

## Использование в кабинке

1. В приложении сгенерировать 16-значный IDm (начинается с `02FE`).
2. Открыть карту, скопировать 20-значный Sega Access Code.
3. В личном кабинете AquaDX открыть привязку карт (Link Card), вставить код и сохранить.
4. В приложении включить эмуляцию NFC.
5. Поднести телефон задней крышкой к считывателю автомата. Ридер считывает IDm, сопоставляет его с привязанным аккаунтом по `decimal(IDm)` и выполняет вход в профиль.
