import Foundation
import CryptoKit

actor ProfileCacheStore {
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    private let fileManager: FileManager
    private let baseDirectory: URL

    init(fileManager: FileManager = .default, baseDirectory: URL? = nil) {
        self.fileManager = fileManager
        if let baseDirectory {
            self.baseDirectory = baseDirectory
        } else {
            let support = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
                ?? fileManager.temporaryDirectory
            self.baseDirectory = support.appendingPathComponent("AquaCard/ProfileCache", isDirectory: true)
        }
    }

    func read(baseURL: String, username: String) -> CachedProfile? {
        let file = fileURL(baseURL: baseURL, username: username)
        guard let data = try? Data(contentsOf: file) else { return nil }
        return try? decoder.decode(CachedProfileSnapshot.self, from: data).cachedProfile
    }

    func write(baseURL: String, username: String, bundle: ProfileBundle) {
        do {
            try fileManager.createDirectory(at: baseDirectory, withIntermediateDirectories: true)
            let snapshot = CachedProfileSnapshot(
                bundle: bundle,
                savedAtMillis: Int64(Date().timeIntervalSince1970 * 1_000)
            )
            let data = try encoder.encode(snapshot)
            try data.write(to: fileURL(baseURL: baseURL, username: username), options: [.atomic])
        } catch {
            // Cache write is best-effort, matching the Android profile cache behavior.
        }
    }

    private func fileURL(baseURL: String, username: String) -> URL {
        let key = "\(baseURL.normalizedBaseURL)|\(username.lowercased())"
        return baseDirectory.appendingPathComponent("profile_v1_\(sha256Hex(key)).json")
    }

    private func sha256Hex(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}

private struct CachedProfileSnapshot: Codable {
    var savedAtMillis: Int64
    var summary: GameSummary?
    var detail: UserDetailDto?
    var best: [BestEntry]
    var bestSecondary: [BestEntry]
    var scores: [ProfileScore]
    var recent: [RecentEntry]
    var trend: [TrendPoint]
    var meta: [Int: MusicMeta]

    init(bundle: ProfileBundle, savedAtMillis: Int64) {
        self.savedAtMillis = savedAtMillis
        self.summary = bundle.summary
        self.detail = bundle.detail
        self.best = bundle.best
        self.bestSecondary = bundle.bestSecondary
        self.scores = bundle.scores
        self.recent = bundle.recent
        self.trend = bundle.trend
        self.meta = bundle.meta
    }

    var cachedProfile: CachedProfile {
        CachedProfile(
            bundle: ProfileBundle(
                summary: summary,
                detail: detail,
                best: best,
                bestSecondary: bestSecondary,
                scores: scores,
                recent: recent,
                trend: trend,
                meta: meta,
                errors: []
            ),
            savedAtMillis: savedAtMillis
        )
    }
}
