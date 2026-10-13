package ingatlanok.web;

public class WebDashboardHtml {

    public static String getHtml(int port) {
        return (getHeaderAndCss() + getBodyMarkup() + getScriptJs()).replace("{{PORT}}", String.valueOf(port));
    }

    private static String getHeaderAndCss() {
        return """
<!DOCTYPE html>
<html lang="hu">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ingatlan Kereső Dashboard</title>
    <link rel="preconnect" href="https:
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-body: #0b0f19;
            --bg-card: #131b2e;
            --bg-card-hover: #1a243d;
            --bg-input: #1a2238;
            --border-color: #263352;
            --text-primary: #f8fafc;
            --text-secondary: #94a3b8;
            --accent: #3b82f6;
            --accent-gradient: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
            --badge-dh: #f97316;
            --badge-oc: #ef4444;
            --badge-jf: #10b981;
            --badge-ic: #8b5cf6;
            --success: #10b981;
            --shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3), 0 8px 10px -6px rgba(0, 0, 0, 0.3);
        }

        body.light-theme {
            --bg-body: #f8fafc;
            --bg-card: #ffffff;
            --bg-card-hover: #f1f5f9;
            --bg-input: #f1f5f9;
            --border-color: #e2e8f0;
            --text-primary: #0f172a;
            --text-secondary: #64748b;
            --shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Plus Jakarta Sans', sans-serif;
        }

        body {
            background-color: var(--bg-body);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            transition: background-color 0.25s ease, color 0.25s ease;
        }

        header {
            background: var(--bg-card);
            border-bottom: 1px solid var(--border-color);
            padding: 16px 24px;
            position: sticky;
            top: 0;
            z-index: 50;
            backdrop-filter: blur(8px);
        }

        .header-content {
            max-width: 1400px;
            margin: 0 auto;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .logo-area {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .logo-icon {
            font-size: 28px;
            background: var(--accent-gradient);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .logo-text h1 {
            font-size: 20px;
            font-weight: 800;
            letter-spacing: -0.5px;
        }

        .logo-text p {
            font-size: 12px;
            color: var(--text-secondary);
        }

        .header-actions {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .theme-toggle-btn {
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 8px 14px;
            border-radius: 10px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s ease;
        }

        .theme-toggle-btn:hover {
            border-color: var(--accent);
        }

        main {
            max-width: 1400px;
            margin: 0 auto;
            padding: 24px;
            flex: 1;
            width: 100%;
        }

        .filter-card {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 24px;
            box-shadow: var(--shadow);
            margin-bottom: 24px;
        }

        .filter-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 20px;
            padding-bottom: 14px;
            border-bottom: 1px solid var(--border-color);
        }

        .filter-title {
            font-size: 16px;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .grid-filters {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }

        .form-group {
            display: flex;
            flex-direction: column;
            gap: 8px;
            position: relative;
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
            transition: all 0.2s ease;
        }

        .form-control:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
        }

        .city-chips-box {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 6px;
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            border-radius: 10px;
            padding: 6px 10px;
            min-height: 44px;
            cursor: text;
            transition: border-color 0.2s ease, box-shadow 0.2s ease;
        }
        .city-chips-box:focus-within {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
        }
        .city-chip {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(59, 130, 246, 0.15);
            color: var(--accent);
            border: 1px solid rgba(59, 130, 246, 0.3);
            padding: 3px 10px;
            border-radius: 16px;
            font-size: 13px;
            font-weight: 600;
        }
        .city-chip .chip-remove {
            cursor: pointer;
            font-size: 14px;
            font-weight: 700;
            opacity: 0.7;
            line-height: 1;
        }
        .city-chip .chip-remove:hover {
            opacity: 1;
            color: #ef4444;
        }
        .city-search-input {
            border: none;
            outline: none;
            background: transparent;
            color: var(--text-primary);
            font-size: 13px;
            flex: 1;
            min-width: 120px;
            padding: 4px 2px;
        }
        .city-dropdown {
            position: absolute;
            top: calc(100% + 6px);
            left: 0;
            right: 0;
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.35);
            z-index: 100;
            max-height: 320px;
            overflow-y: auto;
            display: none;
            padding: 8px;
        }
        .city-dropdown.show {
            display: block;
        }
        .city-presets-bar {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            padding-bottom: 8px;
            margin-bottom: 8px;
            border-bottom: 1px dashed var(--border-color);
        }
        .preset-badge {
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            padding: 4px 10px;
            border-radius: 8px;
            font-size: 11px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        .preset-badge:hover {
            border-color: var(--accent);
            color: var(--accent);
            background: rgba(59, 130, 246, 0.08);
        }
        .city-dropdown-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 12px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 13px;
            transition: background-color 0.15s ease;
        }
        .city-dropdown-item:hover, .city-dropdown-item.active {
            background: var(--bg-input);
        }
        .city-dropdown-item .city-name-group {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .city-dropdown-item .city-meta-badge {
            font-size: 11px;
            padding: 2px 8px;
            border-radius: 6px;
            background: rgba(255, 255, 255, 0.06);
            color: var(--text-secondary);
        }
        .city-dropdown-item.selected .city-meta-badge {
            background: rgba(16, 185, 129, 0.15);
            color: #10b981;
        }
        .city-add-custom {
            padding: 8px 12px;
            font-size: 13px;
            color: var(--accent);
            cursor: pointer;
            font-weight: 600;
            border-top: 1px dashed var(--border-color);
            margin-top: 6px;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .city-add-custom:hover {
            background: rgba(59, 130, 246, 0.08);
        }

        .price-range-inputs {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .price-range-separator {
            color: var(--text-secondary);
            font-weight: 700;
        }

        .stepper-box {
            position: relative;
            flex: 1;
            display: flex;
            align-items: center;
        }

        .stepper-input {
            width: 100%;
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 10px 38px 10px 14px;
            border-radius: 10px;
            font-size: 14px;
            outline: none;
            transition: all 0.2s ease;
            -moz-appearance: textfield;
            appearance: textfield;
        }

        .stepper-input::-webkit-outer-spin-button,
        .stepper-input::-webkit-inner-spin-button {
            -webkit-appearance: none;
            margin: 0;
        }

        .stepper-input:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
        }

        .stepper-arrows {
            position: absolute;
            right: 4px;
            top: 50%;
            transform: translateY(-50%);
            display: flex;
            flex-direction: column;
            gap: 1px;
        }

        .stepper-arrow-btn {
            background: transparent;
            border: none;
            color: var(--text-secondary);
            cursor: pointer;
            padding: 2px 8px;
            font-size: 10px;
            line-height: 1;
            border-radius: 4px;
            transition: all 0.15s ease;
        }

        .stepper-arrow-btn:hover {
            color: var(--accent);
            background: rgba(59, 130, 246, 0.15);
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

        .result-city-filters {
            display: flex;
            gap: 6px;
            flex-wrap: wrap;
            align-items: center;
            width: 100%;
            margin-top: 4px;
        }
        .city-filter-btn {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--text-secondary);
            padding: 5px 12px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        .city-filter-btn:hover {
            border-color: var(--accent);
            color: var(--text-primary);
        }
        .city-filter-btn.active {
            background: rgba(59, 130, 246, 0.2);
            color: var(--accent);
            border-color: var(--accent);
        }

        .city-badge-tag {
            font-size: 11px;
            font-weight: 700;
            padding: 2px 7px;
            border-radius: 6px;
            background: rgba(59, 130, 246, 0.12);
            color: var(--accent);
            display: inline-flex;
            align-items: center;
            gap: 4px;
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

        .sort-box {
            position: relative;
            display: inline-flex;
            align-items: center;
        }
        .sort-box .icon {
            position: absolute;
            left: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-secondary);
            font-size: 13px;
            pointer-events: none;
            z-index: 1;
        }
        .sort-select {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 10px 34px 10px 34px;
            border-radius: 10px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            outline: none;
            appearance: none;
            -webkit-appearance: none;
            transition: border-color 0.2s ease, box-shadow 0.2s ease, background-color 0.2s ease;
        }
        .sort-select:hover, .sort-select:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
        }
        .sort-box .arrow {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-secondary);
            font-size: 11px;
            pointer-events: none;
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

        .listings-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
            gap: 20px;
            margin-top: 16px;
        }

        .listing-card {
            background: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 20px;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            position: relative;
            transition: all 0.2s ease;
            box-shadow: var(--shadow);
        }

        .listing-card:hover {
            transform: translateY(-3px);
            border-color: rgba(59, 130, 246, 0.5);
            background: var(--bg-card-hover);
        }

        .listing-card.is-new {
            border: 2px solid #10b981;
            box-shadow: 0 0 15px rgba(16, 185, 129, 0.25);
        }

        .card-top {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 12px;
            gap: 8px;
        }

        .portal-badge {
            font-size: 11px;
            font-weight: 700;
            padding: 4px 10px;
            border-radius: 8px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .portal-badge.dh { background: rgba(249, 115, 22, 0.15); color: var(--badge-dh); border: 1px solid rgba(249, 115, 22, 0.3); }
        .portal-badge.oc { background: rgba(239, 68, 68, 0.15); color: var(--badge-oc); border: 1px solid rgba(239, 68, 68, 0.3); }
        .portal-badge.jf { background: rgba(16, 185, 129, 0.15); color: var(--badge-jf); border: 1px solid rgba(16, 185, 129, 0.3); }
        .portal-badge.ic { background: rgba(139, 92, 246, 0.15); color: var(--badge-ic); border: 1px solid rgba(139, 92, 246, 0.3); }

        .badge-new {
            background: #10b981;
            color: white;
            font-size: 10px;
            font-weight: 800;
            padding: 3px 8px;
            border-radius: 6px;
            letter-spacing: 0.5px;
            animation: pulse 2s infinite;
        }

        @keyframes pulse {
            0% { transform: scale(1); }
            50% { transform: scale(1.05); }
            100% { transform: scale(1); }
        }

        .card-title {
            font-size: 15px;
            font-weight: 700;
            line-height: 1.4;
            margin-bottom: 8px;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }

        .card-details {
            font-size: 13px;
            color: var(--text-secondary);
            margin-bottom: 16px;
        }

        .card-footer {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding-top: 14px;
            border-top: 1px solid var(--border-color);
        }

        .card-price {
            font-size: 17px;
            font-weight: 800;
            color: #38bdf8;
        }

        .btn-view {
            background: var(--bg-input);
            border: 1px solid var(--border-color);
            color: var(--text-primary);
            padding: 6px 14px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.2s ease;
        }

        .btn-view:hover {
            background: var(--accent);
            border-color: var(--accent);
            color: white;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: var(--text-secondary);
        }
        .empty-state-icon {
            font-size: 48px;
            margin-bottom: 12px;
        }
        .empty-state h3 {
            font-size: 18px;
            color: var(--text-primary);
            margin-bottom: 6px;
        }

        #toast {
            position: fixed;
            bottom: 24px;
            right: 24px;
            background: #1e293b;
            color: white;
            border: 1px solid var(--border-color);
            padding: 12px 20px;
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            box-shadow: 0 10px 25px rgba(0,0,0,0.4);
            display: none;
            align-items: center;
            gap: 10px;
            z-index: 1000;
        }

        .spinner {
            width: 16px;
            height: 16px;
            border: 2px solid rgba(255, 255, 255, 0.3);
            border-top-color: white;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <header>
        <div class="header-content">
            <div class="logo-area">
                <span class="logo-icon">🏠</span>
                <div class="logo-text">
                    <h1>Ingatlan Kereső</h1>
                    <p id="last-run-info">Adatbázis: <strong id="db-count">0</strong> hirdetés tárolva</p>
                </div>
            </div>
            <div class="header-actions">
                <button class="theme-toggle-btn" id="theme-btn" title="Téma váltása">🌙</button>
            </div>
        </div>
    </header>
""";
    }

