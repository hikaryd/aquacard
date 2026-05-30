# Спецификация: Android-приложение для хранения и NFC-эмуляции карт AquaDX (Amusement IC / Aime)

> Документ основан на полном реверс-инжиниринге `eamemu_ex.signed.apk` (`tk.nulldori.eamemu`)
> и на исследовании исходников AquaDX (`MewoLab/AquaDX`), segatools, AIC Pico и официальной
> документации Android HCE-F + bsnk.me (allnet/aimedb/amusement_ic).
> Все алгоритмы проверены тест-векторами (см. §12). Версия: 2026-05-30.

---

## 0. TL;DR (самое главное)

1. **Карта для ридера = это только 8-байтный FeliCa IDm.** Аркадный ридер опознаёт карту по IDm,
   полученному при polling. Никакого чтения блоков (Read Without Encryption) не требуется.
2. **Телефон отдаёт IDm через Android HCE-F.** Жёсткие ограничения платформы:
   System Code обязан быть в диапазоне `4000`–`4FFF` (eamemu использует `4000`),
   NFCID2 (= IDm) обязан начинаться с `02FE`. → **Идеальный клон физической карты невозможен**
   (у настоящих карт префикс производителя `012E`, а телефон вынужден слать `02FE`).
3. **AquaDX опознаёт карту так:** `access_code = decimal(IDm как знаковый int64).padStart(20)`.
   Это детерминированно (в отличие от настоящего SEGA, где маппинг — это таблица).
4. **Эмуляции IDm достаточно для РАСПОЗНАВАНИЯ, но НЕ для ВХОДА в профиль.**
   FeliCa-путь AquaDX не создаёт карту автоматически: при неизвестном IDm возвращается `-1`.
   → access_code эмулируемой карты нужно заранее **зарегистрировать/привязать** на сайте AquaDX.
5. **Профиль с AquaDX тянется по username через публичный API без авторизации**
   (`GET /aqua/api/v2/game/<game>/user-summary?username=...`).
6. **Главное узкое место — поддержка HCE-F устройством.** Это есть в основном у японских/корейских
   телефонов с FeliCa-чипом Sony (Galaxy Note8/9/S10 «N», LG V30, Xperia JP). Большинство
   «глобальных» телефонов и Pixel — **не поддерживают**.
7. **Konami ID (16 символов base32)** к AquaDX отношения не имеет — это идентификатор игр Konami.
   eamemu его вычисляет только для показа. Алгоритм разобран и проверен (§9.3), но для AquaDX не нужен.

---

## 1. Экосистема карт: три независимых пространства идентификаторов

Это ключ к пониманию всей задачи. На одной физической карте Amusement IC живут **три разных
идентификатора в трёх несвязанных формулой пространствах**:

| # | Идентификатор | Формат | Кто использует | Как связан с IDm |
|---|---|---|---|---|
| 1 | **FeliCa IDm** | 8 байт = 16 hex | Все ридеры (читается при polling) | — (это и есть базовый ID) |
| 2 | **SEGA Access Code** | 20 десятичных цифр | SEGA-игры (maimai/CHUNITHM/…), **AquaDX** | На настоящем железе — таблица AiMeDB. **В AquaDX — `decimal(IDm)`** |
| 3 | **Konami Card ID** | 16 символов base32 | Игры Konami (eAmusement) | Алгоритмически (3DES), см. §9.3 |

### 1.1. FeliCa IDm
- 8 байт: `[2 байта manufacturer][6 байт serial]`.
- У **настоящих** Amusement IC карт manufacturer = `01 2E` (десятично 11777). Серийник делится на
  ChipCode(1) / OSVer(1) / Product(2) / Date(2) / SerialOfDay(2).
- У **эмулируемой через HCE-F** карты IDm вынужденно начинается с `02FE` (ограничение Android).
  Это легитимный префикс NFC-Forum Type-3-Tag, ридер его принимает, но это **не** заводской ID
  настоящей карты → байт-в-байт клонировать физическую карту нельзя.
- Реальная FeliCa-система карт = `0x88B4` (FeliCa Lite/Lite-S). Ридеры опрашивают **wildcard
  `0xFFFF`**, и карта отвечает своим System Code. `4000` — это **только** HCE-F-конвенция Android,
  на которую HCE-сервис отвечает на wildcard-опрос.

