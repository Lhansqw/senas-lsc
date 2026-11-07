import Foundation
import SenasShared
import SwiftUI

/// Señas que el modelo instalado sabe reconocer, leídas de `modelo.json` con el lector común.
struct PantallaVocabulario: View {
    @State private var filtro = ""

    private let contrato: ContratoModelo? = {
        guard let url = Bundle.main.url(forResource: "modelo", withExtension: "json"),
              let texto = try? String(contentsOf: url, encoding: .utf8)
        else { return nil }
        return (LectorContrato.shared.leer(texto: texto) as? ResultadoContratoValido)?.contrato
    }()

    var body: some View {
        List {
            if let contrato {
                Section {
                    Text("Estas son exactamente las señas de LSC que el modelo instalado sabe reconocer. Cualquier otra seña se marca como no reconocida.")
                        .font(Tipografia.fuente(.callout, tamano: 16))
                        .foregroundStyle(Paleta.tenue)
                } footer: {
                    Text("Modelo \(contrato.nombre) \(contrato.version) · \(contrato.entradas.count) señas")
                }
                ForEach(contrato.entradas.filter(coincide), id: \.id) { entrada in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entrada.glosa).font(Tipografia.fuente(.headline, tamano: 17, peso: .bold))
                        Text(entrada.significado).font(Tipografia.fuente(.subheadline, tamano: 15)).foregroundStyle(Paleta.tenue)
                    }
                }
            } else {
                Text("Todavía no hay un modelo de señas instalado, así que la app no reconoce ninguna seña.")
                    .font(Tipografia.fuente(.body, tamano: 17))
            }
        }
        .searchable(text: $filtro, prompt: "Buscar seña")
        .navigationTitle("Vocabulario")
    }

    /// Misma búsqueda sin tildes que en Android (`coincideBusqueda`, Kotlin común).
    private func coincide(_ entrada: EntradaVocabulario) -> Bool {
        NormalizarKt.coincideBusqueda(texto: "\(entrada.glosa) \(entrada.significado)", consulta: filtro)
    }
}
