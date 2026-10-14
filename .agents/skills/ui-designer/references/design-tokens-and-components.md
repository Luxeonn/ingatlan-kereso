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

### 2.4 Rendszernapló Betekintő Modal (Log Viewer)
```html
<div id="logModal" class="modal-backdrop fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4 z-50">
  <div class="modal-box bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-4xl max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
    <div class="modal-header flex items-center justify-between px-6 py-4 border-b border-slate-800">
      <h3 class="text-lg font-bold text-white flex items-center gap-2">
        <span class="text-blue-500">📄</span> Rendszernapló (Application Logs)
      </h3>
      <div class="flex items-center gap-3">
        <select id="logLevelFilter" class="bg-slate-800 text-xs text-slate-300 rounded-lg px-2.5 py-1.5 border border-slate-700">
          <option value="ALL">Minden szint</option>
          <option value="INFO">Csak INFO</option>
          <option value="WARN">Csak WARN</option>
          <option value="ERROR">Csak ERROR</option>
        </select>
        <button id="btnCopyLogs" class="btn-sm bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition">
          📋 Másolás
        </button>
        <button class="modal-close text-slate-400 hover:text-white text-xl">✕</button>
      </div>
    </div>
    <div class="modal-body p-4 flex-1 overflow-y-auto font-mono text-xs bg-slate-950/80 text-slate-300 space-y-1">
      <!-- Napló sorok színes badgekkel -->
    </div>
  </div>
</div>
```

### 2.5 Dinamikus M² Rendezés és Kliensoldali Tabnyitás
```javascript
// Fajlagos négyzetméterár és ár szerinti rendezés
function sortItems(items, sortBy) {
  return [...items].sort((a, b) => {
    if (sortBy === 'price_asc') return (a.priceNumeric || 0) - (b.priceNumeric || 0);
    if (sortBy === 'price_desc') return (b.priceNumeric || 0) - (a.priceNumeric || 0);
    if (sortBy === 'sqm_asc') return (a.pricePerSqm || 0) - (b.pricePerSqm || 0);
    if (sortBy === 'sqm_desc') return (b.pricePerSqm || 0) - (a.pricePerSqm || 0);
    if (sortBy === 'area_desc') return (b.floorArea || 0) - (a.floorArea || 0);
    return new Date(b.discoveredAt || 0) - new Date(a.discoveredAt || 0);
  });
}

// Zero-Server-Desktop-Call böngésző tabnyitás
function openUrlsInBrowser(urls, maxTabs = 10) {
  const safeUrls = urls.filter(u => u.startsWith('http://') || u.startsWith('https://')).slice(0, maxTabs);
  safeUrls.forEach(url => window.open(url, '_blank', 'noopener,noreferrer'));
}
```