    private static String getBodyMarkup() {
        return """
    <main>
        <!-- Filter Card -->
        <section class="filter-card">
            <div class="filter-header">
                <div class="filter-title">
                    <span>⚡ Keresési Feltételek</span>
                </div>
            </div>

            <form id="search-form">
                <div class="grid-filters">
                    <div class="form-group">
                        <label for="city-search-input">Települések (több is kiválasztható)</label>
                        <div class="city-multiselect-container" id="city-multiselect">
                            <div class="city-chips-box" id="city-chips-box" onclick="focusCitySearch()">
                                <div id="selected-chips-container" style="display:flex; flex-wrap:wrap; gap:6px;"></div>
                                <input type="text" id="city-search-input" class="city-search-input" placeholder="Város keresése vagy beírása..." autocomplete="off">
                            </div>
                            <div class="city-dropdown" id="city-dropdown">
                                <div class="city-presets-bar" id="city-presets-bar"></div>
                                <div id="city-dropdown-list"></div>
                                <div id="city-custom-add" class="city-add-custom" style="display:none;" onclick="addCustomCityFromInput()"></div>
                            </div>
                        </div>
                        <input type="hidden" id="city-input" value="Debrecen">
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
                                <input type="number" id="min-price-input" class="stepper-input" value="70" placeholder="Min (M Ft)" min="0" step="1">
                                <div class="stepper-arrows">
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('min-price-input', 1)" title="Növelés">▲</button>
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('min-price-input', -1)" title="Csökkentés">▼</button>
                                </div>
                            </div>
                            <span class="price-range-separator">-</span>
                            <div class="stepper-box">
                                <input type="number" id="max-price-input" class="stepper-input" value="120" placeholder="Max (M Ft)" min="0" step="1">
                                <div class="stepper-arrows">
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('max-price-input', 1)" title="Növelés">▲</button>
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('max-price-input', -1)" title="Csökkentés">▼</button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Alapterület (m²)</label>
                        <div class="price-range-inputs">
                            <div class="stepper-box">
                                <input type="number" id="min-area-input" class="stepper-input" value="90" placeholder="Min (m²)" min="0" step="5">
                                <div class="stepper-arrows">
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('min-area-input', 5)" title="Növelés">▲</button>
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('min-area-input', -5)" title="Csökkentés">▼</button>
                                </div>
                            </div>
                            <span class="price-range-separator">-</span>
                            <div class="stepper-box">
                                <input type="number" id="max-area-input" class="stepper-input" value="200" placeholder="Max (m²)" min="0" step="5">
                                <div class="stepper-arrows">
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('max-area-input', 5)" title="Növelés">▲</button>
                                    <button type="button" class="stepper-arrow-btn" onclick="stepNumber('max-area-input', -5)" title="Csökkentés">▼</button>
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
                        <label class="portal-chip">
                            <input type="checkbox" id="portal-ib" value="ingatlanbazar" checked> Ingatlanbazár
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

            <div class="sort-box">
                <span class="icon">↕️</span>
                <select id="sort-select" class="sort-select" onchange="changeSort(this.value)" title="Találati lista rendezése ár szerint">
                    <option value="default">Rendezés: Alapértelmezett</option>
                    <option value="price-asc">Ár: Növekvő (olcsóbbtól)</option>
                    <option value="price-desc">Ár: Csökkenő (drágábbtól)</option>
                </select>
                <span class="arrow">▾</span>
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

            <!-- Dynamic City Filters Bar -->
            <div class="result-city-filters" id="result-city-filters" style="display: none;"></div>
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
""";
    }

