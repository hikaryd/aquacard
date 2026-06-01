import Foundation

struct AquaUser: Codable, Equatable {
    var username: String
    var displayName: String?
    var country: String?
    var regTime: Int64?
    var profileBio: String?
    var profilePicture: String?
}

struct GameSummary: Codable, Equatable {
    var name: String?
    var aquaUser: AquaUser?
    var serverRank: Int?
    var rating: Int?
    var ratingHighest: Int?
    var accuracy: Double?
    var plays: Int?
    var maxCombo: Int?
    var fullCombo: Int?
    var allPerfect: Int?
    var totalScore: Int64?
    var lastVersion: String?
    var joined: String?
    var lastSeen: String?
    var ranks: [RankCount]?
}

struct RankCount: Codable, Equatable, Identifiable {
    var id: String { "\(name ?? "rank")-\(count ?? 0)" }
    var name: String?
    var count: Int?
}

struct UserDetailDto: Codable, Equatable {
    var playerRating: Int?
    var classRank: Int?
    var courseRank: Int?
    var level: Int?
}

struct UserRatingDto: Codable, Equatable {
    var best35: [[String]]?
    var best15: [[String]]?
    var best30: [[String]]?
    var recent10: [[String]]?
    var musicList: [MusicScoreDto]?
}

struct MusicScoreDto: Codable, Equatable {
    var musicId: Int
    var level: Int?
    var playCount: Int?
    var achievement: Int?
    var deluxscoreMax: Int?
    var scoreMax: Int?
    var scoreRank: Int?
    var comboStatus: Int?
    var syncStatus: Int?
    var isFullCombo: Bool?
    var isAllJustice: Bool?

    private enum CodingKeys: String, CodingKey {
        case musicId
        case level
        case playCount
        case achievement
        case deluxscoreMax
        case scoreMax
        case scoreRank
        case comboStatus
        case syncStatus
        case isFullCombo
        case isAllJustice
    }

    init(
        musicId: Int = 0,
        level: Int? = nil,
        playCount: Int? = nil,
        achievement: Int? = nil,
        deluxscoreMax: Int? = nil,
        scoreMax: Int? = nil,
        scoreRank: Int? = nil,
        comboStatus: Int? = nil,
        syncStatus: Int? = nil,
        isFullCombo: Bool? = nil,
        isAllJustice: Bool? = nil
    ) {
        self.musicId = musicId
        self.level = level
        self.playCount = playCount
        self.achievement = achievement
        self.deluxscoreMax = deluxscoreMax
        self.scoreMax = scoreMax
        self.scoreRank = scoreRank
        self.comboStatus = comboStatus
        self.syncStatus = syncStatus
        self.isFullCombo = isFullCombo
        self.isAllJustice = isAllJustice
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        musicId = try container.decodeIfPresent(Int.self, forKey: .musicId) ?? 0
        level = try container.decodeIfPresent(Int.self, forKey: .level)
        playCount = try container.decodeIfPresent(Int.self, forKey: .playCount)
        achievement = try container.decodeIfPresent(Int.self, forKey: .achievement)
        deluxscoreMax = try container.decodeIfPresent(Int.self, forKey: .deluxscoreMax)
        scoreMax = try container.decodeIfPresent(Int.self, forKey: .scoreMax)
        scoreRank = try container.decodeIfPresent(Int.self, forKey: .scoreRank)
        comboStatus = try container.decodeIfPresent(Int.self, forKey: .comboStatus)
        syncStatus = try container.decodeIfPresent(Int.self, forKey: .syncStatus)
        isFullCombo = try container.decodeIfPresent(Bool.self, forKey: .isFullCombo)
        isAllJustice = try container.decodeIfPresent(Bool.self, forKey: .isAllJustice)
    }
}

