# Accessibility Guide

This document establishes the accessibility requirements and implementation guidelines for the **ingatlan-kereso** React 19 frontend, targeting **WCAG 2.2 Level AA** compliance.

> [!NOTE]
> Accessibility is a frontend concern for this project. The headless Spring Boot REST API has no HTML surface and is therefore out of scope.

---

## 1. Standard & Compliance Target

All UI components must conform to **[WCAG 2.2 Level AA](https://www.w3.org/TR/WCAG22/)** success criteria. Key principle areas:

| Principle | Examples |
| :--- | :--- |
| **Perceivable** | Text alternatives, colour contrast ≥ 4.5:1, no content lost on zoom |
| **Operable** | Full keyboard navigability, no keyboard trap, visible focus indicator |
| **Understandable** | Clear labels, consistent navigation, error identification |
| **Robust** | Valid semantics, ARIA used correctly, screen-reader compatible markup |

---

## 2. React 19 Component Guidelines

### 2.1 Semantic HTML First

Prefer native HTML elements over custom ARIA widgets. Browsers provide built-in keyboard handling and screen-reader support for semantic elements.

```tsx
// ✅ Prefer semantic elements
<button onClick={handleSearch}>Search</button>
<nav aria-label="Main navigation">…</nav>
<main>…</main>

// ❌ Avoid ARIA workarounds for native elements
<div role="button" tabIndex={0} onClick={handleSearch}>Search</div>
```

### 2.2 ARIA Labels & Descriptions

Provide accessible names for all interactive controls and landmark regions that lack visible text labels.

```tsx
// Search input with a visible label
<label htmlFor="city-input">City</label>
<input
  id="city-input"
  type="text"
  value={city}
  onChange={(e) => setCity(e.target.value)}
  aria-describedby="city-hint"
/>
<span id="city-hint">Enter a Hungarian city name, e.g. Budapest</span>

// Icon-only button — must have an accessible name
<button aria-label="Clear search">
  <XIcon aria-hidden="true" />
</button>

// Live region for dynamic search results
<div aria-live="polite" aria-atomic="true">
  {isLoading ? 'Searching…' : `${results.length} listings found`}
</div>
```

### 2.3 Keyboard Navigation

Every interactive element must be reachable and operable via keyboard alone.

- **Tab / Shift+Tab** must traverse all controls in a logical document order.
- **Enter / Space** must activate buttons and links.
- **Escape** must close modals, dropdowns, and overlays.
- **Arrow keys** must navigate within composite widgets (e.g., filter dropdowns, tab panels).

```tsx
// Custom dropdown — handle keyboard events explicitly
<div
  role="listbox"
  aria-label="Property type"
  onKeyDown={(e) => {
    if (e.key === 'ArrowDown') focusNextOption();
    if (e.key === 'ArrowUp') focusPrevOption();
    if (e.key === 'Escape') closeDropdown();
  }}
>
  {options.map((opt) => (
    <div key={opt.value} role="option" aria-selected={opt.value === selected}>
      {opt.label}
    </div>
  ))}
</div>
```

### 2.4 Focus Management

- Maintain a visible focus indicator at all times. Never remove the outline without providing an equally visible alternative.
- When a modal or overlay opens, move focus to the first interactive element inside it.
- When a modal closes, return focus to the trigger element.

```tsx
// Move focus to modal on open
useEffect(() => {
  if (isOpen) {
    modalRef.current?.focus();
  }
}, [isOpen]);

<dialog ref={modalRef} open={isOpen} aria-labelledby="modal-title">
  <h2 id="modal-title">Filter Options</h2>
  …
</dialog>
```

```css
/* Tailwind v4 — ensure focus ring is always visible */
.interactive:focus-visible {
  @apply outline-2 outline-offset-2 outline-blue-600;
}
```

### 2.5 Images & Icons

- Decorative images and icons must have `aria-hidden="true"` and an empty `alt=""` (or no `alt`).
- Informative images must have a descriptive `alt` text.

```tsx
// Decorative icon
<HouseIcon aria-hidden="true" className="text-gray-500" />

// Informative image (e.g., portal logo)
<img src="/logos/dh.png" alt="Duna House" width={80} height={24} />
```

### 2.6 Colour Contrast (Tailwind CSS v4)

Text and interactive elements must meet the WCAG 2.2 AA contrast ratio requirements:

| Element | Minimum contrast ratio |
| :--- | :--- |
| Normal text (< 18 pt / < 14 pt bold) | **4.5 : 1** |
| Large text (≥ 18 pt or ≥ 14 pt bold) | **3 : 1** |
| UI components & focus indicators | **3 : 1** |

When choosing Tailwind colour pairs, verify contrast with a tool such as [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/). Avoid low-contrast combinations like `text-gray-400 bg-white` for body text.

### 2.7 Form Accessibility

The search bar, city selector, price range inputs, and floor area filters must be fully accessible:

```tsx
<form role="search" aria-label="Property search">
  <fieldset>
    <legend>Price range (million HUF)</legend>

    <label htmlFor="price-min">Minimum</label>
    <input
      id="price-min"
      type="number"
      min={0}
      aria-describedby="price-min-error"
      aria-invalid={!!errors.priceMin}
    />
    {errors.priceMin && (
      <span id="price-min-error" role="alert">
        {errors.priceMin}
      </span>
    )}

    <label htmlFor="price-max">Maximum</label>
    <input id="price-max" type="number" min={0} />
  </fieldset>
</form>
```

Key rules:
- Every `<input>` and `<select>` must have an associated `<label>` via `htmlFor` / `id`.
- Use `<fieldset>` + `<legend>` to group related controls.
- Use `aria-invalid` and a linked `role="alert"` element for validation errors.
- Use `aria-required="true"` on mandatory fields.

### 2.8 Dynamic Content & Loading States

Use ARIA live regions to communicate asynchronous state changes (search in progress, results loaded, errors).

```tsx
// Announce search status to screen readers
<div aria-live="polite" className="sr-only">
  {status === 'loading' && 'Searching portals, please wait…'}
  {status === 'success' && `Search complete. ${count} listings found.`}
  {status === 'error' && 'Search failed. Please try again.'}
</div>
```

Visually-hidden utility class (for screen-reader-only announcements):

```css
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
```

---

## 3. Portal Status Cards

Each portal status card (Duna House, Otthon Centrum, etc.) must convey status information through more than just colour:

```tsx
<article aria-label={`${portal.name} — status: ${portal.status}`}>
  <span aria-hidden="true" className={badgeClass}>{portal.status}</span>
  {/* Status is also available as text content for screen readers */}
  <span className="sr-only">{portal.status}</span>
  <span>{portal.name}</span>
  <span>{portal.count} listings</span>
</article>
```

---

## 4. Resources

- [WCAG 2.2 Quick Reference](https://www.w3.org/WAI/WCAG22/quickref/)
- [MDN ARIA Authoring Practices](https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Roles)
- [ARIA APG (Patterns)](https://www.w3.org/WAI/ARIA/apg/patterns/)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
