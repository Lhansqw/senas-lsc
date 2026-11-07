import SwiftUI

struct PantallaInicio: View {
    private var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
    }

    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Reconocimiento de Lengua de Señas Colombiana")
                        .font(Tipografia.fuente(.title2, tamano: 24, peso: .bold))
                    Text("Haz señas delante de la cámara frontal: la app las reconoce, las muestra como texto y te propone una frase en español que puedes corregir.")
                        .font(Tipografia.fuente(.body, tamano: 17))
                        .foregroundStyle(Paleta.tenue)
                }
                .padding(.vertical, 8)
            }
            Section {
                NavigationLink("Abrir cámara") { PantallaCamara() }
                NavigationLink("Vocabulario compatible") { PantallaVocabulario() }
                NavigationLink("Acerca de, privacidad y licencias") { PantallaAcerca() }
            }
            .font(Tipografia.fuente(.body, tamano: 17, peso: .semibold))
            Section {
                Label("Si tienes un Apple Watch, abre Señas LSC en el reloj para ver la frase en la muñeca.", systemImage: "applewatch")
                    .font(Tipografia.fuente(.footnote, tamano: 14))
                    .foregroundStyle(Paleta.tenue)
            } footer: {
                Text("Versión \(version)")
            }
        }
        .navigationTitle("Señas LSC")
    }
}
