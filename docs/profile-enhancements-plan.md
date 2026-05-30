# План: AquaCard — maimai-only профиль + обложки/аватар + иконка приложения + авто-кэш

> Статус: **PENDING APPROVAL** (ralplan / consensus, итерация 2 — учтены правки Architect).
> Базируется на готовом и архитектором-APPROVED профиле (docs/profile-ui-plan.md).
> **ВАЖНО — раскладка проекта изменилась (приложение перенесено):**
> Gradle-корень теперь `/Users/user/aquacard` напрямую (НЕ `aquacard_src/android`).
> Исходники: `/Users/user/aquacard/app/src/main/java/net/aquadx/aquacard/...`.
> Ресурсы: `/Users/user/aquacard/app/src/main/res/...`. Иконка-исходник: `/Users/user/aquacard/docs/icon.jpg`.
> Артефакты плана/PRD/progress — только под `/Users/user/aquacard/docs/`.
> Исследование ассетов/Coil — live-verified (HTTP 200), см. §2.

---

## 0. Цель (4 требования пользователя)
1. **Только maimai.** Убрать chu3/ongeki/wacca из профиля.
2. **Обложки песен + аватар игрока.** Сейчас профиль «грустный» без картинок.
3. **Иконка приложения** из `/Users/user/aquacard/docs/icon.jpg` (1024×1024).
4. **Авто-кэш + авто-обновление при открытии.** Без ручной кнопки «Загрузить»; всегда актуально и красиво (stale-while-revalidate).

## 1. Контекст (факты, проверены на ФС)
- Стек: Kotlin 1.9.22, Compose BOM 2023.10.01 (compiler 1.5.8), AGP 8.2.2, **minSdk 26**, targetSdk 34, JDK 17, coroutines 1.7.3, retrofit 2.9, kotlinx-serialization 1.6.2.
- Профиль (готов, APPROVED): `data/{AquaProfileModels,ProfileDomain,AquaProfileService,ScoreFormat,AquaProfileRepository,AquaApi}.kt`, `ui/screens/profile/*`. `AquaProfileRepository.staticBase(baseUrl)` есть и протестирован.
- `AquaUser` объявлен в **`data/AquaApi.kt:16`**, уже `@Serializable`; НЕТ поля `profilePicture` (фикстуры уже шлют его — `ignoreUnknownKeys` его молча отбрасывает). Добавить поле сюда.
- `ProfileDomain.kt`: `sealed interface ProfileScore` (10), `sealed interface RecentEntry` (34), `data class ProfileBundle` (66) — **без `@Serializable`**.
- Параметризация по игре: sealed `.Mai/.Chu`, chu-ветки в репозитории/формате, chu-тесты, GameChips на 4 игры. `allMusicUrl(baseUrl, game)` и многие хелперы берут `game`.
- Старые `AquaService`/`GameSummaryResponse`/`LinkCardResponse`/`createService`/`linkCard`/`getGameSummary` — используются ТОЛЬКО внутри `AquaApi.kt` и в `ApiParseTest.kt` (нет прод-вызовов) → безопасно удалить, мигрировав `ApiParseTest` на `GameSummary`.
- Иконка: только adaptive (`mipmap-anydpi-v26/ic_launcher{,_round}.xml`), legacy-PNG mipmap НЕТ; minSdk 26 ⇒ adaptive на всех. `ic_launcher_background=#1B2233`. `sips` доступен.
- INTERNET-разрешение уже есть. `SettingStore` хранит только `base_url`. Кэш карт — Room (`CardDao`), для профиля Room не используем.
- Сборка: `cd /Users/user/aquacard && JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`.

## 2. Исследование ассетов (live-verified, §11)
- **Обложка:** `DATA_HOST/d/mai2/music/{file}.png`, `file=(musicId % 10000).toString().padStart(6,'0')`. Одна на песню. `musicId=11 → …/000011.png` (HTTP 200). Lossy при `musicId ≥ 10000` (редко) → локальный плейсхолдер поглощает 404.
- **Аватар:** `AQUA_HOST/uploads/net/portrait/{profilePicture}`; пусто → локальные инициалы (не дёргать `/portrait/null`). `iconId` картинкой не отдаётся — отклонено.
- **DATA_HOST = AQUA_HOST = `AquaProfileRepository.staticBase(baseUrl)`** (для `https://aquadx.net/aqua` → `https://aquadx.net`; для self-hosted следует baseUrl).
- **Coil 2.7.0** (`io.coil-kt:coil-compose:2.7.0`, Maven Central; НЕ 3.x). Манифест/ProGuard не трогаем.
- **Фолбэки только локальные**: `ic_jacket_placeholder` (drawable) + `InitialsAvatar` (composable).

