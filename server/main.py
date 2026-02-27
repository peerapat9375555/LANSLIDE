from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
import pandas as pd
import numpy as np
import joblib
import json
import asyncio
import httpx
from pydantic import BaseModel
import datetime
import math
from typing import List, Optional
import os
import mysql.connector
import uuid
import bcrypt
import jwt as pyjwt

app = FastAPI()

# CORS — อนุญาตทุก origin (สำหรับ dev)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

SECRET_KEY = "landslide_secret_key_2025"

# Global states
STATIC_DATA_CACHE = None
ML_MODEL = None
SCALER = None

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Database configuraton
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'landsnot_db',
}

class PinRequest(BaseModel):
    user_id: str
    latitude: float
    longitude: float
    label: Optional[str] = None

class RegisterRequest(BaseModel):
    name: str
    phone: Optional[str] = None
    email: str
    password: str
    role: Optional[str] = "user"

class LoginRequest(BaseModel):
    email: str
    password: str

class PredictionResponseItem(BaseModel):
    id: str
    polygon: List[List[float]]
    color: str

def get_db_connection():
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except Exception as e:
        print(f"Error connecting to DB: {e}")
        return None

def load_resources():
    global STATIC_DATA_CACHE, ML_MODEL, SCALER
    
    print("Loading ML Model...")
    try:
        ML_MODEL = joblib.load(os.path.join(PROJECT_ROOT, 'best_ml_model.pkl'))
    except Exception as e:
        print("Warning: best_ml_model.pkl not found.")
        
    print("Loading Scaler...")
    try:
        SCALER = joblib.load(os.path.join(PROJECT_ROOT, 'landslide_scaler.pkl'))
    except Exception as e:
        print("Warning: landslide_scaler.pkl not found.")
        
    print("Loading Static Nodes from Database into Cache...")
    conn = get_db_connection()
    if conn:
        try:
            query = "SELECT * FROM static_nodes"
            STATIC_DATA_CACHE = pd.read_sql(query, conn)
            print(f"Loaded {len(STATIC_DATA_CACHE)} static nodes.")
        except Exception as e:
            print(f"Failed to load static_nodes table: {e}")
        finally:
            conn.close()

@app.on_event("startup")
async def startup_event():
    load_resources()

# Calculate polygon corners (2x2km box)
def calculate_2x2_polygon(lat, lon):
    lat_offset = 1.0 / 110.574
    lon_offset = 1.0 / (111.320 * math.cos(math.radians(lat)))
    
    return [
        [lat + lat_offset, lon - lon_offset], # Top Left
        [lat + lat_offset, lon + lon_offset], # Top Right
        [lat - lat_offset, lon + lon_offset], # Bottom Right
        [lat - lat_offset, lon - lon_offset]  # Bottom Left
    ]

async def fetch_weather_for_grid(client, grid_id, lat, lon):
    url = f"https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&daily=precipitation_sum&past_days=10&forecast_days=1&timezone=auto"
    try:
        response = await client.get(url, timeout=10.0)
        data = response.json()
        precip = data.get('daily', {}).get('precipitation_sum', [0]*11)
        return {"grid_id": grid_id, "lat": lat, "lon": lon, "rain": precip[:10]}
    except Exception as e:
        print(f"Error fetching Open-Meteo for grid {grid_id}: {e}")
        return {"grid_id": grid_id, "lat": lat, "lon": lon, "rain": [0]*10}

async def fetch_weather_batch(grids):
    semaphore = asyncio.Semaphore(5)
    
    async def fetch_with_delay(client, g):
        async with semaphore:
            await asyncio.sleep(0.2)
            return await fetch_weather_for_grid(client, g['grid_id'], g['lat'], g['lon'])

    async with httpx.AsyncClient() as client:
        tasks = [fetch_with_delay(client, g) for g in grids]
        results = await asyncio.gather(*tasks)
        
    return results

