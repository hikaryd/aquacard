# AquaDX Web API — спецификация для отрисовки профиля (maimai DX / CHUNITHM)

Контракт публичного веб-API AquaDX: шапка профиля, рейтинг, ранги, рейтинг-композиция (best),
все скоры по песням (`musicList`), недавние партии, график роста рейтинга.
Формы ответов сняты с `aquadx.net`. Источник: `MewoLab/AquaDX` (ветка `v1-dev`), `AquaNet/src/libs/sdk.ts`.

---

## 0. Кратко
- Всё, что нужно для отрисовки профиля, доступно публично по `username`, без авторизации.
- Ключевые эндпоинты (на игру `mai2`/`chu3`/`ongeki`/`wacca`):
  `user-summary`, `user-detail`, `user-rating`, `recent`, `trend`; общий `card/user-games`; рейтинг-доска `ranking`.
- «Все скоры» = `user-rating` → `musicList` (лучший результат по каждой сыгранной песне).
- Названия песен/уровни — из статического `all-music.json` (musicId → метаданные), кешировать.
- Идентификатор игрока в API — `username` (имя аккаунта AquaNet), не IDm/access code.

---

## 1. База и конвенции
- **Base URL:** `https://aquadx.net/aqua` (настраиваемый; self-hosted переопределяют). Все пути под `/api/v2`.
- **Метод:** официальный фронт шлёт `POST` form-encoded; публичные читающие эндпоинты отвечают и на `GET` с query-параметрами. Для приложения проще `GET ?username=...`.
- **Параметр игрока:** `username` (выбирает любого игрока по нику). Токен не требуется для чтения публичного профиля.
- **Игры (`{game}`):** `mai2` (maimai DX), `chu3` (CHUNITHM), `ongeki`, `wacca`.
- **CORS:** открыт (`*`) — можно запрашивать напрямую с клиента.
- **Формат:** JSON. Временны́е поля — Unix-секунды или строки дат (зависит от поля, см. ниже).

## 2. Авторизация (нужна только для своих/записывающих операций)
- Для отрисовки профиля по username — не нужна.
- Если понадобятся приватные операции (свой `user-box`, смена настроек, привязка карты):
  `POST /api/v2/user/login` (`email`|username, `password`, `turnstile`) → `{token}`; токен передаётся как обычный параметр запроса. Логин защищён Cloudflare Turnstile — нужен WebView с капчей. Для рендера профиля это не требуется.

---

## 3. Каталог эндпоинтов (на игру)

### 3.1 `GET /api/v2/game/{game}/user-summary?username=NAME`
Агрегированная шапка профиля. maimai пример:
```json
{"name":"Ｉｖｙ！ ０＿０",
 "aquaUser":{"username":"Sigma","displayName":"DEMONDX","country":"US","regTime":1748046905871,
   "profileLocation":"","profileBio":"…","profilePicture":"8781.png"},
 "serverRank":1,"accuracy":94.7967,"rating":16666,"ratingHighest":16666,
 "ranks":[{"name":"SSS+","count":987},{"name":"SSS","count":570}, …],
 "detailedRanks":{"13":{"SSS+":206,"SSS":89,…},"14":{…}},
 "maxCombo":1282,"fullCombo":140,"allPerfect":111,"totalScore":1027767,
 "plays":2795,"totalPlayTime":8385,"joined":"2025-05-13 23:16:44.0",
 "lastSeen":"2026-05-29 23:53:32.0","lastVersion":"1.60.00",
 "ratingComposition":{"best35":"…","best15":"…"},"recent":[ … ≤100 … ],"rival":…,"favorites":…}
```
- `aquaUser.regTime` — число (Long, ms). `accuracy` — Double (%). `ranks`/`detailedRanks` — распределение оценок по уровням.
- CHUNITHM: тот же конверт, но `ranks` = `SSS/SS/S/AAA`, рейтинг по другой шкале (целое ×100, см. user-detail.playerRating).

### 3.2 `GET /api/v2/game/{game}/user-detail?username=NAME`
Косметика/аватар и точный рейтинг. maimai:
```json
{"userName":"…","iconId":10,"plateId":3,"titleId":3,"frameId":3,"partnerId":34,
 "charaSlot":[500202,…],"lastRomVersion":"1.60.00","lastDataVersion":"1.60.08","lastGameId":"SDGA",
 "classRank":15,"playerRating":16666,"courseRank":10}
```
CHUNITHM:
```json
{"userName":"…","nameplateId":2,"frameId":4,"trophyId":744,"mapIconId":1,"voiceId":1,
 "avatarWear":3100101,"avatarHead":3200101,"avatarFace":1300001,…,
 "lastRomVersion":"2.31.00","lastDataVersion":"2.30.19","level":1,"playerRating":52}
```

