import Foundation
import WatchConnectivity

/// Lado iPhone de la conexión con el Apple Watch.
///
/// Envía el resumen con `sendMessage` si el reloj tiene la app abierta (llega al momento) y siempre
/// con `updateApplicationContext`, que el sistema entrega en cuanto el reloj despierta y conserva
/// solo el último valor.
final class ConexionReloj: NSObject, WCSessionDelegate {
    /// Se llama en la cola principal con cada orden que llega del reloj.
    var alRecibirOrden: ((MensajeReloj.Orden) -> Void)?

    private var ultimoEnviado: [String: String] = [:]

    func activar() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    /// Envía el resumen solo si cambió desde el último envío. Llamar desde la cola principal.
    func enviar(_ mensaje: [String: String]) {
        guard WCSession.isSupported(), mensaje != ultimoEnviado else { return }
        let sesion = WCSession.default
        guard sesion.activationState == .activated, sesion.isPaired, sesion.isWatchAppInstalled else { return }
        ultimoEnviado = mensaje
        if sesion.isReachable {
            sesion.sendMessage(mensaje, replyHandler: nil, errorHandler: nil)
        }
        try? sesion.updateApplicationContext(mensaje)
    }

    // MARK: WCSessionDelegate

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        // Al reconectar se reenvía el último estado. ultimoEnviado solo se toca en la cola principal.
        DispatchQueue.main.async {
            let pendiente = self.ultimoEnviado
            self.ultimoEnviado = [:]
            if !pendiente.isEmpty { self.enviar(pendiente) }
        }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    /// Al cambiar de reloj emparejado hay que volver a activar la sesión.
    func sessionDidDeactivate(_ session: WCSession) { session.activate() }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        guard let texto = message[MensajeReloj.Clave.orden] as? String, let orden = MensajeReloj.Orden(rawValue: texto) else { return }
        DispatchQueue.main.async { self.alRecibirOrden?(orden) }
    }
}
