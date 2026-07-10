import re

with open('index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# We want to replace <section class="step-card hidden"> to include the wrapper.
# Since we have exactly 7 steps, we can just replace them one by one.

for i in range(1, 8):
    search_str = f'<!-- Step {i} -->\n        <section class="step-card hidden">'
    replace_str = f'<!-- Step {i} -->\n        <div class="timeline-item hidden">\n            <div class="timeline-indicator">\n                <span class="dot"></span>\n                <span class="number">0{i}</span>\n            </div>\n            <section class="step-card">'
    html = html.replace(search_str, replace_str)

html = html.replace('</section>', '</section>\n        </div>')

with open('index.html', 'w', encoding='utf-8') as f:
    f.write(html)
