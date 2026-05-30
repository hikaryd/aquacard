# План: полноценный экран профиля AquaDX (AquaProfileService + вкладка «Статистика»)

> Статус: **PENDING APPROVAL** (ralplan / consensus, итерация 2 — учтены правки Architect+Critic).
> Источник контракта API: `docs/aquadx-api-spec.md`. Цель — вынести `AquaProfileService`
> и переделать вкладку «Статистика» в полный профиль: шапка + Best + все скоры (с
> названиями песен) + недавние партии + график рейтинга.
>
> **Артефакты плана/PRD/progress живут только под `/Users/user/aquacard/docs/`.
> Никогда не писать в `/Users/user/dots` (это dotfiles — держим чистыми).**

---

## 0. Контекст (что есть сейчас)

- **Слой данных** `data/AquaApi.kt`: `AquaService` знает только `getGameSummary` (GET
  `user-summary`) и `linkCard` (POST). Модели: `AquaUser`, `GameSummaryResponse`,
  `LinkCardResponse`. Парсер `AquaApi.json` уже `ignoreUnknownKeys=true; coerceInputValues=true; isLenient=true`.
  Логирование клиента — `Level.BODY` (для профиля понизим до `BASIC`, чтобы не дампить тысячи строк `musicList`).
- **UI** `ui/screens/MainAppScreen.kt` → `ProfileTab` (стр. 656–849): селектор карты,
  поле username, чипы игры, кнопка «Загрузить», и **только** карточка-шапка. Сам `ProfileTab` — это `LazyColumn`.
- Прежний баг профиля (краш парсинга `regTime` String→Long) уже починен. Текущий запрос — фича «полный профиль».
- Деплой-факты: gradle-wrapper в `/Users/user/aquacard/aquacard_src/android/gradlew`;
  тесты — плоский пакет `app/src/test/java/net/aquadx/aquacard/`; существующие
  `ApiParseTest`, `CardFormatTest`, `CryptoTest` должны остаться зелёными.

## 1. Цель и жёсткое требование

Главный драйвер (слова пользователя): **«100% уверены, что оно не сломается»**. Прошлый сбой —
краш парсинга. Значит устойчивость к дрейфу схемы и частичным отказам важнее богатства визуала.

Важное уточнение из ревью (Architect): union-nullable защищает от *краша парсинга*, но создаёт риск
*тихого неверного рендера* (например, прогон chu3 `scoreMax` 0…1010000 через формат achievement даст
«101000.0000%»). Поэтому: **толерантность на границе провода (wire DTO), но строгая типизация в ядре** —
репозиторий мапит wire-DTO в per-game `sealed`-модель, и `when` в UI становится исчерпывающим (компилятор
запрещает прочитать mai-поле у chu-строки). Это «модельная» половина Option B без её зависимостей.

## 2. Выбранный подход (Option A + sealed-ядро)

Слои: **wire-модели (tolerant) → сервис → репозиторий (параллельно, изоляция отказов, маппинг в sealed)
→ формат-хелперы → UI-компоненты**.

- Полная поддержка **mai2 + chu3**; ongeki/wacca — graceful degrade (шапка/что есть, без краша).
- График — нативный Compose `Canvas` (без charting-библиотек), с **фиксированной высотой**.
- **Без удалённых картинок/аватаров в MVP** (пути ассетов в спеке §4 не подтверждены, Coil — лишняя
  зависимость). Шапка — на инициалах/рейтинге.
- **Один общий `LazyColumn`** на весь экран: длинный `musicList` эмитим как `items(...)`, секции — `item {}`.

### Отклонённые альтернативы
- **Option B целиком** (типизированные per-game + charting-lib Vico/MPAndroidChart + Coil): тяжелее APK,
  больше зависимостей = больше поверхности поломки. Отклонено. **Но** «типизированная per-game модель» из B
  частично принимается как sealed-ядро (см. §1) — без её зависимостей.