## 3. Подход и решения (resequenced: фичи сначала, рискованный рефактор — последним)

> Драйвер «не сломать APPROVED-профиль» (§1) против «красиво/чисто». Синтез: сначала доставляем все
> 4 фичи с НУЛЕВЫМ риском для протестированного слоя (UI-лок maimai + картинки + иконка + кэш), и лишь
> ПОТОМ, отдельной поведенчески-нейтральной фазой, схлопываем sealed и чистим dead-code. Так любая
> регрессия от рефактора изолирована в одном коммите.

### 3.1 (US-A) Лёгкий maimai-lock + плумбинг — zero-risk
- `ProfileScreen.kt`: захардкодить `game = "mai2"` в точке вызова репозитория; удалить `GameChips`/`GAMES`/`game`-state/`loadedGame`. Упростить `primaryTitle/secondaryTitle` до констант («Best 35» / «Best 15»); убрать `game`-параметр из вызовов `ProfileHeader`/`BestRow`/`ScoreRow` (использовать maimai-литералы). Sealed/параметрику слоя данных НЕ трогаем (game по-прежнему передаётся как литерал "mai2" в репозиторий до US-E).
- `data/AquaApi.kt`: `AquaUser` += `profilePicture: String? = null`.
- `@Serializable` на `ProfileScore` (+ subclass `Mai`/`Chu`), `RecentEntry` (+ subclass), `BestEntry`, `ProfileBundle`. kotlinx сам обрабатывает sealed-полиморфизм в пределах модуля (дискриминатор `type`).
- Билд+тесты зелёные (поведение не менялось).

### 3.2 (US-B) Обложки + аватар (Coil)
- `coil-compose:2.7.0` в `libs.versions.toml` + `app/build.gradle.kts`.
- `data/AquaAssets.kt`: `jacketUrl(baseUrl, musicId)` (= `${staticBase(baseUrl)}/d/mai2/music/${(musicId%10000).pad6}.png`), `avatarUrlOrNull(baseUrl, profilePicture)` (пусто → null).
- `res/drawable/ic_jacket_placeholder.xml` — плоский брендовый скруглённый квадрат.
- `ui/screens/profile/AsyncArt.kt`:
  - `JacketImage(baseUrl, musicId, size=48dp)`: `AsyncImage(model=ImageRequest…, placeholder=painterResource(ic_jacket_placeholder), error=painterResource(ic_jacket_placeholder), contentScale=Crop, …clip(RoundedCornerShape(8.dp)))`.
  - `InitialsAvatar(name, size)`: детерминированный цвет из `name.hashCode()`+инициалы (обобщить нынешний `AvatarCircle`).
  - `PlayerAvatar(baseUrl, profilePicture, name)`: **Kotlin-ветка** — если `avatarUrlOrNull(...)==null` → `InitialsAvatar`; иначе `SubcomposeAsyncImage(model=…, error={ InitialsAvatar(name,size) }, loading={…}, …clip(CircleShape))`. (Composable-слот ошибки есть только у `SubcomposeAsyncImage`, не у `AsyncImage`.)
- Встроить `JacketImage` слева в `BestRow/ScoreRow` (`ScoreList.kt`) и `RecentRow` (`RecentSection.kt`); `PlayerAvatar` в `ProfileHeader.kt`.
- **`items(...)` в `ProfileScreen` получают стабильный `key`** (например `key={it.musicId}` для scores/recent) — чтобы Coil не перепривязывал картинки к чужим строкам.
- `AquaAssetsTest` (см. §5).

### 3.3 (US-C) Иконка приложения (adaptive-only, minSdk 26)
- Создать недостающие папки `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` (`mkdir -p`) — сейчас есть только `mipmap-anydpi-v26`.
- `sips` из `docs/icon.jpg` (1024²) → `ic_launcher_image.png` в `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` при 108/162/216/324/432 px.
- `mipmap-anydpi-v26/ic_launcher.xml` и `ic_launcher_round.xml`: `<background android:drawable="@mipmap/ic_launcher_image"/>`, `<foreground android:drawable="@android:color/transparent"/>`. Манифест `@mipmap/ic_launcher` не трогаем.
- **Замечание:** лаунчер маскирует фон под форму + parallax-overscan (~до 33%), поэтому смысловой центр фото должен быть по центру (углы могут срезаться) — это нормально для иконки.
- Старые `ic_launcher_foreground`/`ic_launcher_background` оставить (безвредно).