### 1.2. SEGA Access Code (20 цифр)
- Структура (на физических картах): `[company 3][encoded 12][CRC16 5]`.
  Коды компаний: `500/501`=SEGA, `510`=Bandai Namco, `520`=Konami, `530`=Taito, `0103 5…`=AiMe Mobile.
- На физических картах хранится **зашифрованным в блоке SPAD0** (подстановочный шифр на 9 S-box).
- **Важно:** на настоящем железе **нет формулы** IDm → access code; это таблица в AiMeDB.
- **Но в AquaDX (и minime) формула есть и она тривиальна** (см. §4).

### 1.3. Konami Card ID (16 символов base32)
- Алфавит `0123456789ABCDEFGHJKLMNPRSTUWXYZ` (без I/O/Q/V).
- Выводится из IDm 3DES-шифрованием. Это то, что считает `CardConvModule` в eamemu.
- **К SEGA/AquaDX не относится.** Нужен, только если хотите показывать пользователю
  «номер карты Konami». Алгоритм полностью разобран и проверен — см. §9.3.

---

## 2. Что такое eamemu и что из него взять (результат реверса)

`eamemu_ex.signed.apk` → пакет **`tk.nulldori.eamemu`** («eAMEMu»), приложение на **React Native**.
Это **эмулятор карт e-amusement/Amusement IC через Android HCE-F**. Сетевой интеграции с сервером
**нет** (это локальный менеджер карт + эмулятор). Структура:

- JS-бандл (`assets/index.android.bundle`) — UI и оркестрация.
- Нативные модули (Kotlin):
  - `HcefModule` — мост к `NfcFCardEmulation` (регистрация system code, установка NFCID2, enable/disable).
  - `eAMEMuService extends HostNfcFService` — обработка FeliCa-пакетов.
  - `CardConvModule` → `A`/`B`/`E` — конвертация IDm → Konami ID (3DES).

### 2.1. Модель данных eamemu
- Хранилище: `AsyncStorage`, ключ `'cards'` → JSON-массив.
- Объект карты: **`{ name, sid, uid, image }`**
  - `sid` — IDm, 16 hex, **всегда префикс `02FE`** (UI фиксирует `02FE`, пользователь вводит 12 hex);
  - `uid` — вычисленный Konami ID (для показа);
  - `name`, `image` — имя и фоновая картинка.
- «Random Generate» = `02FE` + 12 случайных hex.

### 2.2. HCE-F-механизм eamemu (это перенимаем 1:1)
- `AndroidManifest`: `uses-feature android.hardware.nfc.hcef` (required), сервис с
  `BIND_NFC_SERVICE` + intent `android.nfc.cardemulation.action.HOST_NFCF_SERVICE` + meta `@xml/nfc_setting`.
- `nfc_setting.xml`: `<system-code-filter name="4000"/>`, `<nfcid2-filter name="null"/>` (NFCID2 динамический).
- `HcefModule`:
  - `registerSystemCodeForService(component, "4000")`;
  - `setNfcid2ForService(component, SID)` — SID валидируется: длина 16, hex, **префикс `02FE`**;
  - `enableService()` / `disableService()` по lifecycle (foreground-only).
- `eAMEMuService.processNfcFPacket`: реализует **только** FeliCa Request Response
  (cmd `0x04` → ответ `0x05`: `[0x0B][0x05][IDm 8B][mode=0x00]`). Polling обрабатывает сам фреймворк.
  **Read/Write Without Encryption не реализованы** — и этого достаточно.

---

## 3. Как AquaDX опознаёт карту и грузит профиль

Источник: `MewoLab/AquaDX` (Spring Boot, Kotlin), ветка `v1-dev`. Игры: `mai2` (maimai DX),
`chu3` (CHUNITHM), `ongeki`, `wacca` (+ diva, cardmaker).

