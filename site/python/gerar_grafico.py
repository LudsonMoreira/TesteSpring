import sqlite3
import sys
from pathlib import Path

import matplotlib
matplotlib.use("Agg")

import matplotlib.pyplot as plt

banco = sys.argv[1]
arquivo_grafico = Path(sys.argv[2])

arquivo_grafico.parent.mkdir(parents=True, exist_ok=True)

# Consulta o mesmo SQLite utilizado pelo Java.
with sqlite3.connect(banco) as conexao:
    resultados = conexao.execute("""
        SELECT linguagem, COUNT(*) AS total
        FROM votos
        GROUP BY linguagem
        ORDER BY total DESC
    """).fetchall()

figura, eixo = plt.subplots(figsize=(9, 5))

if resultados:
    linguagens = [linha[0] for linha in resultados]
    votos = [linha[1] for linha in resultados]

    barras = eixo.bar(linguagens, votos)

    eixo.set_title("Linguagens de programação mais votadas")
    eixo.set_xlabel("Linguagem")
    eixo.set_ylabel("Quantidade de votos")
    eixo.bar_label(barras, padding=3)

    eixo.tick_params(axis="x", labelrotation=30)

    for texto in eixo.get_xticklabels():
        texto.set_ha("right")

    eixo.margins(y=0.15)

else:
    eixo.text(
        0.5, 0.5,
        "Ainda não há votos",
        ha="center",
        va="center",
        transform=eixo.transAxes
    )
    eixo.set_axis_off()

figura.tight_layout()
figura.savefig(arquivo_grafico, dpi=120, bbox_inches="tight")

plt.close(figura)

print("Gráfico gerado com sucesso.")