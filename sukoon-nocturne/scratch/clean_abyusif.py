import subprocess
import os
import sys

ADB = r"C:\Users\Pc\AppData\Local\Android\Sdk\platform-tools\adb.exe"

files_to_clean = [
    {
        "old": "Sa3eed - سعيد(MP3_160K).mp3",
        "new": "Sa3eed.mp3",
        "title": "Sa3eed",
        "artist": "Abyusif",
        "album": "Sa3eed"
    },
    {
        "old": "Chroma - كروما(MP3_160K).mp3",
        "new": "Chroma.mp3",
        "title": "Chroma",
        "artist": "Abyusif",
        "album": "Chroma"
    },
    {
        "old": "DO - دو (Prod. Abyusif)(MP3_320K).mp3",
        "new": "DO.mp3",
        "title": "DO",
        "artist": "Abyusif",
        "album": "DO"
    },
    {
        "old": "Abyusif_EL_GHADAB_Prod_by_Abyusif_أبيوسف_الغضبMP3_160K.mp3",
        "new": "El Ghadab.mp3",
        "title": "El Ghadab",
        "artist": "Abyusif",
        "album": "El Ghadab"
    },
    {
        "old": "Abyusif_MABALASH_Prod_Abyusif_أبيوسف_ما_بلاشMP3_160K.mp3",
        "new": "Mabalash.mp3",
        "title": "Mabalash",
        "artist": "Abyusif",
        "album": "Mabalash"
    },
    {
        "old": "ابوسليمان - Abyusif-Reda -ابيوسف-رضا.mp3",
        "new": "Reda.mp3",
        "title": "Reda",
        "artist": "Abyusif",
        "album": "Reda"
    }
]

dir_path = "/storage/emulated/0/Music/Telegram"

for item in files_to_clean:
    old_file = os.path.join(dir_path, item["old"]).replace("\\", "/")
    new_file = os.path.join(dir_path, item["new"]).replace("\\", "/")
    
    # 1. Get stat mtime
    check_cmd = f'[ -f "{old_file}" ] && echo "FOUND" || echo "NOT_FOUND"'
    res = subprocess.check_output([ADB, "shell", check_cmd]).decode().strip()
    if res != "FOUND":
        print(f"Skipping {old_file}: not found")
        continue

    # Get exact timestamps
    stat_cmd = f'stat -c "%Y %X" "{old_file}"'
    stat_out = subprocess.check_output([ADB, "shell", stat_cmd]).decode().strip().split()
    mtime, atime = stat_out[0], stat_out[1]

    # Rename
    mv_cmd = f'mv "{old_file}" "{new_file}"'
    subprocess.check_call([ADB, "shell", mv_cmd])

    # Restore exact timestamps
    touch_cmd = f'touch -d "@{mtime}" "{new_file}"'
    subprocess.check_call([ADB, "shell", touch_cmd])

    # Scan old and new in MediaStore
    subprocess.call([ADB, "shell", f'am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://{old_file}"'])
    subprocess.call([ADB, "shell", f'am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://{new_file}"'])
    print(f"Successfully cleaned: {item['old']} -> {item['new']} (mtime: {mtime})")

print("Done!")