### 3.3 `GET /api/v2/game/{game}/user-rating?username=NAME` — рейтинг-композиция + все скоры
maimai — `best35` + `best15` (топ для рейтинга) + `musicList` (все сыгранные песни):
```json
{"best35":[["834","4","19998","1008790"], …35],
 "best15":[["11820","4","25510","1005156"], …15],
 "musicList":[{"musicId":22,"level":4,"playCount":4,"achievement":1005590,
   "comboStatus":0,"syncStatus":0,"deluxscoreMax":2625,"scoreRank":13,"extNum1":0}, … ]}
```
- Кортеж `best*`: `[musicId, levelIndex, v3, achievement]`. `achievement` = ачивка ×10000 (1008790 = 100.8790%). `levelIndex` 0–4. `v3` — рейтинг/deluxscore конкретного захода (точнее брать из `musicList`, где поля явные).
- `musicList` — все скоры: по каждой песне+сложности `achievement`, `deluxscoreMax`, `scoreRank`, `comboStatus`, `syncStatus`, `playCount`.

CHUNITHM — `best30` + `recent10` + `musicList`:
```json
{"best30":[["2422","2","993344"],["2252","2","806267"], …],
 "recent10":[["2422","3","924078"], …],
 "musicList":[{"musicId":2252,"level":2,"playCount":1,"scoreMax":806267,"missCount":292,
   "maxComboCount":265,"fullChain":0,"maxChain":0,"scoreRank":4,"theoryCount":0,
   "isFullCombo":false,"isAllJustice":false,"isSuccess":1,"isLock":false}, … ]}
```
- Кортеж `best30`/`recent10`: `[musicId, levelIndex, score]` (chunithm score 0…1010000).
- `musicList.scoreMax` — лучший скор; `scoreRank` — числовой ранг (см. §5).

### 3.4 `GET /api/v2/game/{game}/recent?username=NAME` — недавние партии (playlog)
Массив записей последних заходов (по убыванию времени). maimai, ключевые поля:
`playlogId, musicId, level, trackNo, userPlayDate, placeName, type, achievement, deluxscore, scoreRank,
comboStatus, syncStatus, isClear, beforeRating/afterRating, …` (полный игровой playlog).
CHUNITHM:
```json
[{"romVersion":"2.31.00","orderId":0,"sortNumber":1764006684,"placeId":291,
  "playDate":"2025-11-24T00:00:00","userPlayDate":"2025-11-25T02:42:14","musicId":2252,"level":2,
  "score":806267,"rank":4,"maxCombo":265,"maxChain":0,
  "rateTap":7894,"rateHold":8845,"rateSlide":7939,"rateAir":8158,"rateFlick":0,
  "judgeGuilty":292,"judgeAttack":14,"judge…":…,"track":1}, … ]
```

### 3.5 `GET /api/v2/game/{game}/trend?username=NAME` — рост рейтинга по дням
```json
[{"date":"2025-05-13","rating":691,"plays":3},{"date":"2025-05-14","rating":1368,"plays":3}, … ]
```
Подходит для графика прогресса.

### 3.6 `GET /api/v2/game/{game}/ranking?page=N` — доска лидеров (100/стр)
Не по username; постранично. Для экрана «топ игроков сервера».

### 3.7 `GET /api/v2/card/user-games?username=NAME` — какие игры у аккаунта
```json
{"mai2":{"name":"…","rating":16666,"lastLogin":"2026-05-29 23:53:32.0"},
 "chu3":{"name":"…","rating":52,"lastLogin":"2025-11-25T02:51:19"},
 "ongeki":null,"wacca":null,"diva":null}
```
Удобно как «селектор игр» на экране профиля.

---

## 4. Метаданные песен (названия, уровни, обложки)
Статика, кешировать (большой JSON, меняется редко):
- `GET https://aquadx.net/d/{game}/00/all-music.json` → словарь `musicId → метаданные`.
- maimai пример:
  ```json
  {"8":{"name":"True Love Song","ver":"Ver1.00.00","composer":"Kai/…","genre":"maimai",
        "notes":[{"lv":5},{"lv":7.2},{"lv":10.2},{"lv":12.4}]}, …}
  ```
  `notes[levelIndex].lv` — внутренний уровень сложности (float). Индекс = `levelIndex` из скоров (0=BASIC…).
- Обложки/иконки: поля вроде `profilePicture` (`8781.png`), `iconId`, `plateId` — рендерятся через ассеты AquaNet (`https://aquadx.net/d/{game}/…`); конкретные пути уточнить по фронту, для MVP можно опустить.

---

