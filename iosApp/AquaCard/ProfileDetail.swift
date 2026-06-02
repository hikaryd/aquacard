import SwiftUI

/// Единая UI-модель детального просмотра трека (как maimai TRACK RESULT).
/// Богатые поля (судейство, точность по нотам, fast/late, рейтинг) есть только
/// у партий (recent); у Best/скоров они nil.
struct ScoreDetail: Identifiable, Equatable {
    let id: String
    let musicId: Int
    let level: Int
    let achievement: Int?
    let deluxscore: Int?
    let deluxscoreMax: Int?
    let maxCombo: Int?
    let totalCombo: Int?
    let comboStatus: Int?
    let syncStatus: Int?
    let isClear: Bool?
    let fastCount: Int?
    let lateCount: Int?
    let beforeRating: Int?
    let afterRating: Int?
    let playDate: String?
    let placeName: String?
    let trackNo: Int?
    let judges: JudgeBreakdown?
    let notes: NoteBreakdown?

    // Богатые поля по умолчанию nil — «бедные» источники (Best/score) задают только заголовок.
    init(
        id: String,
        musicId: Int,
        level: Int,
        achievement: Int?,
        deluxscore: Int? = nil,
        deluxscoreMax: Int? = nil,
        maxCombo: Int? = nil,
        totalCombo: Int? = nil,
        comboStatus: Int? = nil,
        syncStatus: Int? = nil,
        isClear: Bool? = nil,
        fastCount: Int? = nil,
        lateCount: Int? = nil,
        beforeRating: Int? = nil,
        afterRating: Int? = nil,
        playDate: String? = nil,
        placeName: String? = nil,
        trackNo: Int? = nil,
        judges: JudgeBreakdown? = nil,
        notes: NoteBreakdown? = nil
    ) {
        self.id = id
        self.musicId = musicId
        self.level = level
        self.achievement = achievement
        self.deluxscore = deluxscore
        self.deluxscoreMax = deluxscoreMax
        self.maxCombo = maxCombo
        self.totalCombo = totalCombo
        self.comboStatus = comboStatus
        self.syncStatus = syncStatus
        self.isClear = isClear
        self.fastCount = fastCount
        self.lateCount = lateCount
        self.beforeRating = beforeRating
        self.afterRating = afterRating
        self.playDate = playDate
        self.placeName = placeName
        self.trackNo = trackNo
        self.judges = judges
        self.notes = notes
    }
}

extension RecentEntry {
    func toScoreDetail() -> ScoreDetail {
        ScoreDetail(
            id: "recent-\(id)",
            musicId: musicId,
            level: level,
            achievement: achievement,
            deluxscore: deluxscore,
            deluxscoreMax: totalCombo.flatMap { $0 > 0 ? $0 * 3 : nil },
            maxCombo: maxCombo,
            totalCombo: totalCombo,
            comboStatus: comboStatus,
            syncStatus: syncStatus,
            isClear: isClear,
            fastCount: fastCount,
            lateCount: lateCount,
            beforeRating: beforeRating,
            afterRating: afterRating,
            playDate: playDate,
            placeName: placeName,
            trackNo: trackNo,
            judges: judges,
            notes: notes
        )
    }
}

extension BestEntry {
    /// Обогащённая деталь: показываем судейство/ноты/комбо из партии playlog'а, которая
    /// дала ИМЕННО этот лучший результат (точное совпадение musicId+level+achievement).
    /// Неточное совпадение намеренно НЕ используем — иначе под лучшим результатом показали бы
    /// судейство другой, более слабой партии. Нет точной партии в recent — «бедная» деталь.
    func toScoreDetail(recent: [RecentEntry]) -> ScoreDetail {
        if let exact = recent.first(where: { $0.musicId == musicId && $0.level == level && $0.achievement == value }) {
            return exact.toScoreDetail()
        }
        return toScoreDetail()
    }

    func toScoreDetail() -> ScoreDetail {
        ScoreDetail(id: "best-\(id)", musicId: musicId, level: level, achievement: value)
    }
}