- **Option C** (WebView со страницей AquaNet): не нативно, нет своего дизайна, трение с Turnstile/логином,
  привязка к разметке сайта. Отклонено.

## 3. Изменения по файлам (развилки разрешены явно)

### Новые файлы
1. `data/AquaProfileModels.kt` — **wire-DTO** (`@Serializable`, **все поля кроме `musicId` имеют
   `= null`/`= emptyList()`; не полагаемся на `coerceInputValues` для non-null без дефолта**):
   - `GameSummary`: `name`, `aquaUser`, `serverRank`, `rating`, `ratingHighest`, `accuracy`(Double?),
     `plays`, `maxCombo`, `fullCombo`, `allPerfect`, `totalScore`, `lastVersion`, `joined`, `lastSeen`,
     `ranks: List<RankCount> = emptyList()`.
   - `RankCount(name: String? = null, count: Int? = null)`.
   - `UserDetailDto` (**типизированный nullable, НЕ JsonObject**): только используемые поля —
     `playerRating: Int? = null`, `classRank: Int? = null`, `courseRank: Int? = null`, `level: Int? = null`.
   - `UserRatingDto(best35, best15, best30, recent10: List<List<String>> = emptyList(),
     musicList: List<MusicScoreDto> = emptyList())`.
   - `MusicScoreDto` (**типизированный union, все nullable**): `musicId: Int`, `level: Int? = null`,
     `playCount`, `achievement`(mai2), `deluxscoreMax`(mai2), `scoreMax`(chu3), `scoreRank`,
     `comboStatus`, `syncStatus`, `isFullCombo`, `isAllJustice` — всё `= null`.
   - `RecentPlayDto` (**типизированный union nullable**): `musicId`, `level`, `userPlayDate`/`playDate`,
     `achievement`(mai2)/`score`(chu3), `scoreRank`/`rank`, `comboStatus`, `isClear`,
     `beforeRating`/`afterRating`, `placeName` — всё `= null`, кроме `musicId`.
   - `TrendPoint(date: String, rating: Int? = null, plays: Int? = null)`.
   - `GameBrief(name, rating, lastLogin)` (nullable).
   - `MusicMeta(name: String? = null, genre: String? = null, notes: List<NoteLv> = emptyList())`,
     `NoteLv(lv: Double? = null)`.
2. `data/ProfileDomain.kt` — **per-game `sealed`-ядро (то, что потребляет UI)**:
   - `sealed interface ProfileScore { val musicId: Int; val level: Int
       data class Mai(musicId, level, achievement: Int?, deluxscore: Int?, comboStatus: Int?, syncStatus: Int?, scoreRank: Int?)
       data class Chu(musicId, level, score: Int?, scoreRank: Int?, isFullCombo: Boolean?, isAllJustice: Boolean?) }`
   - `sealed interface RecentEntry { Mai(...) / Chu(...) }` по той же схеме.
   - `data class BestEntry(musicId: Int, level: Int, value: Int)` (значение = achievement|score, по игре).
   - `data class ProfileBundle(summary: GameSummary?, detail: UserDetailDto?, best: List<BestEntry>,
       bestSecondary: List<BestEntry>, scores: List<ProfileScore>, recent: List<RecentEntry>,
       trend: List<TrendPoint>, meta: Map<Int, MusicMeta>, errors: List<String>)`.
   - UI ветвится исчерпывающим `when(score)`; **никаких строковых `game == "mai2"` проверок в UI**.
3. `data/AquaProfileService.kt` — Retrofit-интерфейс: `summary/detail/rating/recent/trend`
   (`@Path game`, `@Query username`), `userGames(@Query username)`, и
   `allMusic(@Url url: String): Map<String, MusicMeta>`. **`url` строится как АБСОЛЮТНЫЙ**
   (`https://host/d/{game}/00/all-music.json`), не относительный (иначе Retrofit резолвит к `/aqua/` → 404).
