# PRE_COMMIT_HYGIENE — Stage 4.11

Higienização pré-commit dos achados HIGH/MEDIUM da Stage 4.10.  
Sem alteração de catálogo, mídia, importer, Room, schema ou UI funcional.

Data (UTC): 2026-09-29

---

## Status

```text
PASS
```

---

## Alterações realizadas

| Item | Ação |
| ---- | ---- |
| `.gitignore` | Adicionados `__pycache__/`, `*.pyc`, cache V2.1 e `_assets_pre_hash_v2_1.json` |
| `PROJECT_CONTEXT.md` | Removido trecho stale “sem UI, sem tradução, sem imagens”; pack 136 / pt-BR / JPG / debug import documentados |
| `README.md` | Secção Free Exercise DB (876 / 777 / 99 / 136 / 272 JPG); Room 9; preview 2 frames |
| `tmp_diag_tap_settings.py` | **Removido** (script adb/uiautomator de diagnóstico; sem imports/referências de código) |

Não alterados: JSON publicados, `MAIN_CATALOG_V2_1.*`, imagens, importer, Room, testes V1.

---

## Caches (agora ignorados)

Confirmado via `git check-ignore -v`:

| Padrão / path | Regra |
| ------------- | ----- |
| `_media_download_cache_v2_1/**` | `.gitignore:30` |
| `_assets_pre_hash_v2_1.json` | `.gitignore:31` |
| `__pycache__/` | `.gitignore:26` |
| `*.pyc` | `.gitignore:27` |

Os ficheiros físicos do cache **não** foram apagados do disco (apenas deixam de ser candidatos a `git add`).

---

## Documentação

### PROJECT_CONTEXT.md

- Secção identidade externa: deixa claro pt-BR, 272 JPG, preview 2 frames, debug Settings, full-876 só análise/testes; Room sem `sourceData`/blobs/instruções.
- Rodapé: substitui “sem UI, sem tradução, sem imagens” pelo estado Stages 2.5–4.9.

### README.md

- Tabela 876 / 777 / 99 / **136**.
- **272** JPG (`0.jpg`/`1.jpg`); animação = dois frames (não GIF/vídeo/download).
- Room **9** + migration 8→9.
- Estado atual menciona pack V2.1.

---

## Testes

| Comando | Resultado |
| ------- | --------- |
| `:app:compileDebugKotlin` | OK (BUILD SUCCESSFUL) |
| `:app:testDebugUnitTest` | OK |
| `:app:assembleDebug` | OK |

---

## Teste V1 conhecido (fora de escopo)

```text
test_main_catalog_json_matches_curate
```

Drift pré-existente:

```text
"3/4 Sit-Up" vs "Abdominal"
```

**Não** corrigido nesta etapa (conforme Stage 4.10 / 4.11).

---

## Integridade

| Item | Valor |
| ---- | ----- |
| Exercícios publicados | **136** |
| JPG no asset | **272** |
| SHA-256 pack | `11134036b42db0ada619016f5fda35ab264ce50f3c7c38c55bd43552e51c22b8` |
| tools JSON == asset JSON | sim |

---

## Git

```text
sem commit
sem push
sem reset
sem clean
sem restore
sem rebase
sem co-author Cursor
```

Alterações **desta** etapa: `.gitignore`, `PROJECT_CONTEXT.md`, `README.md`, remoção de `tmp_diag_tap_settings.py`, este relatório.  
Restantes mudanças do branch (pipeline Stages 3–4.9) permanecem intocadas em reorganização.

---

## Checklist

```text
[x] caches V2.1 ignorados
[x] __pycache__/ e *.pyc ignorados
[x] tmp_diag_tap_settings.py removido (temporário confirmado)
[x] PROJECT_CONTEXT.md sem trecho stale
[x] README.md documenta 136 + 272 JPG + 876/777/99
[x] catálogo 136 inalterado
[x] SHA preservado
[x] imagens inalteradas
[x] teste V1 não alterado
[x] build/testes relevantes OK
[x] relatório 4.11 criado
[x] nenhum commit / push / co-author Cursor
```
