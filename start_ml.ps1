# SmartInsure ML Service Startup Script
# Run this from the project root directory

Write-Host "Starting SmartInsure ML Service..." -ForegroundColor Cyan

cd ml-service

if (!(Test-Path ".venv")) {
    Write-Host "Virtual environment not found. Creating one..." -ForegroundColor Yellow
    python -m venv .venv
}

Write-Host "Activating virtual environment..." -ForegroundColor Green
& ".\.venv\Scripts\Activate.ps1"

Write-Host "Installing/Updating dependencies..." -ForegroundColor Green
pip install -r requirements.txt
pip install "uvicorn[standard]"

Write-Host "Launching FastAPI server on port 8000..." -ForegroundColor Green
uvicorn app.main:app --reload --port 8000