### 3.4 (US-D) Авто-кэш + авто-обновление (stale-while-revalidate)
- `SettingStore`: + `last_username` (`getLastUsername()/setLastUsername()`).
- `data/ProfileCache.kt`:
  - `@Serializable data class CachedProfile(summary, detail, best, bestSecondary, scores, recent, trend, meta, savedAtMillis)` — снимок **без** `errors` (отдельный тип, НЕ `@Transient`-хак). Сериализация через `AquaApi.json` (Map<Int,MusicMeta> корректно round-trip-ится примитивным ключом).
  - Файл **`filesDir/profile_v2_<sanitizedUsername>.json`** — версионированное имя (`v2`). Так файлы кэша, записанные на US-D-сборке (sealed-домен с дискриминатором `type`), после US-E (плоский домен) просто не читаются по другому имени/формату, а не парсятся ошибочно. `read(username): CachedProfile?` (битый/отсутствует/несовместимый → null), `write(username, bundle, nowMillis)`.
  - **In-memory зеркало — процесс-синглтон** (`companion object val mem = mutableMapOf<String, CachedProfile>()`, по образцу `AquaProfileRepository.sharedMetaCache`), чтобы переживать пересоздание `ProfileScreen` при переключении вкладок (`when(currentTab)` в `MainAppScreen` уничтожает composition-state).
  - `nowMillis` ИНЪЕКТИРУЕТСЯ (в коде `System.currentTimeMillis()`, в тестах фиксируется) — `ProfileCache` не дёргает время сам.
- **`data/CachePolicy.kt` — чистая (НЕ-Compose) логика решения** (тестируемый шов):
  - `sealed interface Decision { Idle; ServeCachedOnly; ServeCachedThenRefresh; RefreshOnly }`.
  - `fun decide(cached: CachedProfile?, now: Long, thresholdMs: Long = 60_000, manualRefresh: Boolean): Decision`:
    `manualRefresh` → если есть кэш `ServeCachedThenRefresh` иначе `RefreshOnly`; нет кэша → `RefreshOnly`; кэш свежий (`now-savedAt ≤ threshold`) → `ServeCachedOnly`; кэш устаревший → `ServeCachedThenRefresh`. (Idle — когда нет username.)
  - `LaunchedEffect` лишь вызывает `CachePolicy.decide(...)` и исполняет ветку — вся развилка покрыта юнит-тестом, в Compose остаётся только плумбинг.
- **Приоритет username (явно):** при первой композиции поле инициализируется из `last_username`; смена выбранной карты пользователем перезаписывает поле её `linkedAquaUsername`; ручной ввод перезаписывает вручную. Авто-загрузка всегда работает по ТЕКУЩЕМУ значению поля. (Существующий `LaunchedEffect(selectedCardIndex, cards)` правится: на первой композиции НЕ затирать `last_username`, если он есть; затирать только при реальной смене индекса пользователем.)
- `ProfileScreen` (stale-while-revalidate):
  1. `LaunchedEffect` при открытии: вычислить username (см. приоритет). Если непустой → `mem`/файл-кэш показать **мгновенно**, затем по `CachePolicy.decide(cached, now, 60_000, manualRefresh=false)` решить, делать ли сетевой refresh.
  2. По успеху refresh: обновить UI, `write` кэш (mem+файл), `setLastUsername`.
  - **Guard от гонки:** in-flight запрос помечается запрошенным `username`; если к моменту ответа `username` в поле сменился — результат отбрасывается (как уже делается для `requestedGame`).
  - Убрать «Загрузить» как основной CTA; оставить редактируемое поле username (смена игрока → загрузка) + иконку **refresh** (`decide(..., manualRefresh=true)` обходит порог) + тонкий индикатор сверху.
- Порог 60 c защищает сервер от спама при частом переключении вкладок.

