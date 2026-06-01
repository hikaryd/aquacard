import SwiftUI

@main
struct AquaCardApp: App {
    @StateObject private var model = ProfileViewModel(
        repository: AquaProfileRepository(
            client: AquaProfileClient(),
            cache: ProfileCacheStore()
        ),
        settings: AppSettings()
    )

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(model)
        }
    }
}
