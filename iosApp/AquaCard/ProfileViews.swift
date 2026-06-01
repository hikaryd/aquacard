import SwiftUI

struct ProfileHeaderView: View {
    let summary: GameSummary
    let detail: UserDetailDto?
    let baseURL: String

    var body: some View {
        let displayName = summary.aquaUser?.displayName?.nilIfBlank
            ?? summary.aquaUser?.username
            ?? summary.name
            ?? "Игрок"
        let rating = detail?.playerRating ?? summary.rating

        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                AvatarView(baseURL: baseURL, user: summary.aquaUser, displayName: displayName)

                VStack(alignment: .leading, spacing: 4) {
                    Text(displayName)
                        .font(.title3.bold())
                        .foregroundStyle(AquaTheme.text)
                        .lineLimit(1)

                    if let username = summary.aquaUser?.username {
                        Text("@\(username)")
                            .font(.subheadline)
                            .foregroundStyle(AquaTheme.mutedText)
                    }

                    if let country = summary.aquaUser?.country?.nilIfBlank {
                        Text(country)
                            .font(.caption)
                            .foregroundStyle(AquaTheme.mutedText)
                    }
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text(ScoreFormat.formatRating(rating))
                        .font(.system(size: 32, weight: .bold, design: .rounded))
                        .foregroundStyle(AquaTheme.primary)
                    Text("Рейтинг")
                        .font(.caption)
                        .foregroundStyle(AquaTheme.mutedText)
                }
            }

            if let bio = summary.aquaUser?.profileBio?.nilIfBlank {
                Text(bio)
                    .font(.footnote)
                    .foregroundStyle(AquaTheme.text.opacity(0.86))
                    .lineLimit(3)
            }

            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                StatCell(label: "Игр", value: summary.plays.map(String.init) ?? "-")
                StatCell(label: "Макс", value: ScoreFormat.formatRating(summary.ratingHighest))
                StatCell(label: "Ранг", value: summary.serverRank.map { "#\($0)" } ?? "-")
                StatCell(label: "Точность", value: summary.accuracy.map { String(format: "%.2f%%", $0) } ?? "-")
            }

            if summary.maxCombo != nil || summary.fullCombo != nil || summary.allPerfect != nil {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                    StatCell(label: "Max Combo", value: summary.maxCombo.map(String.init) ?? "-")
                    StatCell(label: "Full Combo", value: summary.fullCombo.map(String.init) ?? "-")
                    StatCell(label: "All Perfect", value: summary.allPerfect.map(String.init) ?? "-")
                }
            }
        }
        .profileSurface()
    }
}

struct BestRowView: View {
    let index: Int
    let entry: BestEntry
    let meta: [Int: MusicMeta]
    let baseURL: String

    var body: some View {
        ScoreShell(
            baseURL: baseURL,
            musicId: entry.musicId,
            prefix: "\(index + 1)",
            title: ScoreFormat.songName(meta, entry.musicId),
            level: entry.level,
            value: ScoreFormat.bestValueLabel(entry.value),
            badges: []
        )
    }
}

struct ScoreRowView: View {
    let score: ProfileScore
    let meta: [Int: MusicMeta]
    let baseURL: String

    var body: some View {
        ScoreShell(
            baseURL: baseURL,
            musicId: score.musicId,
            prefix: nil,
            title: ScoreFormat.songName(meta, score.musicId),
            level: score.level,
            value: ScoreFormat.achievementPercent(score.achievement),
            badges: [ScoreFormat.comboLabel(score.comboStatus), ScoreFormat.syncLabel(score.syncStatus)].compactMap { $0 }
        )
    }
}

struct RecentRowView: View {
    let entry: RecentEntry
    let meta: [Int: MusicMeta]
    let baseURL: String

    var body: some View {
        ScoreShell(
            baseURL: baseURL,
            musicId: entry.musicId,
            prefix: nil,
            title: ScoreFormat.songName(meta, entry.musicId),
            level: entry.level,
            value: ScoreFormat.achievementPercent(entry.achievement),
            badges: [ScoreFormat.comboLabel(entry.comboStatus), ScoreFormat.syncLabel(entry.syncStatus), shortDate(entry.playDate)].compactMap { $0 }
        )
    }

    private func shortDate(_ raw: String?) -> String? {
        raw?.split(separator: "T").first?.split(separator: " ").first.map(String.init)
    }
}

private struct ScoreShell: View {
    let baseURL: String
    let musicId: Int
    let prefix: String?
    let title: String
    let level: Int
    let value: String
    let badges: [String]