### 3.1. Сетевой конвейер
```
[Ридер/segatools] --IDm--> [aimedb, TCP 22345] --ext_id--> [игра, HTTP :80] --профиль по ext_id-->
```
- **aimedb (TCP 22345):** принимает IDm, выдаёт `ext_id` (идентификатор профиля) или `-1`.
- **Игра (HTTP :80):** `UserLogin`/`GetUserData` по `ext_id` (`findByCardExtId`).
- Таблица `sega_card`: ключи `luid` (строка access code) и `ext_id` (uint32, профиль). Колонки IDm нет.

### 3.2. Ключевой факт — формула IDm → access code в AquaDX
`AimeDB.doFelicaLookupV2` (тип `0x11`):
```kotlin
// псевдокод по исходнику AquaDX
val idm: Long = readLongBE(msg, 0x30)            // 8 байт IDm как ЗНАКОВЫЙ int64
val accessCode = idm.toString().replace("-", "").padStart(20, '0')
val card = cardRepo.findByLuid(accessCode)       // чистый lookup в БД
val extId = card?.extId ?: -1                    // -1 при промахе, БЕЗ авто-создания
```
→ **`access_code = decimal(IDm).padStart(20)`** (детерминированно). Это НЕ алгоритм Konami и
НЕ настоящая таблица SEGA — это специфика AquaDX/minime.

> Нюанс: для IDm с префиксом `02FE` старший байт `0x02` → значение положительное → `replace("-","")`
> ни на что не влияет. Для отрицательных (старший бит = 1) знак вырезается — нам не грозит.

### 3.3. Регистрация: почему эмуляции мало для входа
- FeliCa-путь (`doFelicaLookupV2`) при неизвестном IDm возвращает `-1` и **карту не заводит**.
- Единственные пути регистрации работают с **20-значным access code**, не с сырым IDm:
  `CardService.registerByAccessCode`, доступный через web `POST /api/v2/card/link`, `Frontier.kt`,
  `Fedy.kt`, либо классический Aime `doRegister` (cmd `0x05`).
- **Вывод:** чтобы эмулируемая карта логинилась в нужный профиль, надо заранее
  **привязать её access_code** (`decimal(IDm)`) к аккаунту через сайт AquaDX.

---

## 4. Веб-API AquaNet (для подтягивания профиля и привязки карты)

- **Base URL:** `https://aquadx.net/aqua`, эндпоинты под `/api/v2`. **Делайте base URL настраиваемым**
  (self-hosted инстансы переопределяют `VITE_AQUA_HOST`).
- **Конвенции** (`ext/Ext.kt`): большинство эндпоинтов = `@RequestMapping` (принимают GET и POST),
  параметры — query или form (`@RequestParam`); JSON-тело (`@RequestBody`) только у `transfer`/`import`.
  Официальный фронт (Svelte SDK `AquaNet/src/libs/sdk.ts`) всегда POST-ит form-encoded.

### 4.1. Аутентификация (нужна только для приватных операций)
- `POST /api/v2/user/login` (`email` (или username), `password`, `turnstile`) → `{ token }`.
- **Cloudflare Turnstile** (капча) валидируется сервером и **блокирует чисто программный логин** →
  для авторизации нужен WebView с капчей.
- Токен (JWT, subject = UUID сессии) передаётся как **обычный параметр запроса** на каждый
  приватный вызов (НЕ заголовок Authorization, НЕ cookie). Невалидный/отсутствующий → HTTP 400.

### 4.2. Публичный профиль по username (без авторизации — основной путь «подтягивания»)
- `GET /api/v2/game/<game>/user-summary?username=NAME` → `GenericGameSummary`:
  `name`, `aquaUser{username,displayName,country,regTime,profileBio,profilePicture,…}`,
  `serverRank`, `accuracy`, `rating`, `ratingHighest`, `ranks`, `detailedRanks`, `maxCombo`,
  `fullCombo`, `allPerfect`, `totalScore`, `plays`, `joined`, `lastSeen`, `lastVersion`,
  `ratingComposition`, `recent[<=100]`, `favorites`.
- `GET /api/v2/card/user-games?username=NAME` → карта по играм (`mai2/chu3/ongeki/wacca/diva`:
  `null` либо `{name,rating,lastLogin}`).