extension ProfileScore {
    func toScoreDetail(recent: [RecentEntry]) -> ScoreDetail {
        if let achievement,
           let exact = recent.first(where: { $0.musicId == musicId && $0.level == level && $0.achievement == achievement }) {
            return exact.toScoreDetail()
        }
        return toScoreDetail()
    }

    func toScoreDetail() -> ScoreDetail {
        ScoreDetail(
            id: "score-\(id)",
            musicId: musicId,
            level: level,
            achievement: achievement,
            deluxscore: deluxscore,
            comboStatus: comboStatus,
            syncStatus: syncStatus
        )
    }
}

struct MusicDetailView: View {
    let detail: ScoreDetail
    let meta: [Int: MusicMeta]
    let baseURL: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    header
                    achievementBlock
                    badgesBlock

                    if let judges = detail.judges {
                        DetailSectionLabel("Судейство")
                        HStack(alignment: .top, spacing: 6) {
                            StatNumCell("CRIT", judges.crit, color: Self.critColor)
                            StatNumCell("PERFECT", judges.perfect, color: Self.perfectColor)
                            StatNumCell("GREAT", judges.great, color: Self.greatColor)
                            StatNumCell("GOOD", judges.good, color: Self.goodColor)
                            StatNumCell("MISS", judges.miss, color: Self.missColor)
                        }
                    }

                    if let notes = detail.notes {
                        DetailSectionLabel("Точность по нотам")
                        HStack(alignment: .top, spacing: 6) {
                            StatNumCell("TAP", notes.tap)
                            StatNumCell("HOLD", notes.hold)
                            StatNumCell("SLIDE", notes.slide)
                            StatNumCell("TOUCH", notes.touch)
                            StatNumCell("BREAK", notes.brk)
                        }
                    }

                    Divider()
                        .overlay(AquaTheme.mutedText.opacity(0.3))
                        .padding(.vertical, 14)

                    DetailRow(label: "MAX COMBO", value: comboLabel(detail.maxCombo, detail.totalCombo))
                    DetailRow(label: "DELUXE", value: comboLabel(detail.deluxscore, detail.deluxscoreMax))
                    DetailRow(label: "FAST / LATE", value: fastLateLabel(detail.fastCount, detail.lateCount))
                    DetailRow(label: "Рейтинг", value: ratingDeltaLabel(detail.beforeRating, detail.afterRating))
                    DetailRow(label: "Место", value: detail.placeName?.nonBlank)
                    DetailRow(label: "Сыграно", value: detail.playDate.map(prettyDateTime))
                }
                .padding(20)
            }
            .background(AquaTheme.background.ignoresSafeArea())
            .navigationTitle("Трек")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Готово") { dismiss() }
                        .tint(AquaTheme.primary)
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            DetailJacketView(baseURL: baseURL, musicId: detail.musicId)

            VStack(alignment: .leading, spacing: 4) {
                Text(ScoreFormat.songName(meta, detail.musicId))
                    .font(.title3.bold())
                    .foregroundStyle(AquaTheme.text)
                    .lineLimit(2)

                Text(levelLine)
                    .font(.caption.bold())
                    .foregroundStyle(detailDifficultyColor(detail.level))
            }
            Spacer(minLength: 0)
        }
    }

    private var levelLine: String {
        var line = ScoreFormat.levelName(detail.level)
        if let track = detail.trackNo {
            line += String(format: "  ·  TRACK %02d", track)
        }
        return line
    }

    private var achievementBlock: some View {
        HStack(alignment: .bottom) {
            VStack(alignment: .leading, spacing: 2) {
                Text(ScoreFormat.achievementPercent(detail.achievement))
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .foregroundStyle(AquaTheme.primary)
                Text("Достижение")
                    .font(.caption2)
                    .foregroundStyle(AquaTheme.mutedText)
            }
            Spacer()
            Text(ScoreFormat.maiRank(detail.achievement))
                .font(.title2.bold())
                .foregroundStyle(AquaTheme.text)
        }
        .padding(.top, 16)
    }

    @ViewBuilder
    private var badgesBlock: some View {
        let badges = [
            ScoreFormat.comboLabel(detail.comboStatus),
            ScoreFormat.syncLabel(detail.syncStatus),
            detail.isClear == true ? "CLEAR" : nil
        ].compactMap { $0 }

        if !badges.isEmpty {
            HStack(spacing: 6) {
                ForEach(badges, id: \.self) { DetailBadge(text: $0) }
            }
            .padding(.top, 10)
        }
    }

    static let critColor = Color(red: 1.0, green: 0.82, blue: 0.40)
    static let perfectColor = Color(red: 1.0, green: 0.66, blue: 0.30)
    static let greatColor = Color(red: 0.39, green: 0.90, blue: 0.74)
    static let goodColor = Color(red: 0.45, green: 0.75, blue: 0.99)
    static let missColor = Color(red: 1.0, green: 0.42, blue: 0.42)
}