    var body: some View {
        HStack(spacing: 10) {
            if let prefix {
                Text(prefix)
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(AquaTheme.mutedText)
                    .frame(width: 24, alignment: .leading)
            }

            JacketView(baseURL: baseURL, musicId: musicId)

            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(.subheadline)
                    .foregroundStyle(AquaTheme.text)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Badge(text: ScoreFormat.levelName(level), color: difficultyColor(level))
                    ForEach(badges, id: \.self) { badge in
                        Badge(text: badge, color: AquaTheme.primary)
                    }
                }
            }

            Spacer()

            Text(value)
                .font(.subheadline.monospacedDigit().bold())
                .foregroundStyle(AquaTheme.primary)
        }
        .profileSurface()
    }
}

struct TrendView: View {
    let points: [TrendPoint]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .bottom, spacing: 4) {
                ForEach(points.suffix(20)) { point in
                    Capsule()
                        .fill(AquaTheme.primary.opacity(0.85))
                        .frame(height: barHeight(for: point))
                        .frame(maxWidth: .infinity)
                        .accessibilityLabel("\(point.date): \(point.rating ?? 0)")
                }
            }
            .frame(height: 88, alignment: .bottom)

            if let latest = points.last {
                Text("Последняя точка: \(latest.date) · \(ScoreFormat.formatRating(latest.rating))")
                    .font(.caption)
                    .foregroundStyle(AquaTheme.mutedText)
            }
        }
        .profileSurface()
    }

    private func barHeight(for point: TrendPoint) -> CGFloat {
        let values = points.compactMap(\.rating)
        guard let rating = point.rating, let min = values.min(), let max = values.max(), max > min else {
            return 18
        }
        return 18 + CGFloat(rating - min) / CGFloat(max - min) * 70
    }
}

struct RankDistributionView: View {
    let ranks: [RankCount]

    var body: some View {
        VStack(spacing: 8) {
            ForEach(ranks) { rank in
                HStack {
                    Text(rank.name ?? "Rank")
                        .font(.footnote)
                        .foregroundStyle(AquaTheme.text)
                    Spacer()
                    Text(rank.count.map(String.init) ?? "-")
                        .font(.footnote.monospacedDigit().bold())
                        .foregroundStyle(AquaTheme.primary)
                }
            }
        }
        .profileSurface()
    }
}

private struct StatCell: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.headline.monospacedDigit())
                .foregroundStyle(AquaTheme.text)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
            Text(label)
                .font(.caption2)
                .foregroundStyle(AquaTheme.mutedText)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(8)
        .background(AquaTheme.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

struct SectionTitle: View {
    let title: String
    let count: Int?

    init(_ title: String, count: Int? = nil) {
        self.title = title
        self.count = count
    }

    var body: some View {
        HStack {
            Text(title)
                .font(.headline)
                .foregroundStyle(AquaTheme.text)
            Spacer()
            if let count {
                Text("\(count)")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(AquaTheme.mutedText)
            }
        }
    }
}

private struct Badge: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption2.bold())
            .foregroundStyle(color)
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(color.opacity(0.16))
            .clipShape(RoundedRectangle(cornerRadius: 5, style: .continuous))
    }
}

private struct JacketView: View {
    let baseURL: String
    let musicId: Int

    var body: some View {
        AsyncImage(url: AquaAssets.jacketURL(baseURL: baseURL, musicId: musicId)) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                Image(systemName: "music.note")
                    .font(.title2)
                    .foregroundStyle(AquaTheme.primary)
            }
        }
        .frame(width: 46, height: 46)
        .background(AquaTheme.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

private struct AvatarView: View {
    let baseURL: String
    let user: AquaUser?
    let displayName: String

    var body: some View {
        AsyncImage(url: AquaAssets.avatarURL(baseURL: baseURL, profilePicture: user?.profilePicture)) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                Text(initials(displayName))
                    .font(.headline.bold())
                    .foregroundStyle(AquaTheme.text)
            }
        }
        .frame(width: 58, height: 58)
        .background(AquaTheme.primary.opacity(0.32))
        .clipShape(Circle())
    }

    private func initials(_ value: String) -> String {
        let parts = value.split(separator: " ")
        let letters = parts.prefix(2).compactMap { $0.first }.map(String.init).joined()
        return letters.isEmpty ? "A" : letters.uppercased()
    }
}

private func difficultyColor(_ level: Int) -> Color {
    switch level {
    case 0: return .green
    case 1: return .yellow
    case 2: return .red
    case 3: return .purple
    case 4: return .white
    default: return AquaTheme.mutedText
    }
}

private extension View {
    func profileSurface() -> some View {
        padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AquaTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: AquaTheme.cardRadius, style: .continuous))
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