- Также публично по username: `/trend`, `/recent`, `/user-detail`,
  `/game/mai2/user-rating`, `/game/chu3/user-rating`, `/game/<game>/ranking?page=N` (100/стр).
- Статические метаданные песен: `https://aquadx.net/d/<game>/00/all-music.json`.
- **CORS открыт** (`allowedOrigins=*`), что удобно для мобильного/веб-клиента.

### 4.3. Привязка карты к аккаунту (приватно, нужен токен)
- `POST /api/v2/card/link` (`token`, `cardId` = 20-значный access code, `migrate` = `mai2,chu3,…`).
  Неизвестный код → `registerByAccessCode`; существующий непривязанный → привязка к аккаунту
  и перенацеливание профилей на `ghostCard`. Отвязка: `POST /api/v2/card/unlink`.
- **Публичного lookup `access_code → профиль` нет** (`POST /api/v2/card/summary` требует
  токен-владелец; чужая карта → 404). Поэтому «подтягивание» делаем по **username**, а не по IDm.

---

## 5. Архитектура своего приложения

```
app/
├── nfc/                      # HCE-F слой (нативный, обязательно)
│   ├── CardEmulationService.kt   (extends HostNfcFService)
│   └── HceController.kt           (NfcFCardEmulation: register/setNfcid2/enable/disable)
├── data/
│   ├── Card.kt                    (модель: id, name, idm(SID), color/image, note, linkedUsername?)
│   ├── CardStore.kt               (Room/DataStore; опц. шифрование)
│   └── AquaApi.kt                 (Retrofit/Ktor клиент AquaNet)
├── domain/
│   ├── KonamiId.kt                (IDm <-> Konami ID, §9.3 — опционально)
│   ├── AccessCode.kt              (IDm -> decimal access code, §9.2)
│   └── ProfileSync.kt            (подтягивание профиля по username)
├── ui/
│   ├── CardListScreen, CardEditScreen, EmulateScreen, ProfileScreen
│   └── compatibility (проверка HCE-F, инструкция при отсутствии)
└── AndroidManifest.xml
```
Минимальный жизнеспособный продукт (MVP): `nfc/` + `data/Card*` + список/эмуляция. Профиль и Konami ID — отдельные фичи.

---

## 6. Реализация NFC / HCE-F (детально)

### 6.1. Манифест
```xml
<uses-feature android:name="android.hardware.nfc.hcef" android:required="false"/> <!-- false: чтобы ставилось и для проверки совместимости в рантайме -->
<uses-permission android:name="android.permission.NFC"/>

<service
    android:name=".nfc.CardEmulationService"
    android:exported="true"
    android:permission="android.permission.BIND_NFC_SERVICE">
    <intent-filter>
        <action android:name="android.nfc.cardemulation.action.HOST_NFCF_SERVICE"/>
    </intent-filter>
    <meta-data
        android:name="android.nfc.cardemulation.host_nfcf_service"
        android:resource="@xml/nfc_setting"/>
</service>
```
`res/xml/nfc_setting.xml`:
```xml
<host-nfcf-service xmlns:android="http://schemas.android.com/apk/res/android">
    <system-code-filter android:name="4000"/>
    <nfcid2-filter android:name="02FE000000000000"/>      <!-- плейсхолдер; реальный ставится в рантайме -->
    <t3tPmm-filter android:name="FFFFFFFFFFFFFFFF"/>
</host-nfcf-service>
```

### 6.2. Жёсткие ограничения платформы (проверено по AOSP `NfcFCardEmulation`)
- **System Code:** `4000`–`4FFF`, исключая `4*FF`. Использовать `"4000"`.
- **NFCID2 (= IDm):** строго `02FE000000000000`–`02FEFFFFFFFFFFFF` (16 hex, префикс `02FE`).
  `setNfcid2ForService` при невалидном значении просто вернёт `false` / не зарегистрирует.
- Один сервис = **один** System Code и **один** NFCID2 одновременно.
  → Для нескольких карт NFCID2 переключается динамически перед эмуляцией.
- `processNfcFPacket` выполняется на **main thread**; либо сразу вернуть ответ, либо `null` и позже
  `sendResponsePacket`. После выбора карты все кадры на её NFCID2 идут сюда до разрыва линка (`onDeactivated`).
