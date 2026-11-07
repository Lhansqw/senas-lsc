import AVFoundation
import SenasShared
import SwiftUI

/// Cámara con la frase en «papel» debajo, como la pantalla de cámara rediseñada de Android.
struct PantallaCamara: View {
    @StateObject private var modelo = ModeloReconocimiento()
    @State private var editando = false
    @State private var textoEditado = ""

    var body: some View {
        VStack(spacing: 0) {
            VistaPreviaCamara(sesion: modelo.camara.sesion)
                .overlay(alignment: .topLeading) { indicador }
                .accessibilityLabel("Vista previa de la cámara")

            papel
        }
        .navigationTitle("Cámara")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            Button(modelo.pausado ? "Reanudar" : "Pausar") { modelo.pausarOReanudar() }
        }
        .task { await modelo.iniciarCamara() }
        .onDisappear { modelo.detenerCamara() }
        .alert("Corregir frase", isPresented: $editando) {
            TextField("Frase", text: $textoEditado)
            Button("Guardar") { modelo.corregir(textoEditado) }
            Button("Cancelar", role: .cancel) {}
        }
    }

    private var indicador: some View {
        Text(textoIndicador)
            .font(Tipografia.fuente(.caption, tamano: 13, peso: .semibold))
            .padding(.horizontal, 10).padding(.vertical, 6)
            .background(.ultraThinMaterial, in: Capsule())
            .padding(12)
    }

    private var textoIndicador: String {
        if modelo.pausado { return "Análisis en pausa" }
        switch modelo.estado.modelo {
        case is EstadoModeloSenasCargando: return "Preparando el modelo…"
        case is EstadoModeloSenasSinModelo: return "No hay modelo de señas instalado"
        case is EstadoModeloSenasInvalido, is EstadoModeloSenasFallo: return "El modelo de señas no está disponible"
        default:
            if let p = modelo.estado.ultimaPrediccion, let id = p.candidatas.first?.id, !p.desconocida,
               let listo = modelo.estado.modelo as? EstadoModeloSenasListo, let glosa = listo.glosas[id] {
                return "Viendo: \(glosa)"
            }
            return "Haz una seña"
        }
    }

    private var papel: some View {
        let estado = modelo.estado
        return VStack(alignment: .leading, spacing: 12) {
            if let aviso = modelo.aviso {
                Text(aviso).font(Tipografia.fuente(.footnote, tamano: 14)).foregroundStyle(.red)
            }
            Text(estado.senas.isEmpty ? "Señas: —" : "Señas: " + estado.senas.map { $0.entrada.glosa }.joined(separator: " · "))
                .font(Tipografia.fuente(.subheadline, tamano: 15))
                .foregroundStyle(Paleta.tenue)

            Text(estado.propuesta?.texto ?? (estado.senas.isEmpty ? "La frase aparecerá aquí." : "…"))
                .font(Tipografia.fuente(.title, tamano: 30, peso: .bold))
                .foregroundStyle(Paleta.tinta)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.updatesFrequently)

            if let pregunta = estado.propuesta?.preguntaAclaracion {
                Text(pregunta).font(Tipografia.fuente(.callout, tamano: 16)).foregroundStyle(Paleta.tenue)
            }

            HStack {
                Button("Interpretar") { modelo.interpretar() }
                    .disabled(estado.senas.isEmpty || estado.secuenciaCerrada)
                Button("Borrar última") { modelo.eliminarUltima() }
                    .disabled(estado.senas.isEmpty)
                Spacer()
                Button("Corregir") {
                    textoEditado = estado.propuesta?.texto ?? ""
                    editando = true
                }
                .disabled(estado.propuesta == nil)
                Button("Limpiar", role: .destructive) { modelo.limpiar() }
            }
            .buttonStyle(.bordered)
            .font(Tipografia.fuente(.callout, tamano: 15, peso: .semibold))

            if let texto = estado.propuesta?.texto {
                ShareLink(item: texto) { Label("Compartir", systemImage: "square.and.arrow.up") }
                    .font(Tipografia.fuente(.callout, tamano: 15))
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Paleta.papel)
        .foregroundStyle(Paleta.tinta)
    }
}

/// Vista previa de AVFoundation. Se muestra en espejo (como un espejo real); el análisis no.
struct VistaPreviaCamara: UIViewRepresentable {
    let sesion: AVCaptureSession

    final class Vista: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var capa: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
    }

    func makeUIView(context: Context) -> Vista {
        let v = Vista()
        v.capa.session = sesion
        v.capa.videoGravity = .resizeAspectFill
        return v
    }

    func updateUIView(_ uiView: Vista, context: Context) {}
}
