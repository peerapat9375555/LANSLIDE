# Landslide & Earthquake Prediction System

This project contains the backend server and Android application for the Landslide Prediction System. 

## 🚀 Architecture Updates
In a recent update, the backend architecture was streamlined:
1. **Single Server Architecture**: The former Node.js server (`server.js`) has been completely merged into the Python FastAPI server (`server/main.py`). Everything now runs on a single Python backend listening on **Port 8000**.
2. **OSMDroid Map**: The Android app migrated from Google Maps Compose to **OSMDroid**, making it 100% free and removing the need for any Google API keys.
3. **Database**: Still relies on a local MySQL (XAMPP) instance.

## 📂 Project Structure
- `Landslideproject_cola/` - The Android Kotlin/Jetpack Compose Application.
- `server/main.py` - The FastAPI Backend (handles AI model inference + ALL auth/user endpoints).
- `update_db.sql` - The latest MySQL schema containing all 6 required tables.
- `best_ml_model.pkl` - Pre-trained Random Forest model for landslide risk prediction.
- `.gitignore` - Standard filters (Note: `Landslide_Final_Cleaned_V2.csv` is ignored due to size).

---

## 🛠️ Step 1: Database Setup
1. Open **XAMPP Control Panel** and Start **MySQL**.
2. Open phpMyAdmin (`http://localhost/phpmyadmin`).
3. Create a new database named `landsnot_db`.
4. Import `update_db.sql` to create all required tables (`rain_grids`, `static_nodes`, `users`, `user_pinned_locations`, `prediction_results`, `notifications`).
5. (Optional but recommended) Run `python server/seed_data.py` to populate initial grid geometry.

---

## 🐍 Step 2: Backend Setup (FastAPI)
The backend requires Python 3.9+ (3.11 recommended). It uses `uvicorn` and `fastapi`.

### Option A: Using Conda (Recommended)
```bash
# Create a new conda environment
conda create -n landslide python=3.11 -y

# Activate the environment
conda activate landslide

# Install required dependencies
pip install -r server/requirements.txt

# Start the server (Accessible across network)
uvicorn server.main:app --host 0.0.0.0 --port 8000
```

### Option B: Using Standard Python Virtualenv
```bash
# Create venv
python -m venv venv

# Activate (Windows)
.\venv\Scripts\activate

# Activate (Mac/Linux)
source venv/bin/activate

# Install required dependencies
pip install -r server/requirements.txt

# Start the server
python -m uvicorn server.main:app --host 0.0.0.0 --port 8000
```

*Note: The backend must be running for the Android app to function!*

---

## 📱 Step 3: Android App Setup
1. Open **Android Studio**.
2. Select **Open** and choose the `Landslideproject_cola` folder.
3. Wait for Gradle sync to complete.
4. Open the `app/src/main/java/com/example/landslideproject_cola/EarthquakeClient.kt` file.
5. Change the `BASE_URL`:
   - If running on **Android Studio Emulator**: Use `http://10.0.2.2:8000/`
   - If running on a **Real Android Device**: Use your computer's local Wi-Fi IP address (e.g., `http://192.168.1.xxx:8000/`).
6. Press the **Run** button to install the app on your device/emulator!