struct RecentPlayDto: Codable, Equatable {
    var musicId: Int
    var level: Int?
    var trackNo: Int?
    var userPlayDate: String?
    var playDate: String?
    var achievement: Int?
    var deluxscore: Int?
    var scoreRank: Int?
    var comboStatus: Int?
    var syncStatus: Int?
    var maxCombo: Int?
    var totalCombo: Int?
    var fastCount: Int?
    var lateCount: Int?
    var isClear: Bool?
    var isFullCombo: Bool?
    var isAllPerfect: Bool?
    var beforeRating: Int?
    var afterRating: Int?
    var placeName: String?
    var tapCriticalPerfect: Int?
    var tapPerfect: Int?
    var tapGreat: Int?
    var tapGood: Int?
    var tapMiss: Int?
    var holdCriticalPerfect: Int?
    var holdPerfect: Int?
    var holdGreat: Int?
    var holdGood: Int?
    var holdMiss: Int?
    var slideCriticalPerfect: Int?
    var slidePerfect: Int?
    var slideGreat: Int?
    var slideGood: Int?
    var slideMiss: Int?
    var touchCriticalPerfect: Int?
    var touchPerfect: Int?
    var touchGreat: Int?
    var touchGood: Int?
    var touchMiss: Int?
    var breakCriticalPerfect: Int?
    var breakPerfect: Int?
    var breakGreat: Int?
    var breakGood: Int?
    var breakMiss: Int?

    private enum CodingKeys: String, CodingKey {
        case musicId
        case level
        case trackNo
        case userPlayDate
        case playDate
        case achievement
        case deluxscore
        case scoreRank
        case comboStatus
        case syncStatus
        case maxCombo
        case totalCombo
        case fastCount
        case lateCount
        case isClear
        case isFullCombo
        case isAllPerfect
        case beforeRating
        case afterRating
        case placeName
        case tapCriticalPerfect
        case tapPerfect
        case tapGreat
        case tapGood
        case tapMiss
        case holdCriticalPerfect
        case holdPerfect
        case holdGreat
        case holdGood
        case holdMiss
        case slideCriticalPerfect
        case slidePerfect
        case slideGreat
        case slideGood
        case slideMiss
        case touchCriticalPerfect
        case touchPerfect
        case touchGreat
        case touchGood
        case touchMiss
        case breakCriticalPerfect
        case breakPerfect
        case breakGreat
        case breakGood
        case breakMiss
    }

