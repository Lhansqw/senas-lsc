import AVFoundation
import MediaPipeTasksVision
import SenasShared

/// Puntos de MediaPipe para iOS. Equivale a `ExtractorLandmarks` de Android.
///
/// Android usa Holistic Landmarker, que no existe para iOS. Aquí se combinan Hand Landmarker
/// (hasta 2 manos) y Pose Landmarker. Como el modelo de señas no usa el rostro (`usa_rostro: false`),
/// no hace falta Face Landmarker. `ConstructorFotograma` (Kotlin común) decide qué mano es cuál
/// con las muñecas de la pose y aplica los mismos subconjuntos que Android.
///
/// No es seguro para varios hilos: se usa siempre desde la cola de la cámara.
final class ExtractorLandmarksIOS {
    private let manos: HandLandmarker
    private let pose: PoseLandmarker
    private var ultimoTimestampMs = Int64.min

    enum ErrorExtractor: Error { case faltaModelo(String) }

    init(confianzaMinima: Float = 0.5) throws {
        func ruta(_ nombre: String) throws -> String {
            guard let r = Bundle.main.path(forResource: nombre, ofType: "task") else { throw ErrorExtractor.faltaModelo("\(nombre).task") }
            return r
        }

        let opcionesManos = HandLandmarkerOptions()
        opcionesManos.baseOptions.modelAssetPath = try ruta("hand_landmarker")
        opcionesManos.runningMode = .video
        opcionesManos.numHands = 2
        opcionesManos.minHandDetectionConfidence = confianzaMinima
        opcionesManos.minHandPresenceConfidence = confianzaMinima
        opcionesManos.minTrackingConfidence = confianzaMinima
        manos = try HandLandmarker(options: opcionesManos)

        let opcionesPose = PoseLandmarkerOptions()
        opcionesPose.baseOptions.modelAssetPath = try ruta("pose_landmarker_full")
        opcionesPose.runningMode = .video
        opcionesPose.numPoses = 1
        opcionesPose.minPoseDetectionConfidence = confianzaMinima
        opcionesPose.minPosePresenceConfidence = confianzaMinima
        opcionesPose.minTrackingConfidence = confianzaMinima
        opcionesPose.shouldOutputSegmentationMasks = false
        pose = try PoseLandmarker(options: opcionesPose)
    }

    /// Devuelve `nil` si la marca de tiempo no crece (el modo vídeo de MediaPipe lo exige).
    func extraer(_ muestra: CMSampleBuffer, timestampMs: Int64) throws -> LandmarkFrame? {
        guard timestampMs > ultimoTimestampMs else { return nil }
        ultimoTimestampMs = timestampMs

        let imagen = try MPImage(sampleBuffer: muestra)
        let resultadoManos = try manos.detect(videoFrame: imagen, timestampInMilliseconds: Int(timestampMs))
        let resultadoPose = try pose.detect(videoFrame: imagen, timestampInMilliseconds: Int(timestampMs))

        let detectadas: [ManoDetectada] = resultadoManos.landmarks.enumerated().map { i, puntos in
            // MediaPipe etiqueta las manos suponiendo una imagen en espejo. Esta llega sin espejo,
            // así que «Right» es la mano izquierda de la persona.
            let etiqueta = resultadoManos.handedness[safe: i]?.first?.categoryName
            return ManoDetectada(coordenadas: Self.aCoordenadas(puntos), izquierdaSegunEtiqueta: etiqueta == "Right")
        }
        let puntosPose = resultadoPose.landmarks.first.map(Self.aCoordenadas)

        let ancho = Float(imagen.width)
        let alto = Float(imagen.height)
        return ConstructorFotograma.shared.construir(
            timestampMs: timestampMs,
            manos: detectadas,
            pose: puntosPose,
            relacionAspecto: alto > 0 ? ancho / alto : 1
        )
    }

    private static func aCoordenadas(_ puntos: [NormalizedLandmark]) -> KotlinFloatArray {
        let arreglo = KotlinFloatArray(size: Int32(puntos.count * 3))
        for (i, p) in puntos.enumerated() {
            arreglo.set(index: Int32(3 * i), value: p.x)
            arreglo.set(index: Int32(3 * i + 1), value: p.y)
            arreglo.set(index: Int32(3 * i + 2), value: p.z)
        }
        return arreglo
    }
}

extension Array {
    subscript(safe i: Int) -> Element? { indices.contains(i) ? self[i] : nil }
}