### 3.5 (US-E) Изолированный рефактор-чистка (ПОСЛЕДНЯЯ фаза, поведенчески-нейтрально)
Только после зелёных US-A..D. В одном изолированном наборе изменений:
- Схлопнуть sealed → плоские `data class` (`ProfileScore`, `RecentEntry` с maimai-полями); удалить `.Mai/.Chu`.
- `AquaProfileService`: хардкод `game/mai2/`, убрать `@Path game`. `AquaProfileRepository`: убрать параметр `game`, `mapScores/mapRecent` — maimai-only, `best35/best15`, `allMusicUrl(baseUrl)` хардкод `mai2`, `sharedMetaCache` → `Map<Int,MusicMeta>?` (без игрового измерения). `ScoreFormat`: убрать `game`-параметры (levelName/formatRating/parseBestTuple/bestValueLabel) — maimai-only.
- Удалить dead-code: `AquaService`/`GameSummaryResponse`/`LinkCardResponse`/`createService`/`linkCard`. Мигрировать `ApiParseTest` на `GameSummary`.
- Тесты: удалить chu-кейсы (`ratingChu_*`, `recentChu_*`, chu-кейсы ScoreFormatTest, `unsupportedGame_degradesToHeaderOnly`); **`HostDerivationTest` обновить** (`allMusicUrl` теряет параметр `game`); адаптировать `ProfileCache`-тесты под плоский домен (кэш-формат меняется — старые файлы кэша поглощаются толерантным `read→null`).
- Полный прогон зелёный.

## 4. Изменения по файлам (пути — НОВАЯ раскладка)
**Новые:** `app/src/main/java/.../data/AquaAssets.kt`, `data/ProfileCache.kt`, `data/CachePolicy.kt`, `ui/screens/profile/AsyncArt.kt`, `app/src/main/res/drawable/ic_jacket_placeholder.xml`, `app/src/main/res/mipmap-*dpi/ic_launcher_image.png` (5), тесты `app/src/test/.../AquaAssetsTest.kt`, `ProfileCacheTest.kt`, `CachePolicyTest.kt`.
**Изменяемые:** `data/AquaApi.kt` (AquaUser.profilePicture; в US-E — удаление старых моделей), `data/ProfileDomain.kt` (@Serializable; US-E — collapse), `data/AquaProfileService.kt` (US-E), `data/AquaProfileRepository.kt` (US-E), `data/ScoreFormat.kt` (US-E), `data/SettingStore.kt`, `ui/screens/profile/{ProfileScreen,ProfileHeader,ScoreList,RecentSection}.kt`, `ui/screens/MainAppScreen.kt` (refresh-экшен для tab 2, если в TopAppBar), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher{,_round}.xml`, тесты `ScoreFormatTest/ProfileParseTest/ProfileRepositoryTest/HostDerivationTest` (US-E prune/adapt), `ApiParseTest` (→ GameSummary, US-E).

## 5. Тестовая стратегия (именованные тесты)
- `AquaAssetsTest`:
  - `jacket_pads_to_6_digits`: 11→`…/d/mai2/music/000011.png`; 1571→`001571.png`; 100→`000100.png` (baseUrl `https://aquadx.net/aqua`).
  - `jacket_highId_isLossyButNoThrow`: 11451→`001451.png` (документируем, не падаем).
  - `avatar_blank_returnsNull`; `avatar_builds_portrait_url` (`8781.png`→`…/uploads/net/portrait/8781.png`); self-hosted shape (`https://host:8080/`).
- `ProfileCacheTest` (US-D):
  - `roundTrip_preservesScoresAndSummary` (write→read равно по ключевым полям; meta Map<Int,..> восстановлен);
  - `corruptFile_returnsNull` (битый JSON → null);
  - `incompatibleFormat_readsAsNull` (файл в чужом/старом JSON-формате → null, без краша — подстраховка к версионированному имени `profile_v2_`).
- `CachePolicyTest` (US-D, чистая логика):
  - `noCache_refreshOnly`; `freshCache_serveCachedOnly` (`now-savedAt ≤ 60_000`); `staleCache_serveCachedThenRefresh`; `manualRefresh_bypassesThreshold` (свежий кэш + manualRefresh → ServeCachedThenRefresh); `noUsername_idle`.
- `ScoreFormatTest` (после US-E — maimai-only): achievement%, levelIndex_3_MASTER, bestTuple_mai_parses, bestTuple_shortAndLong_doNotThrow, formatRating_isRawInt, maiRank_thresholds, songName_fallsBackToId.
- `ProfileParseTest`: summary (с `profilePicture`), rating(best35/best15+musicList), recent, trend, userGames, allMusic, unknownFields_doNotThrow. (chu — удалить в US-E.)
- `ProfileRepositoryTest`: oneSectionThrows_othersSurvive, mapsScoresSortedByAchievement, emptyMeta_fallsBackToMusicId.
- `HostDerivationTest`: обновить под `allMusicUrl(baseUrl)` (US-E).
- Иконку юнит-тестом не проверить → критерий = ресурсы созданы + assembleDebug.

