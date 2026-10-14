---
name: workflow-engineer
description: >-
  GitHub Actions és CI/CD automatizálási mérnök és workflow specialista. Használd ezt a skillt
  professzionális, biztonságos, optimalizált és megbízható GitHub Actions munkafolyamatok (.github/workflows/),
  automatizált teszt- és release-folyamatok, CI kapuk, PR guardok, ütemezett botok vagy egyedi
  composite actionök tervezéséhez és implementálásához.
metadata:
  icon: terminal
---

# Workflow Engineer & GitHub Actions Specialista Skill

Ez a skill a professzionális **GitHub Actions CI/CD munkafolyamatok**, automatizált ellenőrző kapuk, konténer-publikálási pipeline-ok és biztonsági mechanizmusok tervezési és implementációs irányelveit rögzíti.

---

## 1. Alapelvek és Professzionális Tervezési Szabályok

### 1.1 Biztonság és Jogosultságkezelés (Least Privilege Principle)
* **Explicit `permissions` blokk:** Minden workflow-ban és job-ban kötelezően deklaráld a minimálisan szükséges jogosultságokat (pl. `permissions: contents: read`, `packages: write`, `pull-requests: write`). Soha ne hagyatkozz a repó alapértelmezett, túl tág engedélyeire!
* **Shell Injekció Megelőzése:** Soha ne használj közvetlen `${{ github.event... }}` behelyettesítést a `run:` blokkok szkriptjeiben!
  - ❌ **Rossz (Sérülékeny):**
    ```yaml
    run: echo "PR cím: ${{ github.event.pull_request.title }}"
    ```
  - ✅ **Helyes (Biztonságos):**
    ```yaml
    env:
      PR_TITLE: ${{ github.event.pull_request.title }}
    run: echo "PR cím: $PR_TITLE"
    ```
* **Titkok és Tokenek védelme:** Érzékeny adatokat és tokeneket (`GITHUB_TOKEN`) kizárólag a `secrets.*` kontextusból és környezeti változóként adj át.

### 1.2 Teljesítmény és Költséghatékonyság
* **Concurrency és Cancel-in-Progress:** Új commit pusholásakor a korábbi, még futó felesleges futásokat azonnal állítsd le:
  ```yaml
  concurrency:
    group: ${{ github.workflow }}-${{ github.ref }}
    cancel-in-progress: true
  ```
* **Intelligens Gyorstárazás (Caching):**
  - Maven: `actions/setup-java` beépített `cache: maven`.
  - Docker Buildx: `cache-from: type=gha` és `cache-to: type=gha,mode=max`.
* **Időkorlátok (Timeouts):** Minden job-hoz rendelj hozzá explicit `timeout-minutes` értéket (alapértelmezetten max. 10-15 perc), megakadályozva a beragadó végtelen ciklusokat és action-perc pazarlást.

### 1.3 Megfigyelhetőség és Lépés-összefoglalók ($GITHUB_STEP_SUMMARY)
Minden jelentős workflow tartalmazzon formázott Markdown jelentést a `$GITHUB_STEP_SUMMARY` fájlba írva:
- Sikeres és sikertelen lépések összefoglalása táblázatban.
- Publikált Docker image tagek és sha256 digestek.
- JaCoCo tesztlefedettségi mutatók.
- Érintett komponensek és jóváhagyási kapuk állapota.

---

## 2. Kulcs Munkafolyamat Típusok és Felépítésük

### 2.1 PR Guard & Minőségbiztosítási Kapu (`pr-guard.yml`)
* **Trigger:** `pull_request` (opened, edited, synchronize, reopened) `main` és `develop` célágakra.
* **Feladatok:**
  1. Conventional Commits PR cím ellenőrzés (`feat:`, `fix:`, stb.).
  2. Forráság elnevezés szabályossága (`feature/*`, `fix/*`, `chore/*`).
  3. PR leírás minimális tartalmi hossza és issue lezárási kulcsszavak (`Closes #<id>`).
  4. **Thematic Agent Approval Gate:** A git diff alapján azonosítja a módosított területeket, és megköveteli a megfelelő AI szakértői ágens jóváhagyását a PR leírásában.

### 2.2 CI Építés és Teszt Pipeline (`ci.yml`)
* **Trigger:** `push` és `pull_request` események.
* **Feladatok:**
  1. JDK 21 beállítása és Maven cache.
  2. Párhuzamos fordítás és egységtesztek (`mvn test package`).
  3. JaCoCo lefedettség ellenőrzése.
  4. Összefoglaló generálása a GitHub Actions felületén.

### 2.3 Konténer Build & GHCR Publish (`docker-publish.yml`)
* **Trigger:** `push` a `develop` ágra, release tag (`v*.*.*`), vagy `workflow_dispatch`.
* **Feladatok:**
  1. Bejelentkezés a GitHub Container Registry-be (`ghcr.io`) `GITHUB_TOKEN`-nel (`packages: write`).
  2. Docker Buildx beállítása és GHA réteggyorsítótárazás.
  3. Szigorú tagelési stratégia:
     - `develop` ágról: `ghcr.io/<repo>:latest` (vagy `:dev`).
     - Release tag vagy fix verzió: `ghcr.io/<repo>:<version>` és `:<major>.<minor>`.
     - Explicit védelem a lebegő `latest` tag felülírására release-eknél.
  4. Részletes összefoglaló a `$GITHUB_STEP_SUMMARY`-ban az image digesttel és futási adatokkal.

A részletes, azonnal használható munkafolyamat sablonokat a [Workflow Sablonok Referenciájában](./references/workflow-templates.md) találod.

---

## 3. Workflow Mérnök Ellenőrző Lista (Engineer's Checklist)

- [ ] **Biztonságos bemenet:** Minden dinamikus változó (`github.event.*`) `env:` változón keresztül érkezik, shell injection kizárva?
- [ ] **Minimális jogosultság:** A `permissions` blokk explicit meg van adva (pl. `contents: read`, `packages: write`)?
- [ ] **Concurrency védelem:** Be van állítva a `cancel-in-progress: true`?
- [ ] **Időkorlát:** Minden job rendelkezik `timeout-minutes` beállítással?
- [ ] **Gyorstárazás:** A Maven és Docker Buildx rétegek cache-elve vannak?
- [ ] **Docker verziózás:** Szigorú tagelés történik, elkerülve a lebegő latest tag nem szándékolt felülírását?
- [ ] **Tiszta kimenet:** A workflow hibája esetén az ok egyértelműen megjelenik a `$GITHUB_STEP_SUMMARY`-ban?
