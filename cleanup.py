import os
import re

java_dir = r'd:\8)Android Development\myschool\app\src\main\java'
strings_file = r'd:\8)Android Development\myschool\app\src\main\res\values\strings.xml'
values_mr_strings = r'd:\8)Android Development\myschool\app\src\main\res\values-mr\strings.xml'

def fix_java_files():
    count = 0
    for root, dirs, files in os.walk(java_dir):
        for file in files:
            if file.endswith('.java'):
                path = os.path.join(root, file)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Replace R.string.str_empty -> R.string.msg_empty
                new_content = re.sub(r'R\.string\.str_empty(_\d+)?', r'R.string.msg_empty\1', content)
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    count += 1
    print(f'Fixed {count} Java files.')

def fix_xml_file(filepath):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    in_java_section = False
    modified = False
    
    for i, line in enumerate(lines):
        if 'name="msg_class_saved"' in line:
            in_java_section = True
        
        if in_java_section and 'name="str_empty' in line:
            lines[i] = re.sub(r'name="str_empty(_\d+)?"', r'name="msg_empty\1"', line)
            modified = True
            
    if modified:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(lines)
        print(f'Fixed duplicates in {filepath}')

fix_java_files()
fix_xml_file(strings_file)
fix_xml_file(values_mr_strings)
