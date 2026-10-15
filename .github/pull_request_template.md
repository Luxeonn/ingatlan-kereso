### 🛡️ Thematic Agent Approval Gate

> [!IMPORTANT]
> A [GEMINI.md](file:///e:/github/ingatlan-kereso/gemini.md) 6. fejezete és a `.github/workflows/pr-guard.yml` előírja a kijelölt szakértői jóváhagyást. Az ágensek nem hagyhatják üresen az eredményt: kötelező **pontosan 1 tömör mondatban megindokolni**, hogy az adott szakértői terület miért lett **`[OK]`**, **`[NOK]`** vagy **`[N/A]`**!

- [ ] Architecture (`deep-architect`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] UI/UX (`ui-designer`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] Security (`security-specialist`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] Database (`database-specialist`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] Workflow/CI (`workflow-engineer`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] DevOps (`devops-specialist`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] Scraper Reliability (`scraper-reliability-engineer`): [OK / NOK / N/A] – [1 mondatos indoklás]
- [ ] QA/Testing (`qa-test-engineer`): [OK / NOK / N/A] – [1 mondatos indoklás]

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
- [ ] 🟢 **Minden CI státusz zöld:** Szigorúan CSAK AKKOR mergelhető, ha a Java CI és a PR Guard is 100%-ban zöld!

