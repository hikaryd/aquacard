import Foundation

struct AquaProfileRepository {
    let client: AquaProfileClient
    let cache: ProfileCacheStore

    func readCache(baseURL: String, username: String) async -> CachedProfile? {
        await cache.read(baseURL: baseURL, username: username)
    }

    func writeCache(baseURL: String, username: String, bundle: ProfileBundle) async {
        await cache.write(baseURL: baseURL, username: username, bundle: bundle)
    }

    func load(baseURL: String, username: String) async throws -> ProfileBundle {
        async let summaryResult = section("summary") { try await client.summary(baseURL: baseURL, username: username) }
        async let detailResult = section("detail") { try await client.detail(baseURL: baseURL, username: username) }
        async let ratingResult = section("rating") { try await client.rating(baseURL: baseURL, username: username) }
        async let recentResult = section("recent") { try await client.recent(baseURL: baseURL, username: username) }
        async let trendResult = section("trend") { try await client.trend(baseURL: baseURL, username: username) }
        async let metaResult = musicMetadata(baseURL: baseURL)

        try Task.checkCancellation()
        let summary = await summaryResult
        let detail = await detailResult
        let rating = await ratingResult
        let recent = await recentResult
        let trend = await trendResult
        let meta = await metaResult
        try Task.checkCancellation()

        var errors = [String]()
        errors.append(contentsOf: [summary.error, detail.error, rating.error, recent.error, trend.error, meta.error].compactMap { $0 })

        if summary.value == nil,
           detail.value == nil,
           rating.value == nil,
           recent.value == nil,
           trend.value == nil {
            throw AquaRepositoryError.noProfileSections(errors)
        }

        return ProfileBundle(
            summary: summary.value,
            detail: detail.value,
            best: mapBest(rating.value?.best35 ?? []),
            bestSecondary: mapBest(rating.value?.best15 ?? []),
            scores: mapScores(rating.value?.musicList ?? []),
            recent: mapRecent(recent.value ?? []),
            trend: trend.value ?? [],
            meta: meta.value ?? [:],
            errors: errors
        )
    }

    private func section<T>(_ name: String, block: () async throws -> T) async -> SectionResult<T> {
        do {
            try Task.checkCancellation()
            return SectionResult(value: try await block(), error: nil)
        } catch is CancellationError {
            return SectionResult(value: nil, error: nil)
        } catch {
            return SectionResult(value: nil, error: "\(name): \(error.localizedDescription)")
        }
    }

    private func musicMetadata(baseURL: String) async -> SectionResult<[Int: MusicMeta]> {
        let key = AquaAssets.staticBase(baseURL)
        if let cached = await MusicMetadataCache.shared.value(for: key) {
            return SectionResult(value: cached, error: nil)
        }
        do {
            try Task.checkCancellation()
            let loaded = try await client.allMusic(baseURL: baseURL)
            try Task.checkCancellation()
            await MusicMetadataCache.shared.store(loaded, for: key)
            return SectionResult(value: loaded, error: nil)
        } catch is CancellationError {
            return SectionResult(value: nil, error: nil)
        } catch {
            return SectionResult(value: nil, error: "meta: \(error.localizedDescription)")
        }
    }

    private func mapBest(_ tuples: [[String]]) -> [BestEntry] {
        tuples.compactMap { ScoreFormat.parseBestTuple($0) }
    }

    private func mapScores(_ list: [MusicScoreDto]) -> [ProfileScore] {
        list.map {
            ProfileScore(
                musicId: $0.musicId,
                level: $0.level ?? 0,
                achievement: $0.achievement,
                deluxscore: $0.deluxscoreMax,
                comboStatus: $0.comboStatus,
                syncStatus: $0.syncStatus,
                scoreRank: $0.scoreRank
            )
        }.sorted { ($0.achievement ?? 0) > ($1.achievement ?? 0) }
    }

