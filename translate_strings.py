import os
import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time

res_dir = r'd:\8)Android Development\myschool\app\src\main\res'
strings_file = os.path.join(res_dir, 'values', 'strings.xml')
values_mr_dir = os.path.join(res_dir, 'values-mr')
if not os.path.exists(values_mr_dir):
    os.makedirs(values_mr_dir)

out_file = os.path.join(values_mr_dir, 'strings.xml')

tree = ET.parse(strings_file)
root = tree.getroot()

translator = GoogleTranslator(source='auto', target='mr')

def contains_marathi(text):
    # Basic check for Devanagari block
    return any('\u0900' <= c <= '\u097F' for c in text)

total = len(root.findall('string'))
print(f"Total strings to process: {total}")

translated_count = 0
for i, elem in enumerate(root.findall('string')):
    text = elem.text
    if text:
        # Don't translate if it's already Marathi, or just symbols/numbers
        if not contains_marathi(text) and any(c.isalpha() for c in text):
            try:
                # Replace format placeholders temporarily
                temp_text = text.replace('%s', 'XXXX').replace('%1$s', 'YYYY').replace('%d', 'ZZZZ').replace('%1$d', 'WWWW').replace('%2$s', 'AAAA').replace('%2$d', 'BBBB')
                
                translated = translator.translate(temp_text)
                
                # Restore placeholders
                translated = translated.replace('XXXX', '%s').replace('YYYY', '%1$s').replace('ZZZZ', '%d').replace('WWWW', '%1$d').replace('AAAA', '%2$s').replace('BBBB', '%2$d')
                
                elem.text = translated
                translated_count += 1
                
                if translated_count % 50 == 0:
                    print(f"Translated {translated_count} / {total}...")
                    
                time.sleep(0.1) # Be nice to the API
            except Exception as e:
                print(f"Failed to translate: {text} - {e}")
    
tree.write(out_file, encoding='utf-8', xml_declaration=True)
print(f"Finished! Translated {translated_count} new strings. Saved to values-mr/strings.xml")
