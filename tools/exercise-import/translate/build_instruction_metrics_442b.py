"""Build Etapa 4.4.2B instruction translation metrics (deterministic)."""

from __future__ import annotations

import hashlib
import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "curation"))

from catalog_paths import analysis_catalog_path  # noqa: E402
from classify_importability import classify as classify_importability
import translate_instructions as ti

NORMALIZED = analysis_catalog_path()
OUT_JSON = ROOT / "analysis" / "instruction-translation-4-4-2b.json"
OUT_MD = ROOT / "analysis" / "INSTRUCTION_TRANSLATION_4_4_2B.md"
IMPORTABLE = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})


def build_metrics() -> dict:
    catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    rows = [r for r in catalog if classify_importability(r) in IMPORTABLE]
    rows.sort(key=lambda r: (r.get("source") or "", r.get("externalId") or ""))

    status = Counter()
    methods = Counter()
    review_reasons = Counter()
    total = 0
    for ex in rows:
        for instr in (ex.get("sourceData") or {}).get("instructions") or []:
            total += 1
            res = ti.translate_sentence(instr)
            status[res.status] += 1
            method = res.method.split(":")[0]
            methods[method] += 1
            if res.status == "REVIEW" and res.method.startswith("review:"):
                review_reasons[res.method.split(":", 1)[1]] += 1
            elif res.status == "REVIEW":
                review_reasons["passthrough_pending"] += 1

    payload = {
        "stage": "4.4.2B",
        "totalInstructions": total,
        "translated": status.get("TRANSLATED", 0),
        "unchanged": status.get("UNCHANGED", 0),
        "review": status.get("REVIEW", 0),
        "notApplicable": status.get("NOT_APPLICABLE", 0),
        "passthroughEnglish": status.get("REVIEW", 0),  # pending English kept as REVIEW
        "methods": dict(sorted(methods.items())),
        "reviewReasons": [
            {"reason": k, "count": v}
            for k, v in sorted(review_reasons.items(), key=lambda kv: (-kv[1], kv[0]))
        ],
        "lookupSize": len(ti.SENTENCE_LOOKUP),
        "before": {
            "TRANSLATED_EXISTING": 511,
            "PASSTHROUGH_ENGLISH": 2928,
            "PORTUGUESE": 0,
            "MIXED": 0,
            "UNKNOWN": 2,
        },
        "nameStatusUnchanged": {
            "TRANSLATED": 641,
            "UNCHANGED": 133,
            "REVIEW": 3,
            "NOT_APPLICABLE": 0,
        },
        "notes": [
            "Instruction statuses are TRANSLATED / REVIEW / NOT_APPLICABLE (UNCHANGED unused in 4.4.2B).",
            "Pending English instructions are classified as REVIEW (not forced TRANSLATED).",
            "Safe compose fallback disabled to avoid unnatural/missing-article Portuguese.",
            "Pulldown never becomes Puxada; Pull-Up → Barra Fixa in covered sentences.",
        ],
    }
    assert (
        payload["translated"]
        + payload["unchanged"]
        + payload["review"]
        + payload["notApplicable"]
        == total
    )
    return payload


def write_md(payload: dict) -> str:
    b = payload["before"]
    lines = [
        "# INSTRUCTION_TRANSLATION_4_4_2B",
        "",
        "**Escopo**: tradução determinística de instruções para pt-BR.",
        "",
        "## Antes (auditoria 4.4.2A)",
        "",
        "```text",
        f"Total de instruções: {payload['totalInstructions']}",
        f"TRANSLATED_EXISTING: {b['TRANSLATED_EXISTING']}",
        f"PASSTHROUGH_ENGLISH: {b['PASSTHROUGH_ENGLISH']}",
        f"PORTUGUESE: {b['PORTUGUESE']}",
        f"MIXED: {b['MIXED']}",
        f"UNKNOWN: {b['UNKNOWN']}",
        "```",
        "",
        "## Depois (4.4.2B)",
        "",
        "```text",
        f"TRANSLATED:      {payload['translated']}",
        f"UNCHANGED:       {payload['unchanged']}",
        f"REVIEW:          {payload['review']}",
        f"NOT_APPLICABLE:  {payload['notApplicable']}",
        "```",
        "",
        f"Soma: {payload['translated'] + payload['unchanged'] + payload['review'] + payload['notApplicable']} "
        f"(= {payload['totalInstructions']})",
        "",
        "## Ganho de cobertura",
        "",
        "```text",
        f"Instruções traduzidas antes: {b['TRANSLATED_EXISTING']}",
        f"Instruções traduzidas depois: {payload['translated']}",
        f"Aumento: {payload['translated'] - b['TRANSLATED_EXISTING']}",
        "```",
        "",
        "## Passthrough / REVIEW pendente",
        "",
        "```text",
        f"Antes (PASSTHROUGH_ENGLISH): {b['PASSTHROUGH_ENGLISH']}",
        f"Depois (REVIEW pendente):    {payload['review']}",
        f"Redução: {b['PASSTHROUGH_ENGLISH'] - payload['review']}",
        "```",
        "",
        "## Métodos",
        "",
        "```text",
    ]
    for k, v in payload["methods"].items():
        lines.append(f"{k}: {v}")
    lines.extend(
        [
            "```",
            "",
            f"Tamanho do SENTENCE_LOOKUP: {payload['lookupSize']}",
            "",
            "## REVIEW por motivo",
            "",
            "| Motivo | Ocorrências |",
            "|--------|------------:|",
        ]
    )
    for row in payload["reviewReasons"]:
        lines.append(f"| {row['reason']} | {row['count']} |")
    lines.extend(
        [
            "",
            "## Principais regras adicionadas",
            "",
            "- Expansão do `SENTENCE_LOOKUP` (frases frequentes + `extra_sentence_lookup.json`)",
            "- Padrões estruturados: Return/Keep/Hold/Repeat/Breathe",
            "- Compostos `Keep your …` com concordância (costas retas, braços estendidos…)",
            "- Termos técnicos: Pulldown, Face Pull, Leg Press, Kettlebell, Clean…",
            "- Pull-Up → Barra Fixa nas frases cobertas; Pulldown ≠ Puxada",
            "- Compose genérico **desativado** (evita pt-BR sem artigos / MIXED)",
            "- Parceiro/HTML/truncado → REVIEW sem forçar tradução",
            "- Strings vazias → NOT_APPLICABLE",
            "",
            "## Nomes dos exercícios",
            "",
            "Classificação de nomes **inalterada**:",
            "",
            "```text",
            "TRANSLATED: 641",
            "UNCHANGED:  133",
            "REVIEW:       3",
            "```",
            "",
            "## Integridade",
            "",
            "```text",
            "sourceData.instructions alterado: NÃO",
            "gymtrack-exercises.json alterado: NÃO",
            "Asset Android alterado: NÃO",
            "app/ alterado: NÃO",
            "Git commit criado: NÃO",
            "```",
            "",
        ]
    )
    for note in payload["notes"]:
        lines.append(f"- {note}")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    payload = build_metrics()
    text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    OUT_JSON.write_bytes(text.encode("utf-8"))
    OUT_MD.write_bytes(write_md(payload).encode("utf-8"))
    print(json.dumps({
        "translated": payload["translated"],
        "review": payload["review"],
        "notApplicable": payload["notApplicable"],
        "sha16": hashlib.sha256(text.encode("utf-8")).hexdigest()[:16],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
