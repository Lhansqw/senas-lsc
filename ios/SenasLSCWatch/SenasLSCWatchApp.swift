import SwiftUI

@main
struct SenasLSCWatchApp: App {
    @StateObject private var conexion = ConexionTelefono()

    var body: some Scene {
        WindowGroup {
            PantallaReloj()
                .environmentObject(conexion)
                .onAppear { conexion.activar() }
        }
    }
}
