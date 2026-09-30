"""Etapa 4.5 — Image audit + asset copy for main catalog selection."""

from __future__ import annotations

import hashlib
import json
import shutil
import sys
import tempfile
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parents[1]
MAIN_CATALOG_JSON = ROOT / "analysis" / "MAIN_CATALOG.json"
OUT_AUDIT_JSON = ROOT / "analysis" / "exercise-image-audit-4-5.json"
OUT_AUDIT_MD = ROOT / "analysis" / "EXERCISE_IMAGE_AUDIT.md"
OUT_MAIN_MD = ROOT / "analysis" / "MAIN_CATALOG.md"
ASSETS_ROOT = REPO / "app" / "src" / "main" / "assets" / "exercises"
CACHE_ROOT = ROOT / "input" / "free-exercise-db" / "exercises"
RAW_BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/master/exercises/"

sys.path.insert(0, str(ROOT / "curation"))
import curate_main_catalog as curate_mod  # noqa: E402


def _sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _image_dimensions(path: Path) -> tuple[int | None, int | None]:
    try:
        from PIL import Image  # type: ignore

        with Image.open(path) as im:
            return im.size
    except Exception:
        return None, None


def _webp_size_estimate(jpg_path: Path) -> int | None:
    try:
        from PIL import Image  # type: ignore

        with Image.open(jpg_path) as im:
            with tempfile.NamedTemporaryFile(suffix=".webp", delete=False) as tmp:
                tmp_path = Path(tmp.name)
            try:
                im.save(tmp_path, format="WEBP", quality=80, method=6)
                return tmp_path.stat().st_size
            finally:
                tmp_path.unlink(missing_ok=True)
    except Exception:
        return None


def _fetch_image(rel_path: str, dest: Path) -> bool:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists() and dest.stat().st_size > 0:
        return True
    url = RAW_BASE + rel_path.replace("\\", "/")
    try:
        with urllib.request.urlopen(url, timeout=60) as resp:
            data = resp.read()
        if not data:
            return False
        dest.write_bytes(data)
        return True
    except (urllib.error.URLError, TimeoutError, OSError):
        return False


def _resolve_local(rel_path: str) -> Path:
    return CACHE_ROOT / rel_path.replace("/", "\\") if sys.platform == "win32" else CACHE_ROOT / rel_path


def _ensure_local(rel_path: str) -> Path | None:
    local = _resolve_local(rel_path)
    if local.exists():
        return local
    if _fetch_image(rel_path, local):
        return local
    return None


def classify_images(paths: list[str], local_files: list[Path | None]) -> str:
    if not paths:
        return "NO_IMAGE"
    existing = [p for p in local_files if p is not None and p.exists()]
    if not existing:
        return "NO_IMAGE"
    if len(existing) == 1:
        return "LIMITED"
    if len(existing) >= 2:
        h0 = _sha256_file(existing[0])
        h1 = _sha256_file(existing[1])
        if h0 == h1:
            return "LIMITED"
        return "GOOD_CANDIDATE"
    return "NO_IMAGE"


def copy_to_assets(external_id: str, local_files: list[Path]) -> list[str]:
    dest_dir = ASSETS_ROOT / external_id
    dest_dir.mkdir(parents=True, exist_ok=True)
    copied: list[str] = []
    for idx, src in enumerate(sorted(local_files, key=lambda p: p.name)[:2]):
        name = f"{idx}.jpg"
        dest = dest_dir / name
        shutil.copy2(src, dest)
        copied.append(str(dest.relative_to(REPO)).replace("\\", "/"))
    return copied


