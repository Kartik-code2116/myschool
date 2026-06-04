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
    # Convert to RGB to ignore alpha for diffing
    bg = Image.new('RGB', im.size, (255, 255, 255))
    diff = ImageChops.difference(im.convert('RGB'), bg)
    diff = ImageChops.add(diff, diff, 2.0, -100)
    bbox = diff.getbbox()
    if bbox:
        # Give a very tiny 2% padding around the trimmed image so it doesn't touch the literal pixels
        pad = int(min(im.width, im.height) * 0.02)
        bbox = (
            max(0, bbox[0] - pad),
            max(0, bbox[1] - pad),
            min(im.width, bbox[2] + pad),
            min(im.height, bbox[3] + pad)
        )
        return im.crop(bbox)
    return im

try:
    img = Image.open(logo_path).convert("RGBA")
    
    # Trim the excess white background
    trimmed_img = trim(img)
    
    # 1. Save trimmed image for the app UI so it appears much larger in splash/login screens
    drawable_dir = os.path.join(base_res_dir, "drawable-nodpi")
    os.makedirs(drawable_dir, exist_ok=True)
    trimmed_img.save(os.path.join(drawable_dir, "app_logo.png"))
    print("Saved trimmed app_logo.png for in-app UI.")
    
    # 2. Update mipmaps to make the icon even bigger
    for density, (legacy_size, adaptive_size) in sizes.items():
        dir_path = os.path.join(base_res_dir, f"mipmap-{density}")
        os.makedirs(dir_path, exist_ok=True)
        
        # Legacy icons: take up almost the full size (just a tiny 4% padding)
        pad_legacy = int(legacy_size * 0.04)
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
        # 108dp canvas, safe zone is 72dp.
        # We will make the logo size 82dp (which is larger than safe zone, making it HUGE and full bleed).
        # padding = (108 - 82)/2 = 13dp -> ratio is ~ 12%
        pad_adaptive = int(adaptive_size * 0.12)
        target_adaptive = adaptive_size - (2 * pad_adaptive)
        
        adaptive_scale = min(target_adaptive / trimmed_img.width, target_adaptive / trimmed_img.height)
        new_w_adp = int(trimmed_img.width * adaptive_scale)
        new_h_adp = int(trimmed_img.height * adaptive_scale)
        
        resized_for_adaptive = trimmed_img.resize((new_w_adp, new_h_adp), Image.Resampling.LANCZOS)
        
        adaptive_canvas = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
        offset_adp = ((adaptive_size - new_w_adp) // 2, (adaptive_size - new_h_adp) // 2)
        adaptive_canvas.paste(resized_for_adaptive, offset_adp, resized_for_adaptive)
        
        adaptive_canvas.save(os.path.join(dir_path, "ic_launcher_foreground.png"))
        
    print("Maximally enlarged icons generated successfully.")
except Exception as e:
    print(f"Error: {e}")