- **Foreground-only:** эмуляция активна, пока активити на экране; на Samsung выставить обработчик
  NFC-оплаты по умолчанию в «Android OS», другие card-emulation приложения (транспорт/T-money) закрыть.

### 6.3. Контроллер HCE-F
```kotlin
class HceController(activity: Activity) {
    private val adapter = NfcAdapter.getDefaultAdapter(activity)
    private val nfcF = NfcFCardEmulation.getInstance(adapter)
    private val component = ComponentName(activity, CardEmulationService::class.java)

    fun isSupported(ctx: Context) =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)

    init { nfcF.registerSystemCodeForService(component, "4000") }

    /** SID = 16 hex, ОБЯЗАТЕЛЬНО начинается с 02FE */
    fun selectCard(sid: String): Boolean {
        require(sid.length == 16 && sid.matches(Regex("[0-9A-Fa-f]+")) && sid.uppercase().startsWith("02FE"))
        return nfcF.setNfcid2ForService(component, sid.uppercase())
    }
    fun enable(activity: Activity)  = nfcF.enableService(activity, component)
    fun disable(activity: Activity) = nfcF.disableService(activity)
}
```

### 6.4. Сервис (минимально достаточный — копия логики eamemu)
```kotlin
class CardEmulationService : HostNfcFService() {
    override fun processNfcFPacket(command: ByteArray, extras: Bundle?): ByteArray? {
        if (command.size < 10) return null
        val idm = command.copyOfRange(2, 10)            // 8 байт IDm из запроса
        return when (command[1].toInt() and 0xFF) {
            0x04 -> byteArrayOf(0x0B, 0x05) + idm + byteArrayOf(0x00)  // Request Response -> mode 0
            else -> null                                 // Polling обрабатывает фреймворк
        }
    }
    override fun onDeactivated(reason: Int) {}
}
```
> Опционально для большей совместимости можно дополнительно реализовать FeliCa Polling-ответ и
> Read Without Encryption (cmd `0x06`→`0x07`) с фейковым SPAD0 — **но для AquaDX/AIME это не нужно**
> (ридер опознаёт по IDm). Добавлять только если конкретный ридер этого потребует (см. §11, открытые вопросы).

### 6.5. Поток эмуляции (UI)
1. Проверить `isSupported()`. Если нет — показать экран несовместимости (см. §11) и не пускать дальше.
2. Проверить, что NFC включён (`adapter.isEnabled`); иначе — отправить в настройки.
3. Пользователь выбирает карту → `selectCard(sid)` → `enable()`.
4. Держать активити на переднем плане; крупно показать «приложите телефон к ридеру».
5. По `onHostPause` → `disable()`.

---

## 7. Модель данных и хранилище

```kotlin
@Entity data class Card(
    @PrimaryKey val id: String,            // UUID
    val name: String,
    val idm: String,                       // 16 hex, префикс 02FE (это и есть SID/NFCID2)
    val colorOrImage: String?,             // оформление
    val note: String? = null,
    val linkedAquaUsername: String? = null,// для подтягивания профиля
    val createdAt: Long
)
```
- Хранилище: Room (или DataStore для простого случая). Импорт/экспорт JSON, совместимый по полям
  `{name, sid, image}` с eamemu — приятный бонус для миграции.
- **Безопасность:** IDm — это, по сути, «номер карты». Шифровать БД (SQLCipher) и/или прятать значения
  за биометрией. `android:allowBackup="false"`.
- **Генерация новой карты:** `"02FE" + 12 случайных hex` (CSPRNG). Перед использованием — проверить
  через AquaDX, что такой access_code ещё не занят (опционально).

---

## 8. Поток интеграции с AquaDX

### 8.1. Регистрация эмулируемой карты в AquaDX (чтобы вход работал)
1. Сгенерировать/выбрать IDm (`02FE…`).
2. Вычислить `access_code = decimal(IDm).padStart(20)` (§9.2).
3. Пользователь логинится на сайте AquaDX (или в приложении через WebView с Turnstile) →
   `POST /api/v2/card/link` с `cardId = access_code`, `migrate = mai2,chu3,…`.