4. `data/AquaProfileRepository.kt` — оркестрация:
   `suspend fun load(baseUrl, game, username): ProfileBundle`.
   - Запускает summary/detail/rating/recent/trend/meta **параллельно** в `supervisorScope { async { } }`,
     **try/catch ВНУТРИ каждого `async`** → секция = null/пусто при ошибке, ошибка пишется в `errors`
     (провал одной секции НЕ отменяет соседей).
   - Маппинг wire→sealed по `game` (mai2→`Mai`, chu3→`Chu`, иначе пусто). Best-кортежи парсятся
     через **`getOrNull(i)?.toIntOrNull()`** (без bare `[i]`): mai = `[musicId,levelIndex,v3,achievement]` (4),
     chu = `[musicId,levelIndex,score]` (3).
   - Статик-хост: `staticBase(baseUrl)` = детерминированная функция (тестируемая), meta-URL абсолютный.
   - Кеш meta — in-memory по игре (грузим один раз за сессию). Нет meta → пустая карта (UI фолбэкнет на musicId).
   - Запуск на `Dispatchers.IO`; запись Compose-стейта — на Main (как в текущем коде).
5. `data/ScoreFormat.kt` — чистые хелперы (из спеки §5), все **null-safe**:
   `achievementPercent(Int?) → "100.8790%"`, `levelName(Int?) → BASIC..Re:MASTER/ULTIMA`, `levelColor(Int?)`,
   `maiRank(achievement: Int?) → S/SS/SSS…`, `comboLabel(Int?)`, `syncLabel(Int?)`, `formatRating(game, Int?)`,
   `chuScore(Int?)`, `parseBestTuple(game, List<String>) → BestEntry?` (size-gated, не кидает).
6. `ui/screens/AquaApi.kt` правка — добавить `createProfileService(baseUrl)` рядом с `createService`
   (переиспользует **тот же** `json`; **не создаём второй `Json` с другими настройками**; logging `BASIC`).
7. `ui/screens/profile/ProfileScreen.kt` + компоненты (каждый <400 строк, у каждого `@Preview`):
   - `ProfileScreen.kt` — контролы (селектор карты, username, чипы игры, Load) + единый `LazyColumn`
     со всеми секциями + состояния loading/section-errors.
   - `ProfileHeader.kt` — шапка: имя/displayName, @username, страна, bio, рейтинг (крупно), ratingHighest,
     serverRank, plays, accuracy, maxCombo/fullCombo/allPerfect.
   - `RankDistribution.kt` — `ranks` чипами.
   - `TrendChart.kt` — линейный график `trend` на `Canvas` с **фиксированной `.height(160.dp)`**.
   - `ScoreList.kt` — `ScoreRow` (название из meta, сложность+цвет, achievement/score, ранг, FC/AP),
     Best-секции и полный `scores` (исчерпывающий `when(ProfileScore)`).
   - `RecentSection.kt` — лента недавних партий.

### Изменяемые файлы
- `ui/screens/MainAppScreen.kt` — тело `ProfileTab` → вызов `ProfileScreen(cards, baseUrl)` (минус ~190 строк),
  вкладочную обвязку оставить.
- `data/AquaApi.kt` — `createProfileService` (см. п.6). `AquaService`/`linkCard` не трогаем.
- `res/values/strings.xml` + `res/values-en/strings.xml` — новые строки секций/лейблов.
- Тесты (ниже).

## 4. Тестовая стратегия (именованные тесты)

Файлы в `app/src/test/java/net/aquadx/aquacard/`:

- `ProfileParseTest.kt`:
  - `summaryMai_parses()` — реальный mai2 `user-summary` из спеки §3.1, проверка полей + не кидает.
  - `ratingMai_parsesBestAndMusicList()` — mai2 `best35`/`best15`(4-кортеж) + `musicList`.
  - `ratingChu_parsesBest30Recent10AndMusicList()` — **chu3** `best30`/`recent10`(3-кортеж) + `musicList`.
  - `recentChu_parses()` / `trend_parses()` / `userGames_parses()` / `allMusic_parses()`.
  - `unknownFields_doNotThrow()` — объект с лишними/`null` полями парсится без исключения.
