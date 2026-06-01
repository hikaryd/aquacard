import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var model: ProfileViewModel

    var body: some View {
        TabView {
            ProfileScreen()
                .tabItem {
                    Label("Профиль", systemImage: "person.crop.circle")
                }
                .accessibilityIdentifier("tab.profile")

            SettingsScreen()
                .tabItem {
                    Label("Настройки", systemImage: "gearshape")
                }
                .accessibilityIdentifier("tab.settings")
        }
        .tint(AquaTheme.primary)
        .preferredColorScheme(.dark)
        .onAppear {
            model.loadOnAppear()
        }
    }
}

struct ProfileScreen: View {
    @EnvironmentObject private var model: ProfileViewModel

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    UsernamePanel()

                    if model.isRefreshing {
                        ProgressView("Обновляю профиль")
                            .progressViewStyle(.circular)
                            .frame(maxWidth: .infinity)
                            .tint(AquaTheme.primary)
                            .foregroundStyle(AquaTheme.mutedText)
                    }

                    if let message = model.message {
                        MessageBanner(message: message)
                    }

                    if let bundle = model.bundle {
                        ProfileContent(bundle: bundle, baseURL: model.baseURL)
                    } else {
                        EmptyProfileState()
                    }
                }
                .padding(16)
            }
            .background(AquaTheme.background.ignoresSafeArea())
            .navigationTitle("AquaDX")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        model.refresh()
                    } label: {
                        Label("Обновить", systemImage: "arrow.clockwise")
                    }
                    .accessibilityIdentifier("profile.refresh")
                    .disabled(model.isRefreshing)
                }
            }
            .refreshable {
                await model.refreshForPull()
            }
        }
    }
}

private struct UsernamePanel: View {
    @EnvironmentObject private var model: ProfileViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Профиль maimai")
                .font(.headline)
                .foregroundStyle(AquaTheme.text)

            HStack(spacing: 10) {
                TextField("username", text: $model.username)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .padding(12)
                    .background(AquaTheme.elevated)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .accessibilityIdentifier("profile.username")

                Button("Загрузить") {
                    model.refresh()
                }
                .buttonStyle(.borderedProminent)
                .tint(AquaTheme.primary)
                .accessibilityIdentifier("profile.load")
                .disabled(model.isRefreshing)
            }

            Text("Введите ник AquaDX. При повторном запуске приложение откроет последний загруженный профиль.")
                .font(.caption)
                .foregroundStyle(AquaTheme.mutedText)
        }
        .profilePanel()
    }
}

private struct ProfileContent: View {
    let bundle: ProfileBundle
    let baseURL: String

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let summary = bundle.summary {
                ProfileHeaderView(summary: summary, detail: bundle.detail, baseURL: baseURL)
                    .accessibilityIdentifier("profile.summary")
            }

            if !bundle.best.isEmpty {
                SectionTitle("Best 35", count: bundle.best.count)
                LazyVStack(spacing: 8) {
                    ForEach(Array(bundle.best.enumerated()), id: \.element.id) { index, entry in
                        BestRowView(index: index, entry: entry, meta: bundle.meta, baseURL: baseURL)
                    }
                }
                .accessibilityIdentifier("profile.best")
            }

            if !bundle.bestSecondary.isEmpty {
                SectionTitle("Best 15", count: bundle.bestSecondary.count)
                LazyVStack(spacing: 8) {
                    ForEach(Array(bundle.bestSecondary.prefix(15).enumerated()), id: \.element.id) { index, entry in
                        BestRowView(index: index, entry: entry, meta: bundle.meta, baseURL: baseURL)
                    }
                }
            }

            if !bundle.recent.isEmpty {
                SectionTitle("Недавние игры", count: bundle.recent.count)
                LazyVStack(spacing: 8) {
                    ForEach(bundle.recent) { entry in
                        RecentRowView(entry: entry, meta: bundle.meta, baseURL: baseURL)
                    }
                }
                .accessibilityIdentifier("profile.recent")
            }

            if !bundle.trend.isEmpty {
                SectionTitle("Динамика рейтинга", count: bundle.trend.count)
                TrendView(points: bundle.trend)
                    .accessibilityIdentifier("profile.trend")
            }

            if let ranks = bundle.summary?.ranks, !ranks.isEmpty {
                SectionTitle("Ранги", count: ranks.count)
                RankDistributionView(ranks: ranks)
                    .accessibilityIdentifier("profile.ranks")
            }

            if !bundle.errors.isEmpty {
                MessageBanner(message: .warning("Часть секций не загрузилась: \(bundle.errors.joined(separator: "; "))"))
            }
        }
    }
}

private struct SettingsScreen: View {
    @EnvironmentObject private var model: ProfileViewModel

    var body: some View {
        NavigationStack {
            Form {
                Section("AquaDX") {
                    TextField("Base URL", text: $model.baseURL)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .accessibilityIdentifier("settings.baseUrl")

                    Button("Сохранить URL") {
                        model.saveSettings()
                    }
                    .accessibilityIdentifier("settings.save")
                }

                Section("Кэш") {
                    Text("Профиль кэшируется отдельно для каждого base URL и username.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .scrollContentBackground(.hidden)
            .background(AquaTheme.background)
            .navigationTitle("Настройки")
        }
    }
}

private struct EmptyProfileState: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "person.text.rectangle")
                .font(.system(size: 48))
                .foregroundStyle(AquaTheme.primary)
            Text("Профиль ещё не загружен")
                .font(.headline)
                .foregroundStyle(AquaTheme.text)
            Text("Укажите username и нажмите «Загрузить».")
                .font(.subheadline)
                .foregroundStyle(AquaTheme.mutedText)
        }
        .frame(maxWidth: .infinity)
        .padding(28)
        .profilePanel()
    }
}

private struct MessageBanner: View {
    let message: ProfileMessage
    private var color: Color {
        switch message.kind {
        case .info: return AquaTheme.primary
        case .warning: return AquaTheme.warning
        case .error: return AquaTheme.danger
        }
    }

    var body: some View {
        Text(message.text)
            .font(.footnote)
            .foregroundStyle(AquaTheme.text)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color.opacity(0.18))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private extension View {
    func profilePanel() -> some View {
        padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AquaTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: AquaTheme.cardRadius, style: .continuous))
    }
}
