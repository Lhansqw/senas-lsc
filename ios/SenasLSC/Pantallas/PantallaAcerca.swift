import SwiftUI

/// Privacidad, límites y licencias. La cita de LSC70 es obligatoria (CC BY 4.0) y debe ser igual
/// que `Creditos.CITA_LSC70` de Android; lo comprueba `CoherenciaIosTest` en app/src/test.
struct PantallaAcerca: View {
    // En una sola línea para que la prueba pueda compararla tal cual.
    static let citaLSC70 = "LSC70: conjunto de datos de Lengua de Señas Colombiana, Universidad del Cauca (2024). Mendeley Data, doi:10.17632/9ssyn8tff5.2. Licencia CC BY 4.0. El modelo de señas de esta app se entrenó con estos datos; no se modificaron las imágenes originales."
    static let enlaceLSC70 = "https://data.mendeley.com/datasets/9ssyn8tff5/2"

    private let componentes: [(nombre: String, uso: String, licencia: String)] = [
        ("MediaPipe Hand y Pose Landmarker", "Detectar manos y cuerpo en la cámara", "Apache 2.0"),
        ("TensorFlow Lite", "Ejecutar el modelo de señas en el iPhone", "Apache 2.0"),
        ("Kotlin Multiplatform", "Lógica de reconocimiento compartida con Android", "Apache 2.0"),
        ("Atkinson Hyperlegible Next", "Tipografía de la app, pensada para leerse con baja visión", "SIL OFL 1.1"),
    ]

    var body: some View {
        List {
            Section("Qué hace y qué no") {
                Text("La app reconoce un vocabulario limitado de la LSC, no toda la lengua. La frase es siempre una propuesta: revísala antes de usarla.")
            }
            Section("Privacidad") {
                Text("Las imágenes de la cámara se analizan en el iPhone y no se guardan ni se envían a ningún sitio. Al Apple Watch solo llega el texto de la frase.")
            }
            Section("Datos de entrenamiento") {
                Text(Self.citaLSC70)
                Link(Self.enlaceLSC70, destination: URL(string: Self.enlaceLSC70)!)
                    .font(Tipografia.fuente(.footnote, tamano: 14))
            }
            Section("Software de terceros") {
                ForEach(componentes, id: \.nombre) { c in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(c.nombre).font(Tipografia.fuente(.headline, tamano: 16, peso: .bold))
                        Text("\(c.uso) · \(c.licencia)").font(Tipografia.fuente(.subheadline, tamano: 14)).foregroundStyle(Paleta.tenue)
                    }
                }
            }
        }
        .font(Tipografia.fuente(.body, tamano: 16))
        .navigationTitle("Acerca de")
    }
}