4. После привязки тап телефоном на ридере (segatools/AquaDX) залогинит в этот профиль.

> Альтернатива без RF-эмуляции: на ПК прописать `access_code` в `aime.txt` segatools
> (`useAimeDBForPhysicalCards`), либо привязать на сайте и играть. Это обходит требования HCE-F.

### 8.2. Подтягивание профиля (read-only, без авторизации)
```kotlin
suspend fun fetchSummary(base: String, game: String, username: String): GameSummary =
    http.get("$base/api/v2/game/$game/user-summary") { parameter("username", username) }.body()
// game ∈ {mai2, chu3, ongeki, wacca}; base настраиваемый, по умолчанию https://aquadx.net/aqua
```
- Пользователь указывает свой **username** AquaNet (вручную или через QR). Привязки по IDm/access_code
  публично нет — поэтому ключ именно username.
- Кэшировать, не долбить часто (на реверс-прокси возможны rate-limit/WAF, в репозитории не видны).

---

## 9. Алгоритмы

### 9.1. Что нужно для AquaDX
Минимум: **IDm** (эмуляция) и **access_code = decimal(IDm)** (регистрация). Konami ID — опционально.

### 9.2. IDm → AquaDX access code (точная формула)
Точно как в `AimeDB.doFelicaLookupV2`: IDm читается как **знаковый** `Long`, берётся десятичная
строка, вырезается знак `-`, дополняется нулями до 20 символов.
```kotlin
fun aquaAccessCode(idmHex: String): String {
    val idm: Long = java.lang.Long.parseUnsignedLong(idmHex, 16) // 16 hex -> 64-битное значение
    return idm.toString().replace("-", "").padStart(20, '0')     // signed toString, как в AquaDX
}
```
> Для IDm с префиксом `02FE` старший байт `0x02` → значение всегда положительное, минуса нет,
> результат совпадает с беззнаковым десятичным. Минус-ветка важна лишь для других префиксов (нам не грозит).

Примеры (проверено): `02FE000000000001` → `00215609832160362497`;
`02FEDEADBEEF1234` → `00215854669974409780`.

### 9.3. IDm ↔ Konami Card ID (опционально, для показа «номера карты Konami»)
**Шифр:** `DESede/ECB/NoPadding` (3DES-EDE), но с **поэлементно удвоенным ключом**.
Стандартный 3DES со «обычным» ключом даёт неверный результат (eamemu/игровой код используют
вариант, где DES читает биты 0–6 сырого ключа; удвоение байта сдвигает их в позиции 1–7,
где их читает стандартный DES — поэтому стандартная библиотека + удвоенный ключ совпадает 1:1).

```kotlin
object KonamiId {
    private const val ALPHABET = "0123456789ABCDEFGHJKLMNPRSTUWXYZ"
    private val KEY: ByteArray = run {
        val raw = "?I'llB2c.YouXXXeMeHaYpy!".toByteArray(Charsets.US_ASCII) // 24 байта
        ByteArray(24) { (raw[it] * 2).toByte() }                            // <-- удвоение каждого байта
    }
    private fun cipher(mode: Int) = Cipher.getInstance("DESede/ECB/NoPadding")
        .apply { init(mode, SecretKeySpec(KEY, "DESede")) }

    private fun cardType(idm: String): Int = when {
        idm.startsWith("E004", true) -> 1                       // магнитная (Konami)
        idm[0] == '0' || idm.startsWith("C02", true) ||
        idm.startsWith("D01", true) || idm.startsWith("E11", true) -> 2  // FeliCa (02FE -> сюда)
        else -> error("bad prefix")
    }

    fun fromIdm(idmHex: String): String {
        val ct = cardType(idmHex)
        val idm = hex(idmHex).reversedArray()                   // реверс байт
        val enc = cipher(Cipher.ENCRYPT_MODE).doFinal(idm)      // 3DES-encrypt 8 байт
        val sym = IntArray(16)
        unpack5(enc).copyInto(sym, 0, 0, 13)                    // 64 бита -> 13 групп по 5 бит
        sym[13] = 1
        sym[0] = sym[0] xor ct
        for (i in 1..13) sym[i] = sym[i] xor sym[i - 1]         // бегущий XOR
        sym[14] = ct
        sym[15] = checksum(sym)
        return sym.joinToString("") { ALPHABET[it].toString() }
    }
    // unpack5: 8 байт -> биты MSB-first -> 5-битные значения (pad нулями)
    // checksum: chk = Σ d[i]*((i%3)+1), i=0..14; while chk>31: chk=(chk>>5)+(chk&31)
}
```
**Проверенные тест-векторы** (совпадают с каноном eamuse.bsnk.me и с eamemu):

