import re

with open('style.css', 'r', encoding='utf-8') as f:
    css = f.read()

# Replace root variables
css = re.sub(r':root \{.*?\n\}', 
    ':root {\n    --primary: #4F46E5;\n    --primary-light: #818CF8;\n    --bg-color: #f4f5f7;\n    --surface: #ffffff;\n    --text-main: #0F172A;\n    --text-sec: #475569;\n    --timeline-cyan: #00BCD4;\n    --timeline-line: #d1d5db;\n}', 
    css, flags=re.DOTALL)

# Hide blobs
css = css.replace('.background-blobs {', '.background-blobs {\n    display: none;')

# Replace timeline and step-card css
timeline_css_start = css.find('.timeline {')
footer_btn_start = css.find('/* Close Button */')

new_timeline_css = '''
.timeline {
    max-width: 800px;
    margin: 0 auto;
    padding: 0 20px 80px;
    display: flex;
    flex-direction: column;
    position: relative;
}

.timeline::before {
    content: '';
    position: absolute;
    top: 20px;
    bottom: 80px;
    left: 45px;
    width: 2px;
    background-color: var(--timeline-line);
}

.timeline::after {
    content: '';
    position: absolute;
    top: 10px;
    left: 38px;
    width: 0;
    height: 0;
    border-left: 8px solid transparent;
    border-right: 8px solid transparent;
    border-bottom: 10px solid var(--timeline-line);
}

.timeline-item {
    position: relative;
    display: flex;
    align-items: flex-start;
    margin-bottom: 40px;
    z-index: 1;
}

.timeline-indicator {
    display: flex;
    align-items: center;
    width: 100px;
    flex-shrink: 0;
    margin-top: 22px;
}

.timeline-indicator .dot {
    width: 12px;
    height: 12px;
    background-color: var(--timeline-cyan);
    border-radius: 50%;
    margin-left: 20px;
    margin-right: 15px;
    box-shadow: 0 0 0 4px var(--bg-color);
}

.timeline-indicator .number {
    font-size: 1.4rem;
    font-weight: 800;
    color: #000;
}

.step-card {
    background: var(--surface);
    border-radius: 4px;
    padding: 24px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
    position: relative;
    flex-grow: 1;
    border: 1px solid #e5e7eb;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.step-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0,0,0,0.08);
}

.step-card::before {
    content: '';
    position: absolute;
    left: -10px;
    top: 22px;
    width: 0;
    height: 0;
    border-top: 10px solid transparent;
    border-bottom: 10px solid transparent;
    border-right: 10px solid var(--surface);
    z-index: 2;
}

.step-card::after {
    content: '';
    position: absolute;
    left: -11px;
    top: 22px;
    width: 0;
    height: 0;
    border-top: 10px solid transparent;
    border-bottom: 10px solid transparent;
    border-right: 10px solid #e5e7eb;
    z-index: 1;
}

.step-content h2 {
    font-size: 1.3rem;
    color: #000;
    margin: 0 0 12px;
    font-weight: 800;
}

.step-content p {
    font-size: 1.05rem;
    line-height: 1.6;
    color: var(--text-sec);
    margin: 0 0 20px;
}

.step-image {
    width: 100%;
    border-radius: 8px;
    overflow: hidden;
    background: #fff;
    border: 1px solid #f3f4f6;
}

.step-image img {
    width: 100%;
    display: block;
    object-fit: cover;
}

/* Scroll Animations */
.timeline-item.hidden {
    opacity: 0;
    transform: translateY(30px);
}

.timeline-item.show {
    opacity: 1;
    transform: translateY(0);
    transition: opacity 0.6s ease-out, transform 0.6s ease-out;
}

'''

css = css[:timeline_css_start] + new_timeline_css + css[footer_btn_start:]

with open('style.css', 'w', encoding='utf-8') as f:
    f.write(css)
