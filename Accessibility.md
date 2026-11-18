# Akadálymentességi útmutató (WCAG 2.2 AA)

Ez az útmutató a React 19 + Tailwind CSS v4 alapú webes felület gyakorlati **WCAG 2.2 AA** követelményeit foglalja össze. A headless Spring Boot REST API nem szolgál ki HTML felületet.

## 1. Gyakorlati WCAG 2.2 AA ellenőrzőlista

| Terület | Kötelező elvárás a felületen |
| :--- | :--- |
| **Szemantikus HTML** | Natív `<button>`, `<nav>`, `<main>`, `<form>` elemek használata kattintható `<div>` helyett. |
| **Kontraszt és szín** | Normál szöveg ≥ `4.5:1`, nagy szöveg / UI keret / fókuszgyűrű ≥ `3:1` (világos és sötét témában is). Állapotot soha ne csak szín jelezzen (ikon + szöveg együtt). |
| **Billentyűzet és fókusz** | Minden vezérlő elérhető `Tab` / `Shift+Tab` billentyűkkel, `Enter`/`Space` aktivál, `Escape` zárja a modálokat és popovereket (`useOutsideDismiss`). A `focus-visible` körvonal maradjon jól látható. |
| **Űrlapok és ikonok** | Minden `<input>`/`<select>` rendelkezzen `<label htmlFor>` vagy `aria-label` névvel és hiba esetén `aria-invalid`/`role="alert"` jelzéssel. Dekoratív Lucide ikonokon `aria-hidden="true"`, ikon-gombokon kötelező `aria-label`. |
| **Élő régiók** | Hibákhoz `role="alert"`, értesítésekhez és részleges portáljelzéshez `role="status"` és `aria-live="polite"`. |

## 2. React 19 + Tailwind CSS v4 kódpéldák

### Navigációs fülek és ikon-gomb (`aria-pressed`, `aria-hidden`)

```tsx
<nav aria-label="Fő nézetek" className="flex items-center gap-2">
  <button type="button" aria-pressed={tab === 'listings'}>
    Találatok
  </button>
</nav>

<button
  type="button"
  aria-label="Bezárás"
  className="rounded-lg p-1.5 focus-visible:outline-2 focus-visible:outline-blue-500"
>
  <X className="h-4 w-4" aria-hidden="true" />
</button>
```

### Modál (`useOutsideDismiss`, `role="dialog"`, `aria-modal`)

```tsx
const modalRef = useRef<HTMLDivElement>(null)
useOutsideDismiss(modalRef, isOpen, onClose, true)

<div ref={modalRef} role="dialog" aria-modal="true" aria-label="Ingatlan részletei">
  …
</div>
```

### Űrlapmező, hibajelzés, élő toast és portál állapotkártya (`PortalStatusDto`)

```tsx
<label htmlFor="min-price">Minimális ár (M Ft)</label>
<input id="min-price" type="number" aria-invalid={Boolean(error)} />

{error && <div role="alert">Hiba történt a keresés során.</div>}
<aside role="status" aria-live="polite">{toastMessage}</aside>

<article aria-label={`${p.portalName} — állapot: ${p.status}`}>
  <CheckCircle className="h-5 w-5 text-emerald-400" aria-hidden="true" />
  <span>{p.portalName}</span>
  <span>{p.foundCount} találat ({p.status})</span>
</article>
```

## 3. Ellenőrzési eszközök

- [WCAG 2.2 Quick Reference](https://www.w3.org/WAI/WCAG22/quickref/)
- [WAI-ARIA Authoring Practices Guide (APG)](https://www.w3.org/WAI/ARIA/apg/patterns/)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