| IDm | card_type | Konami ID |
|---|---|---|
| `0000000000000000` | 2 | `007TUT8XJNSSPN2P` |
| `0000000000000001` | 2 | `GUDBG7NTAC75DC2L` |
| `0123456789ABCDEF` | 2 | `F53E0PX2NCAZ982Z` |
| `E004010203040506` | 1 | `KWRJ9P4MLSK6NP1E` |
| `02FE000000000001` | 2 | `3020GJSPZRYWAB2Z` |
| `02FEDEADBEEF1234` | 2 | `5JADWNB0K814012S` |

Полный рабочий референс на Python (без зависимостей, проходит все векторы и round-trip):
`.reverse/eamemu/konami_ref.py`.

---

## 10. UX-рекомендации
- Первый запуск: тест HCE-F → если нет, честно объяснить и предложить альтернативы (§11).
- Экран карты: имя, оформление, **показать и IDm, и AquaDX access_code, и (опц.) Konami ID** с кнопкой «копировать».
- Кнопка «Привязать к AquaDX»: WebView-логин (Turnstile) → `card/link`.
- Экран профиля: ввод/выбор username → `user-summary` (рейтинг, плеи, последние треки).
- Эмуляция: крупный полноэкранный режим «приложите к ридеру», подсказки про чехол/антенну/foreground.
- Импорт/экспорт карт (совместимость с eamemu JSON).

---

## 11. Совместимость устройств и ридеров (реалистично) + альтернативы

### 11.1. Поддержка HCE-F устройством — главный риск
- HCE-F требует FeliCa-middleware Sony в NFC-стеке. **NFC ≠ FeliCa.**
- Подтверждённо работают (по сообществу eAMEMu, «ориентир»): Galaxy Note8 `SM-N950N`, Note9 `SM-N960N`,
  LG V30 `LGM-V300L`, Galaxy S10 5G `SM-G977N` — корейские «N»-SKU; японские Sony/Sharp/Samsung.
- Не работают: большинство глобальных SKU, Pixel (Google не лицензировал FeliCa-middleware),
  кастомные ROM. Проверять в рантайме: `FEATURE_NFC_HOST_CARD_EMULATION_NFCF`.
- Список устройств устарел (эра Note 8/9) — актуальный стоит собирать из Discord AquaDX/segatools и
  issue-трекера eAMEMu.

### 11.2. Приём телефона ридером
- Konami e-amusement: eAMEMu реально работает «в полях».
- SEGA AIC-кабинеты / AquaDX-сетапы: распознают по IDm, должно работать; **прямых пользовательских
  подтверждений «SEGA-кабинет принял HCE-F телефон» в этом исследовании не нашлось** (см. §13).
- Кэвиаты: задержка HCE-F ~0.5–1 c (приложить и подержать), чехол/позиция антенны критичны,
  только foreground, закрыть другие card-emulation приложения.

