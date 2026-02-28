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
LOCATION_LOOKUP_DF = None

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

class VerifyAlertRequest(BaseModel):
    action: str

class UpdateEmergencyRequest(BaseModel):
    service_name: str
    phone_number: str

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
    global STATIC_DATA_CACHE, ML_MODEL, SCALER, LOCATION_LOOKUP_DF
    
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

    print("Loading Location Lookup CSV...")
    csv_path = os.path.join(PROJECT_ROOT, 'nan_province_data.csv')
    try:
        LOCATION_LOOKUP_DF = pd.read_csv(csv_path)
        print(f"Loaded {len(LOCATION_LOOKUP_DF)} location records for tambon/district lookup.")
    except Exception as e:
        print(f"Warning: nan_province_data.csv not found: {e}")
        LOCATION_LOOKUP_DF = None
        
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

def lookup_tambon_district(lat, lon):
    """Find nearest tambon/district from CSV by lat/lon."""
    if LOCATION_LOOKUP_DF is None or LOCATION_LOOKUP_DF.empty:
        return None, None
    try:
        df = LOCATION_LOOKUP_DF
        dist = ((df['LATITUDE'] - lat)**2 + (df['LONGITUDE'] - lon)**2)
        idx = dist.idxmin()
        return str(df.loc[idx, 'TAMBON']), str(df.loc[idx, 'DISTRICT'])
    except:
        return None, None

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
        if prob >= 0.85:
            risk = "High"
            color = "#FF0000"
        elif prob >= 0.50:
            risk = "Medium"
            color = "#FFFF00"
            
        log_id = str(uuid.uuid4())
        
        # Capture the 23 features from this row
        row_features = df.iloc[i][FEATURE_ORDER].to_dict()
        features_json = json.dumps(row_features)
        
        # Appending status 'pending' and features_json
        log_inserts.append((log_id, node_id, risk, prob, 'pending', features_json))
        
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
            # Batch insert prediction_logs in chunks to avoid max_allowed_packet
            BATCH_SIZE = 200
            if log_inserts:
                for i in range(0, len(log_inserts), BATCH_SIZE):
                    batch = log_inserts[i:i+BATCH_SIZE]
                    cursor.executemany("INSERT INTO prediction_logs (log_id, node_id, risk_level, probability, status, features_json) VALUES (%s, %s, %s, %s, %s, %s)", batch)
                
            conn.commit()
        except Exception as e:
            conn.rollback()
            print(f"Failed to save prediction logs or notifications: {e}")
        finally:
            cursor.close()
            conn.close()
            
    with open(os.path.join(PROJECT_ROOT, 'latest_predictions.json'), 'w') as f:
        json.dump(response_payload, f)
        
    return {"status": "success", "grids_fetched": len(unique_grids), "points_predicted": len(response_payload)}


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

# =============================================================
# ADMIN: GET PENDING ALERTS
# =============================================================
@app.get("/api/admin/alerts/pending")
async def get_pending_alerts():
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        query = """
        SELECT pl.log_id, pl.node_id, pl.risk_level, pl.probability, pl.timestamp, 
               sn.latitude, sn.longitude
        FROM prediction_logs pl
        JOIN static_nodes sn ON pl.node_id = sn.node_id
        WHERE pl.status = 'pending'
        ORDER BY pl.timestamp DESC
        """
        cursor.execute(query)
        rows = cursor.fetchall()
        for row in rows:
            for key, val in row.items():
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[key] = val.isoformat()
            tambon, district = lookup_tambon_district(float(row['latitude']), float(row['longitude']))
            row['tambon'] = tambon
            row['district'] = district
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()