- `ScoreFormatTest.kt`:
  - `achievement_1008790_to_percent()` → `"100.8790%"`.
  - `levelIndex_3_is_MASTER()`; rank-маппинг.
  - `bestTuple_mai_parses()` → `["834","4","19998","1008790"]` → (834, lvl4, 1008790).
  - `bestTuple_chu_parses()` → 3-элементный кортеж.
  - `bestTuple_shortAndLong_doNotThrow()` — кортежи длины 2 и 6 → `null`/безопасно, без исключения.
- `ProfileRepositoryTest.kt` (фейковый сервис):
  - `oneSectionThrows_othersSurvive()` — бросающий `trend`: бандл содержит summary/rating непустыми,
    `errors` содержит запись (изоляция отказа, acceptance §5.3).
  - `unsupportedGame_degradesToHeaderOnly()` — game=`ongeki`: `scores`/`recent`/`best` пусты, summary есть,
    без краша (acceptance §5.8).
  - `emptyMeta_fallsBackToMusicId()` — пустая meta-карта: join не кидает, имя = `musicId` строкой.
- `HostDerivationTest.kt`:
  - `staticBase_for_3_shapes()` — вход `https://aquadx.net/aqua`, `https://aquadx.net/aqua/`,
    `https://host:8080/` → проверяем итоговый абсолютный meta-URL для каждого.
- Существующие `ApiParseTest`, `CardFormatTest`, `CryptoTest` — остаются зелёными.

## 5. Acceptance criteria (каждый механически проверяем)

1. **Сервис компилируется.** `AquaProfileService` отдаёт summary/detail/rating/recent/trend/userGames +
   `allMusic(@Url)`. Проверка: `assembleDebug` успешен.
2. **Парсинг обеих игр без исключений.** Проверка: `ProfileParseTest` (mai2 и chu3 кейсы) зелёный.
3. **Изоляция отказа секции.** Проверка: `ProfileRepositoryTest.oneSectionThrows_othersSurvive` зелёный.
4. **Защита позиционных кортежей.** Проверка: `ScoreFormatTest.bestTuple_chu_parses` +
   `bestTuple_shortAndLong_doNotThrow` зелёные.
5. **Деривация хоста зафиксирована.** Проверка: `HostDerivationTest.staticBase_for_3_shapes` зелёный
   (3 формы baseUrl → корректные абсолютные meta-URL).
6. **Sealed-ядро принято.** Репозиторий возвращает `ProfileScore.Mai|Chu` и `RecentEntry.Mai|Chu`;
   UI использует исчерпывающий `when`, без строковых `game ==` проверок. Проверка: grep отсутствия
   `game ==` в `ui/screens/profile/` + `assembleDebug` (исчерпывающий `when` компилируется).
7. **Хелперы кодировок корректны.** Проверка: `ScoreFormatTest` (achievement→%, levelIndex→имя, ранги) зелёный.
8. **Graceful degrade ongeki/wacca.** Проверка: `ProfileRepositoryTest.unsupportedGame_degradesToHeaderOnly` зелёный.
9. **Фолбэк имён песен.** Проверка: `ProfileRepositoryTest.emptyMeta_fallsBackToMusicId` зелёный.
10. **UI собирается и превью рендерятся.** Каждый компонент (`ProfileHeader`, `RankDistribution`,
    `TrendChart`, `ScoreList`, `RecentSection`) имеет `@Preview`; `TrendChart` Canvas — фикс-высота.
    Проверка: `assembleDebug` успешен (компиляция превью). *Живой UI-QA на устройстве — follow-up.*
11. **Регрессия зелёная.** Проверка: `./gradlew :app:testDebugUnitTest :app:assembleDebug` успешны;
    `lsp_diagnostics` 0 ошибок на новых `.kt` (`AquaProfileModels/ProfileDomain/AquaProfileService/`
    `AquaProfileRepository/ScoreFormat/ProfileScreen` и компоненты).

