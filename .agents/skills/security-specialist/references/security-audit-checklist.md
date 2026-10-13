# Alkalmazásbiztonsági (AppSec) Audit Ellenőrző Lista

Ez az ellenőrző lista a kódbázis biztonsági felülvizsgálatának lépéseit tartalmazza.

---

## 1. Hálózati és Szerver Biztonság
- [ ] A `WebServer` a `127.0.0.1` (loopback) címhez kötődik, nem hallgatózik nyíltan a helyi hálózaton.
- [ ] Minden REST API végpont ellenőrzi a megengedett HTTP metódust (GET/POST), és 405-ös hibát ad más metódusokra.
- [ ] A szerver válaszai tartalmazzák az alapvető biztonsági fejléceket (`X-Content-Type-Options`, `X-Frame-Options`).
- [ ] A REST API kérések mérete ésszerű korlátok közé van szorítva a memória-túlterheléses DoS ellen.

---

## 2. Bemenet-ellenőrzés és Fájlkezelés
- [ ] Nincs Path Traversal sebezhetőség a városonkénti fájlkezelésben (`data/cities/<cityKey>.json`).
- [ ] A fájlelérési utak ellenőrzése a `Path.normalize()` és `startsWith(baseDir)` metódusokkal történik.
- [ ] Az ár, alapterület és szobaszám bemenetek numerikus tartományellenőrzésen esnek át (nincs negatív érték vagy extrém nagy szám).
- [ ] A perzisztencia fájlok írása atomi módon történik, megakadályozva a fájlsérülést váratlan leálláskor.

---

## 3. Külső Folyamatok és Parancsvégrehajtás
- [ ] A `SidecarCliExecutor` `ProcessBuilder`-t használ argumentumlistával, nem pedig shell összefűzést.
- [ ] A CLI argumentumok (városnév, portál) szigorúan szűrtek és ellenőrzöttek.
- [ ] A folyamatfutás szigorú időkorláthoz kötött (max. 20 másodperc), utána `destroyForcibly()` fut le.

---

## 4. Kliensoldali Biztonság (SPA / DOM)
- [ ] A külső oldalakról lekapart hirdetésszövegek nem kerülnek nyersen `innerHTML`-be tisztítás nélkül.
- [ ] Minden dinamikus mező szövegként (`textContent`) vagy `escapeHtml()` függvényen átfutva renderelődik.
- [ ] A külső linkek `rel="noopener noreferrer"` és `target="_blank"` attribútumokkal nyílnak meg.

---

## 5. Böngésző Integráció és SSRF
- [ ] A `BrowserUtils.openUrls()` csak `http://` és `https://` sémájú linkeket fogad el.
- [ ] A tiltott sémák (`file:`, `javascript:`, `cmd:`, `powershell:`) azonnal elutasításra kerülnek.
- [ ] Az egyszerre megnyitható lapok száma korlátozott (alapértelmezett maximum: 10 lap).
