   cd ml-service
   python -m venv .venv
   .\.venv\Scripts\activate   # Use `source .venv/bin/activate` on Mac/Linux
   pip install -r requirements.txt
   uvicorn app.main:app --reload --port 8090
   