import XCTest
@testable import AquaCard

final class AquaCardTests: XCTestCase {
    func testAchievementPercentMatchesAndroidFormat() {
        XCTAssertEqual(ScoreFormat.achievementPercent(1_008_790), "100.8790%")
        XCTAssertEqual(ScoreFormat.achievementPercent(nil), "-")
    }

    func testAssetUrlsMirrorAndroidRules() {
        XCTAssertEqual(AquaAssets.staticBase("https://aquadx.net/aqua"), "https://aquadx.net")
        XCTAssertEqual(AquaAssets.staticBase("https://aquadx.net/aqua/"), "https://aquadx.net")
        XCTAssertEqual(AquaAssets.staticBase("https://host:8080/"), "https://host:8080")
        XCTAssertEqual(AquaAssets.jacketURL(baseURL: "https://aquadx.net/aqua", musicId: 834)?.absoluteString, "https://aquadx.net/d/mai2/music/000834.png")
    }

    func testDtoMissingIdentifiersUseAndroidCompatibleDefaults() throws {
        let score = try JSONDecoder().decode(MusicScoreDto.self, from: Data(#"{"achievement":1005590}"#.utf8))
        XCTAssertEqual(score.musicId, 0)
        XCTAssertEqual(score.achievement, 1_005_590)

        let recent = try JSONDecoder().decode(RecentPlayDto.self, from: Data(#"{"level":3}"#.utf8))
        XCTAssertEqual(recent.musicId, 0)
        XCTAssertEqual(recent.level, 3)

        let trend = try JSONDecoder().decode(TrendPoint.self, from: Data(#"{"rating":16666}"#.utf8))
        XCTAssertEqual(trend.date, "")
        XCTAssertEqual(trend.rating, 16_666)
    }

    func testMusicMetaDecodesNumericName() throws {
        // Песня «39»: сервер отдаёт name числом. Строгий String-декод падал —
        // теперь имя читается и из числа.
        let meta = try JSONDecoder().decode(MusicMeta.self, from: Data(#"{"name":39,"genre":"maimai","notes":[{"lv":5}]}"#.utf8))
        XCTAssertEqual(meta.name, "39")
        XCTAssertEqual(meta.genre, "maimai")
    }

    func testCatalogueDecodeSurvivesNumericNameEntry() throws {
        // Раньше одна запись с name-числом роняла декод всего словаря -> meta пустой -> имена как ID.
        let json = #"{"8":{"name":"True Love Song"},"146":{"name":39},"10146":{"name":39}}"#
        let raw = try JSONDecoder().decode([String: FailableDecodable<MusicMeta>].self, from: Data(json.utf8))
        let map = Dictionary(uniqueKeysWithValues: raw.compactMap { key, wrapped -> (Int, MusicMeta)? in
            guard let id = Int(key), let meta = wrapped.value else { return nil }
            return (id, meta)
        })
        XCTAssertEqual(map.count, 3)
        XCTAssertEqual(ScoreFormat.songName(map, 8), "True Love Song")
        XCTAssertEqual(ScoreFormat.songName(map, 146), "39")
    }

    func testFailableDecodableSkipsBrokenEntryNotWholeMap() throws {
        // Совсем несовместимая запись (не объект) отбрасывается, остальные выживают.
        let json = #"{"8":{"name":"Color My World"},"9":"garbage"}"#
        let raw = try JSONDecoder().decode([String: FailableDecodable<MusicMeta>].self, from: Data(json.utf8))
        let valid = raw.compactMap { key, wrapped -> (Int, MusicMeta)? in
            guard let id = Int(key), let meta = wrapped.value else { return nil }
            return (id, meta)
        }
        XCTAssertEqual(valid.count, 1)
        XCTAssertEqual(valid.first?.0, 8)
    }

    func testBestEnrichmentUsesExactMatchingPlay() {
        let play = RecentEntry(
            musicId: 11676, level: 3, achievement: 1_007_114,
            totalCombo: 954, fastCount: 9, lateCount: 12,
            judges: JudgeBreakdown(crit: 673, perfect: 265, great: 16, good: 0, miss: 0),
            notes: NoteBreakdown(tap: 672, hold: 76, slide: 113, touch: 55, brk: 38)
        )
        let best = BestEntry(musicId: 11676, level: 3, value: 1_007_114)
        let detail = best.toScoreDetail(recent: [play])

        XCTAssertEqual(detail.achievement, 1_007_114)
        XCTAssertEqual(detail.judges?.crit, 673)
        XCTAssertEqual(detail.notes?.tap, 672)
        XCTAssertEqual(detail.deluxscoreMax, 954 * 3) // DX-максимум = totalCombo*3
        XCTAssertEqual(detail.fastCount, 9)
    }

    func testBestEnrichmentIgnoresNonExactPlayToAvoidMisleadingDetail() {
        // В recent есть партия того же чарта, но со СЛАБЕЕ результатом — её судейство
        // не должно подмешиваться под лучший результат Best.
        let weaker = RecentEntry(
            musicId: 11676, level: 3, achievement: 990_000,
            judges: JudgeBreakdown(crit: 1, perfect: 2, great: 3, good: 4, miss: 5)
        )
        let best = BestEntry(musicId: 11676, level: 3, value: 1_007_114)
        let detail = best.toScoreDetail(recent: [weaker])

        XCTAssertNil(detail.judges, "Неточная партия не должна обогащать деталь")
        XCTAssertEqual(detail.achievement, 1_007_114, "Заголовок остаётся лучшим результатом Best")
    }

    func testBestEnrichmentFallsBackToPoorDetailWhenNoRecent() {
        let best = BestEntry(musicId: 42, level: 2, value: 980_000)
        let detail = best.toScoreDetail(recent: [])
        XCTAssertEqual(detail.musicId, 42)
        XCTAssertEqual(detail.achievement, 980_000)
        XCTAssertNil(detail.judges)
        XCTAssertNil(detail.deluxscoreMax)
    }

    func testRecentDeluxMaxNilWhenNoTotalCombo() {
        let play = RecentEntry(musicId: 1, level: 0, achievement: 1_000_000, totalCombo: 0)
        XCTAssertNil(play.toScoreDetail().deluxscoreMax)
    }

    func testManualRefreshServesCacheThenRefresh() {
        let cached = CachedProfile(bundle: ProfileBundle(), savedAtMillis: 0)
        XCTAssertEqual(CachePolicy.decide(query: "player", cached: cached, now: Date(), manualRefresh: true), .serveCachedThenRefresh)
    }

    func testFutureDatedCacheRevalidates() {
        let future = Int64(Date().addingTimeInterval(60).timeIntervalSince1970 * 1_000)
        let cached = CachedProfile(bundle: ProfileBundle(), savedAtMillis: future)
        XCTAssertEqual(CachePolicy.decide(query: "player", cached: cached, now: Date(), manualRefresh: false), .serveCachedThenRefresh)
    }

    func testCachedProfileDoesNotPersistTransientErrors() async throws {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("AquaCardTests-\(UUID().uuidString)", isDirectory: true)
        defer { try? FileManager.default.removeItem(at: directory) }

        let store = ProfileCacheStore(fileManager: FileManager.default, baseDirectory: directory)
        var bundle = ProfileBundle()
        bundle.errors = ["recent: timeout"]
        await store.write(baseURL: "https://aquadx.net/aqua", username: "Player", bundle: bundle)

        let cached = await store.read(baseURL: "https://aquadx.net/aqua", username: "player")
        XCTAssertEqual(cached?.bundle.errors, [])
    }
}