## 6. Acceptance criteria (механически проверяемо; пути новые)
1. **maimai-only (UI):** в `app/src/main/java/.../ui/screens/profile/` нет `GameChips`/чипов 4 игр; профиль грузит только mai2. assembleDebug ок (US-A).
   - **Ручной smoke (named, не-механический):** открыть вкладку профиля с привязанной maimai-картой — шапка (аватар) + Best + недавние + все скоры заполняются, у строк видны обложки. Это единственная честно-ручная проверка; фиксируется явно.
2. **Обложки/аватар:** `AquaAssetsTest` зелёный; `JacketImage`/`PlayerAvatar`/`InitialsAvatar` есть; `ic_jacket_placeholder.xml` есть; `coil-compose:2.7.0` в `libs.versions.toml`+`build.gradle.kts`; `AquaUser.profilePicture` парсится; `items(...)` со стабильными `key`.
3. **Иконка:** 5 `mipmap-*dpi/ic_launcher_image.png` из `docs/icon.jpg`; adaptive XML ссылаются на `@mipmap/ic_launcher_image`; assembleDebug ок; `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep ic_launcher_image` — присутствует (надёжнее `aapt dump`).
4. **Авто-кэш/refresh:** `ProfileCacheTest` + `CachePolicyTest` зелёные; `CachePolicy.decide` существует как чистая функция (юнит-покрыта); кэш-файл с именем `profile_v2_*`; `SettingStore.last_username` есть; `ProfileScreen` имеет `LaunchedEffect`-автозагрузку (вызывает `CachePolicy.decide`) + refresh-иконку; основной CTA «Загрузить» убран (grep старого текста кнопки → пусто); username-guard присутствует.
5. **Чистка (US-E):** домен без sealed (grep `sealed interface ProfileScore` → пусто); `AquaService`/`GameSummaryResponse`/`createService` удалены (grep → пусто); сервис содержит `game/mai2/` литералом; `HostDerivationTest` обновлён.
6. **Регрессия:** `cd /Users/user/aquacard && JAVA_HOME=/opt/homebrew/opt/openjdk@17 ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain` → BUILD SUCCESSFUL, 0 failures; APK обновлён в `~/aquacard/AquaCard-debug.apk` (источник `app/build/outputs/apk/debug/app-debug.apk`).

## 7. Риски → митигайии
| Риск | Митигайия | Проверка |
|---|---|---|
| Обложка lossy при musicId≥10000 | локальный плейсхолдер + Coil error= | `jacket_highId_isLossyButNoThrow` |
| Самохостед-хост | оба хоста из `staticBase(baseUrl)` | `AquaAssetsTest` self-hosted |
| Пустой/404 аватар | Kotlin-ветка + `SubcomposeAsyncImage error={}` | `avatar_blank_returnsNull` |
| Coil 3.x несовместим | пин 2.7.0 | build (compiler 1.5.8) |
| `when(currentTab)` уничтожает state | кэш-зеркало = процесс-синглтон; время из файла/инъекции | `ProfileCacheTest` + ручная проверка переключения вкладок |
| Авто-refresh спамит | порог 60 c; ручной refresh обходит | `CachePolicyTest` (fresh/stale/manualRefresh) |
| Гонка кэш/сеть/смена игрока | username-guard отбрасывает устаревший ответ | код-ревью + ручная проверка |
| `items()` без key → чужие картинки | стабильные `key={it.musicId}` | код-ревью |
| Collapse ломает тестируемый слой | US-E последняя, изолированная, поведенчески-нейтральная; полный прогон | §6 |
| Битый кэш | толерантное чтение → null | `corruptFile_returnsNull` |
| Смена формата кэша US-D→US-E (sealed→плоский) | версионированное имя `profile_v2_`; чужой формат → null (саморегенерация одним refresh) | `incompatibleFormat_readsAsNull` |
| Self-hosted по `http://` (cleartext) | вне scope: targetSdk 34 по умолчанию запрещает cleartext, предполагается https (как и нынешний профиль — регрессии нет). При необходимости — `networkSecurityConfig` отдельной задачей | примечание/осознанное решение |
| Иконка-фото обрезается маской | смысловой центр по центру; full-bleed background | визуально + assembleDebug |