private struct DetailSectionLabel: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text.uppercased())
            .font(.caption2.bold())
            .foregroundStyle(AquaTheme.mutedText)
            .padding(.top, 14)
            .padding(.bottom, 6)
    }
}

private struct StatNumCell: View {
    let label: String
    let value: Int
    let color: Color?

    init(_ label: String, _ value: Int, color: Color? = nil) {
        self.label = label
        self.value = value
        self.color = color
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(value)")
                .font(.subheadline.monospacedDigit().bold())
                .foregroundStyle(color ?? AquaTheme.text)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(label)
                .font(.system(size: 9, weight: .semibold))
                .foregroundStyle(AquaTheme.mutedText)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct DetailRow: View {
    let label: String
    let value: String?

    var body: some View {
        if let value, !value.isEmpty {
            HStack {
                Text(label)
                    .font(.footnote)
                    .foregroundStyle(AquaTheme.mutedText)
                Spacer()
                Text(value)
                    .font(.footnote.bold())
                    .foregroundStyle(AquaTheme.text)
            }
            .padding(.vertical, 4)
        }
    }
}

private struct DetailBadge: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2.bold())
            .foregroundStyle(AquaTheme.primary)
            .padding(.horizontal, 7)
            .padding(.vertical, 3)
            .background(AquaTheme.primary.opacity(0.16))
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

private struct DetailJacketView: View {
    let baseURL: String
    let musicId: Int

    var body: some View {
        AsyncImage(url: AquaAssets.jacketURL(baseURL: baseURL, musicId: musicId)) { phase in
            switch phase {
            case .success(let image):
                image.resizable().scaledToFill()
            default:
                Image(systemName: "music.note")
                    .font(.title)
                    .foregroundStyle(AquaTheme.primary)
            }
        }
        .frame(width: 64, height: 64)
        .background(AquaTheme.elevated)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private func detailDifficultyColor(_ level: Int) -> Color {
    switch level {
    case 0: return .green
    case 1: return .yellow
    case 2: return .red
    case 3: return .purple
    case 4: return .white
    default: return AquaTheme.mutedText
    }
}

private func comboLabel(_ value: Int?, _ max: Int?) -> String? {
    guard let value else { return nil }
    if let max, max > 0 { return "\(value) / \(max)" }
    return "\(value)"
}

private func fastLateLabel(_ fast: Int?, _ late: Int?) -> String? {
    guard fast != nil || late != nil else { return nil }
    return "\(fast ?? 0) / \(late ?? 0)"
}

private func ratingDeltaLabel(_ before: Int?, _ after: Int?) -> String? {
    guard before != nil || after != nil else { return nil }
    let b = before.map(String.init) ?? "?"
    let a = after.map(String.init) ?? "?"
    var delta = ""
    if let before, let after {
        let d = after - before
        delta = " (\(d >= 0 ? "+" : "")\(d))"
    }
    return "\(b) → \(a)\(delta)"
}

/// "2026-05-29T20:11:00" / "2026-05-29 20:11:00.0" -> "2026-05-29 20:11".
private func prettyDateTime(_ raw: String) -> String {
    let normalized = raw.replacingOccurrences(of: "T", with: " ")
    let trimmed = normalized.split(separator: ".").first.map(String.init) ?? normalized
    return trimmed.count >= 16 ? String(trimmed.prefix(16)) : trimmed
}

private extension String {
    var nonBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : trimmed
    }
}
