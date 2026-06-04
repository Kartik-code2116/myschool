import os
import re

directory = r"d:\8)Android Development\myschool\app\src\main\res\layout"
files_to_update = ["activity_splash.xml", "activity_login.xml", "nav_header.xml"]

for file in os.listdir(directory):
    if file in files_to_update:
        filepath = os.path.join(directory, file)
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Replace @drawable/ic_school with @drawable/app_logo
            new_content = content.replace("@drawable/ic_school", "@drawable/app_logo")
            
            # Remove app:tint and android:tint because the new logo is full color
            new_content = re.sub(r'\s*app:tint="[^"]*"', '', new_content)
            new_content = re.sub(r'\s*android:tint="[^"]*"', '', new_content)
            
            # Specifically for Splash and Login, they might have padding or smaller sizes, but let's keep them as is.
            # Actually, the splash has a 64dp image inside a 110dp card. The new logo has its own white background.
            # Let's change the layout_width/height to match_parent for splash and login logo ImageViews if they are inside a card.
            
            if new_content != content:
                with open(filepath, 'w', encoding='utf-8', newline='') as f:
                    f.write(new_content)
                print(f"Updated {file}")
        except Exception as e:
            print(f"Error processing {file}: {e}")
