import Foundation
import SenasShared
import TensorFlowLite

/// Ejecuta `reconocedor_lsc.tflite` con TensorFlow Lite. Es el único trozo del reconocedor que
/// depende de la plataforma: características y post-procesado se calculan en Kotlin común
/// (`ReconocedorExterno`), igual que en Android.
final class InferenciaTFLite: InferenciaModelo {
    private let interprete: Interpreter

    /// Devuelve `nil` si el modelo no está en la app.
    static func cargar() throws -> InferenciaTFLite? {
        guard let ruta = Bundle.main.path(forResource: "reconocedor_lsc", ofType: "tflite") else { return nil }
        var opciones = Interpreter.Options()
        opciones.threadCount = 2
        let interprete = try Interpreter(modelPath: ruta, options: opciones)
        try interprete.allocateTensors()
        return InferenciaTFLite(interprete)
    }

    private init(_ interprete: Interpreter) { self.interprete = interprete }

    func ejecutar(entrada: KotlinFloatArray) -> KotlinFloatArray {
        let n = Int(entrada.size)
        var valores = [Float](repeating: 0, count: n)
        for i in 0..<n { valores[i] = entrada.get(index: Int32(i)) }
        do {
            try interprete.copy(valores.withUnsafeBufferPointer { Data(buffer: $0) }, toInputAt: 0)
            try interprete.invoke()
            let salida = try interprete.output(at: 0)
            let logits = salida.data.withUnsafeBytes { Array($0.bindMemory(to: Float.self)) }
            let resultado = KotlinFloatArray(size: Int32(logits.count))
            for (i, v) in logits.enumerated() { resultado.set(index: Int32(i), value: v) }
            return resultado
        } catch {
            // Una salida vacía hace que Kotlin lance un error con el motivo; Swift lo recoge con try.
            NSLog("TensorFlow Lite falló: \(error)")
            return KotlinFloatArray(size: 0)
        }
    }
}