def build_reports(regenerate_catalog: bool = True) -> dict[str, Any]:
    if regenerate_catalog or not MAIN_CATALOG_JSON.exists():
        payload = curate_mod.curate()
        MAIN_CATALOG_JSON.write_bytes(
            curate_mod.dumps_deterministic(payload).encode("utf-8")
        )
    else:
        payload = json.loads(MAIN_CATALOG_JSON.read_text(encoding="utf-8"))

    selected = payload["selected"]
    audit_rows: list[dict] = []
    total_bytes = 0
    webp_bytes = 0
    class_counts: Counter[str] = Counter()
    img_count_dist: Counter[int] = Counter()

    # Only touch image subfolders; never modify gymtrack-exercises.json
    ASSETS_ROOT.mkdir(parents=True, exist_ok=True)
    selected_ids = {e["externalId"] for e in selected}
    for child in ASSETS_ROOT.iterdir():
        if child.is_dir() and child.name not in selected_ids:
            shutil.rmtree(child)

    for entry in selected:
        eid = entry["externalId"]
        paths = entry.get("imagePaths") or []
        locals_list: list[Path | None] = []
        file_meta: list[dict] = []
        for rel in paths[:2]:
            local = _ensure_local(rel)
            locals_list.append(local)
            if local and local.exists():
                w, h = _image_dimensions(local)
                size = local.stat().st_size
                total_bytes += size
                est = _webp_size_estimate(local)
                if est:
                    webp_bytes += est
                file_meta.append({
                    "sourcePath": rel,
                    "localPath": str(local.relative_to(ROOT)).replace("\\", "/"),
                    "fileName": local.name,
                    "bytes": size,
                    "width": w,
                    "height": h,
                    "sha256": _sha256_file(local)[:16],
                    "webpEstimateBytes": est,
                })

        classification = classify_images(paths, locals_list)
        class_counts[classification] += 1
        img_count_dist[len(paths)] += 1

        asset_paths: list[str] = []
        existing = [p for p in locals_list if p is not None and p.exists()]
        if existing:
            asset_paths = copy_to_assets(eid, existing)

        audit_rows.append({
            "externalId": eid,
            "originalName": entry["originalName"],
            "translatedName": entry["translatedName"],
            "muscleGroup": entry["muscleGroup"],
            "equipmentType": entry["equipmentType"],
            "imageCount": len(paths),
            "classification": classification,
            "files": file_meta,
            "assetPaths": asset_paths,
        })

    two_img = sum(1 for r in audit_rows if r["imageCount"] == 2)
    one_img = sum(1 for r in audit_rows if r["imageCount"] == 1)
    zero_img = sum(1 for r in audit_rows if r["imageCount"] == 0)
    good = class_counts["GOOD_CANDIDATE"]
    limited = class_counts["LIMITED"]
    no_img = class_counts["NO_IMAGE"]

    reduction_pct = round((1 - webp_bytes / total_bytes) * 100, 1) if total_bytes and webp_bytes else None

    result = {
        "stage": "4.5",
        "selectedCount": len(selected),
        "imageClassification": dict(class_counts),
        "imageCountDistribution": {str(k): v for k, v in sorted(img_count_dist.items())},
        "twoImages": two_img,
        "oneImage": one_img,
        "zeroImages": zero_img,
        "goodCandidate": good,
        "limited": limited,
        "noImage": no_img,
        "totalAssetBytesJpg": total_bytes,
        "estimatedWebpBytes": webp_bytes,
        "estimatedWebpReductionPercent": reduction_pct,
        "assetsRoot": str(ASSETS_ROOT.relative_to(REPO)).replace("\\", "/"),
        "licenseNote": (
            "Free Exercise DB dataset uses Unlicense; image provenance/licensing "
            "has been questioned in community discussions. Treat bundled images as "
            "development/prototype assets until legal review before commercial release."
        ),
        "exercises": audit_rows,
    }

    OUT_AUDIT_JSON.write_bytes(
        (json.dumps(result, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    )
    OUT_AUDIT_MD.write_bytes(_write_audit_md(result).encode("utf-8"))
    OUT_MAIN_MD.write_bytes(_write_main_md(payload, result).encode("utf-8"))
    return result


def _write_audit_md(result: dict) -> str:
    lines = [
        "# EXERCISE_IMAGE_AUDIT — Etapa 4.5",
        "",
        f"Exercícios selecionados: **{result['selectedCount']}**",
        "",
        "## Classificação",
        "",
        "| Classe | Quantidade |",
        "|--------|----------:|",
    ]
    for k, v in sorted(result["imageClassification"].items()):
        lines.append(f"| {k} | {v} |")
    lines.extend([
        "",
        "## Contagem de imagens (metadados)",
        "",
        f"- 2 imagens: {result['twoImages']}",
        f"- 1 imagem: {result['oneImage']}",
        f"- 0 imagens: {result['zeroImages']}",
        "",
        "## Tamanho",
        "",
        f"- JPG total (copiado): **{result['totalAssetBytesJpg']:,} bytes** "
        f"({result['totalAssetBytesJpg'] / 1024 / 1024:.2f} MiB)",
    ])
    if result.get("estimatedWebpBytes"):
        lines.append(
            f"- Estimativa WebP (quality=80, experimental): "
            f"**{result['estimatedWebpBytes']:,} bytes** "
            f"({result['estimatedWebpBytes'] / 1024 / 1024:.2f} MiB)"
        )
        if result.get("estimatedWebpReductionPercent") is not None:
            lines.append(
                f"- Redução estimada: **{result['estimatedWebpReductionPercent']}%** "
                "(JPGs originais não foram substituídos)"
            )
    lines.extend([
        "",
        "## Licença",
        "",
        result["licenseNote"],
        "",
        "## Detalhe por exercício",
        "",
        "| externalId | Classe | Imagens | Assets |",
        "|------------|--------|--------:|--------|",
    ])
    for row in result["exercises"]:
        assets = ", ".join(row["assetPaths"]) if row["assetPaths"] else "—"
        lines.append(
            f"| {row['externalId']} | {row['classification']} | "
            f"{row['imageCount']} | {assets} |"
        )
    lines.append("")
    return "\n".join(lines)


def _write_main_md(catalog: dict, audit: dict) -> str:
    from collections import Counter

    muscle = Counter(e["muscleGroup"] for e in catalog["selected"])
    equip = Counter(e["equipmentType"] or "null" for e in catalog["selected"])

    slot_ids = sorted(
        {e["slot"] for e in catalog["selected"] if e.get("slot") and not e["slot"].startswith("fill_")}
    )

    lines = [
        "# MAIN_CATALOG — Etapa 4.5",
        "",
        "## Resumo",
        "",
        "```text",
        f"Exercícios atuais (importáveis): {catalog['totalImportable']}",
        f"Exercícios selecionados: {catalog['selectedCount']}",
        f"Exercícios removidos: {catalog['removedCount']}",
        f"Redução: {catalog['reductionPercent']}%",
        "```",
        "",
        "## Distribuição muscular",
        "",
        "| Grupo | Quantidade |",
        "|-------|----------:|",
    ]
    for mg, c in muscle.most_common():
        lines.append(f"| {mg} | {c} |")

    lines.extend([
        "",
        "## Padrões de movimento (slots canônicos)",
        "",
        "Cada linha é um slot determinístico da curadoria (Etapa 4.5):",
        "",
    ])
    for sid in slot_ids:
        lines.append(f"- `{sid}`")

    lines.extend(["", "## Distribuição por equipamento", "", "| Equipamento | Quantidade |", "|-------------|----------:|"])
    equip_map = {
        "Barra": "Barra",
        "Halteres": "Halteres",
        "Máquina": "Máquina",
        "Cabos": "Cabo",
        "Elástico": "Faixa Elástica",
        "Peso corporal": "Peso corporal",
        "Kettlebell": "Kettlebell",
        "Outro": "Outro",
        "null": "Sem equipamento",
    }
    for eq, c in equip.most_common():
        label = equip_map.get(eq, eq)
        lines.append(f"| {label} | {c} |")

    lines.extend([
        "",
        "## Imagens",
        "",
        f"- GOOD_CANDIDATE: {audit['goodCandidate']}",
        f"- LIMITED: {audit['limited']}",
        f"- NO_IMAGE: {audit['noImage']}",
        f"- Com 2 imagens (metadado): {audit['twoImages']}",
        f"- Com 1 imagem: {audit['oneImage']}",
        "",
        f"Assets APK: `{audit['assetsRoot']}/<externalId>/0.jpg`",
        "",
        "## Licença de imagens",
        "",
        audit["licenseNote"],
        "",
        "## Exercícios selecionados",
        "",
        "| externalId | Nome PT | Grupo | Equip. | Img | Classe |",
        "|------------|---------|-------|--------|----:|--------|",
    ])
    audit_by_id = {r["externalId"]: r for r in audit["exercises"]}
    for e in sorted(catalog["selected"], key=lambda x: x["externalId"]):
        a = audit_by_id.get(e["externalId"], {})
        name = e["translatedName"].replace("|", "\\|")
        lines.append(
            f"| {e['externalId']} | {name} | {e['muscleGroup']} | "
            f"{e['equipmentType'] or '—'} | {e['imageCount']} | "
            f"{a.get('classification', '—')} |"
        )
    lines.extend([
        "",
        "## Justificativa da curadoria",
        "",
        "Seleção determinística em três camadas: (1) slots canônicos cobrindo padrões "
        "fundamentais de peito, costas, ombros, braços, pernas, glúteos, panturrilhas e "
        "abdômen; (2) preenchimento até mínimos por grupo muscular; (3) complemento até "
        "115 exercícios priorizando 2 imagens. Redundâncias foram evitadas nos slots "
        "iniciais; imagem dupla é preferida, mas exercícios importantes permanecem mesmo "
        "com classificação LIMITED.",
        "",
        "Matriz completa selected/removed: `MAIN_CATALOG.json` → `auditMatrix`.",
        "",
    ])
    return "\n".join(lines)


def main() -> None:
    result = build_reports(regenerate_catalog=True)
    print(json.dumps({
        "selected": result["selectedCount"],
        "good": result["goodCandidate"],
        "totalBytes": result["totalAssetBytesJpg"],
        "assets": result["assetsRoot"],
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
