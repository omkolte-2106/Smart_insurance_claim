import requests
import json

urls = [
    "http://127.0.0.1:8000/ml/analyze-damage",
    "http://127.0.0.1:8000/ml/part-damage-detection"
]

images = [
    r"C:\Users\omkol\.gemini\antigravity\brain\52d8940a-b957-486c-bbd7-56bea48feccb\media__1777920473384.png",
    r"C:\Users\omkol\.gemini\antigravity\brain\52d8940a-b957-486c-bbd7-56bea48feccb\media__1777920473450.png",
    r"C:\Users\omkol\.gemini\antigravity\brain\52d8940a-b957-486c-bbd7-56bea48feccb\media__1777920473481.png"
]

for idx, img_path in enumerate(images):
    print(f"\n--- Image {idx + 1} ---")
    try:
        with open(img_path, 'rb') as f:
            files = {'file': (f"image{idx}.png", f, 'image/png')}
            resp = requests.post(urls[1], files=files)
            print("Parts Detected:", json.dumps(resp.json(), indent=2))
        
        with open(img_path, 'rb') as f:
            files = {'file': (f"image{idx}.png", f, 'image/png')}
            resp = requests.post(urls[0], files=files)
            print("Severity:", json.dumps(resp.json(), indent=2))
    except Exception as e:
        print("Error:", e)
