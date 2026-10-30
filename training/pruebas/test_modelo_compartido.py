import json

import generar_modelo_compartido as G


def test_el_archivo_compartido_esta_al_dia():
    guardado = json.loads(G.RUTA.read_text(encoding="utf-8"))
    assert guardado == G.construir(), "Regenera con: python pruebas/generar_modelo_compartido.py"
