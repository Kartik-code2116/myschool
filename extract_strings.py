import os
import re
import xml.etree.ElementTree as ET

res_dir = r'd:\8)Android Development\myschool\app\src\main\res'
layout_dir = os.path.join(res_dir, 'layout')
menu_dir = os.path.join(res_dir, 'menu')
strings_file = os.path.join(res_dir, 'values', 'strings.xml')

# We'll use regex to find android:text="..." and android:hint="..." and app:title="..."
pattern = re.compile(r'(android:text|android:hint|app:title|android:title)="([^@\n]+?)"')

extracted_strings = {}

def sanitize_id(text):
    s = re.sub(r'[^a-zA-Z0-9]+', '_', text.strip().lower())
    s = s.strip('_')
    if not s:
        return 'str_empty'
    if len(s) > 30:
        s = s[:30].strip('_')
    return 'txt_' + s

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    
    def replacer(match):
        nonlocal modified
        attr = match.group(1)
        val = match.group(2)
        if val.startswith('@') or val.startswith('?') or '||' in val or '{' in val:
            return match.group(0) # Keep dynamic/bound/resource strings
        
        # Extract everything that contains alphabet characters
        if not any(c.isalpha() for c in val):
            return match.group(0)

        # Skip formatting placeholders if they are the only thing
        if val == '%s' or val == '%1$s':
             return match.group(0)

        modified = True
        str_id = sanitize_id(val)
        
        # Ensure uniqueness
        base_id = str_id
        counter = 1
        while str_id in extracted_strings and extracted_strings[str_id] != val:
            str_id = f'{base_id}_{counter}'
            counter += 1
            
        extracted_strings[str_id] = val
        return f'{attr}="@string/{str_id}"'

    new_content = pattern.sub(replacer, content)
    
    if modified:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)

for d in [layout_dir, menu_dir]:
    if os.path.exists(d):
        for root, dirs, files in os.walk(d):
            for file in files:
                if file.endswith('.xml'):
                    process_file(os.path.join(root, file))

print(f'Extracted {len(extracted_strings)} strings.')

# Now append to strings.xml
with open(strings_file, 'r', encoding='utf-8') as f:
    strings_content = f.read()

# Find the closing </resources>
closing_tag = '</resources>'
if closing_tag in strings_content and extracted_strings:
    new_strings = ''
    for k, v in extracted_strings.items():
        # Escape characters
        escaped_val = v.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace("'", "\\'")
        new_strings += f'    <string name="{k}">{escaped_val}</string>\n'
    
    final_content = strings_content.replace(closing_tag, new_strings + closing_tag)
    with open(strings_file, 'w', encoding='utf-8') as f:
        f.write(final_content)
    print('Updated strings.xml')
else:
    print('No strings extracted or missing closing tag.')