## 5. Справочник кодировок
- `maimai achievement`: целое = `% × 10000` (1008790 → 100.8790%). Ранги: 100.5%+ = SSS+, 100% SSS, 99.5% SS+, 99% SS, 98% S+, 97% S, …
- `maimai levelIndex`: 0 BASIC, 1 ADVANCED, 2 EXPERT, 3 MASTER, 4 Re:MASTER.
- `maimai comboStatus`: 0 нет, 1 FC, 2 FC+, 3 AP, 4 AP+. `syncStatus`: 0 нет, 1 FS, 2 FS+, 3 FDX, 4 FDX+. `deluxscoreMax` — DX-счёт (для ★-рейтинга).
- `CHUNITHM score`: 0…1010000. `scoreRank`/`rank` (числовой): 0 D … SSS/SSS+ (точную таблицу взять из фронта `chu3` enums). `isFullCombo`/`isAllJustice` — флаги.
- `CHUNITHM levelIndex`: 0 BASIC, 1 ADVANCED, 2 EXPERT, 3 MASTER, 4 ULTIMA, (5 WORLD'S END).
- `rating`: maimai — целое (16666 = 166.66, DX рейтинг ×100). CHUNITHM — целое ×100 (52 = 0.52 у новичка; топовые ~17.xx → 1700+). Источник истины — `playerRating` из `user-detail`.

---

## 6. Порядок вызовов для полного профиля
Для экрана профиля игрока `NAME`, игра `G`:
1. `card/user-games?username=NAME` → показать доступные игры (табы), их рейтинг/последний вход.
2. `game/G/user-summary?username=NAME` → шапка: имя, рейтинг, serverRank, plays, accuracy, распределение рангов (`ranks`/`detailedRanks`), bio/страна/аватар.
3. `game/G/user-detail?username=NAME` → точный `playerRating`, плашки/аватар/класс.
4. `game/G/user-rating?username=NAME` → секции «Best» (`best35`+`best15` / `best30`+`recent10`) и «Все скоры» (`musicList`).
5. `game/G/recent?username=NAME` → лента «Недавние партии».
6. `game/G/trend?username=NAME` → график роста рейтинга.
7. Объединить любые `musicId` с кешированным `all-music.json` (название/жанр/внутр. уровень) для рендера.

Кешируй `all-music.json` (надолго) и ответы профиля (коротко, ~30–60с); не дёргай часто (за реверс-прокси возможны rate-limit/WAF).

---

## 7. Эскиз моделей и клиента (Kotlin/Retrofit)
```kotlin
interface AquaProfileService {
    @GET("api/v2/game/{game}/user-summary") suspend fun summary(@Path("game") g: String, @Query("username") u: String): GameSummary
    @GET("api/v2/game/{game}/user-detail")  suspend fun detail(@Path("game") g: String, @Query("username") u: String): JsonObject // поля отличаются по играм
    @GET("api/v2/game/{game}/user-rating")  suspend fun rating(@Path("game") g: String, @Query("username") u: String): UserRating
    @GET("api/v2/game/{game}/recent")       suspend fun recent(@Path("game") g: String, @Query("username") u: String): List<JsonObject>
    @GET("api/v2/game/{game}/trend")        suspend fun trend(@Path("game") g: String, @Query("username") u: String): List<TrendPoint>
    @GET("api/v2/card/user-games")          suspend fun userGames(@Query("username") u: String): Map<String, GameBrief?>
}

@Serializable data class TrendPoint(val date: String, val rating: Int, val plays: Int)
@Serializable data class GameBrief(val name: String? = null, val rating: Int? = null, val lastLogin: String? = null)

// maimai user-rating
@Serializable data class UserRating(
    val best35: List<List<String>> = emptyList(),  // [musicId, levelIndex, v3, achievement]
    val best15: List<List<String>> = emptyList(),
    val best30: List<List<String>> = emptyList(),   // chunithm
    val recent10: List<List<String>> = emptyList(), // chunithm
    val musicList: List<JsonObject> = emptyList()    // поля зависят от игры (см. §3.3)
)
```
> `user-detail`/`recent`/`musicList` сильно отличаются между играми — держи их как `JsonObject`/
> per-game data class и парси по `game`. `ignoreUnknownKeys=true`, числовые поля — `Long?`/`Int?`/`Double?`.

---

## 8. Различия maimai ↔ CHUNITHM (главное)
| | maimai (`mai2`) | CHUNITHM (`chu3`) |
|---|---|---|
| метрика результата | `achievement` (%×10000) | `score` (0…1010000) |
| рейтинг-композиция | `best35`+`best15` | `best30`+`recent10` |
| полный список скоров | `musicList` (achievement, deluxscoreMax, comboStatus, syncStatus) | `musicList` (scoreMax, scoreRank, isFullCombo, isAllJustice) |
| levelIndex 4 | Re:MASTER | ULTIMA |
| user-detail | plate/title/frame/chara, classRank/courseRank | nameplate/trophy/avatar*, level |

---

## 9. Ограничения и этикет
- Публичного списка карт аккаунта нет (см. отдельную спеку линкинга). Профиль ключуется по `username`.
- `recent` ограничен последними N заходами; полную историю партий API не отдаёт — только агрегаты и `musicList` с лучшими результатами.
- `musicList` содержит лучший скор по каждой песне, не всю историю.
- Сервер приватный: не спамь запросами, кешируй, держи base URL настраиваемым.
- Поля могут расширяться от версии игры — всегда `ignoreUnknownKeys=true`.

## 10. Источники
- Репозиторий: `github.com/MewoLab/AquaDX` (`v1-dev`): `net/games/GameApiController.kt`, `net/games/Models.kt`, `AquaNet/src/libs/sdk.ts`.
- Формы ответов сняты с `https://aquadx.net/aqua/api/v2/...` и `https://aquadx.net/d/{game}/00/all-music.json`.
- Связанная спека эмуляции/линкинга карт: `docs/AquaDX-Card-App-Spec.md`, `docs/plan.md`.