    init(
        musicId: Int = 0,
        level: Int? = nil,
        trackNo: Int? = nil,
        userPlayDate: String? = nil,
        playDate: String? = nil,
        achievement: Int? = nil,
        deluxscore: Int? = nil,
        scoreRank: Int? = nil,
        comboStatus: Int? = nil,
        syncStatus: Int? = nil,
        maxCombo: Int? = nil,
        totalCombo: Int? = nil,
        fastCount: Int? = nil,
        lateCount: Int? = nil,
        isClear: Bool? = nil,
        isFullCombo: Bool? = nil,
        isAllPerfect: Bool? = nil,
        beforeRating: Int? = nil,
        afterRating: Int? = nil,
        placeName: String? = nil,
        tapCriticalPerfect: Int? = nil,
        tapPerfect: Int? = nil,
        tapGreat: Int? = nil,
        tapGood: Int? = nil,
        tapMiss: Int? = nil,
        holdCriticalPerfect: Int? = nil,
        holdPerfect: Int? = nil,
        holdGreat: Int? = nil,
        holdGood: Int? = nil,
        holdMiss: Int? = nil,
        slideCriticalPerfect: Int? = nil,
        slidePerfect: Int? = nil,
        slideGreat: Int? = nil,
        slideGood: Int? = nil,
        slideMiss: Int? = nil,
        touchCriticalPerfect: Int? = nil,
        touchPerfect: Int? = nil,
        touchGreat: Int? = nil,
        touchGood: Int? = nil,
        touchMiss: Int? = nil,
        breakCriticalPerfect: Int? = nil,
        breakPerfect: Int? = nil,
        breakGreat: Int? = nil,
        breakGood: Int? = nil,
        breakMiss: Int? = nil
    ) {
        self.musicId = musicId
        self.level = level
        self.trackNo = trackNo
        self.userPlayDate = userPlayDate
        self.playDate = playDate
        self.achievement = achievement
        self.deluxscore = deluxscore
        self.scoreRank = scoreRank
        self.comboStatus = comboStatus
        self.syncStatus = syncStatus
        self.maxCombo = maxCombo
        self.totalCombo = totalCombo
        self.fastCount = fastCount
        self.lateCount = lateCount
        self.isClear = isClear
        self.isFullCombo = isFullCombo
        self.isAllPerfect = isAllPerfect
        self.beforeRating = beforeRating
        self.afterRating = afterRating
        self.placeName = placeName
        self.tapCriticalPerfect = tapCriticalPerfect
        self.tapPerfect = tapPerfect
        self.tapGreat = tapGreat
        self.tapGood = tapGood
        self.tapMiss = tapMiss
        self.holdCriticalPerfect = holdCriticalPerfect
        self.holdPerfect = holdPerfect
        self.holdGreat = holdGreat
        self.holdGood = holdGood
        self.holdMiss = holdMiss
        self.slideCriticalPerfect = slideCriticalPerfect
        self.slidePerfect = slidePerfect
        self.slideGreat = slideGreat
        self.slideGood = slideGood
        self.slideMiss = slideMiss
        self.touchCriticalPerfect = touchCriticalPerfect
        self.touchPerfect = touchPerfect
        self.touchGreat = touchGreat
        self.touchGood = touchGood
        self.touchMiss = touchMiss
        self.breakCriticalPerfect = breakCriticalPerfect
        self.breakPerfect = breakPerfect
        self.breakGreat = breakGreat
        self.breakGood = breakGood
        self.breakMiss = breakMiss
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        musicId = try container.decodeIfPresent(Int.self, forKey: .musicId) ?? 0
        level = try container.decodeIfPresent(Int.self, forKey: .level)
        trackNo = try container.decodeIfPresent(Int.self, forKey: .trackNo)
        userPlayDate = try container.decodeIfPresent(String.self, forKey: .userPlayDate)
        playDate = try container.decodeIfPresent(String.self, forKey: .playDate)
        achievement = try container.decodeIfPresent(Int.self, forKey: .achievement)
        deluxscore = try container.decodeIfPresent(Int.self, forKey: .deluxscore)
        scoreRank = try container.decodeIfPresent(Int.self, forKey: .scoreRank)
        comboStatus = try container.decodeIfPresent(Int.self, forKey: .comboStatus)
        syncStatus = try container.decodeIfPresent(Int.self, forKey: .syncStatus)
        maxCombo = try container.decodeIfPresent(Int.self, forKey: .maxCombo)
        totalCombo = try container.decodeIfPresent(Int.self, forKey: .totalCombo)
        fastCount = try container.decodeIfPresent(Int.self, forKey: .fastCount)
        lateCount = try container.decodeIfPresent(Int.self, forKey: .lateCount)
        isClear = try container.decodeIfPresent(Bool.self, forKey: .isClear)
        isFullCombo = try container.decodeIfPresent(Bool.self, forKey: .isFullCombo)
        isAllPerfect = try container.decodeIfPresent(Bool.self, forKey: .isAllPerfect)
        beforeRating = try container.decodeIfPresent(Int.self, forKey: .beforeRating)
        afterRating = try container.decodeIfPresent(Int.self, forKey: .afterRating)
        placeName = try container.decodeIfPresent(String.self, forKey: .placeName)
        tapCriticalPerfect = try container.decodeIfPresent(Int.self, forKey: .tapCriticalPerfect)
        tapPerfect = try container.decodeIfPresent(Int.self, forKey: .tapPerfect)
        tapGreat = try container.decodeIfPresent(Int.self, forKey: .tapGreat)
        tapGood = try container.decodeIfPresent(Int.self, forKey: .tapGood)
        tapMiss = try container.decodeIfPresent(Int.self, forKey: .tapMiss)
        holdCriticalPerfect = try container.decodeIfPresent(Int.self, forKey: .holdCriticalPerfect)
        holdPerfect = try container.decodeIfPresent(Int.self, forKey: .holdPerfect)
        holdGreat = try container.decodeIfPresent(Int.self, forKey: .holdGreat)
        holdGood = try container.decodeIfPresent(Int.self, forKey: .holdGood)
        holdMiss = try container.decodeIfPresent(Int.self, forKey: .holdMiss)
        slideCriticalPerfect = try container.decodeIfPresent(Int.self, forKey: .slideCriticalPerfect)
        slidePerfect = try container.decodeIfPresent(Int.self, forKey: .slidePerfect)
        slideGreat = try container.decodeIfPresent(Int.self, forKey: .slideGreat)
        slideGood = try container.decodeIfPresent(Int.self, forKey: .slideGood)
        slideMiss = try container.decodeIfPresent(Int.self, forKey: .slideMiss)
        touchCriticalPerfect = try container.decodeIfPresent(Int.self, forKey: .touchCriticalPerfect)
        touchPerfect = try container.decodeIfPresent(Int.self, forKey: .touchPerfect)
        touchGreat = try container.decodeIfPresent(Int.self, forKey: .touchGreat)
        touchGood = try container.decodeIfPresent(Int.self, forKey: .touchGood)
        touchMiss = try container.decodeIfPresent(Int.self, forKey: .touchMiss)
        breakCriticalPerfect = try container.decodeIfPresent(Int.self, forKey: .breakCriticalPerfect)
        breakPerfect = try container.decodeIfPresent(Int.self, forKey: .breakPerfect)
        breakGreat = try container.decodeIfPresent(Int.self, forKey: .breakGreat)
        breakGood = try container.decodeIfPresent(Int.self, forKey: .breakGood)
        breakMiss = try container.decodeIfPresent(Int.self, forKey: .breakMiss)
    }
}

