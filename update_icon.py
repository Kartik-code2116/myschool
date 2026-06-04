import os
from PIL import Image

logo_path = r"C:\Users\KARTIK DILIP THORAT\.gemini\antigravity-ide\brain\ae0fe7e3-c096-433b-9555-46eba9c1bb42\myschool_app_logo_1780598603808.png"
base_res_dir = r"d:\8)Android Development\myschool\app\src\main\res"

sizes = {
    "mdpi": (48, 108),
    "hdpi": (72, 162),
    "xhdpi": (96, 216),
    "xxhdpi": (144, 324),
    "xxxhdpi": (192, 432)
}

try:
    img = Image.open(logo_path).convert("RGBA")
    
    for density, (legacy_size, adaptive_size) in sizes.items():
        dir_path = os.path.join(base_res_dir, f"mipmap-{density}")
        os.makedirs(dir_path, exist_ok=True)
        
        # Legacy icons
        legacy_img = img.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS)
        legacy_img.save(os.path.join(dir_path, "ic_launcher.png"))
        legacy_img.save(os.path.join(dir_path, "ic_launcher_round.png"))
        
        # Adaptive foreground
        adaptive_img = img.resize((adaptive_size, adaptive_size), Image.Resampling.LANCZOS)
        adaptive_img.save(os.path.join(dir_path, "ic_launcher_foreground.png"))
        print(f"Generated icons for {density}")
        
    # Delete the vector drawable so it uses the PNGs in mipmap
    vector_drawable = os.path.join(base_res_dir, "drawable", "ic_launcher_foreground.xml")
    if os.path.exists(vector_drawable):
        os.remove(vector_drawable)
        print("Removed old vector drawable.")
        
    # Update ic_launcher.xml and ic_launcher_round.xml in mipmap-anydpi-v26 to use @mipmap/ic_launcher_foreground
    anydpi_dir = os.path.join(base_res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)
    xml_content = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>'''
    
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding='utf-8') as f:
        f.write(xml_content)
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding='utf-8') as f:
        f.write(xml_content)
    print("Updated adaptive icon XMLs.")
        
    print("Icon updated successfully.")
except Exception as e:
    print(f"Error: {e}")
