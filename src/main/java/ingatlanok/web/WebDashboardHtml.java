package ingatlanok.web;

/**
 * Modern, reszponzív Web Dashboard HTML, CSS és JavaScript sablon.
 */
public class WebDashboardHtml {

    public static String getHtml(int port) {
        return """
<!DOCTYPE html>
<html lang="hu">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ingatlan Kereső Dashboard</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-primary: #0f172a;
            --bg-secondary: #1e293b;
            --bg-card: #1e293b;
            --bg-input: #334155;
            --border-color: #334155;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --accent: #3b82f6;
            --accent-hover: #2563eb;
            --accent-gradient: linear-gradient(135deg, #3b82f6, #8b5cf6);
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
            --card-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3), 0 8px 10px -6px rgba(0, 0, 0, 0.3);
        }

        body.light-theme {
            --bg-primary: #f1f5f9;
            --bg-secondary: #ffffff;
            --bg-card: #ffffff;
            --bg-input: #f8fafc;
            --border-color: #e2e8f0;
            --text-primary: #0f172a;
            --text-secondary: #64748b;
            --accent: #2563eb;
            --accent-hover: #1d4ed8;
            --accent-gradient: linear-gradient(135deg, #2563eb, #7c3aed);
            --card-shadow: 0 10px 20px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Plus Jakarta Sans', system-ui, -apple-system, sans-serif;
            transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease;
        }

        body {
            background-color: var(--bg-primary);
            color: var(--text-primary);
            min-height: 100vh;
            padding-bottom: 40px;
        }

        .container {
            max-width: 1240px;
            margin: 0 auto;
            padding: 0 20px;
        }

        /* Header */
        header {
            background-color: var(--bg-secondary);
            border-bottom: 1px solid var(--border-color);
            padding: 16px 0;
            position: sticky;
            top: 0;
            z-index: 100;
            backdrop-filter: blur(12px);
        }

        .header-content {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .brand {
            display: flex;
            align-items: center;
            gap: 12px;
            text-decoration: none;
            color: var(--text-primary);
        }

        .brand-icon {
            font-size: 28px;
            background: var(--accent-gradient);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .brand-text h1 {
            font-size: 20px;
            font-weight: 800;
            letter-spacing: -0.5px;
        }

        .brand-text p {
            font-size: 12px;
            color: var(--text-secondary);
            font-weight: 500;
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 12px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            background: rgba(16, 185, 129, 0.15);
            color: #10b981;
            border: 1px solid rgba(16, 185, 129, 0.3);
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background-color: #10b981;
            box-shadow: 0 0 8px #10b981;
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.5; transform: scale(0.9); }
        }

        .theme-toggle-btn {
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 8px 12px;
            border-radius: 10px;
            cursor: pointer;
            font-size: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .theme-toggle-btn:hover {
            border-color: var(--accent);
        }

        /* Filter Panel */
        .filter-card {
            background-color: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 24px;
            margin-top: 24px;
            box-shadow: var(--card-shadow);
        }

        .filter-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }

        .filter-title {
            font-size: 18px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .grid-filters {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
            margin-bottom: 20px;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }

        .form-group label {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-secondary);
        }

        .form-control {
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 10px 14px;
            border-radius: 10px;
            font-size: 14px;
            outline: none;
        }
        .form-control:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.25);
        }

        /* Rejtsük el a csúnya böngészős natív number nyilakat */
        input[type="number"]::-webkit-outer-spin-button,
        input[type="number"]::-webkit-inner-spin-button {
            -webkit-appearance: none;
            margin: 0;
        }
        input[type="number"] {
            -moz-appearance: textfield;
            appearance: textfield;
        }

        .price-range-inputs {
            display: flex;
            gap: 8px;
            align-items: center;
        }
        .stepper-box {
            position: relative;
            display: flex;
            align-items: center;
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            flex: 1;
            min-width: 0;
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }
        .stepper-box:focus-within {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.25);
        }
        .stepper-box .stepper-input {
            background: transparent;
            border: none;
            color: var(--text-primary);
            padding: 10px 32px 10px 14px;
            font-size: 14px;
            outline: none;
            width: 100%;
            min-width: 0;
        }
        .stepper-arrows {
            position: absolute;
            right: 4px;
            top: 4px;
            bottom: 4px;
            display: flex;
            flex-direction: column;
            width: 22px;
            justify-content: center;
            gap: 2px;
        }
        .stepper-arrow-btn {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 4px;
            color: var(--text-secondary);
            font-size: 8px;
            height: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            padding: 0;
            line-height: 1;
            transition: all 0.15s ease;
            user-select: none;
        }
        .stepper-arrow-btn:hover {
            background: var(--accent);
            color: white;
            border-color: var(--accent);
        }
        .price-range-separator {
            color: var(--text-secondary);
            font-weight: 700;
            user-select: none;
        }

        .portals-filter-section {
            padding-top: 16px;
            border-top: 1px dashed var(--border-color);
            margin-bottom: 20px;
        }

        .form-section-label {
            font-size: 13px;
            font-weight: 700;
            color: var(--text-secondary);
            display: flex;
            align-items: center;
            gap: 6px;
            margin-bottom: 10px;
        }

        .portal-checkboxes {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
        }

        .portal-chip {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 14px;
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            cursor: pointer;
            font-size: 13px;
            font-weight: 600;
            user-select: none;
        }
        .portal-chip input {
            accent-color: var(--accent);
            cursor: pointer;
        }

        .btn-star {
            background: transparent;
            border: 1px solid transparent;
            font-size: 19px;
            cursor: pointer;
            color: var(--text-secondary);
            line-height: 1;
            padding: 4px 6px;
            border-radius: 8px;
            transition: transform 0.15s ease, color 0.15s ease, background 0.15s ease;
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }
        .btn-star:hover {
            transform: scale(1.15);
            color: #facc15;
            background: rgba(250, 204, 21, 0.1);
        }
        .btn-star.active {
            color: #facc15;
            text-shadow: 0 0 10px rgba(250, 204, 21, 0.5);
        }

        .result-portal-filters {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
            align-items: center;
        }
        .portal-filter-btn {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            padding: 7px 13px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        .portal-filter-btn:hover {
            border-color: var(--accent);
            color: var(--text-primary);
        }
        .portal-filter-btn.active {
            background: var(--accent);
            color: white;
            border-color: var(--accent);
        }

        .filter-only-new-btn {
            background: rgba(16, 185, 129, 0.1);
            border: 1px solid rgba(16, 185, 129, 0.35);
            color: #10b981;
            padding: 7px 14px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 700;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s ease;
        }
        .filter-only-new-btn:hover {
            background: rgba(16, 185, 129, 0.2);
            border-color: #10b981;
            color: #10b981;
            transform: translateY(-1px);
        }
        .filter-only-new-btn.active {
            background: #10b981;
            border-color: #10b981;
            color: white;
            box-shadow: 0 0 12px rgba(16, 185, 129, 0.4);
        }
        .filter-only-new-btn.disabled-btn {
            opacity: 0.45;
            cursor: pointer;
        }

        .action-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
            padding-top: 16px;
            border-top: 1px solid var(--border-color);
        }

        .toggle-switch-group {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 13px;
            font-weight: 500;
            color: var(--text-secondary);
            cursor: pointer;
        }
        .toggle-switch-group input {
            accent-color: var(--accent);
            width: 18px;
            height: 18px;
        }

        .btn-search {
            background: var(--accent-gradient);
            color: white;
            border: none;
            padding: 12px 28px;
            border-radius: 12px;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 10px;
            box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
        }
        .btn-search:hover:not(:disabled) {
            transform: translateY(-1px);
            box-shadow: 0 6px 20px rgba(59, 130, 246, 0.6);
        }
        .btn-search:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }

        /* Stats & Tab bar */
        .controls-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
            margin-top: 32px;
            margin-bottom: 16px;
        }

        .tabs {
            display: flex;
            background: var(--bg-card);
            padding: 4px;
            border-radius: 12px;
            border: 1px solid var(--border-color);
        }

        .tab-btn {
            background: transparent;
            border: none;
            color: var(--text-secondary);
            padding: 8px 18px;
            border-radius: 8px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
        }
        .tab-btn.active {
            background: var(--accent);
            color: white;
        }

        .search-box {
            position: relative;
            min-width: 260px;
        }
        .search-box input {
            width: 100%;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            padding: 10px 14px 10px 36px;
            border-radius: 10px;
            color: var(--text-primary);
            font-size: 13px;
            outline: none;
        }
        .search-box input:focus {
            border-color: var(--accent);
        }
        .search-box .icon {
            position: absolute;
            left: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-secondary);
            font-size: 14px;
        }

        .quick-actions {
            display: flex;
            gap: 10px;
        }

        .btn-secondary {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 8px 16px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        .btn-secondary:hover {
            border-color: var(--accent);
            color: var(--accent);
        }

        /* Result Cards Grid */
        .listings-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
            gap: 20px;
            margin-top: 16px;
        }

        .listing-card {
            background-color: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 20px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            box-shadow: var(--card-shadow);
            overflow: hidden;
        }
        .listing-card.is-new {
            border-color: rgba(16, 185, 129, 0.6);
            box-shadow: 0 0 15px rgba(16, 185, 129, 0.15);
        }

        .card-top {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 12px;
        }

        .portal-badge {
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .portal-badge.dh { background: #1e3a8a; color: #93c5fd; }
        .portal-badge.oc { background: #831843; color: #fbcfe8; }
        .portal-badge.jf { background: #7c2d12; color: #fed7aa; }
        .portal-badge.ic { background: #9a3412; color: #ffedd5; }

        .badge-new {
            background: rgba(16, 185, 129, 0.2);
            color: #10b981;
            border: 1px solid rgba(16, 185, 129, 0.4);
            padding: 3px 8px;
            border-radius: 6px;
            font-size: 11px;
            font-weight: 800;
            animation: pulse 2s infinite;
        }

        .card-title {
            font-size: 16px;
            font-weight: 700;
            line-height: 1.4;
            margin-bottom: 8px;
            color: var(--text-primary);
        }

        .card-details {
            font-size: 13px;
            color: var(--text-secondary);
            margin-bottom: 16px;
            line-height: 1.5;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }

        .card-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding-top: 14px;
            border-top: 1px solid var(--border-color);
        }

        .card-price {
            font-size: 17px;
            font-weight: 800;
            color: #10b981;
        }

        .btn-view {
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 6px 14px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 700;
            text-decoration: none;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .btn-view:hover {
            background: var(--accent);
            color: white;
            border-color: var(--accent);
        }

        /* Empty State */
        .empty-state {
            text-align: center;
            padding: 60px 20px;
            background: var(--bg-card);
            border: 1px dashed var(--border-color);
            border-radius: 16px;
            margin-top: 20px;
        }
        .empty-state-icon {
            font-size: 48px;
            margin-bottom: 12px;
        }
        .empty-state h3 {
            font-size: 18px;
            margin-bottom: 8px;
        }
        .empty-state p {
            color: var(--text-secondary);
            font-size: 14px;
        }

        /* Toast notifications */
        #toast {
            position: fixed;
            bottom: 24px;
            right: 24px;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 12px 20px;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0,0,0,0.4);
            display: none;
            align-items: center;
            gap: 10px;
            z-index: 1000;
            font-size: 14px;
            font-weight: 600;
        }

        /* Spinner */
        .spinner {
            border: 3px solid rgba(255, 255, 255, 0.3);
            border-top: 3px solid white;
            border-radius: 50%;
            width: 18px;
            height: 18px;
            animation: spin 0.8s linear infinite;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>

    <!-- Header -->
    <header>
        <div class="container header-content">
            <a href="/" class="brand">
                <span class="brand-icon">🏡</span>
                <div class="brand-text">
                    <h1>Ingatlan Kereső</h1>
                    <p>Élő ingatlanfigyelő & scraper rendszer</p>
                </div>
            </a>
            <div class="header-actions">
                <div class="status-badge">
                    <span class="status-dot"></span>
                    <span id="server-status-text">Port: {{PORT}} • Adatbázis: <strong id="db-count">0</strong> db</span>
                </div>
                <button class="theme-toggle-btn" id="theme-btn" title="Téma váltása">🌙</button>
            </div>
        </div>
    </header>

    <main class="container">
        <!-- Filter Card -->
        <section class="filter-card">
            <div class="filter-header">
                <div class="filter-title">
                    <span>⚡ Keresési Paraméterek</span>
                </div>
                <div style="font-size: 13px; color: var(--text-secondary);" id="last-run-info">
                    Még nem futott keresés ebben a munkamenetben.
                </div>
            </div>

            <form id="search-form">
                <div class="grid-filters">
                    <div class="form-group">
                        <label for="city-input">Település / Város</label>
                        <input type="text" id="city-input" class="form-control" value="Debrecen" placeholder="Pl. Debrecen">
                    </div>
                    <div class="form-group">
                        <label for="type-select">Ingatlan jellege</label>
                        <select id="type-select" class="form-control">
                            <option value="lakas" selected>Eladó lakás</option>
                            <option value="haz">Eladó ház</option>
                            <option value="telek">Telek</option>
                            <option value="osszes">Összes típus</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Ársáv (Millió Ft)</label>
                        <div class="price-range-inputs">
                            <div class="stepper-box">
                                <input type="number" id="min-price-input" class="stepper-input" placeholder="Min (M Ft)" min="0" step="1">
                                <div class="stepper-arrows">
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('min-price-input', 1)" title="Növelés">▲</button>
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('min-price-input', -1)" title="Csökkentés">▼</button>
                                </div>
                            </div>
                            <span class="price-range-separator">-</span>
                            <div class="stepper-box">
                                <input type="number" id="max-price-input" class="stepper-input" placeholder="Max (M Ft)" min="0" step="1">
                                <div class="stepper-arrows">
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('max-price-input', 1)" title="Növelés">▲</button>
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('max-price-input', -1)" title="Csökkentés">▼</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="portals-filter-section">
                    <label class="form-section-label">🌐 Aktív weboldalak keresése</label>
                    <div class="portal-checkboxes">
                        <label class="portal-chip">
                            <input type="checkbox" id="portal-dh" value="dh" checked> Duna House
                        </label>
                        <label class="portal-chip">
                            <input type="checkbox" id="portal-oc" value="oc" checked> Otthon Centrum
                        </label>
                        <label class="portal-chip">
                            <input type="checkbox" id="portal-jf" value="jofogas" checked> Jófogás
                        </label>
                        <label class="portal-chip">
                            <input type="checkbox" id="portal-ic" value="ingatlancom" checked> Ingatlan.com
                        </label>
                    </div>
                </div>

                <div class="action-row">
                    <label class="toggle-switch-group">
                        <input type="checkbox" id="auto-open-toggle">
                        <span>Új találatok azonnali megnyitása a böngésző fülein</span>
                    </label>

                    <button type="submit" class="btn-search" id="submit-btn">
                        <span id="btn-text">🔍 Keresés indítása most</span>
                        <div class="spinner" id="btn-spinner" style="display: none;"></div>
                    </button>
                </div>
            </form>
        </section>

        <!-- Controls Bar -->
        <section class="controls-bar">
            <div class="tabs">
                <button class="tab-btn active" id="tab-current" onclick="switchTab('current')">
                    🔥 Aktuális keresés (<span id="current-count">0</span>)
                </button>
                <button class="tab-btn" id="tab-history" onclick="switchTab('history')">
                    📚 Teljes archívum (<span id="history-count">0</span>)
                </button>
                <button class="tab-btn" id="tab-starred" onclick="switchTab('starred')">
                    ⭐ Kedvencek (<span id="starred-count">0</span>)
                </button>
            </div>

            <div class="result-portal-filters">
                <button class="portal-filter-btn active" data-portal="all" onclick="filterByPortal('all')">Összes forrás (<span id="count-portal-all">0</span>)</button>
                <button class="portal-filter-btn" data-portal="dh" onclick="filterByPortal('dh')">Duna House (<span id="count-portal-dh">0</span>)</button>
                <button class="portal-filter-btn" data-portal="oc" onclick="filterByPortal('oc')">Otthon Centrum (<span id="count-portal-oc">0</span>)</button>
                <button class="portal-filter-btn" data-portal="jf" onclick="filterByPortal('jf')">Jófogás (<span id="count-portal-jf">0</span>)</button>
                <button class="portal-filter-btn" data-portal="ic" onclick="filterByPortal('ic')">Ingatlan.com (<span id="count-portal-ic">0</span>)</button>
                <button class="filter-only-new-btn" id="filter-only-new-btn" onclick="toggleOnlyNewFilter()" title="Kizárólag az új hirdetések megjelenítése">
                    ✨ Csak újak (<span id="only-new-count">0</span>)
                </button>
            </div>

            <div class="search-box">
                <span class="icon">🔎</span>
                <input type="text" id="filter-input" placeholder="Gyorskeresés a találatok között...">
            </div>

            <div class="quick-actions">
                <button class="btn-secondary" id="btn-open-all-new" onclick="openAllNew()" style="display: none;">
                    🚀 Összes új megnyitása (<span id="new-badge-count">0</span>)
                </button>
                <button class="btn-secondary" onclick="clearHistoryConfirm()">
                    🗑️ Előzmények törlése
                </button>
            </div>
        </section>

        <!-- Listings Grid -->
        <div id="listings-container" class="listings-grid"></div>

        <!-- Empty State -->
        <div id="empty-state" class="empty-state">
            <div class="empty-state-icon">🔎</div>
            <h3>Még nem indítottál keresést</h3>
            <p>Állítsd be a kívánt várost és kattints a <strong>Keresés indítása most</strong> gombra!</p>
        </div>
    </main>

    <!-- Toast Notification -->
    <div id="toast"></div>

    <script>
        // State
        let currentItems = [];
        let historyItems = [];
        let activeTab = 'current'; // 'current', 'history', 'starred'
        let selectedResultPortal = 'all'; // 'all', 'dh', 'oc', 'jf', 'ic'
        let onlyNewFilter = false;
        let starredUrls = new Set(JSON.parse(localStorage.getItem('starred_urls') || '[]'));

        function updateStarredCount() {
            const starredCountEl = document.getElementById('starred-count');
            if (starredCountEl) starredCountEl.textContent = starredUrls.size;
        }

        function toggleStar(encodedUrl) {
            const url = decodeURIComponent(encodedUrl);
            if (starredUrls.has(url)) {
                starredUrls.delete(url);
                showToast("⭐ Eltávolítva a kedvencek közül");
            } else {
                starredUrls.add(url);
                showToast("⭐ Hozzáadva a kedvencekhez!");
            }
            localStorage.setItem('starred_urls', JSON.stringify(Array.from(starredUrls)));
            updateStarredCount();
            renderListings();
        }

        function toggleOnlyNewFilter() {
            onlyNewFilter = !onlyNewFilter;
            const btn = document.getElementById('filter-only-new-btn');
            if (btn) {
                btn.classList.toggle('active', onlyNewFilter);
            }
            renderListings();
        }

        function stepNumber(inputId, step) {
            const el = document.getElementById(inputId);
            if (!el) return;
            let val = parseInt(el.value, 10);
            if (isNaN(val)) val = 0;
            val = Math.max(0, val + step);
            el.value = val;
        }

        function updatePortalFilterCounts() {
            let list;
            if (activeTab === 'current') {
                list = currentItems;
            } else if (activeTab === 'starred') {
                const allKnown = [...currentItems, ...historyItems];
                const seen = new Set();
                list = allKnown.filter(item => {
                    if (starredUrls.has(item.url) && !seen.has(item.url)) {
                        seen.add(item.url);
                        return true;
                    }
                    return false;
                });
            } else {
                list = historyItems;
            }

            const counts = { all: list.length, dh: 0, oc: 0, jf: 0, ic: 0 };
            let newItemsCount = 0;
            list.forEach(item => {
                if (item.isNew) newItemsCount++;
                const p = (item.portal || '').toLowerCase();
                if (p.includes('duna')) counts.dh++;
                else if (p.includes('otthon')) counts.oc++;
                else if (p.includes('jófogás') || p.includes('jofogas')) counts.jf++;
                else if (p.includes('ingatlan.com')) counts.ic++;
            });

            const countAll = document.getElementById('count-portal-all');
            const countDh = document.getElementById('count-portal-dh');
            const countOc = document.getElementById('count-portal-oc');
            const countJf = document.getElementById('count-portal-jf');
            const countIc = document.getElementById('count-portal-ic');
            const onlyNewCountEl = document.getElementById('only-new-count');
            const onlyNewBtn = document.getElementById('filter-only-new-btn');

            if (countAll) countAll.textContent = counts.all;
            if (countDh) countDh.textContent = counts.dh;
            if (countOc) countOc.textContent = counts.oc;
            if (countJf) countJf.textContent = counts.jf;
            if (countIc) countIc.textContent = counts.ic;
            if (onlyNewCountEl) onlyNewCountEl.textContent = newItemsCount;
            if (onlyNewBtn) onlyNewBtn.classList.toggle('disabled-btn', newItemsCount === 0);

            ['dh', 'oc', 'jf', 'ic'].forEach(k => {
                const btn = document.querySelector(`.portal-filter-btn[data-portal="${k}"]`);
                if (btn) btn.style.opacity = counts[k] === 0 ? '0.45' : '1';
            });
        }

        function filterByPortal(portalKey) {
            selectedResultPortal = portalKey;
            document.querySelectorAll('.portal-filter-btn').forEach(btn => {
                btn.classList.toggle('active', btn.getAttribute('data-portal') === portalKey);
            });
            renderListings();
        }

        // Elements
        const form = document.getElementById('search-form');
        const submitBtn = document.getElementById('submit-btn');
        const btnText = document.getElementById('btn-text');
        const btnSpinner = document.getElementById('btn-spinner');
        const listingsContainer = document.getElementById('listings-container');
        const emptyState = document.getElementById('empty-state');
        const filterInput = document.getElementById('filter-input');
        const lastRunInfo = document.getElementById('last-run-info');
        const dbCountEl = document.getElementById('db-count');
        const currentCountEl = document.getElementById('current-count');
        const historyCountEl = document.getElementById('history-count');
        const btnOpenAllNew = document.getElementById('btn-open-all-new');
        const newBadgeCount = document.getElementById('new-badge-count');
        const themeBtn = document.getElementById('theme-btn');

        // Theme management
        if (localStorage.getItem('theme') === 'light') {
            document.body.classList.add('light-theme');
            themeBtn.textContent = '☀️';
        }

        themeBtn.addEventListener('click', () => {
            document.body.classList.toggle('light-theme');
            const isLight = document.body.classList.contains('light-theme');
            themeBtn.textContent = isLight ? '☀️' : '🌙';
            localStorage.setItem('theme', isLight ? 'light' : 'dark');
        });

        // Initialize status and saved history
        async function init() {
            try {
                // Fetch config
                const confRes = await fetch('/api/config');
                if (confRes.ok) {
                    const conf = await confRes.json();
                    if (conf.city) document.getElementById('city-input').value = conf.city;
                    if (conf.propertyType) document.getElementById('type-select').value = conf.propertyType;
                    if (conf.autoOpenBrowser !== undefined) document.getElementById('auto-open-toggle').checked = conf.autoOpenBrowser;
                    if (conf.minPriceMillion !== undefined && conf.minPriceMillion !== null) document.getElementById('min-price-input').value = conf.minPriceMillion;
                    if (conf.maxPriceMillion !== undefined && conf.maxPriceMillion !== null) document.getElementById('max-price-input').value = conf.maxPriceMillion;
                    if (conf.portals) {
                        document.getElementById('portal-dh').checked = conf.portals.includes('dh');
                        document.getElementById('portal-oc').checked = conf.portals.includes('oc');
                        document.getElementById('portal-jf').checked = conf.portals.includes('jofogas');
                        document.getElementById('portal-ic').checked = conf.portals.includes('ingatlancom');
                    }
                }

                // Fetch history
                await refreshHistory();
                updateStarredCount();
            } catch (e) {
                console.error("Hiba az inicializáláskor:", e);
            }
        }

        async function refreshHistory() {
            const histRes = await fetch('/api/history');
            if (histRes.ok) {
                historyItems = await histRes.json();
                historyCountEl.textContent = historyItems.length;
                dbCountEl.textContent = historyItems.length;
            }
        }

        // Search execution
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const portals = [];
            if (document.getElementById('portal-dh').checked) portals.push('dh');
            if (document.getElementById('portal-oc').checked) portals.push('oc');
            if (document.getElementById('portal-jf').checked) portals.push('jofogas');
            if (document.getElementById('portal-ic').checked) portals.push('ingatlancom');

            if (portals.length === 0) {
                showToast("Válassz ki legalább egy weboldalt a kereséshez!");
                return;
            }

            const minPriceVal = document.getElementById('min-price-input').value.trim();
            const maxPriceVal = document.getElementById('max-price-input').value.trim();

            const payload = {
                city: document.getElementById('city-input').value.trim() || 'Debrecen',
                propertyType: document.getElementById('type-select').value,
                minPriceMillion: minPriceVal ? parseInt(minPriceVal, 10) : null,
                maxPriceMillion: maxPriceVal ? parseInt(maxPriceVal, 10) : null,
                autoOpenBrowser: document.getElementById('auto-open-toggle').checked,
                portals: portals
            };

            // UI loading state
            submitBtn.disabled = true;
            btnText.textContent = "Keresés folyamatban...";
            btnSpinner.style.display = "inline-block";
            showToast("🔍 Portálok lekérdezése folyamatban...");

            try {
                const response = await fetch('/api/search', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                if (!response.ok) throw new Error("Hálózati hiba: " + response.status);

                const data = await response.json();
                currentItems = data.items || [];
                currentCountEl.textContent = currentItems.length;

                lastRunInfo.innerHTML = `Utolsó futás: <strong>${data.executedAt}</strong> • Találat: <strong>${data.totalFound}</strong> (Új: <span style="color:#10b981; font-weight:700;">${data.newCount}</span>)`;

                // Update "Open all new" button
                const newItems = currentItems.filter(i => i.isNew);
                if (newItems.length > 0) {
                    btnOpenAllNew.style.display = 'inline-flex';
                    newBadgeCount.textContent = newItems.length;
                } else {
                    btnOpenAllNew.style.display = 'none';
                }

                await refreshHistory();
                // Szűrők automatikus nullázása új kereséskor alapértelmezettre
                filterInput.value = '';
                selectedResultPortal = 'all';
                onlyNewFilter = false;
                const onlyNewBtn = document.getElementById('filter-only-new-btn');
                if (onlyNewBtn) onlyNewBtn.classList.remove('active');
                document.querySelectorAll('.portal-filter-btn').forEach(btn => {
                    btn.classList.toggle('active', btn.getAttribute('data-portal') === 'all');
                });
                switchTab('current');
                showToast(`✅ Kész! ${data.totalFound} hirdetés feldolgozva (${data.newCount} új)`);
            } catch (err) {
                showToast("❌ Hiba történt: " + err.message);
            } finally {
                submitBtn.disabled = false;
                btnText.textContent = "🔍 Keresés indítása most";
                btnSpinner.style.display = "none";
            }
        });

        // Tab Switching
        function switchTab(tab) {
            activeTab = tab;
            document.getElementById('tab-current').classList.toggle('active', tab === 'current');
            document.getElementById('tab-history').classList.toggle('active', tab === 'history');
            document.getElementById('tab-starred').classList.toggle('active', tab === 'starred');
            renderListings();
        }

        // Render Cards
        function renderListings() {
            updatePortalFilterCounts();
            let listToRender;
            if (activeTab === 'current') {
                listToRender = currentItems;
            } else if (activeTab === 'starred') {
                const allKnown = [...currentItems, ...historyItems];
                const seen = new Set();
                listToRender = allKnown.filter(item => {
                    if (starredUrls.has(item.url) && !seen.has(item.url)) {
                        seen.add(item.url);
                        return true;
                    }
                    return false;
                });
            } else {
                listToRender = historyItems;
            }

            const filterTerm = filterInput.value.toLowerCase().trim();

            const filtered = listToRender.filter(item => {
                // Csak új találatok szűrése
                if (onlyNewFilter && !item.isNew) {
                    return false;
                }

                // Portál szerinti szűrés a találatok között
                if (selectedResultPortal !== 'all') {
                    const p = (item.portal || '').toLowerCase();
                    if (selectedResultPortal === 'dh' && !p.includes('duna')) return false;
                    if (selectedResultPortal === 'oc' && !p.includes('otthon')) return false;
                    if (selectedResultPortal === 'jf' && !p.includes('jófogás') && !p.includes('jofogas')) return false;
                    if (selectedResultPortal === 'ic' && !p.includes('ingatlan.com')) return false;
                }

                // Szöveges szűrés
                if (!filterTerm) return true;
                return (item.title && item.title.toLowerCase().includes(filterTerm)) ||
                       (item.price && item.price.toLowerCase().includes(filterTerm)) ||
                       (item.details && item.details.toLowerCase().includes(filterTerm)) ||
                       (item.portal && item.portal.toLowerCase().includes(filterTerm)) ||
                       (item.url && item.url.toLowerCase().includes(filterTerm));
            });

            if (filtered.length === 0) {
                listingsContainer.innerHTML = '';
                emptyState.style.display = 'block';
                if (filterTerm) {
                    emptyState.innerHTML = `<div class="empty-state-icon">❌</div><h3>Nincs egyezés a keresésre: "${filterTerm}"</h3><p>Próbáld meg módosítani a kifejezést vagy a szűrőket.</p>`;
                } else if (onlyNewFilter) {
                    emptyState.innerHTML = `<div class="empty-state-icon">✨</div><h3>Nincs új találat ebben a nézetben</h3><p>Kapcsold ki a "Csak újak" szűrőt a korábbi hirdetések megjelenítéséhez!</p>`;
                } else if (selectedResultPortal !== 'all') {
                    emptyState.innerHTML = `<div class="empty-state-icon">🔎</div><h3>Nincs találat a kiválasztott weboldalhoz</h3><p>Válassz másik portált vagy kattints az "Összes forrás" gombra!</p>`;
                } else if (activeTab === 'starred') {
                    emptyState.innerHTML = `<div class="empty-state-icon">⭐</div><h3>Még nincsenek kedvenc hirdetéseid</h3><p>Kattints a csillag (☆) ikonra bármelyik kártyán a mentéshez!</p>`;
                } else if (activeTab === 'current') {
                    emptyState.innerHTML = `<div class="empty-state-icon">🔎</div><h3>Nincs találat az aktuális keresésben</h3><p>Indíts új keresést a fenti gombbal!</p>`;
                } else {
                    emptyState.innerHTML = `<div class="empty-state-icon">📂</div><h3>Az archívum üres</h3><p>A lefutott keresések itt fognak megjelenni.</p>`;
                }
                return;
            }

            emptyState.style.display = 'none';
            listingsContainer.innerHTML = filtered.map(item => {
                let portalClass = 'dh';
                if (item.portal && item.portal.includes('Otthon')) portalClass = 'oc';
                if (item.portal && item.portal.includes('Jófogás')) portalClass = 'jf';
                if (item.portal && item.portal.toLowerCase().includes('ingatlan.com')) portalClass = 'ic';

                const isStarred = starredUrls.has(item.url);

                return `
                    <div class="listing-card ${item.isNew ? 'is-new' : ''}">
                        <div>
                            <div class="card-top">
                                <div style="display: flex; align-items: center; gap: 8px;">
                                    <span class="portal-badge ${portalClass}">${item.portal || 'Hirdetés'}</span>
                                    ${item.isNew ? '<span class="badge-new">✨ ÚJ TALÁLAT</span>' : ''}
                                </div>
                                <button class="btn-star ${isStarred ? 'active' : ''}" 
                                        onclick="toggleStar('${encodeURIComponent(item.url)}')" 
                                        title="${isStarred ? 'Eltávolítás a kedvencek közül' : 'Mentés a kedvencek közé'}">
                                    ${isStarred ? '★' : '☆'}
                                </button>
                            </div>
                            <h3 class="card-title">${escapeHtml(item.title || 'Ingatlan')}</h3>
                            <p class="card-details">${escapeHtml(item.details || item.location || '')}</p>
                        </div>
                        <div class="card-footer">
                            <span class="card-price">${escapeHtml(item.price || 'Ár a hirdetésben')}</span>
                            <a href="${item.url}" target="_blank" rel="noopener noreferrer" class="btn-view">
                                Megtekintés ↗
                            </a>
                        </div>
                    </div>
                `;
            }).join('');
        }

        // Live filter on typing
        filterInput.addEventListener('input', renderListings);

        // Open All New URLs
        async function openAllNew() {
            const newUrls = currentItems.filter(i => i.isNew).map(i => i.url);
            if (newUrls.length === 0) {
                showToast("Nincs új megnyitható hirdetés.");
                return;
            }

            showToast(`🚀 ${newUrls.length} böngészőfül megnyitása...`);
            try {
                await fetch('/api/open-urls', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(newUrls)
                });
            } catch (e) {
                showToast("Hiba a fülek megnyitásakor: " + e.message);
            }
        }

        // Clear history
        async function clearHistoryConfirm() {
            if (!confirm("Biztosan törölni szeretnéd a korábban elmentett hirdetések előzményeit?")) {
                return;
            }
            try {
                await fetch('/api/history/clear', { method: 'POST' });
                historyItems = [];
                currentItems = [];
                currentCountEl.textContent = '0';
                historyCountEl.textContent = '0';
                dbCountEl.textContent = '0';
                btnOpenAllNew.style.display = 'none';
                renderListings();
                showToast("🗑️ Előzmények sikeresen törölve.");
            } catch (e) {
                showToast("Hiba a törléskor: " + e.message);
            }
        }

        // Toast utility
        function showToast(msg) {
            const toast = document.getElementById('toast');
            toast.textContent = msg;
            toast.style.display = 'flex';
            setTimeout(() => {
                toast.style.display = 'none';
            }, 3500);
        }

        function escapeHtml(text) {
            if (!text) return '';
            return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
        }

        // Start
        init();
    </script>
</body>
</html>
        """.replace("{{PORT}}", String.valueOf(port));
    }
}