    private static String getScriptJs() {
        return """
    <script>

        let currentItems = [];
        let historyItems = [];
        let activeTab = 'current';
        let selectedResultPortal = 'all';
        let selectedResultCity = 'all';
        let onlyNewFilter = false;
        let currentSort = 'default';
        let starredUrls = new Set(JSON.parse(localStorage.getItem('starred_urls') || '[]'));

        let selectedCities = ['Debrecen'];
        let predefinedCities = [];
        let cityPresets = {};
        let trackedCities = [];

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

        function changeSort(val) {
            currentSort = val;
            renderListings();
        }

        function parsePrice(raw) {
            if (!raw || typeof raw !== 'string') return null;
            let str = raw.trim();
            if (!str || str.toLowerCase().includes('ár a hirdetésben') || str.toLowerCase().includes('érdeklődjön')) {
                return null;
            }

            if (str.includes('-')) {
                const parts = str.split('-');
                if (parts.length > 0 && parts[0].trim()) {
                    const parsedFirst = parsePrice(parts[0].trim());
                    if (parsedFirst !== null) return parsedFirst;
                }
            }

            const millionMatch = str.match(/([0-9]+(?:[.,][0-9]+)?)\\s*(?:m\\s*ft|mft|millió|m)/i);
            if (millionMatch) {
                const numStr = millionMatch[1].replace(',', '.');
                const val = parseFloat(numStr);
                if (!isNaN(val) && val > 0) {
                    return Math.round(val * 1000000);
                }
            }

            const digitsOnly = str.replace(/[^0-9]/g, '');
            if (digitsOnly.length >= 6) {
                const val = parseInt(digitsOnly, 10);
                if (!isNaN(val) && val > 0) {
                    return val;
                }
            }

            return null;
        }

        function stepNumber(inputId, step) {
            const el = document.getElementById(inputId);
            if (!el) return;
            let val = parseInt(el.value, 10);
            if (isNaN(val)) val = 0;
            val = Math.max(0, val + step);
            el.value = val;
        }

        function focusCitySearch() {
            const input = document.getElementById('city-search-input');
            if (input) input.focus();
            openCityDropdown();
        }

        function openCityDropdown() {
            const dropdown = document.getElementById('city-dropdown');
            if (dropdown) {
                dropdown.classList.add('show');
                renderCityDropdownList();
            }
        }

        function closeCityDropdown() {
            const dropdown = document.getElementById('city-dropdown');
            if (dropdown) dropdown.classList.remove('show');
        }

        document.addEventListener('click', (e) => {
            const container = document.getElementById('city-multiselect');
            if (container && !container.contains(e.target)) {
                closeCityDropdown();
            }
        });

        const citySearchInput = document.getElementById('city-search-input');
        if (citySearchInput) {
            citySearchInput.addEventListener('focus', openCityDropdown);
            citySearchInput.addEventListener('input', () => {
                openCityDropdown();
                renderCityDropdownList();
            });
            citySearchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    addCustomCityFromInput();
                } else if (e.key === 'Backspace' && citySearchInput.value === '' && selectedCities.length > 0) {
                    removeCityTag(selectedCities[selectedCities.length - 1]);
                }
            });
        }

        function renderSelectedCityChips() {
            const container = document.getElementById('selected-chips-container');
            const hiddenCityInput = document.getElementById('city-input');
            if (!container) return;

            if (selectedCities.length === 0) {
                selectedCities = ['Debrecen'];
            }

            if (hiddenCityInput) {
                hiddenCityInput.value = selectedCities[0] || 'Debrecen';
            }

            container.innerHTML = selectedCities.map(city => `
                <span class="city-chip">
                    📍 ${escapeHtml(city)}
                    <span class="chip-remove" onclick="removeCityTag('${escapeHtml(city)}')">&times;</span>
                </span>
            `).join('');
        }

        function addCityTag(cityName) {
            const trimmed = cityName.trim();
            if (!trimmed) return;
            if (!selectedCities.includes(trimmed)) {
                selectedCities.push(trimmed);
                renderSelectedCityChips();
                renderCityDropdownList();
            }
            if (citySearchInput) {
                citySearchInput.value = '';
            }
        }

        function removeCityTag(cityName) {
            if (selectedCities.length <= 1) {
                showToast("Legalább egy várost ki kell választani!");
                return;
            }
            selectedCities = selectedCities.filter(c => c !== cityName);
            renderSelectedCityChips();
            renderCityDropdownList();
        }

        function addCustomCityFromInput() {
            if (!citySearchInput) return;
            const val = citySearchInput.value.trim();
            if (val) {
                addCityTag(val);
                citySearchInput.value = '';
                closeCityDropdown();
            }
        }

        function applyCityPreset(presetName) {
            const list = cityPresets[presetName];
            if (list && list.length > 0) {
                selectedCities = [...list];
                renderSelectedCityChips();
                closeCityDropdown();
                showToast(`🎯 Sablon betöltve: ${presetName} (${list.length} város)`);
            }
        }

        function renderCityPresetsBar() {
            const bar = document.getElementById('city-presets-bar');
            if (!bar) return;
            bar.innerHTML = Object.keys(cityPresets).map(presetKey => `
                <button type="button" class="preset-badge" onclick="applyCityPreset('${escapeHtml(presetKey)}')">
                    ${escapeHtml(presetKey)}
                </button>
            `).join('');
        }

        function renderCityDropdownList() {
            const listEl = document.getElementById('city-dropdown-list');
            const customAddEl = document.getElementById('city-custom-add');
            if (!listEl) return;

            const term = (citySearchInput ? citySearchInput.value : '').toLowerCase().trim();
            const trackedKeys = new Set(trackedCities.map(tc => (tc.key || '').toLowerCase()));
            const trackedCounts = {};
            trackedCities.forEach(tc => {
                trackedCounts[(tc.key || '').toLowerCase()] = tc.itemCount || 0;
            });

            const filtered = predefinedCities.filter(c => {
                if (!term) return true;
                return (c.name && c.name.toLowerCase().includes(term)) ||
                       (c.county && c.county.toLowerCase().includes(term)) ||
                       (c.group && c.group.toLowerCase().includes(term));
            });

            listEl.innerHTML = filtered.map(c => {
                const isSelected = selectedCities.includes(c.name);
                const isTracked = trackedKeys.has((c.key || '').toLowerCase());
                const count = trackedCounts[(c.key || '').toLowerCase()] || 0;

                let metaText = '✨ Új város';
                if (isTracked) {
                    metaText = `💾 Mentve (${count} db)`;
                }

                return `
                    <div class="city-dropdown-item ${isSelected ? 'selected' : ''}" onclick="addCityTag('${escapeHtml(c.name)}')">
                        <div class="city-name-group">
                            <span>${isSelected ? '✓ ' : '+ '}<strong>${escapeHtml(c.name)}</strong></span>
                            <span style="font-size:11px; color:var(--text-secondary);">${escapeHtml(c.county || '')}</span>
                        </div>
                        <span class="city-meta-badge">${metaText}</span>
                    </div>
                `;
            }).join('');

            if (term && !predefinedCities.some(c => c.name.toLowerCase() === term) && !selectedCities.some(c => c.toLowerCase() === term)) {
                if (customAddEl) {
                    customAddEl.style.display = 'flex';
                    customAddEl.innerHTML = `➕ "<strong>${escapeHtml(citySearchInput.value.trim())}</strong>" hozzáadása új városként`;
                }
            } else if (customAddEl) {
                customAddEl.style.display = 'none';
            }
        }

        function updateResultCityFilters(items) {
            const container = document.getElementById('result-city-filters');
            if (!container) return;

            const counts = {};
            items.forEach(it => {
                const city = it.city || it.location || 'Debrecen';
                counts[city] = (counts[city] || 0) + 1;
            });

            const cityNames = Object.keys(counts);
            if (cityNames.length <= 1) {
                container.style.display = 'none';
                return;
            }

            container.style.display = 'flex';
            let html = `
                <button type="button" class="city-filter-btn ${selectedResultCity === 'all' ? 'active' : ''}" onclick="filterByCity('all')">
                    📍 Összes város (${items.length})
                </button>
            `;

            cityNames.forEach(c => {
                html += `
                    <button type="button" class="city-filter-btn ${selectedResultCity === c ? 'active' : ''}" onclick="filterByCity('${escapeHtml(c)}')">
                        ${escapeHtml(c)} (${counts[c]})
                    </button>
                `;
            });

            container.innerHTML = html;
        }

        function filterByCity(cityName) {
            selectedResultCity = cityName;
            updateResultCityFilters(activeTab === 'current' ? currentItems : historyItems);
            renderListings();
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

        async function init() {
            try {

                try {
                    const citiesRes = await fetch('/api/cities');
                    if (citiesRes.ok) {
                        const citiesData = await citiesRes.json();
                        predefinedCities = citiesData.predefined || [];
                        cityPresets = citiesData.presets || {};
                        trackedCities = citiesData.trackedCities || [];
                        renderCityPresetsBar();
                        renderCityDropdownList();
                    }
                } catch (ce) {
                    console.warn("Nem sikerült betölteni a városlistát:", ce);
                }

                const confRes = await fetch('/api/config');
                if (confRes.ok) {
                    const conf = await confRes.json();
                    if (conf.cities && Array.isArray(conf.cities) && conf.cities.length > 0) {
                        selectedCities = conf.cities;
                    } else if (conf.city) {
                        selectedCities = [conf.city];
                    }
                    renderSelectedCityChips();

                    if (conf.propertyType) document.getElementById('type-select').value = conf.propertyType;
                    if (conf.autoOpenBrowser !== undefined) document.getElementById('auto-open-toggle').checked = conf.autoOpenBrowser;
                    if (conf.minPriceMillion !== undefined && conf.minPriceMillion !== null) document.getElementById('min-price-input').value = conf.minPriceMillion;
                    if (conf.maxPriceMillion !== undefined && conf.maxPriceMillion !== null) document.getElementById('max-price-input').value = conf.maxPriceMillion;
                    if (conf.minArea !== undefined && conf.minArea !== null) document.getElementById('min-area-input').value = conf.minArea;
                    if (conf.maxArea !== undefined && conf.maxArea !== null) document.getElementById('max-area-input').value = conf.maxArea;
                    if (conf.portals) {
                        document.getElementById('portal-dh').checked = conf.portals.includes('dh');
                        document.getElementById('portal-oc').checked = conf.portals.includes('oc');
                        document.getElementById('portal-jf').checked = conf.portals.includes('jofogas');
                        document.getElementById('portal-ic').checked = conf.portals.includes('ingatlancom');
                        document.getElementById('portal-ib').checked = conf.portals.includes('ingatlanbazar');
                    }
                } else {
                    renderSelectedCityChips();
                }

                await refreshHistory();
                updateStarredCount();
            } catch (e) {
                console.error("Hiba az inicializáláskor:", e);
                renderSelectedCityChips();
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

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const portals = [];
            if (document.getElementById('portal-dh').checked) portals.push('dh');
            if (document.getElementById('portal-oc').checked) portals.push('oc');
            if (document.getElementById('portal-jf').checked) portals.push('jofogas');
            if (document.getElementById('portal-ic').checked) portals.push('ingatlancom');
            if (document.getElementById('portal-ib').checked) portals.push('ingatlanbazar');

            if (portals.length === 0) {
                showToast("Válassz ki legalább egy weboldalt a kereséshez!");
                return;
            }

            if (selectedCities.length === 0) {
                showToast("Válassz ki legalább egy települést a kereséshez!");
                return;
            }

            const minPriceVal = document.getElementById('min-price-input').value.trim();
            const maxPriceVal = document.getElementById('max-price-input').value.trim();
            const minAreaVal = document.getElementById('min-area-input').value.trim();
            const maxAreaVal = document.getElementById('max-area-input').value.trim();

            const payload = {
                city: selectedCities[0] || 'Debrecen',
                cities: selectedCities,
                propertyType: document.getElementById('type-select').value,
                minPriceMillion: minPriceVal ? parseInt(minPriceVal, 10) : null,
                maxPriceMillion: maxPriceVal ? parseInt(maxPriceVal, 10) : null,
                minArea: minAreaVal ? parseInt(minAreaVal, 10) : null,
                maxArea: maxAreaVal ? parseInt(maxAreaVal, 10) : null,
                autoOpenBrowser: document.getElementById('auto-open-toggle').checked,
                portals: portals
            };

            submitBtn.disabled = true;
            btnText.textContent = "Keresés folyamatban...";
            btnSpinner.style.display = "inline-block";
            showToast(`🔍 Keresés indítása: ${selectedCities.join(', ')}...`);

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

                if (data.newCitiesInitialized && data.newCitiesInitialized.length > 0) {
                    showToast(`✨ Új város adatbázis létrehozva: ${data.newCitiesInitialized.join(', ')} (alapállapot rögzítve)`);
                }

                const newItems = currentItems.filter(i => i.isNew);
                if (newItems.length > 0) {
                    btnOpenAllNew.style.display = 'inline-flex';
                    newBadgeCount.textContent = newItems.length;
                } else {
                    btnOpenAllNew.style.display = 'none';
                }

                await refreshHistory();

                filterInput.value = '';
                selectedResultPortal = 'all';
                selectedResultCity = 'all';
                onlyNewFilter = false;
                currentSort = 'default';
                const sortSelect = document.getElementById('sort-select');
                if (sortSelect) sortSelect.value = 'default';
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

        function switchTab(tab) {
            activeTab = tab;
            selectedResultCity = 'all';
            document.getElementById('tab-current').classList.toggle('active', tab === 'current');
            document.getElementById('tab-history').classList.toggle('active', tab === 'history');
            document.getElementById('tab-starred').classList.toggle('active', tab === 'starred');
            renderListings();
        }

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

            updateResultCityFilters(listToRender);

            const filterTerm = filterInput.value.toLowerCase().trim();

            const filtered = listToRender.filter(item => {

                if (onlyNewFilter && !item.isNew) {
                    return false;
                }

                if (selectedResultPortal !== 'all') {
                    const p = (item.portal || '').toLowerCase();
                    if (selectedResultPortal === 'dh' && !p.includes('duna')) return false;
                    if (selectedResultPortal === 'oc' && !p.includes('otthon')) return false;
                    if (selectedResultPortal === 'jf' && !p.includes('jófogás') && !p.includes('jofogas')) return false;
                    if (selectedResultPortal === 'ic' && !p.includes('ingatlan.com')) return false;
                }

                if (selectedResultCity !== 'all') {
                    const c = (item.city || item.location || '').toLowerCase();
                    if (!c.includes(selectedResultCity.toLowerCase())) return false;
                }

                if (!filterTerm) return true;
                return (item.title && item.title.toLowerCase().includes(filterTerm)) ||
                       (item.price && item.price.toLowerCase().includes(filterTerm)) ||
                       (item.details && item.details.toLowerCase().includes(filterTerm)) ||
                       (item.location && item.location.toLowerCase().includes(filterTerm)) ||
                       (item.city && item.city.toLowerCase().includes(filterTerm)) ||
                       (item.portal && item.portal.toLowerCase().includes(filterTerm)) ||
                       (item.url && item.url.toLowerCase().includes(filterTerm));
            });

            if (currentSort === 'price-asc' || currentSort === 'price-desc') {
                filtered.sort((a, b) => {
                    const pA = parsePrice(a.price);
                    const pB = parsePrice(b.price);
                    if (pA === null && pB === null) return 0;
                    if (pA === null) return 1;
                    if (pB === null) return -1;
                    return currentSort === 'price-asc' ? pA - pB : pB - pA;
                });
            }

            if (filtered.length === 0) {
                listingsContainer.innerHTML = '';
                emptyState.style.display = 'block';
                if (filterTerm) {
                    emptyState.innerHTML = `<div class="empty-state-icon">❌</div><h3>Nincs egyezés a keresésre: "${escapeHtml(filterTerm)}"</h3><p>Próbáld meg módosítani a kifejezést vagy a szűrőket.</p>`;
                } else if (onlyNewFilter) {
                    emptyState.innerHTML = `<div class="empty-state-icon">✨</div><h3>Nincs új találat ebben a nézetben</h3><p>Kapcsold ki a "Csak újak" szűrőt a korábbi hirdetések megjelenítéséhez!</p>`;
                } else if (selectedResultCity !== 'all') {
                    emptyState.innerHTML = `<div class="empty-state-icon">📍</div><h3>Nincs találat "${escapeHtml(selectedResultCity)}" városban</h3><p>Kattints az "Összes város" gombra a többi település megjelenítéséhez!</p>`;
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
                const itemCity = item.city || item.location || 'Debrecen';

                return `
                    <div class="listing-card ${item.isNew ? 'is-new' : ''}">
                        <div>
                            <div class="card-top">
                                <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
                                    <span class="portal-badge ${portalClass}">${item.portal || 'Hirdetés'}</span>
                                    <span class="city-badge-tag">📍 ${escapeHtml(itemCity)}</span>
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

        filterInput.addEventListener('input', renderListings);

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
                currentSort = 'default';
                const sortSelect = document.getElementById('sort-select');
                if (sortSelect) sortSelect.value = 'default';
                renderListings();
                showToast("🗑️ Előzmények sikeresen törölve.");
            } catch (e) {
                showToast("Hiba a törléskor: " + e.message);
            }
        }

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

        init();
    </script>
</body>
</html>
""";
    }
}
