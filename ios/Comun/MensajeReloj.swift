import Foundation

/// Diccionario que viaja entre el iPhone y el Apple Watch por WatchConnectivity.
///
/// Las claves y valores son los de `ResumenReloj` (shared/…/ResumenReloj.kt), que también usará
/// Wear OS. El reloj no enlaza el código Kotlin, por eso se repiten aquí: si cambian allí, hay que
/// cambiarlas aquí.
enum MensajeReloj {
    enum Clave {
        static let estado = "estado"
        static let frase = "frase"
        static let ultimaSena = "ultimaSena"
        static let numeroSenas = "numeroSenas"
        static let incierta = "incierta"
        static let pausado = "pausado"
        static let orden = "orden"
    }

    /// Valores de `EstadoReloj`.
    enum Estado: String {
        case preparando = "PREPARANDO"
        case noDisponible = "NO_DISPONIBLE"
        case reconociendo = "RECONOCIENDO"
        case fraseLista = "FRASE_LISTA"
    }

    /// Órdenes que el reloj envía al iPhone.
    enum Orden: String {
        case iniciar, pausar, borrar, interpretar
    }
}
