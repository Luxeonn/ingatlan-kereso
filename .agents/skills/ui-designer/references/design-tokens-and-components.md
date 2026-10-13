# UI Design Tokenek és Komponens Specifikáció

Ez a dokumentum a `WebDashboardHtml.java` beágyazott Single Page Application felületének CSS változóit, típusait és HTML komponens mintáit rögzíti.

---

## 1. CSS Változók és Tokenek

```css
:root {
  /* Színpaletta */
  --bg-primary: #0f172a;       /* slate-900 */
  --bg-surface: #1e293b;       /* slate-800 */
  --bg-surface-hover: #334155; /* slate-700 */
  --text-primary: #f8fafc;     /* slate-50 */
  --text-secondary: #94a3b8;   /* slate-400 */
  --text-muted: #64748b;       /* slate-500 */
  
  --brand-primary: #3b82f6;    /* blue-500 */
  --brand-primary-hover: #2563eb;
  --accent-success: #10b981;   /* emerald-500 */
  --accent-warning: #f59e0b;   /* amber-500 */
  --accent-danger: #ef4444;    /* rose-500 */
  
  /* Rácsszerkezet és Távolságok */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-full: 9999px;
  
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.2);
  --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.3);
  --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.4);
  
  /* Animációk */
  --transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 250ms cubic-bezier(0.4, 0, 0.2, 1);
}
```

---

## 2. Kulcskomponensek HTML/CSS Mintái

### 2.1 "ÚJ TALÁLAT" Pulzáló Badge
```html
<span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 animate-pulse">
  <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
  ÚJ TALÁLAT
</span>
```

### 2.2 Numerikus Stepper Vezérlő
```html
<div class="stepper-control flex items-center bg-slate-800 border border-slate-700 rounded-lg overflow-hidden">
  <button type="button" class="btn-step px-3 py-1.5 bg-slate-700 text-slate-200 hover:bg-slate-600 transition" aria-label="Csökkentés">-</button>
  <input type="number" class="w-16 text-center bg-transparent text-white font-semibold focus:outline-none" value="50" min="0" step="5">
  <button type="button" class="btn-step px-3 py-1.5 bg-slate-700 text-slate-200 hover:bg-slate-600 transition" aria-label="Növelés">+</button>
</div>
```

### 2.3 Shimmer Loading Skeleton Kártya
```html
<div class="skeleton-card bg-slate-800/60 rounded-xl p-5 border border-slate-700/50 space-y-4 animate-pulse">
  <div class="flex justify-between items-center">
    <div class="h-5 w-24 bg-slate-700 rounded-full"></div>
    <div class="h-6 w-20 bg-slate-700 rounded-md"></div>
  </div>
  <div class="h-4 w-3/4 bg-slate-700 rounded"></div>
  <div class="grid grid-cols-3 gap-2 pt-2">
    <div class="h-8 bg-slate-700/50 rounded"></div>
    <div class="h-8 bg-slate-700/50 rounded"></div>
    <div class="h-8 bg-slate-700/50 rounded"></div>
  </div>
</div>
```
