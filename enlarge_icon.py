from PIL import Image, ImageChops
import os

logo_path = r"C:\Users\KARTIK DILIP THORAT\.gemini\antigravity-ide\brain\ae0fe7e3-c096-433b-9555-46eba9c1bb42\myschool_app_logo_1780598603808.png"
base_res_dir = r"d:\8)Android Development\myschool\app\src\main\res"

sizes = {
    "mdpi": (48, 108),
    "hdpi": (72, 162),
    "xhdpi": (96, 216),
    "xxhdpi": (144, 324),
    "xxxhdpi": (192, 432)
}

def trim(im):
    bg = Image.new(im.mode, im.size, (255, 255, 255, 255))
    diff = ImageChops.difference(im, bg)
    diff = ImageChops.add(diff, diff, 2.0, -100)
    bbox = diff.getbbox()
    if bbox:
        return im.crop(bbox)
    return im

try:
    img = Image.open(logo_path).convert("RGBA")
    
    # 1. Trim the excess white background so the logo tightly fits
    trimmed_img = trim(img)
    
    # 2. To make it bigger in the adaptive icon, we scale it to take up about 70-80% of the 108dp canvas.
    # The "safe zone" is 72dp. So if we scale it to ~85dp, it will be quite large.
    
    for density, (legacy_size, adaptive_size) in sizes.items():
        dir_path = os.path.join(base_res_dir, f"mipmap-{density}")
        os.makedirs(dir_path, exist_ok=True)
        
        # Legacy icons: usually take up the full size (but a bit of padding is good)
        pad_legacy = int(legacy_size * 0.05)
        target_legacy = legacy_size - (2 * pad_legacy)
        
        legacy_scale = min(target_legacy / trimmed_img.width, target_legacy / trimmed_img.height)
        new_w_leg = int(trimmed_img.width * legacy_scale)
        new_h_leg = int(trimmed_img.height * legacy_scale)
        
        resized_for_legacy = trimmed_img.resize((new_w_leg, new_h_leg), Image.Resampling.LANCZOS)
        
        legacy_canvas = Image.new("RGBA", (legacy_size, legacy_size), (255, 255, 255, 255))
        offset_leg = ((legacy_size - new_w_leg) // 2, (legacy_size - new_h_leg) // 2)
        legacy_canvas.paste(resized_for_legacy, offset_leg, resized_for_legacy)
        
        legacy_canvas.save(os.path.join(dir_path, "ic_launcher.png"))
        legacy_canvas.save(os.path.join(dir_path, "ic_launcher_round.png"))
        
        # Adaptive foreground
        # Let's make it take up 70% of the adaptive size (adaptive size is 108dp)
        # 108 * 0.70 = ~76dp (which is larger than the safe zone of 72dp, but some clipping is fine for full bleed, or we keep it around 66dp)
        # If user wants it BIGGER, let's do 75% so it's very prominent.
        pad_adaptive = int(adaptive_size * 0.125) # 12.5% padding each side = 25% total = 75% logo size
        target_adaptive = adaptive_size - (2 * pad_adaptive)
        
        adaptive_scale = min(target_adaptive / trimmed_img.width, target_adaptive / trimmed_img.height)
        new_w_adp = int(trimmed_img.width * adaptive_scale)
        new_h_adp = int(trimmed_img.height * adaptive_scale)
        
        resized_for_adaptive = trimmed_img.resize((new_w_adp, new_h_adp), Image.Resampling.LANCZOS)
        
        # The background of adaptive is often handled by ic_launcher_background.xml, 
        # but since we deleted the XMLs before, let's make the foreground have a transparent background 
        # so the standard white background shows through, or just fill it with white.
        # Actually, Android 8+ expects foreground to be transparent if there's a background layer.
        adaptive_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
        offset_adp = ((adaptive_size - new_w_adp) // 2, (adaptive_size - new_h_adp) // 2)
        adaptive_canvas.paste(resized_for_adaptive, offset_adp, resized_for_adaptive)
        
        adaptive_canvas.save(os.path.join(dir_path, "ic_launcher_foreground.png"))
        
    print("Enlarged icons generated successfully.")
except Exception as e:
    print(f"Error: {e}")