# =============================================================
# ADMIN: GET ALERT HISTORY (approved alerts)
# =============================================================
@app.get("/api/admin/alerts/history")
async def get_alert_history():
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        query = """
        SELECT pl.log_id, pl.node_id, pl.risk_level, pl.probability, pl.timestamp, 
               sn.latitude, sn.longitude
        FROM prediction_logs pl
        JOIN static_nodes sn ON pl.node_id = sn.node_id
        WHERE pl.status = 'approved'
        ORDER BY pl.timestamp DESC
        """
        cursor.execute(query)
        rows = cursor.fetchall()
        for row in rows:
            for key, val in row.items():
                if isinstance(val, (datetime.datetime, datetime.date)):
                    row[key] = val.isoformat()
            tambon, district = lookup_tambon_district(float(row['latitude']), float(row['longitude']))
            row['tambon'] = tambon
            row['district'] = district
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()

# =============================================================
# ADMIN: GET ALERT DETAILS
# =============================================================
@app.get("/api/admin/alerts/{log_id}")
async def get_alert_details(log_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("""
            SELECT pl.*, rg.rain_values_json, sn.latitude, sn.longitude
            FROM prediction_logs pl
            JOIN static_nodes sn ON pl.node_id = sn.node_id
            LEFT JOIN rain_grids rg ON sn.grid_id = rg.grid_id
            WHERE pl.log_id = %s
        """, (log_id,))
        row = cursor.fetchone()
        
        if not row:
            raise HTTPException(status_code=404, detail="Alert not found")
            
        for key, val in row.items():
            if isinstance(val, (datetime.datetime, datetime.date)):
                row[key] = val.isoformat()
                
        if isinstance(row.get('features_json'), str):
            try:
                row['features_json'] = json.loads(row['features_json'])
            except:
                pass
        if isinstance(row.get('rain_values_json'), str):
            try:
                row['rain_values_json'] = json.loads(row['rain_values_json'])
            except:
                pass
        
        tambon, district = lookup_tambon_district(float(row['latitude']), float(row['longitude']))
        row['tambon'] = tambon
        row['district'] = district
                
        return row
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# ADMIN: VERIFY ALERT (Approve/Reject)
# =============================================================
@app.put("/api/admin/alerts/{log_id}/verify")
async def verify_alert(log_id: str, payload: VerifyAlertRequest):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        new_status = 'approved' if payload.action.lower() == 'approve' else 'rejected'
        cursor.execute("UPDATE prediction_logs SET status = %s WHERE log_id = %s", (new_status, log_id))
        
        notifications_sent = 0
        if new_status == 'approved':
            cursor.execute("SELECT node_id, risk_level FROM prediction_logs WHERE log_id = %s", (log_id,))
            log = cursor.fetchone()
            if log and log['risk_level'] in ['High', 'Medium']:
                cursor.execute("SELECT user_id FROM user_pinned_locations WHERE nearest_node_id = %s", (log['node_id'],))
                pinned_users = cursor.fetchall()
                notification_inserts = []
                
                title = "High Risk Alert" if log['risk_level'] == 'High' else "Medium Risk Notice"
                msg = f"A {log['risk_level'].lower()} landslide risk has been officially verified near your pinned location."
                
                for user_result in pinned_users:
                    notifications_sent += 1
                    notification_inserts.append((
                        str(uuid.uuid4()), 
                        user_result['user_id'], 
                        log_id, 
                        title, 
                        msg
                    ))
                    
                if notification_inserts:
                    cursor.executemany("INSERT INTO notifications (notification_id, user_id, log_id, title, message) VALUES (%s, %s, %s, %s, %s)", notification_inserts)
                    
        conn.commit()
        return {"status": "success", "message": f"Alert {new_status}", "notifications_sent": notifications_sent}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# ADMIN: UPDATE EMERGENCY SERVICE
# =============================================================
@app.put("/api/emergency/{service_id}")
async def update_emergency(service_id: int, payload: UpdateEmergencyRequest):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor()
        cursor.execute("UPDATE emergency_services SET service_name = %s, phone_number = %s WHERE service_id = %s", 
                      (payload.service_name, payload.phone_number, service_id))
        conn.commit()
        return {"status": "success", "message": "Emergency contact updated."}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# ADMIN: ADD EMERGENCY SERVICE
# =============================================================
@app.post("/api/emergency")
async def add_emergency_service(payload: UpdateEmergencyRequest):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor()
        cursor.execute("INSERT INTO emergency_services (service_name, phone_number) VALUES (%s, %s)", 
                      (payload.service_name, payload.phone_number))
        conn.commit()
        return {"status": "success", "message": "Emergency contact added."}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()
# =============================================================
# USER: GET PIN DASHBOARD
# =============================================================
@app.get("/api/pins/{pin_id}/dashboard")
async def get_pin_dashboard(pin_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        query = """
            SELECT p.label, p.latitude, p.longitude, rg.rain_values_json 
            FROM user_pinned_locations p
            JOIN static_nodes sn ON p.nearest_node_id = sn.node_id
            LEFT JOIN rain_grids rg ON sn.grid_id = rg.grid_id
            WHERE p.pin_id = %s
        """
        cursor.execute(query, (pin_id,))
        row = cursor.fetchone()
        
        if not row:
            raise HTTPException(status_code=404, detail="Pin not found")
            
        rain_data = []
        if row.get('rain_values_json') and isinstance(row['rain_values_json'], str):
            try:
                rain_data = json.loads(row['rain_values_json'])
            except:
                pass
                
        return {
            "label": row['label'],
            "latitude": float(row['latitude']),
            "longitude": float(row['longitude']),
            "rain_trend": rain_data
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# USER: CLEAR PINS
# =============================================================
@app.delete("/api/pins/{user_id}")
async def clear_user_pins(user_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM user_pinned_locations WHERE user_id = %s", (user_id,))
        deleted_count = cursor.rowcount
        conn.commit()
        return {"status": "success", "message": f"Cleared {deleted_count} pins.", "deleted": deleted_count}
    except Exception as e:
        conn.rollback()
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        cursor.close()
        conn.close()

# =============================================================
# USER: GET PINS
# =============================================================
@app.get("/api/pins/user/{user_id}")
async def get_user_pins(user_id: str):
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT pin_id, latitude, longitude, label FROM user_pinned_locations WHERE user_id = %s", (user_id,))
        rows = cursor.fetchall()
        for r in rows:
            r['latitude'] = float(r['latitude'])
            r['longitude'] = float(r['longitude'])
        return rows
    except Exception as e:
        return []
    finally:
        cursor.close()
        conn.close()

# =============================================================
# ADMIN: TRIGGER GEE (static features - rarely changes)
# =============================================================
@app.post("/trigger-gee")
async def trigger_gee():
    """Fetch static features from Google Earth Engine and update DB."""
    global STATIC_DATA_CACHE
    import sys, math
    
    # Add parent dir so we can import gee_extractor
    parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    if parent_dir not in sys.path:
        sys.path.insert(0, parent_dir)
    
    try:
        from gee_extractor import initialize_gee, build_combined_image
        import ee
    except ImportError as e:
        raise HTTPException(status_code=500, detail=f"Missing GEE dependencies: {e}. Install: pip install earthengine-api python-dotenv")
    
    # Load .env for GEE_PROJECT_ID
    from dotenv import load_dotenv
    load_dotenv(os.path.join(parent_dir, '.env'))
    
    conn = get_db_connection()
    if not conn:
        raise HTTPException(status_code=500, detail="Database connection error")
    
    try:
        # 0. Clean up duplicates (keep lowest node_id per lat/lon pair)
        cursor = conn.cursor(dictionary=True)
        print("[GEE] Checking for duplicate nodes...")
        cursor.execute("""
            DELETE s1 FROM static_nodes s1
            INNER JOIN static_nodes s2
            WHERE s1.node_id > s2.node_id
            AND s1.latitude = s2.latitude
            AND s1.longitude = s2.longitude
        """)
        deleted = cursor.rowcount
        if deleted > 0:
            conn.commit()
            print(f"[GEE] Removed {deleted} duplicate nodes.")
        
        # 1. Read all nodes from DB
        cursor.execute("SELECT node_id, latitude, longitude FROM static_nodes")
        nodes = cursor.fetchall()
        if not nodes:
            raise HTTPException(status_code=400, detail="No nodes found in static_nodes table")
        
        print(f"[GEE] Starting extraction for {len(nodes)} nodes...")
        
        # 2. Initialize GEE and build image
        initialize_gee()
        combined_img = build_combined_image()
        print("[GEE] Combined image ready.")
        
        # 3. Helper: convert distance to road_zone
        def distance_to_road_zone(dist_meters):
            if dist_meters is None: return 5
            if dist_meters <= 50: return 1
            elif dist_meters <= 100: return 2
            elif dist_meters <= 200: return 3
            elif dist_meters <= 500: return 4
            else: return 5
        
        # 4. Process in chunks
        CHUNK_SIZE = 500
        updated_count = 0
        
        for chunk_start in range(0, len(nodes), CHUNK_SIZE):
            chunk = nodes[chunk_start:chunk_start + CHUNK_SIZE]
            print(f"[GEE] Processing chunk {chunk_start // CHUNK_SIZE + 1}/{math.ceil(len(nodes) / CHUNK_SIZE)} ({len(chunk)} nodes)...")
            
            # Create ee.FeatureCollection
            ee_features = []
            for node in chunk:
                geom = ee.Geometry.Point([float(node['longitude']), float(node['latitude'])])
                feat = ee.Feature(geom, {'node_id': node['node_id']})
                ee_features.append(feat)
            
            ee_fc = ee.FeatureCollection(ee_features)
            
            try:
                results = combined_img.sampleRegions(
                    collection=ee_fc,
                    scale=500,
                    geometries=False
                ).getInfo()['features']
            except Exception as e:
                print(f"[GEE] Error on chunk starting at {chunk_start}: {e}")
                continue
            
            # 5. Update DB for each result
            update_sql = """
                UPDATE static_nodes SET
                    elevation_extracted = %s,
                    slope_extracted = %s,
                    aspect_extracted = %s,
                    modis_lc = %s,
                    ndvi = %s,
                    ndwi = %s,
                    twi = %s,
                    soil_type = %s,
                    road_zone = %s
                WHERE node_id = %s
            """
            
            batch_updates = []
            for f in results:
                props = f.get('properties', {})
                nid = props.get('node_id')
                if nid is None:
                    continue
                
                elevation = props.get('Elevation', 0) or 0
                slope = props.get('Slope', 0) or 0
                aspect = props.get('Aspect', 0) or 0
                modis_lc = props.get('MODIS_LC', 0) or 0
                ndvi = props.get('NDVI', 0) or 0
                ndwi = props.get('NDWI', 0) or 0
                twi = props.get('TWI', 0) or 0
                soil_type = props.get('Soil_Type', 0) or 0
                dist_road = props.get('Distance_to_Road', 5000) or 5000
                road_zone = distance_to_road_zone(dist_road)
                
                batch_updates.append((elevation, slope, aspect, modis_lc, ndvi, ndwi, twi, soil_type, road_zone, nid))
            
            if batch_updates:
                cursor.executemany(update_sql, batch_updates)
                conn.commit()
                updated_count += len(batch_updates)
                print(f"[GEE] Updated {len(batch_updates)} nodes in this chunk. Total: {updated_count}")
        
        # 6. Reload cache
        cursor.close()
        query = "SELECT * FROM static_nodes"
        STATIC_DATA_CACHE = pd.read_sql(query, conn)
        
        print(f"[GEE] Done! Updated {updated_count} nodes. Cache reloaded with {len(STATIC_DATA_CACHE)} nodes.")
        return {"status": "success", "message": f"Fetched GEE data and updated {updated_count} nodes. Cache reloaded."}
    
    except HTTPException:
        raise
    except Exception as e:
        print(f"[GEE] Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
    finally:
        conn.close()

# =============================================================
# ADMIN: TRIGGER RAIN FETCH + PREDICT
# =============================================================
@app.post("/trigger-rain")
async def trigger_rain():
    """Fetch rain from Open-Meteo and run ML prediction."""
    return await trigger_prediction()
