# AquaCard — Amusement IC & Aime NFC Emulator for AquaDX

**AquaCard** — приложение для хранения, управления и беспроводной NFC-эмуляции карт Amusement IC и Sega Aime для приватного сервера **AquaDX** / **AquaNet**.

- **Android** — нативный клиент на Kotlin, Jetpack Compose и Room с полноценной HCE-F эмуляцией карты.
- **iOS** — нативный клиент на SwiftUI (просмотр профиля, генерация и хранение карт; аппаратная NFC-эмуляция FeliCa на iOS недоступна).

> 📦 Готовые сборки `.apk` и `.ipa` — на вкладке [**Releases**](../../releases). Установка iOS-сборки — через [SideStore](https://sidestore.io) (см. раздел [Установка](#-установка)).

## 🚀 Описание архитектуры и возможностей

1. **Host Card Emulation FeliCa (HCE-F):**
   - Реализует нативный Android-сервис `CardEmulationService`, расширяющий `HostNfcFService`.
   - Эмулирует карту FeliCa препарируя сигналы Polling и отвечая на запрос Request Response (код команды `0x04` -> ответ `0x05`) с передачей dynamic NFCID2 (IDm).
   - Использует фиксированный System Code `4000` согласно спецификации Android.

2. **Математические преобразования (Источник Истины):**
   - **Sega Access Code:** IDm математически парсится как 64-битное знаковое целое число с добавлением паддинга до 20 цифр. Наш алгоритм детерминирован и 100% совпадает с реверс-кодом `AimeDB.doFelicaLookupV2` в сервере AquaDX.
   - **Konami Card ID (3DES ECE):** Для показа кода eAmusement вычисляется Triple-DES шифрование со сдвигом байт-ключа на вектор (`* 2`), распаковкой в 5-битные группы Base32 символов и циклической контрольной суммой.

3. **Сетевой клиент Retrofit:**
   - Позволяет осуществлять привязку вызовом `POST /api/v2/card/link` и загружает статистику игрока по username (`GET /api/v2/game/{game}/user-summary`).

## 🧱 Требования к устройству и совместимости

> **ВАЖНО:** Для успешной беспроводной эмуляции ваш смартфон должен обладать аппаратной поддержкой NFC для FeliCa (HCE-F). 

* **Совместимые устройства (ориентиры):** Смартфоны Samsung корейской серии ( Note 8/9/S15 "N"), японские версии Xperia, Sharp Aquos, или иные телефоны с лицензированным на уровне ОС Middleware-стеком Sony FeliCa.
* **Несовместимые устройства:** Смартфоны Google Pixel, глобальные версии OnePlus, Xiaomi (без FeliCa в NFC-чипсете). На таких устройствах приложение будет выдавать предупреждение, однако вы по-прежнему сможете использовать генератор карт, копировать Access Code и регистрировать их на сайте AquaNet для игры без телефона.

## 📁 Структура проекта

```text
.
├── app/                      # Android-модуль приложения
│   └── src/
│       ├── main/java/net/aquadx/aquacard/
│       │   ├── nfc/          # HCE-F сервис (CardEmulationService, HceController)
│       │   ├── crypto/       # AccessCode / CardFormat (Источник Истины)
│       │   ├── data/         # Room БД + Retrofit-клиент AquaDX
│       │   └── ui/           # Jetpack Compose экраны и тема
│       └── test/             # JVM юнит-тесты (крипто, формат, профиль)
├── iosApp/                   # iOS-модуль (SwiftUI)
│   ├── AquaCard/             # Исходники: профиль, карты, тема, сетевой клиент
│   ├── AquaCardTests/        # XCTest юнит-тесты
│   └── AquaCard.xcodeproj/
├── .github/workflows/        # CI: сборка apk+ipa и публикация в Releases
├── gradle/                   # Version Catalog (libs.versions.toml) + wrapper
├── docs/                     # Спецификации, API, дизайн-планы
├── build.gradle.kts          # Корневой Gradle-конфиг
└── settings.gradle.kts
```

## 🛠️ Сборка

### Android Studio

1. Откройте **корневую директорию проекта** в **Android Studio**.
2. Дождитесь синхронизации Gradle-проекта (используется Version Catalog `gradle/libs.versions.toml`).
3. Подключите Android-устройство по USB.
4. Выполните **Build > Build APK(s)** или запустите напрямую через **Run 'app'**.

### Командная строка

```bash
# Требуется JDK 17 и Android SDK (ANDROID_HOME или local.properties)
./gradlew :app:assembleDebug          # собрать debug APK
./gradlew :app:testDebugUnitTest      # запустить JVM юнит-тесты
```

### Makefile (удобные обёртки)

```bash
make doctor    # проверить окружение (JDK17 + Android SDK)
make build     # собрать debug APK
make test      # юнит-тесты
make verify    # тесты + сборка APK (как в CI)
make help      # список всех целей
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.
Юнит-тесты (`CryptoTest.kt`, `CardFormatTest.kt` и др.) проверяют криптографию на соответствие эталонным тест-векторам.

### iOS (Xcode)

```bash
open iosApp/AquaCard.xcodeproj          # открыть в Xcode
# или из командной строки:
make ios-build                          # сборка под симулятор
make ios-test                           # XCTest юнит-тесты
make ipa                                # собрать неподписанную .ipa -> dist/
```

Для запуска на своём устройстве в Xcode откройте таргет **AquaCard → Signing & Capabilities**,
включите **Automatically manage signing** и выберите свою команду (Personal Team бесплатного Apple ID).

## 📲 Установка

Готовые пакеты публикуются автоматически на вкладке [**Releases**](../../releases)
(CI собирает их из тега `vX.Y.Z`).

### Android (.apk)

1. Скачайте `AquaCard-vX.Y.Z.apk` из Releases.
2. Откройте файл на телефоне, разрешите **«Установка из неизвестных источников»** для браузера/проводника.
3. Установите и откройте.

### iOS (.ipa) через SideStore

IPA из Releases **не подписана** — она устанавливается через [**SideStore**](https://sidestore.io),
который переподписывает её вашим Apple ID прямо на устройстве (бесплатно, без Mac рядом).

1. Установите SideStore по официальной инструкции: **<https://sidestore.io>**
   (потребуется один раз настроить через компьютер; дальше переподпись идёт сама по Wi-Fi).
2. Скачайте `AquaCard-vX.Y.Z.ipa` из Releases на устройство.
3. В SideStore: **My Apps → +** → выберите скачанный `.ipa`.
4. SideStore подпишет и установит приложение; он же будет автоматически
   продлевать подпись до истечения 7-дневного лимита бесплатного сертификата —
   фактически приложение работает бессрочно.

> Альтернатива — [AltStore](https://altstore.io) (тот же принцип, но переподпись требует включённого компьютера в той же сети).
> На старых версиях iOS (≤ 16.6.1) можно использовать [TrollStore](https://ios.cfw.guide/installing-trollstore/) — постоянная подпись без 7-дневного лимита.

## 📚 Документация

Подробные спецификации и дизайн-материалы — в каталоге [`docs/`](docs/):

- `AquaDX-Card-App-Spec.md` — спецификация приложения и алгоритмов
- `aquadx-api-spec.md` — описание API сервера AquaDX
- `prd.json`, `plan.md`, `profile-ui-plan.md` — требования и планы UI

## 🔗 Порядок интеграции и игры в кабинках

1. Запустите приложение **AquaCard** на телефоне.
2. Сгенерируйте случайный 16-значный IDm (он начнется с `02FE`).
3. Раскройте карточку, скопируйте её 20-значный **Sega Access Code**.
4. Зайдите в личный кабинет на сайте вашего сервера **AquaDX** и перейдите в меню привязки карт (`Link Card`).
5. Вставьте скопированный 20-значный код и подтвердите сохранение.
6. В приложении нажмите кнопку **Эмулировать NFC**.
7. Поднесите телефон задней спинкой к считывателю автомата — терминал мгновенно считает эмулируемый IDm, сопоставит его с привязанным аккаунтом по формуле `decimal(IDm)` и выполнит вход в ваш игровой профиль!

---
*Разработка выполнена в строгом соответствии с нативным протоколом NFC FeliCa.*