### 11.3. Альтернативы, если HCE-F недоступен
- **Запись на пустую FeliCa Lite-S — НЕ клон:** заводской IDm read-only. Нельзя записать произвольный IDm.
- **Flipper Zero:** читает/парсит AIC (PR #4259), эмуляция есть, но кросс-вендорно «как повезёт» (issue #3871).
- **PC + segatools:** AIC Pico (PN532/PN5180), PaSoRi/ACR122U + `aimeio-pcsc`/`aimeio-cardreader`.
  Для AquaDX проще всего: вписать access_code в `aime.txt` или привязать на сайте — вообще без RF.

---

## 12. Тест-план
1. **Юнит-тесты алгоритмов** против тест-векторов §9 (Konami ID, access_code).
   Контроль: round-trip `to_konami_id`/`to_uid` и `decimal(IDm)` сверить с поведением AquaDX
   (`AimeDB.doFelicaLookupV2`).
2. **HCE-F на устройстве:** `isSupported()` true/false; `setNfcid2ForService` для валидного/невалидного
   SID; снятие SENSF_RES внешним ридером (PaSoRi/AIC Pico/Proxmark) — убедиться, что отдаётся выбранный IDm.
3. **Интеграция AquaDX (self-hosted dev-инстанс):** привязать access_code через `card/link`, затем тап →
   проверить логин в профиль; `user-summary` по username возвращает данные.
4. **Реальный ридер/кабинет:** распознавание, задержка, влияние чехла, foreground-поведение.
5. **Негатив:** незарегистрированный IDm → `ext_id = -1` (карта не входит) — подтвердить и обработать в UI.

---

## 13. Риски, ограничения, открытые вопросы
- ⚠️ **HCE-F есть у меньшинства телефонов** — это ограничивает аудиторию сильнее всего.
- ⚠️ **Идеальный клон физической карты невозможен** (`02FE` vs `012E`). Эмулируемая карта — всегда «новая».
- ⚠️ **Вход требует предварительной регистрации** access_code на стороне AquaDX (FeliCa-путь не авто-создаёт).
- ⚠️ **Turnstile** блокирует программный логин → авторизация только через WebView. Read-only профиль — без неё.
- Открытые вопросы для проверки на железе:
  1. Принимают ли реальные **SEGA AIC**-кабинеты (не только Konami) HCE-F телефон с `02FE` IDm?
  2. Какой **PMm/OS-version** байт фреймворк Android кладёт в SENSF_RES, и проходит ли он проверку
     AquaDX/AiMeDB (иначе — путь mobile-регистрации). Снять Proxmark-сниффом.
  3. Требует ли конкретная игра пост-polling команду (Read Without Encryption SPAD0)? eamemu её не делает
     и работает у Konami; на всякий случай заложить опциональную реализацию (§6.4).
  4. Авто-регистрирует ли целевой AquaDX-инстанс неизвестный IDm или строго нужен предварительный `card/link`.
  5. Точная семантика знака в `decimal(IDm)` для не-`02FE` префиксов (нам не критично).

### Правовые/этические заметки
- Делать только для **своих** карт/аккаунтов и приватных серверов (AquaDX — приватный сервер).
- Не использовать для обхода оплаты на коммерческих кабинетах и не выдавать чужие карты за свои.
- IDm/access_code — чувствительные данные: шифрование хранилища, без бэкапа в облако по умолчанию.

---

## 14. Приложения

### 14.1. Артефакты реверса (в репозитории)
- `.reverse/eamemu/jadx-out/` — Java-декомпиляция (jadx).
- `.reverse/eamemu/apktool-out/`, `.reverse/eamemu/apktool-smali/` — ресурсы, манифест, smali.
- `.reverse/eamemu/konami_ref.py` — проверенный референс конвертера (Python, без зависимостей).
- Ключевые классы: `tk.nulldori.eamemu.{HcefModule, eAMEMuService, CardConvModule, A, B, E}`.

### 14.2. Источники (high-confidence)
- AquaDX: `github.com/MewoLab/AquaDX` (`AimeDB.kt`, `CardController.kt`, `GameApiController.kt`,
  `SecurityConfig.kt`, `AquaNet/src/libs/sdk.ts`).
- Карты/AIC: `sega.bsnk.me/allnet/{amusement_ic,aimedb/felica,access_codes/aicc}`.
- Konami ID: `eamuse.bsnk.me/cardid.html` (каноничный алгоритм и векторы).
- Android HCE-F: `developer.android.com/reference/android/nfc/cardemulation/{NfcFCardEmulation,HostNfcFService}`,
  `source.android.com/docs/core/connect/felica`, AOSP `NfcFCardEmulation.java`.
- Ридеры: `github.com/whowechina/aic_pico`, `github.com/djhackersdev/segatools`,
  `github.com/Nat-Lab/aimeio-pcsc`, eAMEMu (`github.com/Tudwad/eAMEMu_RN-English`).
