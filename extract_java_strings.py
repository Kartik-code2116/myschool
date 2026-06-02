import os
import re

java_dir = r'd:\8)Android Development\myschool\app\src\main\java'
strings_file = r'd:\8)Android Development\myschool\app\src\main\res\values\strings.xml'

# Regex to find set[Text|Title]("...") or Toast.makeText(..., "...", ...)
# Note: This is a bit brittle, but handles basic cases.
pattern_toast = re.compile(r'Toast\.makeText\(\s*[^,]+,\s*"([^"\n]+)"\s*,')
pattern_settext = re.compile(r'\.setText\(\s*"([^"\n]+)"\s*\)')
pattern_settitle = re.compile(r'\.setTitle\(\s*"([^"\n]+)"\s*\)')

extracted_strings = {}

def sanitize_id(text):
    s = re.sub(r'[^a-zA-Z0-9]+', '_', text.strip().lower())
    s = s.strip('_')
    if not s:
        return 'str_empty'
    if len(s) > 30:
        s = s[:30].strip('_')
    return 'msg_' + s

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    
    def handle_match(val):
        nonlocal modified
        # Skip if dynamic/placeholder or no alphabets
        if val.startswith('@') or val.startswith('?') or val == '%s' or not any(c.isalpha() for c in val):
            return None
            
        modified = True
        str_id = sanitize_id(val)
        
        base_id = str_id
        counter = 1
        while str_id in extracted_strings and extracted_strings[str_id] != val:
            str_id = f'{base_id}_{counter}'
            counter += 1
            
        extracted_strings[str_id] = val
        return str_id

    # We need to replace carefully
    # Let's replace Toast.makeText(..., "val", ...) -> Toast.makeText(..., getString(R.string.str_id), ...)
    def replacer_toast(match):
        val = match.group(1)
        str_id = handle_match(val)
        if str_id:
            # Reconstruct the call but use getString()
            # wait, Toast context can be `this`, `requireContext()`, `getContext()`.
            # To be safe, we might need context.getString(R.string.x), but often in fragments getContext() or requireContext() is needed.
            # actually, R.string.x is just an int, we can pass it directly to Toast.makeText(context, resId, duration)
            # So Toast.makeText(ctx, "val", len) -> Toast.makeText(ctx, R.string.str_id, len)
            full_match = match.group(0)
            return full_match.replace(f'"{val}"', f'R.string.{str_id}')
        return match.group(0)

    # For setText / setTitle we can usually pass R.string.str_id if it's a TextView, but some setTexts might require string.
    # Let's stick to using R.string.str_id if the method is overloaded for int resId.
    # .setText() and .setTitle() take int resId!
    def replacer_set(match):
        val = match.group(1)
        str_id = handle_match(val)
        if str_id:
            full_match = match.group(0)
            return full_match.replace(f'"{val}"', f'R.string.{str_id}')
        return match.group(0)

    content = pattern_toast.sub(replacer_toast, content)
    content = pattern_settext.sub(replacer_set, content)
    content = pattern_settitle.sub(replacer_set, content)
    
    if modified:
        # Also need to import R if not present
        if 'import com.example.myschool.R;' not in content and 'import android.' not in content:
            # We might not strictly need it if it's in the same package, but let's add it carefully.
            # Actually, R is in com.example.myschool, which is the base package for all fragments/activities.
            pass
            
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)

for root, dirs, files in os.walk(java_dir):
    for file in files:
        if file.endswith('.java'):
            process_file(os.path.join(root, file))

print(f'Extracted {len(extracted_strings)} java strings.')

with open(strings_file, 'r', encoding='utf-8') as f:
    strings_content = f.read()

closing_tag = '</resources>'
if closing_tag in strings_content and extracted_strings:
    new_strings = ''
    for k, v in extracted_strings.items():
        escaped_val = v.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace("'", "\\'")
        new_strings += f'    <string name="{k}">{escaped_val}</string>\n'
    
    final_content = strings_content.replace(closing_tag, new_strings + closing_tag)
    with open(strings_file, 'w', encoding='utf-8') as f:
        f.write(final_content)
    print('Updated strings.xml with Java strings')
else:
    print('No java strings extracted or missing closing tag.')
