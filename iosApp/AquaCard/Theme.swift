import SwiftUI

enum AquaTheme {
    static let background = Color(red: 0.055, green: 0.059, blue: 0.067)
    static let surface = Color(red: 0.094, green: 0.106, blue: 0.133)
    static let elevated = Color(red: 0.133, green: 0.153, blue: 0.188)
    static let primary = Color(red: 0.486, green: 0.612, blue: 1.0)
    static let secondary = Color(red: 0.455, green: 0.839, blue: 0.839)
    static let warning = Color(red: 1.0, green: 0.737, blue: 0.357)
    static let danger = Color(red: 1.0, green: 0.408, blue: 0.408)
    static let text = Color.white
    static let mutedText = Color.white.opacity(0.68)

    static let cardRadius: CGFloat = 18
}