## 8. Порядок исполнения (фазы)
1. **US-A** maimai-lock (UI) + `profilePicture` + `@Serializable` домена. Зелёный.
2. **US-B** Coil + `AquaAssets` + placeholder + `AsyncArt` (Jacket/Avatar/Initials) + вставка + `items` keys + `AquaAssetsTest`. Зелёный.
3. **US-C** иконка (sips 5 png + adaptive XML). Зелёный assembleDebug.
4. **US-D** SettingStore + `ProfileCache` (синглтон+файл+инъекция времени) + stale-while-revalidate + username-guard + refresh-UI + `ProfileCacheTest`. Зелёный.
5. **US-E** изолированный collapse sealed→data class + хардкод mai2 + прун chu/dead-code + миграция/обновление тестов (вкл. `HostDerivationTest`, `ApiParseTest`). Полный зелёный.
6. **US-F** финальная верификация §6 + обновить APK.

## 9. RALPLAN-DR
**Принципы:** (1) Не сломать APPROVED-профиль — фичи сначала, рискованный рефактор последним и изолированно. (2) maimai-only ⇒ убрать YAGNI (sealed) — но в самом конце, поведенчески-нейтрально. (3) Картинки устойчивы: только локальные фолбэки. (4) Кэш-первый UX: мгновенный показ из синглтон/файл-кэша + фоновое обновление с троттлингом и guard'ом. (5) Малые файлы, механически проверяемые критерии, верные пути ФС.

**Драйверы:** (1) «только маймай + красиво»; (2) «грустно без картинок»; (3) «не нажимать каждый раз».

**Опции:**
- **A (выбрано, resequenced):** фичи (UI-лок maimai + Coil-картинки + adaptive-иконка + файловый stale-while-revalidate кэш) сначала, изолированный collapse — последним.
  - Плюсы: вся запрошенная поверхность; нулевой риск тестируемому слою до самого конца; чистый итог. Минусы: параметрика живёт до US-E (временный dead-код).
- **B (отклонено):** только спрятать UI-чипы, слой данных не трогать вовсе.
  - Минусы: навсегда остаётся мёртвый chu-код (против «красиво»).
- **C (отклонено):** collapse первым (исходный порядок).
  - Минусы: рискованный рефактор тестируемого слоя ДО доставки ценности; удаляет тесты, защищающие удаляемый код, — нарушает драйвер «не сломать».

Инвалидция: B оставляет мёртвый код (против «красиво»); C ставит наибольший риск вперёд (против «не сломать»). A даёт чистый итог при изолированном риске в конце.

## 10. ADR
- **Decision:** resequenced-доставка: (A) UI-лок maimai + `@Serializable` + `profilePicture`; (B) Coil 2.7.0 обложки/аватар с локальными фолбэками и хостами из `staticBase`, стабильные `items`-keys; (C) adaptive-иконка full-bleed из `docs/icon.jpg` (5 плотностей, sips); (D) файловый `CachedProfile` stale-while-revalidate + процесс-синглтон-зеркало + `last_username` + инъекция времени + username-guard, порог 60 c, авто-загрузка при открытии, ручной refresh; (E) изолированный collapse sealed→data class + прун chu/dead-code в самом конце.
- **Drivers:** «только маймай/красиво», «грустно без картинок», «не нажимать каждый раз».
- **Alternatives considered:** B (только UI) — мёртвый код; C (collapse первым) — риск вперёд.
- **Why chosen:** вся поверхность, реальные данные с устойчивыми фолбэками, мгновенный кэш-первый UX, риск рефактора изолирован в финале.
- **Consequences:** Coil-зависимость; кэш-файлы в filesDir; иконка через сборочный шаг sips; временный dead-код до US-E.
- **Follow-ups:** pull-to-refresh; экран-детализация песни; конфигурируемые DATA_HOST/AQUA_HOST в настройках; скриншот-тесты; дисковый кэш обложек (Coil сам кеширует).

## 11. Источники
- Обложки/аватар: MewoLab/AquaDX @ v1-dev — `AquaNet/src/libs/scoring.ts` (jacket builder), `ui.ts` `pfp()`, `config.ts` (DATA_HOST/AQUA_HOST); live HTTP 200 (`000011/000100/001571.png`) 2026-05-30.
- Coil 2.7.0 — Maven Central `io.coil-kt:coil-compose` (verified); совместим с Compose 1.5.x/compiler 1.5.8; НЕ 3.x.
