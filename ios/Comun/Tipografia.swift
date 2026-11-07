import CoreText
import SwiftUI

/// Atkinson Hyperlegible Next, la misma tipografía que la app Android (diseñada para baja visión).
///
/// Se registra en tiempo de ejecución desde el .ttf de Android, así no hace falta declararla en
/// Info.plist ni adivinar su nombre PostScript.
enum Tipografia {
    private static let nombre: String? = {
        guard let url = Bundle.main.url(forResource: "atkinson_hyperlegible_next", withExtension: "ttf"),
              let proveedor = CGDataProvider(url: url as CFURL),
              let fuente = CGFont(proveedor)
        else { return nil }
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        return fuente.postScriptName as String?
    }()

    /// Fuente escalable con el tamaño de texto del sistema; si no se pudo cargar, usa la del sistema.
    static func fuente(_ estilo: Font.TextStyle, tamano: CGFloat, peso: Font.Weight = .regular) -> Font {
        guard let nombre else { return .system(estilo, design: .default).weight(peso) }
        return .custom(nombre, size: tamano, relativeTo: estilo).weight(peso)
    }
}
