import re

# 1. Update index.html
with open('index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Add the floating button just inside the body
floating_btn = '''
    <button id="floatingCloseBtn" class="floating-close-btn" aria-label="Close Guide">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
    </button>
'''
html = html.replace('<body>', f'<body>{floating_btn}')

# Add JS logic for the floating button
js_logic = '''
        const closeApp = () => {
            if (typeof Android !== 'undefined' && Android.navigateHome) {
                Android.navigateHome();
            } else {
                window.close();
                if (window.history.length > 1) {
                    window.history.back();
                }
            }
        };

        document.getElementById('closeBtn').addEventListener('click', closeApp);
        document.getElementById('floatingCloseBtn').addEventListener('click', closeApp);
'''
# replace the old close logic
html = re.sub(r"document\.getElementById\('closeBtn'\)\.addEventListener\('click', \(\) => \{.*?\}\);", js_logic, html, flags=re.DOTALL)

with open('index.html', 'w', encoding='utf-8') as f:
    f.write(html)

# 2. Update style.css
with open('style.css', 'r', encoding='utf-8') as f:
    css = f.read()

floating_css = '''
/* Floating Close Button */
.floating-close-btn {
    position: fixed;
    top: 20px;
    right: 20px;
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: #ffffff;
    border: 1px solid #e5e7eb;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    color: var(--text-sec);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    z-index: 100;
    transition: all 0.2s ease;
}

.floating-close-btn:hover {
    background: #fef2f2;
    color: #ef4444;
    border-color: #fca5a5;
    transform: scale(1.05);
}

.floating-close-btn svg {
    width: 20px;
    height: 20px;
}
'''
css = css + '\n' + floating_css

with open('style.css', 'w', encoding='utf-8') as f:
    f.write(css)

