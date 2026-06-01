import Foundation

struct AquaProfileClient {
    private let session: URLSession
    private let decoder: JSONDecoder

    init(session: URLSession = .shared) {
        self.session = session
        self.decoder = JSONDecoder()
    }

    func summary(baseURL: String, username: String) async throws -> GameSummary {
        try await get(baseURL: baseURL, path: "api/v2/game/mai2/user-summary", username: username)
    }

    func detail(baseURL: String, username: String) async throws -> UserDetailDto {
        try await get(baseURL: baseURL, path: "api/v2/game/mai2/user-detail", username: username)
    }

    func rating(baseURL: String, username: String) async throws -> UserRatingDto {
        try await get(baseURL: baseURL, path: "api/v2/game/mai2/user-rating", username: username)
    }

    func recent(baseURL: String, username: String) async throws -> [RecentPlayDto] {
        try await get(baseURL: baseURL, path: "api/v2/game/mai2/recent", username: username)
    }

    func trend(baseURL: String, username: String) async throws -> [TrendPoint] {
        try await get(baseURL: baseURL, path: "api/v2/game/mai2/trend", username: username)
    }

    func allMusic(baseURL: String) async throws -> [Int: MusicMeta] {
        guard let url = URL(string: AquaAssets.allMusicURL(baseURL)) else {
            throw AquaProfileError.invalidURL
        }
        let raw: [String: MusicMeta] = try await fetch(url)
        return Dictionary(uniqueKeysWithValues: raw.compactMap { key, value in
            guard let id = Int(key) else { return nil }
            return (id, value)
        })
    }

    private func get<T: Decodable>(baseURL: String, path: String, username: String) async throws -> T {
        let cleanBase = baseURL.hasSuffix("/") ? baseURL : "\(baseURL)/"
        guard let base = URL(string: cleanBase),
              let url = URL(string: path, relativeTo: base),
              var components = URLComponents(url: url, resolvingAgainstBaseURL: true) else {
            throw AquaProfileError.invalidURL
        }
        components.queryItems = [URLQueryItem(name: "username", value: username)]
        guard let finalURL = components.url else {
            throw AquaProfileError.invalidURL
        }
        return try await fetch(finalURL)
    }

    private func fetch<T: Decodable>(_ url: URL) async throws -> T {
        let (data, response) = try await session.data(from: url)
        if let http = response as? HTTPURLResponse, !(200...299).contains(http.statusCode) {
            throw AquaProfileError.httpStatus(http.statusCode)
        }
        return try decoder.decode(T.self, from: data)
    }
}

enum AquaProfileError: LocalizedError {
    case invalidURL
    case httpStatus(Int)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid AquaDX URL"
        case .httpStatus(let status):
            return "AquaDX returned HTTP \(status)"
        }
    }
}
