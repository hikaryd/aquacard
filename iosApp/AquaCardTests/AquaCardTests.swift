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