struct TrendPoint: Codable, Equatable, Identifiable {
    var id: String { "\(date)-\(rating ?? 0)-\(plays ?? 0)" }
    var date: String
    var rating: Int?
    var plays: Int?

    init(date: String = "", rating: Int? = nil, plays: Int? = nil) {
        self.date = date
        self.rating = rating
        self.plays = plays
    }

    private enum CodingKeys: String, CodingKey {
        case date
        case rating
        case plays
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        date = try container.decodeIfPresent(String.self, forKey: .date) ?? ""
        rating = try container.decodeIfPresent(Int.self, forKey: .rating)
        plays = try container.decodeIfPresent(Int.self, forKey: .plays)
    }
}

struct MusicMeta: Codable, Equatable {
    var name: String?
    var genre: String?
    var notes: [NoteLv]?
}

struct NoteLv: Codable, Equatable {
    var lv: Double?
}

struct ProfileScore: Codable, Equatable, Identifiable {
    var id: String { "\(musicId)-\(level)" }
    var musicId: Int
    var level: Int
    var achievement: Int?
    var deluxscore: Int?
    var comboStatus: Int?
    var syncStatus: Int?
    var scoreRank: Int?
}

struct JudgeBreakdown: Codable, Equatable {
    var crit: Int = 0
    var perfect: Int = 0
    var great: Int = 0
    var good: Int = 0
    var miss: Int = 0
}

struct NoteBreakdown: Codable, Equatable {
    var tap: Int = 0
    var hold: Int = 0
    var slide: Int = 0
    var touch: Int = 0
    var brk: Int = 0
}

struct RecentEntry: Codable, Equatable, Identifiable {
    var id: String { "\(musicId)-\(level)-\(playDate ?? "")-\(achievement ?? 0)" }
    var musicId: Int
    var level: Int
    var playDate: String?
    var achievement: Int?
    var rank: Int?
    var comboStatus: Int?
    var isClear: Bool?
    var syncStatus: Int?
    var deluxscore: Int?
    var beforeRating: Int?
    var afterRating: Int?
    var placeName: String?
    var maxCombo: Int?
    var totalCombo: Int?
    var fastCount: Int?
    var lateCount: Int?
    var isFullCombo: Bool?
    var isAllPerfect: Bool?
    var trackNo: Int?
    var judges: JudgeBreakdown?
    var notes: NoteBreakdown?
}

struct BestEntry: Codable, Equatable, Identifiable {
    var id: String { "\(musicId)-\(level)-\(value)" }
    var musicId: Int
    var level: Int
    var value: Int
}

struct ProfileBundle: Codable, Equatable {
    var summary: GameSummary?
    var detail: UserDetailDto?
    var best: [BestEntry] = []
    var bestSecondary: [BestEntry] = []
    var scores: [ProfileScore] = []
    var recent: [RecentEntry] = []
    var trend: [TrendPoint] = []
    var meta: [Int: MusicMeta] = [:]
    var errors: [String] = []
}

struct CachedProfile: Equatable {
    var bundle: ProfileBundle
    var savedAtMillis: Int64
}
