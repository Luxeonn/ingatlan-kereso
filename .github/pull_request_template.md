## Cél és kontextus (Context & Goal)

<!-- Foglald össze tömören és érthetően, hogy miért készült ez a módosítás, és milyen problémát vagy fejlesztési igényt old meg. -->

---

## Részletes Technikai Változtatások

<!-- Részletezd a változtatásokat komponens- és fájlszinten:
1. Mely osztályok, modulok, interfészek módosultak és miért?
2. Milyen tervezési minták, új függvények vagy adatmodellek kerültek bevezetésre? -->

---

## Kapcsolódó Hibajegyek (Related Issues)

<!-- Használj automatikusan záró kulcsszavakat (pl. Closes #123, Fixes #456). 
Ha csak hivatkozol egy megbeszélésre: Related to #123 -->
- Closes #

---

## Felhasználói és Működési Hatás

<!-- Mit tapasztal a végfelhasználó a felületen vagy a működésben?
Változik-e az adatbázis formátum, a keresési viselkedés vagy a CLI paraméterezés? -->

---

## Verifikáció és Tesztelés (How to Validate)

<!-- Részletezd az elvégzett automatizált és manuális ellenőrzéseket:
- [ ] Maven tesztek lefutása: `mvn test` (sikeres lefutás, 0 hiba)
- [ ] Manuális ellenőrzés / füstteszt lépései
- [ ] JaCoCo tesztlefedettségi eredmények -->

---

### 🛡️ Thematic Agent Approval Gate

> [!IMPORTANT]
> A [GEMINI.md](file:///e:/github/ingatlan-kereso/gemini.md) 6. fejezete és a `.github/workflows/pr-guard.yml` előírja, hogy a PR-ban érintett szakterületek kijelölt AI szakértőjének jóvá kell hagynia a módosítást a merge-eléshez. Jelöld be az érintett jóváhagyásokat pontosan **1 mondatos indoklással** alátámasztva, vagy jelöld `N/A`-val a nem érintett témákat!

- [ ] Architecture Review: Approved by Deep Architect Agent (`deep-architect`) – [1 mondatos indoklás]
- [ ] UI/UX Review: Approved by UI Designer Agent (`ui-designer`) – [1 mondatos indoklás]
- [ ] Security Review: Approved by Security Specialist Agent (`security-specialist`) – [1 mondatos indoklás]
- [ ] Database Review: Approved by Database Specialist Agent (`database-specialist`) – [1 mondatos indoklás]
- [ ] Workflow/CI Review: Approved by Workflow Engineer Agent (`workflow-engineer`) – [1 mondatos indoklás]

---

## Leadás Előtti Ellenőrző Lista (Pre-Submission Checklist)

- [ ] A PR címe követi a Conventional Commits szabványt (`feat: ...`, `fix: ...`, `chore: ...`).
- [ ] A forráság neve követi az előírt formátumot (`feature/...`, `fix/...`, `chore/...`).
- [ ] A forráság a `develop` ágból indul ki, és oda is céloz.
- [ ] Minden változtatás új, leíró üzenetű commitban van rögzítve (nincs elnyomott commit).
- [ ] Megfelelő GitHub címke hozzáadva a PR-hoz (`enhancement`, `bug`, `chore`, `documentation`).
