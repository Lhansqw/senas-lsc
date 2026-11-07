import SwiftUI

@main
struct SenasLSCApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationStack {
                PantallaInicio()
            }
            .tint(Paleta.tinta)
        }
    }
}

/// Colores de la app (los mismos tonos de papel y tinta que el rediseño de Android).
enum Paleta {
    static let papel = Color(red: 0.98, green: 0.96, blue: 0.91)
    static let tinta = Color(red: 0.13, green: 0.17, blue: 0.29)
    static let tenue = Color(red: 0.42, green: 0.42, blue: 0.45)
}
