import AVFoundation

/// Cámara frontal con AVFoundation. Entrega cada fotograma en vertical y **sin espejo**, igual que
/// CameraX en Android, para que MediaPipe y el modelo vean lo mismo en las dos plataformas.
///
/// Si el análisis va más lento que la cámara, AVFoundation descarta los fotogramas atrasados
/// (`alwaysDiscardsLateVideoFrames`), como `KEEP_ONLY_LATEST` en Android.
final class CamaraFrontal: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    let sesion = AVCaptureSession()

    /// Cola serie donde llegan los fotogramas y donde corre todo el reconocimiento.
    let cola = DispatchQueue(label: "senas-camara", qos: .userInitiated)

    /// Se llama en [cola] con cada fotograma y su marca de tiempo en milisegundos.
    /// Es `@Sendable` para que no herede el actor principal de quien la asigna.
    var alRecibirFotograma: (@Sendable (CMSampleBuffer, Int64) -> Void)?

    private var configurada = false

    enum ErrorCamara: Error { case sinCamaraFrontal, sinPermiso, noConfigurable }

    /// Pide permiso si hace falta. Devuelve `false` si el usuario lo negó.
    static func pedirPermiso() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return true
        case .notDetermined: return await AVCaptureDevice.requestAccess(for: .video)
        default: return false
        }
    }

    func configurar() throws {
        guard !configurada else { return }
        guard let camara = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front) else {
            throw ErrorCamara.sinCamaraFrontal
        }
        sesion.beginConfiguration()
        defer { sesion.commitConfiguration() }
        // 640×480 basta para MediaPipe y es lo que usa Android para el análisis.
        sesion.sessionPreset = .vga640x480

        let entrada = try AVCaptureDeviceInput(device: camara)
        guard sesion.canAddInput(entrada) else { throw ErrorCamara.noConfigurable }
        sesion.addInput(entrada)

        let salida = AVCaptureVideoDataOutput()
        salida.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        salida.alwaysDiscardsLateVideoFrames = true
        salida.setSampleBufferDelegate(self, queue: cola)
        guard sesion.canAddOutput(salida) else { throw ErrorCamara.noConfigurable }
        sesion.addOutput(salida)

        if let conexion = salida.connection(with: .video) {
            if conexion.isVideoRotationAngleSupported(90) { conexion.videoRotationAngle = 90 }
            if conexion.isVideoMirroringSupported {
                conexion.automaticallyAdjustsVideoMirroring = false
                conexion.isVideoMirrored = false
            }
        }

        // Unos 30 FPS como máximo, como el ritmo de análisis de Android.
        try camara.lockForConfiguration()
        camara.activeVideoMinFrameDuration = CMTime(value: 1, timescale: 30)
        camara.unlockForConfiguration()
        configurada = true
    }

    func iniciar() { cola.async { [sesion] in if !sesion.isRunning { sesion.startRunning() } } }
    func detener() { cola.async { [sesion] in if sesion.isRunning { sesion.stopRunning() } } }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        let tiempo = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)
        let ms = Int64(CMTimeGetSeconds(tiempo) * 1000)
        alRecibirFotograma?(sampleBuffer, ms)
    }
}
