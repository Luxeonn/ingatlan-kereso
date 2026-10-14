### 🛡️ Thematic Agent Approval Gate

> [!IMPORTANT]
> A [GEMINI.md](file:///e:/github/ingatlan-kereso/gemini.md) 6. fejezete és a `.github/workflows/pr-guard.yml` előírja a kijelölt szakértői jóváhagyást. A státusz `OK` (ha megfelelt) vagy `N/A` (ha nem érintett). Kizárólag nem megfelelő (`NOK`) értékelés esetén kötelező a rövid, 1 mondatos indoklás!

- [ ] Architecture (`deep-architect`): OK
- [ ] UI/UX (`ui-designer`): OK
- [ ] Security (`security-specialist`): OK
- [ ] Database (`database-specialist`): OK
- [ ] Workflow/CI (`workflow-engineer`): OK

---

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

## Leadás Előtti Ellenőrző Lista (Pre-Submission Checklist)

- [ ] A PR címe követi a Conventional Commits szabványt (`feat: ...`, `fix: ...`, `chore: ...`).
- [ ] A forráság neve követi az előírt formátumot (`feature/...`, `fix/...`, `chore/...`).
- [ ] A forráság a `develop` ágból indul ki, és oda is céloz.
- [ ] Minden változtatás új, leíró üzenetű commitban van rögzítve (nincs elnyomott commit).
- [ ] Megfelelő GitHub címke hozzáadva a PR-hoz (`enhancement`, `bug`, `chore`, `documentation`).
