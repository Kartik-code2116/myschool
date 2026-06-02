import os
import xml.etree.ElementTree as ET

def append_strings(filepath, new_strings):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Insert before the last </resources>
    insert_pos = content.rfind('</resources>')
    if insert_pos == -1:
        print(f"Error: </resources> not found in {filepath}")
        return
        
    xml_to_insert = "\n" + "\n".join(new_strings) + "\n"
    
    new_content = content[:insert_pos] + xml_to_insert + content[insert_pos:]
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f"Updated {filepath}")


en_strings = [
    '    <string name="fmt_school_name" formatted="false">Name: %s</string>',
    '    <string name="fmt_school_udise" formatted="false">UDISE Code: %s</string>',
    '    <string name="fmt_school_board" formatted="false">Board: %s</string>',
    '    <string name="fmt_school_address" formatted="false">Address: %s</string>',
    '    <string name="btn_edit_school">Edit Details</string>',
    '    <string name="title_edit_school">Edit School Details</string>'
]

mr_strings = [
    '    <string name="fmt_school_name" formatted="false">नाव: %s</string>',
    '    <string name="fmt_school_udise" formatted="false">UDISE कोड: %s</string>',
    '    <string name="fmt_school_board" formatted="false">बोर्ड: %s</string>',
    '    <string name="fmt_school_address" formatted="false">पत्ता: %s</string>',
    '    <string name="btn_edit_school">तपशील संपादित करा</string>',
    '    <string name="title_edit_school">शाळेचा तपशील संपादित करा</string>'
]

append_strings(r'd:\8)Android Development\myschool\app\src\main\res\values\strings.xml', en_strings)
append_strings(r'd:\8)Android Development\myschool\app\src\main\res\values-mr\strings.xml', mr_strings)
