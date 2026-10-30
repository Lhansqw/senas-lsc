"""Permite importar el paquete `senas` al ejecutar pytest desde cualquier carpeta."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