**Команды верификации (рабочая директория зафиксирована):**
```
cd /Users/user/aquacard/aquacard_src/android
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

## 6. Риски → проверяемые митигайии

| Риск | Митигайия | Чем проверяется |
|---|---|---|
| Дрейф схемы объектов | nullable+default все wire-поля, `ignoreUnknownKeys`, try/catch в каждом `async` | `unknownFields_doNotThrow`, `oneSectionThrows_othersSurvive` |
| Дрейф позиционных кортежей `best*` | `getOrNull`-гейтинг, без bare `[i]` | `bestTuple_shortAndLong_doNotThrow`, `bestTuple_chu_parses` |
| Тихий неверный рендер mai/chu | wire→sealed маппинг, исчерпывающий `when` | критерий 6 (grep + компиляция) |
| Неверный статик-хост (self-host) | детерминированная `staticBase`, абсолютный `@Url`, фолбэк на musicId | `staticBase_for_3_shapes`, `emptyMeta_fallsBackToMusicId` |
| Огромный `musicList` | единый `LazyColumn`+`items()`, logging `BASIC` | компиляция + ручной smoke |
| Неизвестный `scoreRank` chu3 | null-safe формат, числовой ранг как есть | `ScoreFormatTest` (не кидает на неизвестном) |
| Невидимый Canvas в скролле | фиксированная `.height(160.dp)` | критерий 10 (`@Preview`) |
| Отмена соседних секций | `supervisorScope` + try/catch внутри `async` | `oneSectionThrows_othersSurvive` |

## 7. Порядок исполнения (фазы для ralph/team)

1. `AquaProfileModels.kt` + `ProfileDomain.kt` + `AquaProfileService.kt` + `createProfileService` в `AquaApi.kt`.
2. `ScoreFormat.kt` + `ScoreFormatTest.kt` (TDD: хелперы и tuple-парс первыми).
3. `AquaProfileRepository.kt` (supervisorScope/изоляция/маппинг/staticBase) +
   `ProfileRepositoryTest.kt` + `ProfileParseTest.kt` + `HostDerivationTest.kt`.
4. UI: `ProfileScreen.kt` + компоненты (с `@Preview`), замена `ProfileTab` в `MainAppScreen.kt`, строки.
5. `cd .../aquacard_src/android && ./gradlew :app:testDebugUnitTest :app:assembleDebug`, lsp, фикс до зелёного.

## 8. ADR

- **Decision:** слоистый нативный профиль; **tolerant wire-DTO → per-game `sealed`-ядро**; параллельная
  загрузка в `supervisorScope` с изоляцией отказов внутри каждого `async`; `getOrNull`-защита кортежей;
  абсолютный `@Url` для `all-music.json` + тест деривации хоста; Canvas-график фикс-высоты; без
  charting/image-deps; MVP полностью mai2+chu3, ongeki/wacca degrade; единый `LazyColumn`.
- **Drivers:** (1) «не сломается» → устойчивость+тесты на самых рисковых поверхностях (кортежи, хост,
  мульти-игра); (2) per-game дивергенция схемы → sealed-ядро против тихого неверного рендера;
  (3) большой musicList + ловушка вложенного скролла → один список.
- **Alternatives considered:** B целиком (типизация + charting-lib + Coil) — лишние зависимости/поверхность
  отказа (частично принята лишь sealed-модель без её зависимостей); C (WebView) — не нативно, теряет дизайн-цель.
- **Why chosen:** даёт всю запрошенную поверхность (шапка+Best+все скоры+недавние+график) при минимальном
  риске поломки, без новых зависимостей, с механически проверяемыми критериями приёмки.
- **Consequences:** доп. слой маппинга wire→sealed в репозитории; график рисуем руками; нет аватаров в MVP;
  живой UI-QA на устройстве вынесен в follow-up.
- **Follow-ups:** аватары/обложки через Coil; фильтр/сортировка musicList; кеш профиля на диск;
  ongeki/wacca как первоклассные; инструментальный/скриншот-тест для рендера.