    private func mapRecent(_ list: [RecentPlayDto]) -> [RecentEntry] {
        list.sorted {
            ($0.userPlayDate ?? $0.playDate ?? "").split(separator: ".").first ?? "" >
            ($1.userPlayDate ?? $1.playDate ?? "").split(separator: ".").first ?? ""
        }.map {
            RecentEntry(
                musicId: $0.musicId,
                level: $0.level ?? 0,
                playDate: $0.userPlayDate ?? $0.playDate,
                achievement: $0.achievement,
                rank: $0.scoreRank,
                comboStatus: $0.comboStatus,
                isClear: $0.isClear,
                syncStatus: $0.syncStatus,
                deluxscore: $0.deluxscore,
                beforeRating: $0.beforeRating,
                afterRating: $0.afterRating,
                placeName: $0.placeName,
                maxCombo: $0.maxCombo,
                totalCombo: $0.totalCombo,
                fastCount: $0.fastCount,
                lateCount: $0.lateCount,
                isFullCombo: $0.isFullCombo,
                isAllPerfect: $0.isAllPerfect,
                trackNo: $0.trackNo,
                judges: judgeBreakdown($0),
                notes: noteBreakdown($0)
            )
        }
    }

    private func judgeBreakdown(_ dto: RecentPlayDto) -> JudgeBreakdown? {
        let crit = sum(dto.tapCriticalPerfect, dto.holdCriticalPerfect, dto.slideCriticalPerfect, dto.touchCriticalPerfect, dto.breakCriticalPerfect)
        let perfect = sum(dto.tapPerfect, dto.holdPerfect, dto.slidePerfect, dto.touchPerfect, dto.breakPerfect)
        let great = sum(dto.tapGreat, dto.holdGreat, dto.slideGreat, dto.touchGreat, dto.breakGreat)
        let good = sum(dto.tapGood, dto.holdGood, dto.slideGood, dto.touchGood, dto.breakGood)
        let miss = sum(dto.tapMiss, dto.holdMiss, dto.slideMiss, dto.touchMiss, dto.breakMiss)
        return crit + perfect + great + good + miss == 0 ? nil : JudgeBreakdown(crit: crit, perfect: perfect, great: great, good: good, miss: miss)
    }

    private func noteBreakdown(_ dto: RecentPlayDto) -> NoteBreakdown? {
        let tap = sum(dto.tapCriticalPerfect, dto.tapPerfect, dto.tapGreat, dto.tapGood, dto.tapMiss)
        let hold = sum(dto.holdCriticalPerfect, dto.holdPerfect, dto.holdGreat, dto.holdGood, dto.holdMiss)
        let slide = sum(dto.slideCriticalPerfect, dto.slidePerfect, dto.slideGreat, dto.slideGood, dto.slideMiss)
        let touch = sum(dto.touchCriticalPerfect, dto.touchPerfect, dto.touchGreat, dto.touchGood, dto.touchMiss)
        let brk = sum(dto.breakCriticalPerfect, dto.breakPerfect, dto.breakGreat, dto.breakGood, dto.breakMiss)
        return tap + hold + slide + touch + brk == 0 ? nil : NoteBreakdown(tap: tap, hold: hold, slide: slide, touch: touch, brk: brk)
    }

    private func sum(_ values: Int?...) -> Int {
        values.reduce(0) { $0 + ($1 ?? 0) }
    }
}

private struct SectionResult<T> {
    var value: T?
    var error: String?
}

private actor MusicMetadataCache {
    static let shared = MusicMetadataCache()
    private let maxEntries = 3
    private var cachedByStaticBase: [String: [Int: MusicMeta]] = [:]
    private var recency: [String] = []

    func value(for key: String) -> [Int: MusicMeta]? {
        guard let value = cachedByStaticBase[key] else { return nil }
        markRecent(key)
        return value
    }

    func store(_ value: [Int: MusicMeta], for key: String) {
        cachedByStaticBase[key] = value
        markRecent(key)
        while recency.count > maxEntries, let oldest = recency.first {
            recency.removeFirst()
            cachedByStaticBase.removeValue(forKey: oldest)
        }
    }

    private func markRecent(_ key: String) {
        recency.removeAll { $0 == key }
        recency.append(key)
    }
}

enum AquaRepositoryError: LocalizedError {
    case noProfileSections([String])

    var errorDescription: String? {
        switch self {
        case .noProfileSections(let errors):
            return errors.isEmpty ? "No profile data was returned" : errors.joined(separator: "; ")
        }
    }
}