# 1. POST /api/pins : Find nearest node_id and insert pin
@app.post("/api/pins")
async def create_pin(pin: PinRequest):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
        
    try:
        cursor = conn.cursor(dictionary=True)
        # O(1) query finding Euclidean nearest node_id from static_nodes
        query_node = """
        SELECT node_id FROM static_nodes 
        ORDER BY (POW(latitude - %s, 2) + POW(longitude - %s, 2)) ASC 
        LIMIT 1
        """
        cursor.execute(query_node, (pin.latitude, pin.longitude))
        result = cursor.fetchone()
        
        if not result:
            raise HTTPException(status_code=404, detail="No static geographic nodes found in database")
            
        nearest_node_id = result['node_id']
        pin_id = str(uuid.uuid4())
        
        insert_pin = """
        INSERT INTO user_pinned_locations (pin_id, user_id, latitude, longitude, label, nearest_node_id)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        cursor.execute(insert_pin, (pin_id, pin.user_id, pin.latitude, pin.longitude, pin.label, nearest_node_id))
        conn.commit()
        
        return {
            "status": "success", 
            "pin_id": pin_id, 
            "nearest_node_id": nearest_node_id, 
            "message": "Pinned location mapped to nearest geographic node successfully."
        }
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

@app.post("/trigger-prediction")
async def trigger_prediction():
    if STATIC_DATA_CACHE is None or STATIC_DATA_CACHE.empty:
        # Reload attempt
        load_resources()
        if STATIC_DATA_CACHE is None or STATIC_DATA_CACHE.empty:
            raise HTTPException(status_code=500, detail="Static cache empty. Insert nodes into static_nodes table first.")
    
    df = STATIC_DATA_CACHE.copy()
    
    unique_grids = df[['grid_id']].drop_duplicates()
    grids_to_fetch = []
    
    for _, row in unique_grids.iterrows():
        # pick representative node coordinates for this grid
        grid_data = df[df['grid_id'] == row['grid_id']].iloc[0]
        grids_to_fetch.append({
            'grid_id': row['grid_id'],
            'lat': grid_data['latitude'],
            'lon': grid_data['longitude']
        })
        
    rain_results = await fetch_weather_batch(grids_to_fetch)
    rain_map = {r['grid_id']: r['rain'] for r in rain_results}
    
    # Optional: Update rain_grids table to store current weather
    conn = get_db_connection()
    if conn:
        cursor = conn.cursor()
        grid_inserts = []
        for r in rain_results:
            grid_inserts.append((r['grid_id'], float(r['lat']), float(r['lon']), json.dumps(r['rain'])))
        try:
            # Upsert into rain_grids
            cursor.executemany("""
                INSERT INTO rain_grids (grid_id, center_lat, center_long, rain_values_json) 
                VALUES (%s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE 
                center_lat=VALUES(center_lat), center_long=VALUES(center_long), rain_values_json=VALUES(rain_values_json), last_updated=CURRENT_TIMESTAMP
            """, grid_inserts)
            conn.commit()
        except Exception as e:
            conn.rollback()
            print(f"Warning: Failed to log to rain_grids: {e}")
    
    for day in range(1, 11):
        df[f'CHIRPS_Day_{day}'] = df['grid_id'].apply(lambda g: rain_map.get(g, [0]*10)[day-1])
        
    df['Rain_3D_Prior'] = df['CHIRPS_Day_1'] + df['CHIRPS_Day_2'] + df['CHIRPS_Day_3']
    df['Rain_5D_Prior'] = df['Rain_3D_Prior'] + df['CHIRPS_Day_4'] + df['CHIRPS_Day_5']
    df['Rain_7D_Prior'] = df['Rain_5D_Prior'] + df['CHIRPS_Day_6'] + df['CHIRPS_Day_7']
    df['Rain_10D_Prior'] = df['Rain_7D_Prior'] + df['CHIRPS_Day_8'] + df['CHIRPS_Day_9'] + df['CHIRPS_Day_10']
    
    df['Rain3D_x_Slope'] = df['Rain_3D_Prior'] * df['slope_extracted']
    df['Rain5D_x_Slope'] = df['Rain_5D_Prior'] * df['slope_extracted']
    df['Rain7D_x_Slope'] = df['Rain_7D_Prior'] * df['slope_extracted']
    df['Rain10D_x_Slope'] = df['Rain_10D_Prior'] * df['slope_extracted']
    
    FEATURE_ORDER = [
        'CHIRPS_Day_1', 'CHIRPS_Day_2', 'CHIRPS_Day_3', 'CHIRPS_Day_4', 'CHIRPS_Day_5', 
        'CHIRPS_Day_6', 'CHIRPS_Day_7', 'CHIRPS_Day_8', 'CHIRPS_Day_9', 'CHIRPS_Day_10', 
        'elevation_extracted', 'slope_extracted', 'aspect_extracted', 'modis_lc', 
        'ndvi', 'ndwi', 'twi', 'soil_type', 'road_zone', 
        'Rain_3D_Prior', 'Rain_5D_Prior', 'Rain_7D_Prior', 'Rain_10D_Prior',
        'Rain3D_x_Slope', 'Rain5D_x_Slope', 'Rain7D_x_Slope', 'Rain10D_x_Slope'
    ]
    
    X_vals = df[FEATURE_ORDER].fillna(0).values
    
    if SCALER:
        X_vals = SCALER.transform(X_vals)
        
    probs = ML_MODEL.predict_proba(X_vals)[:, 1] if ML_MODEL.classes_.shape[0] > 1 else ML_MODEL.predict(X_vals)
    
    # Compile Logs and Notifications
    log_inserts = []
    notification_inserts = []
    response_payload = []
    
    for i, row in df.iterrows():
        node_id = row['node_id']
        prob = float(probs[i])
        
        risk = "Low"
        color = "#00FF00"
        if prob >= 0.70:
            risk = "High"
            color = "#FF0000"
        elif prob >= 0.35:
            risk = "Medium"
            color = "#FFFF00"
            
        log_id = str(uuid.uuid4())
        log_inserts.append((log_id, node_id, risk, prob))
        
        if conn and risk == "High":
            # 2. Trigger Notification logic
            try:
                cursor.execute("SELECT user_id FROM user_pinned_locations WHERE nearest_node_id = %s", (node_id,))
                pinned_users = cursor.fetchall()
                for user_result in pinned_users:
                    notif_id = str(uuid.uuid4())
                    notification_inserts.append((
                        notif_id, 
                        user_result[0], 
                        log_id, 
                        "High Risk Alert", 
                        "A critical landslide risk has been detected near your pinned location over the next 24 hours."
                    ))
            except Exception as e:
                print(f"Error querying users for notification: {e}")
                
        poly = calculate_2x2_polygon(row['latitude'], row['longitude'])
        response_payload.append({
            "id": str(node_id),
            "latitude": float(row['latitude']),
            "longitude": float(row['longitude']),
            "risk_level": risk,
            "color": color,
            "polygon": poly
        })
        
    if conn:
        try:
            # Batch insert prediction_logs
            if log_inserts:
                cursor.executemany("INSERT INTO prediction_logs (log_id, node_id, risk_level, probability) VALUES (%s, %s, %s, %s)", log_inserts)
            
            # Batch insert notifications
            if notification_inserts:
                cursor.executemany("INSERT INTO notifications (notification_id, user_id, log_id, title, message) VALUES (%s, %s, %s, %s, %s)", notification_inserts)
                
            conn.commit()
        except Exception as e:
            conn.rollback()
            print(f"Failed to save prediction logs or notifications: {e}")
        finally:
            cursor.close()
            conn.close()
            
    with open(os.path.join(PROJECT_ROOT, 'latest_predictions.json'), 'w') as f:
        json.dump(response_payload, f)
        
    return {"status": "success", "grids_fetched": len(unique_grids), "points_predicted": len(response_payload), "notifications_triggered": len(notification_inserts)}


@app.get("/api/predictions", response_model=List[PredictionResponseItem])
async def get_predictions():
    # Return sorted by Risk where Red (High) is at the back of the array so it draws ON TOP (Z-Index rendering)
    try:
        with open(os.path.join(PROJECT_ROOT, 'latest_predictions.json'), 'r') as f:
            data = json.load(f)
            
        # Z-Index Priority: Green (Low) -> Yellow (Medium) -> Red (High)
        color_order = {"#00FF00": 1, "#FFFF00": 2, "#FF0000": 3}
        data.sort(key=lambda x: color_order.get(x['color'], 0))
        
        return data
    except Exception:
        return []

# =============================================================
# REGISTER - สมัครสมาชิก
# =============================================================
@app.post("/api/register")
async def register(data: RegisterRequest):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        # Check duplicate email
        cursor.execute("SELECT user_id FROM users WHERE email = %s", (data.email,))
        if cursor.fetchone():
            return {"error": True, "message": "อีเมลนี้มีในระบบแล้ว"}
        
        hashed = bcrypt.hashpw(data.password.encode('utf-8'), bcrypt.gensalt())
        user_id = str(uuid.uuid4())
        cursor.execute(
            "INSERT INTO users (user_id, name, phone, email, password_hash, role) VALUES (%s, %s, %s, %s, %s, %s)",
            (user_id, data.name, data.phone, data.email, hashed.decode('utf-8'), data.role)
        )
        conn.commit()
        return {"error": False, "message": "สมัครสมาชิกสำเร็จ", "user_id": user_id}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# LOGIN - เข้าสู่ระบบ
# =============================================================
@app.post("/api/login")
async def login(data: LoginRequest):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM users WHERE email = %s", (data.email,))
        user = cursor.fetchone()
        
        if not user:
            return {"error": True, "message": "อีเมลหรือรหัสผ่านไม่ถูกต้อง", "user_id": "", "role": ""}
        
        if not bcrypt.checkpw(data.password.encode('utf-8'), user['password_hash'].encode('utf-8')):
            return {"error": True, "message": "อีเมลหรือรหัสผ่านไม่ถูกต้อง", "user_id": "", "role": ""}
        
        token = pyjwt.encode(
            {"userId": user['user_id'], "role": user['role'], "exp": datetime.datetime.utcnow() + datetime.timedelta(hours=24)},
            SECRET_KEY, algorithm="HS256"
        )
        
        return {
            "error": False,
            "message": "เข้าสู่ระบบสำเร็จ",
            "token": token,
            "user_id": user['user_id'],
            "name": user['name'],
            "email": user['email'],
            "role": user['role']
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# GET USER PROFILE
# =============================================================
@app.get("/api/user/{user_id}")
async def get_user_profile(user_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT user_id, name, email, phone, role FROM users WHERE user_id = %s", (user_id,))
        user = cursor.fetchone()
        if not user:
            return {"error": True, "message": "ไม่พบข้อมูลผู้ใช้"}
        return {"error": False, **user}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# GET LANDSLIDE EVENTS
# =============================================================
@app.get("/api/events")
async def get_events():
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM landslide_events ORDER BY occurred_at DESC LIMIT 50")
        rows = cursor.fetchall()
        # Convert datetime objects to strings for JSON serialization
        for row in rows:
            for key, val in row.items():
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[key] = val.isoformat()
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()

# =============================================================
# GET NOTIFICATIONS FOR USER
# =============================================================
@app.get("/api/notifications/{user_id}")
async def get_notifications(user_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM notifications WHERE user_id = %s ORDER BY sent_at DESC", (user_id,))
        rows = cursor.fetchall()
        for row in rows:
            for key, val in row.items():
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[key] = val.isoformat()
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()

# =============================================================
# MARK NOTIFICATION AS READ
# =============================================================
@app.put("/api/notifications/{notification_id}/read")
async def mark_notification_read(notification_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor()
        cursor.execute("UPDATE notifications SET is_read = 1 WHERE notification_id = %s", (notification_id,))
        conn.commit()
        return {"error": False, "message": "อ่านการแจ้งเตือนแล้ว"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# GET EMERGENCY SERVICES
# =============================================================
@app.get("/api/emergency")
async def get_emergency_services():
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM emergency_services ORDER BY service_name")
        rows = cursor.fetchall()
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()

# =============================================================
# GET ALL USERS (admin)
# =============================================================
@app.get("/api/users")
async def get_all_users():
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT user_id, name, email, phone, role, created_at FROM users ORDER BY created_at DESC")
        rows = cursor.fetchall()
        for row in rows:
            for key, val in row.items():
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[key] = val.isoformat()
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()
