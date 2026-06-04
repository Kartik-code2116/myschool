import os

search_text = "com.example.myschool"
replace_text = "com.kartik.myschool"
extensions = ['.java', '.xml', '.kt', '.json', '.pro', '.gradle', '.kts']

updated_files = []
for root, dirs, files in os.walk(r'd:\8)Android Development\myschool'):
    if '.git' in root or '.gradle' in root or 'build' in root or '.idea' in root:
        continue
    for file in files:
        if any(file.endswith(ext) for ext in extensions):
            filepath = os.path.join(root, file)
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                if search_text in content:
                    new_content = content.replace(search_text, replace_text)
                    with open(filepath, 'w', encoding='utf-8', newline='') as f:
                        f.write(new_content)
                    updated_files.append(filepath)
            except Exception as e:
                print(f"Failed to process {filepath}: {e}")

print(f"Updated {len(updated_files)} files.")
