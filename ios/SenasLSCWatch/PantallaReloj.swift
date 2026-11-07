import SwiftUI

/// Frase en grande en la muñeca. Se desplaza con la Digital Crown si no cabe.
struct PantallaReloj: View {
    @EnvironmentObject private var conexion: ConexionTelefono

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                Text(estadoCorto)
                    .font(Tipografia.fuente(.footnote, tamano: 13, peso: .semibold))
                    .foregroundStyle(.secondary)

                Text(fraseVisible)
                    .font(Tipografia.fuente(.title3, tamano: 22, peso: .bold))
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.updatesFrequently)

                if conexion.incierta {
                    Label("Revisa la frase en el iPhone", systemImage: "questionmark.circle")
                        .font(Tipografia.fuente(.footnote, tamano: 13))
                        .foregroundStyle(.yellow)
                }

                HStack {
                    Button {
                        conexion.enviar(conexion.pausado ? .iniciar : .pausar)
                    } label: {
                        Image(systemName: conexion.pausado ? "play.fill" : "pause.fill")
                    }
                    .accessibilityLabel(conexion.pausado ? "Reanudar análisis" : "Pausar análisis")

                    Button(role: .destructive) {
                        conexion.enviar(.borrar)
                    } label: {
                        Image(systemName: "trash")
                    }
                    .accessibilityLabel("Borrar frase")
                    .disabled(conexion.numeroSenas == 0)
                }
                .disabled(!conexion.conectado)
                .padding(.top, 4)
            }
            .padding(.horizontal, 4)
        }
        .navigationTitle("Señas LSC")
    }

    private var fraseVisible: String {
        if conexion.sinDatos { return "Abre la cámara en el iPhone." }
        if conexion.estado == .noDisponible { return "El iPhone no tiene modelo de señas." }
        return conexion.frase.isEmpty ? "Haz una seña…" : conexion.frase
    }

    private var estadoCorto: String {
        if !conexion.conectado { return "Sin conexión con el iPhone" }
        if conexion.pausado { return "En pausa" }
        switch conexion.estado {
        case .preparando: return "Preparando…"
        case .noDisponible: return "No disponible"
        case .reconociendo: return conexion.ultimaSena.map { "Última: \($0)" } ?? "Reconociendo"
        case .fraseLista: return "Frase lista"
        }
    }
}
