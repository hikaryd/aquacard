import Foundation
import SwiftUI

@MainActor
final class ProfileViewModel: ObservableObject {
    @Published var username: String
    @Published var baseURL: String
    @Published private(set) var bundle: ProfileBundle?
    @Published private(set) var isRefreshing = false
    @Published var message: ProfileMessage?

    private let repository: AquaProfileRepository
    private let settings: AppSettings
    private var activeLoadID: UUID?
    private var refreshTask: Task<Void, Never>?

    init(repository: AquaProfileRepository, settings: AppSettings) {
        self.repository = repository
        self.settings = settings
        self.username = settings.lastUsername
        self.baseURL = settings.baseURL
    }

    func saveSettings() {
        settings.baseURL = baseURL.normalizedBaseURL
        baseURL = settings.baseURL
    }

    func loadOnAppear() {
        startLoad(manualRefresh: false)
    }

    func refresh() {
        startLoad(manualRefresh: true)
    }

    func refreshForPull() async {
        cancelActiveLoad()
        await load(manualRefresh: true)
    }

    private func startLoad(manualRefresh: Bool) {
        cancelActiveLoad()
        refreshTask = Task { [weak self] in
            await self?.load(manualRefresh: manualRefresh)
        }
    }

    private func cancelActiveLoad() {
        refreshTask?.cancel()
        refreshTask = nil
        activeLoadID = nil
        isRefreshing = false
    }

    func load(manualRefresh: Bool) async {
        let query = username.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else {
            cancelActiveLoad()
            message = .info("Введите username AquaDX")
            return
        }

        let normalizedBase = baseURL.normalizedBaseURL
        baseURL = normalizedBase
        settings.baseURL = normalizedBase

        let request = LoadRequest(id: UUID(), username: query, baseURL: normalizedBase)
        activeLoadID = request.id
        isRefreshing = true
        defer { finishLoadIfCurrent(request) }

        let cached = await repository.readCache(baseURL: normalizedBase, username: query)
        guard isCurrent(request), !Task.isCancelled else { return }
        switch CachePolicy.decide(query: query, cached: cached, now: Date(), manualRefresh: manualRefresh) {
        case .idle:
            return
        case .serveCachedOnly:
            bundle = cached?.bundle
            message = .info("Показан свежий кэш")
            return
        case .serveCachedThenRefresh:
            if let cached {
                bundle = cached.bundle
                message = .info("Показан кэш, обновляю профиль")
            }
            await refreshFromNetwork(request)
        case .refreshOnly:
            await refreshFromNetwork(request)
        }
    }

    private func refreshFromNetwork(_ request: LoadRequest) async {
        do {
            let loaded = try await repository.load(baseURL: request.baseURL, username: request.username)
            guard isCurrent(request) else { return }
            bundle = loaded
            settings.lastUsername = request.username
            await repository.writeCache(baseURL: request.baseURL, username: request.username, bundle: loaded)
            guard isCurrent(request) else { return }
            if loaded.errors.isEmpty {
                message = .info("Профиль обновлён")
            } else {
                message = .warning("Профиль обновлён частично: \(loaded.errors.joined(separator: "; "))")
            }
        } catch {
            if error is CancellationError { return }
            guard isCurrent(request) else { return }
            message = .error("Не удалось обновить профиль: \(error.localizedDescription)")
        }
    }

    private func isCurrent(_ request: LoadRequest) -> Bool {
        activeLoadID == request.id &&
        username.trimmingCharacters(in: .whitespacesAndNewlines) == request.username &&
        baseURL.normalizedBaseURL == request.baseURL
    }

    private func finishLoadIfCurrent(_ request: LoadRequest) {
        guard activeLoadID == request.id else { return }
        activeLoadID = nil
        isRefreshing = false
    }
}

private struct LoadRequest {
    let id: UUID
    let username: String
    let baseURL: String
}

struct ProfileMessage: Identifiable, Equatable {
    enum Kind {
        case info
        case warning
        case error
    }

    let id = UUID()
    let kind: Kind
    let text: String

    static func info(_ text: String) -> ProfileMessage { ProfileMessage(kind: .info, text: text) }
    static func warning(_ text: String) -> ProfileMessage { ProfileMessage(kind: .warning, text: text) }
    static func error(_ text: String) -> ProfileMessage { ProfileMessage(kind: .error, text: text) }
}

final class AppSettings {
    private enum Keys {
        static let baseURL = "base_url"
        static let lastUsername = "last_username"
    }

    private let defaults: UserDefaults
    static let defaultBaseURL = "https://aquadx.net/aqua"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var baseURL: String {
        get {
            defaults.string(forKey: Keys.baseURL)?.normalizedBaseURL ?? Self.defaultBaseURL
        }
        set {
            defaults.set(newValue.normalizedBaseURL, forKey: Keys.baseURL)
        }
    }

    var lastUsername: String {
        get {
            defaults.string(forKey: Keys.lastUsername) ?? ""
        }
        set {
            defaults.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines), forKey: Keys.lastUsername)
        }
    }
}

enum Decision {
    case idle
    case serveCachedOnly
    case serveCachedThenRefresh
    case refreshOnly
}

enum CachePolicy {
    static let defaultThreshold: TimeInterval = 60

    static func decide(query: String?, cached: CachedProfile?, now: Date, threshold: TimeInterval = defaultThreshold, manualRefresh: Bool) -> Decision {
        guard let query, !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return .idle
        }
        if manualRefresh {
            return cached == nil ? .refreshOnly : .serveCachedThenRefresh
        }
        guard let cached else { return .refreshOnly }
        let ageSeconds = now.timeIntervalSince1970 - Double(cached.savedAtMillis) / 1_000
        guard ageSeconds >= 0 else { return .serveCachedThenRefresh }
        return ageSeconds <= threshold ? .serveCachedOnly : .serveCachedThenRefresh
    }
}

extension String {
    var normalizedBaseURL: String {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return AppSettings.defaultBaseURL }
        return trimmed.hasSuffix("/") ? String(trimmed.dropLast()) : trimmed
    }
}
