import Foundation

enum ScoreFormat {
    private static let maiLevels = ["BASIC", "ADVANCED", "EXPERT", "MASTER", "Re:MASTER"]

    static func achievementPercent(_ achievement: Int?) -> String {
        guard let achievement else { return "-" }
        let whole = achievement / 10_000
        let fraction = achievement % 10_000
        return String(format: "%d.%04d%%", locale: Locale(identifier: "en_US_POSIX"), whole, fraction)
    }

    static func levelName(_ level: Int?) -> String {
        guard let level else { return "?" }
        guard maiLevels.indices.contains(level) else { return "LV\(level)" }
        return maiLevels[level]
    }

    static func maiRank(_ achievement: Int?) -> String {
        guard let achievement else { return "-" }
        switch achievement {
        case 1_005_000...: return "SSS+"
        case 1_000_000...: return "SSS"
        case 995_000...: return "SS+"
        case 990_000...: return "SS"
        case 980_000...: return "S+"
        case 970_000...: return "S"
        case 940_000...: return "AAA"
        case 900_000...: return "AA"
        case 800_000...: return "A"
        default: return "-"
        }
    }

    static func comboLabel(_ status: Int?) -> String? {
        switch status {
        case 1: return "FC"
        case 2: return "FC+"
        case 3: return "AP"
        case 4: return "AP+"
        default: return nil
        }
    }

    static func syncLabel(_ status: Int?) -> String? {
        switch status {
        case 1: return "FS"
        case 2: return "FS+"
        case 3: return "FDX"
        case 4: return "FDX+"
        default: return nil
        }
    }

    static func formatRating(_ rating: Int?) -> String {
        rating.map(String.init) ?? "-"
    }

    static func songName(_ meta: [Int: MusicMeta], _ musicId: Int) -> String {
        let name = meta[musicId]?.name?.trimmingCharacters(in: .whitespacesAndNewlines)
        return name?.isEmpty == false ? name! : "\(musicId)"
    }

    static func parseBestTuple(_ tuple: [String]) -> BestEntry? {
        guard tuple.count > 3,
              let musicId = Int(tuple[0]),
              let value = Int(tuple[3]) else {
            return nil
        }
        return BestEntry(musicId: musicId, level: Int(tuple[1]) ?? 0, value: value)
    }

    static func bestValueLabel(_ value: Int) -> String {
        achievementPercent(value)
    }
}

enum AquaAssets {
    static func staticBase(_ baseURL: String) -> String {
        let trimmed = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
            .removingTrailingSlash()
        if trimmed.hasSuffix("/aqua") {
            return String(trimmed.dropLast(5))
        }
        return trimmed
    }

    static func allMusicURL(_ baseURL: String) -> String {
        "\(staticBase(baseURL))/d/mai2/00/all-music.json"
    }

    static func jacketURL(baseURL: String, musicId: Int) -> URL? {
        let file = String(format: "%06d", musicId % 10_000)
        return URL(string: "\(staticBase(baseURL))/d/mai2/music/\(file).png")
    }

    static func avatarURL(baseURL: String, profilePicture: String?) -> URL? {
        guard let picture = profilePicture?.trimmingCharacters(in: .whitespacesAndNewlines),
              !picture.isEmpty else {
            return nil
        }
        return URL(string: "\(staticBase(baseURL))/uploads/net/portrait/\(picture)")
    }
}

private extension String {
    func removingTrailingSlash() -> String {
        var value = self
        while value.hasSuffix("/") {
            value.removeLast()
        }
        return value
    }
}
