import Foundation
import WatchConnectivity
import WatchKit

/// Lado Apple Watch de la conexión: recibe el resumen del iPhone y le envía órdenes.
///
/// El reloj no tiene cámara ni modelo: todo lo reconoce el iPhone. Aquí solo se muestra la frase y
/// se vibra cuando se confirma una seña nueva.
@MainActor
final class ConexionTelefono: NSObject, ObservableObject, WCSessionDelegate {
    @Published private(set) var estado: MensajeReloj.Estado = .preparando
    @Published private(set) var frase = ""
    @Published private(set) var ultimaSena: String?
    @Published private(set) var numeroSenas = 0
    @Published private(set) var incierta = false
    @Published private(set) var pausado = false
    @Published private(set) var conectado = false
    /// Aún no ha llegado nada del iPhone desde que se abrió la app.
    @Published private(set) var sinDatos = true

    func activar() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func enviar(_ orden: MensajeReloj.Orden) {
        let sesion = WCSession.default
        guard sesion.activationState == .activated, sesion.isReachable else { return }
        sesion.sendMessage([MensajeReloj.Clave.orden: orden.rawValue], replyHandler: nil, errorHandler: nil)
        // Se refleja ya en pantalla; el iPhone lo confirmará en el siguiente resumen.
        if orden == .pausar { pausado = true }
        if orden == .iniciar { pausado = false }
    }

    private func aplicar(_ mensaje: [String: Any]) {
        guard let texto = mensaje[MensajeReloj.Clave.estado] as? String,
              let nuevoEstado = MensajeReloj.Estado(rawValue: texto)
        else { return }
        let nuevas = Int(mensaje[MensajeReloj.Clave.numeroSenas] as? String ?? "") ?? 0

        // Vibración corta por cada seña confirmada y otra distinta cuando la frase queda lista.
        if !sinDatos {
            if nuevas > numeroSenas { WKInterfaceDevice.current().play(.click) }
            if nuevoEstado == .fraseLista && estado != .fraseLista { WKInterfaceDevice.current().play(.success) }
        }
        estado = nuevoEstado
        frase = mensaje[MensajeReloj.Clave.frase] as? String ?? ""
        ultimaSena = mensaje[MensajeReloj.Clave.ultimaSena] as? String
        numeroSenas = nuevas
        incierta = (mensaje[MensajeReloj.Clave.incierta] as? String) == "true"
        pausado = (mensaje[MensajeReloj.Clave.pausado] as? String) == "true"
        sinDatos = false
    }

    // MARK: WCSessionDelegate (llegan en una cola de fondo)

    nonisolated func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        let contexto = session.receivedApplicationContext
        let alcanzable = session.isReachable
        Task { @MainActor in
            self.conectado = alcanzable
            if !contexto.isEmpty { self.aplicar(contexto) }
        }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        let alcanzable = session.isReachable
        Task { @MainActor in self.conectado = alcanzable }
    }

    nonisolated func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        Task { @MainActor in self.aplicar(message) }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        Task { @MainActor in self.aplicar(applicationContext) }
    }
}
