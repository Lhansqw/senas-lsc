import AVFoundation
import Foundation
import SenasShared

/// Estado de la pantalla de cámara. Une la cámara, MediaPipe, el modelo de señas y el reloj.
///
/// Todo el reconocimiento corre en la cola serie de la cámara (`CamaraFrontal.cola`), y las órdenes
/// del usuario o del reloj también se encolan ahí: igual que en Android, nunca hay dos accesos a la
/// vez al procesador compartido. La interfaz se actualiza en la cola principal.
@MainActor
final class ModeloReconocimiento: ObservableObject {
    @Published private(set) var estado: EstadoReconocimiento
    @Published private(set) var pausado = false
    @Published private(set) var aviso: String?

    let camara = CamaraFrontal()
    private let reloj = ConexionReloj()

    // Solo se usan desde camara.cola.
    private nonisolated(unsafe) let procesador: ProcesadorCompartido
    private nonisolated(unsafe) var extractor: ExtractorLandmarksIOS?
    private nonisolated(unsafe) var analizando = true

    init() {
        var aviso: String?
        let inferencia: InferenciaTFLite?
        do {
            inferencia = try InferenciaTFLite.cargar()
        } catch {
            inferencia = nil
            aviso = "No se pudo cargar el modelo de señas: \(error.localizedDescription)"
        }
        let json = Bundle.main.url(forResource: "modelo", withExtension: "json").flatMap { try? String(contentsOf: $0, encoding: .utf8) }
        procesador = ProcesadorCompartido.Companion.shared.crear(modeloJson: json, inferencia: inferencia)
        estado = procesador.estado
        do {
            extractor = try ExtractorLandmarksIOS()
        } catch {
            aviso = "No se pudo preparar MediaPipe: \(error.localizedDescription)"
        }
        self.aviso = aviso

        reloj.alRecibirOrden = { [weak self] orden in self?.atender(orden) }
        reloj.activar()
        publicar(procesador.estado)
    }

    var resumen: ResumenReloj { ResumenReloj.Companion.shared.de(estado: estado) }

    // MARK: Cámara

    func iniciarCamara() async {
        guard await CamaraFrontal.pedirPermiso() else {
            aviso = "Sin permiso para usar la cámara. Actívalo en Ajustes › Señas LSC."
            return
        }
        do {
            try camara.configurar()
        } catch {
            aviso = "Este iPhone no tiene una cámara frontal disponible."
            return
        }
        camara.alRecibirFotograma = { [weak self] muestra, ms in self?.procesar(muestra, ms) }
        camara.iniciar()
    }

    func detenerCamara() { camara.detener() }

    /// En la cola de la cámara.
    private nonisolated func procesar(_ muestra: CMSampleBuffer, _ ms: Int64) {
        guard analizando, let extractor else { return }
        do {
            guard let frame = try extractor.extraer(muestra, timestampMs: ms) else { return }
            let nuevo = try procesador.agregarFotograma(frame: frame)
            Task { @MainActor in self.publicar(nuevo) }
        } catch {
            let mensaje = error.localizedDescription
            Task { @MainActor in self.aviso = mensaje }
        }
    }

    // MARK: Órdenes (botones y reloj)

    func pausarOReanudar() { establecerPausa(!pausado) }

    func interpretar() { orden { try $0.interpretar() } }
    func eliminarUltima() { orden { $0.eliminarUltima() } }
    func limpiar() { orden { $0.limpiar() } }
    func corregir(_ texto: String) { orden { p in _ = p.corregir(texto: texto); return p.estado } }

    private func establecerPausa(_ valor: Bool) {
        pausado = valor
        camara.cola.async { self.analizando = !valor }
        enviarAlReloj()
    }

    private func atender(_ orden: MensajeReloj.Orden) {
        switch orden {
        case .iniciar: establecerPausa(false)
        case .pausar: establecerPausa(true)
        case .borrar: limpiar()
        case .interpretar: interpretar()
        }
    }

    private func orden(_ accion: @escaping (ProcesadorCompartido) throws -> EstadoReconocimiento) {
        camara.cola.async {
            do {
                let nuevo = try accion(self.procesador)
                Task { @MainActor in self.publicar(nuevo) }
            } catch {
                let mensaje = error.localizedDescription
                Task { @MainActor in self.aviso = mensaje }
            }
        }
    }

    private func publicar(_ nuevo: EstadoReconocimiento) {
        estado = nuevo
        enviarAlReloj()
    }

    private func enviarAlReloj() {
        var mensaje = resumen.aMapa()
        mensaje[MensajeReloj.Clave.pausado] = pausado ? "true" : "false"
        reloj.enviar(mensaje)
    }
}
