import pandas as pd
import mysql.connector
import os
import json
import uuid
import bcrypt

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'landsnot_db',
}

def get_db_connection():
    try:
        return mysql.connector.connect(**DB_CONFIG)
    except Exception as e:
        print(f"Error connecting to DB: {e}")
        return None

def seed_database():
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target_csv = os.path.join(project_root, 'nan_province_data.csv')
    
    print(f"Loading {target_csv}...")
    try:
        df = pd.read_csv(target_csv)
    except Exception as e:
        print(f"Failed to read CSV file: {e}")
        return

    conn = get_db_connection()
    if not conn:
        print("Database connection failed. Exiting.")
        return
        
    cursor = conn.cursor()
    
    # Track statistics
    grids_inserted = 0
    nodes_inserted = 0
    unique_grids = {}
    
    # 1. Calculate grids and prepare unique grid data
    print("Mappping coordinates to 5x5km grids...")
    for index, row in df.iterrows():
        lat = row['LATITUDE']
        lon = row['LONGITUDE']
        
        # Calculate grid coordinates (approx. 5km)
        grid_lat = round(lat / 0.045) * 0.045
        grid_lon = round(lon / 0.045) * 0.045
        grid_id = f"g_{grid_lat:.3f}_{grid_lon:.3f}"
        
        df.at[index, 'grid_id'] = grid_id
        
        if grid_id not in unique_grids:
            unique_grids[grid_id] = {
                'center_lat': grid_lat,
                'center_long': grid_lon,
                # Initial empty rain values formatted as JSON array
                'rain_values_json': json.dumps([0] * 10) 
            }

    # 2. Insert into rain_grids table
    print(f"Inserting {len(unique_grids)} unique grids into 'rain_grids' table...")
    grid_insert_query = """
    INSERT IGNORE INTO rain_grids (grid_id, center_lat, center_long, rain_values_json) 
    VALUES (%s, %s, %s, %s)
    """
    
    grid_data_tuples = [
        (g_id, data['center_lat'], data['center_long'], data['rain_values_json']) 
        for g_id, data in unique_grids.items()
    ]
    
    try:
        cursor.executemany(grid_insert_query, grid_data_tuples)
        conn.commit()
        grids_inserted = cursor.rowcount
        print(f"Successfully processed grids. Total row operations: {grids_inserted}")
    except Exception as e:
        conn.rollback()
        print(f"Error inserting into rain_grids: {e}")
        return

    # 3. Insert into static_nodes table
    print(f"Inserting {len(df)} nodes into 'static_nodes' table...")
    node_insert_query = """
    INSERT IGNORE INTO static_nodes (
        grid_id, latitude, longitude, 
        elevation_extracted, slope_extracted, aspect_extracted, 
        modis_lc, ndvi, ndwi, twi, soil_type, road_zone
    ) 
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    
    node_data_tuples = []
    for _, row in df.iterrows():
        grid_id = row.get('grid_id')
        lat = row.get('LATITUDE')
        lon = row.get('LONGITUDE')
        
        # Check if static feature columns exist in the CSV, otherwise default to 0
        elevation = row.get('Elevation_Extracted', 0)
        slope = row.get('Slope_Extracted', 0)
        aspect = row.get('Aspect_Extracted', 0)
        modis_lc = row.get('MODIS_LC', 0)
        ndvi = row.get('NDVI', 0)
        ndwi = row.get('NDWI', 0)
        twi = row.get('TWI', 0)
        soil = row.get('Soil_Type', 0)
        road_zone = row.get('Road_Zone', 0)
        
        # Handle NA values from pandas appropriately
        elevation = 0 if pd.isna(elevation) else elevation
        slope = 0 if pd.isna(slope) else slope
        aspect = 0 if pd.isna(aspect) else aspect
        modis_lc = 0 if pd.isna(modis_lc) else modis_lc
        ndvi = 0 if pd.isna(ndvi) else ndvi
        ndwi = 0 if pd.isna(ndwi) else ndwi
        twi = 0 if pd.isna(twi) else twi
        soil = 0 if pd.isna(soil) else soil
        road_zone = 0 if pd.isna(road_zone) else road_zone
        
        node_data_tuples.append((
            grid_id, lat, lon, 
            elevation, slope, aspect, 
            modis_lc, ndvi, ndwi, twi, soil, road_zone
        ))
        
    try:
        # Use executemany in chunks to avoid memory issues for large datasets
        CHUNK_SIZE = 1000
        for i in range(0, len(node_data_tuples), CHUNK_SIZE):
            chunk = node_data_tuples[i:i + CHUNK_SIZE]
            cursor.executemany(node_insert_query, chunk)
            nodes_inserted += cursor.rowcount
            
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"Error inserting into static_nodes at chunk {i}: {e}")
        return
    finally:
        cursor.close()

    # 4. Seed Admin User
    print("Seeding Admin User...")
    cursor = conn.cursor()
    admin_email = "admin@admin.com"
    cursor.execute("SELECT user_id FROM users WHERE email = %s", (admin_email,))
    if not cursor.fetchone():
        admin_id = str(uuid.uuid4())
        admin_hash = bcrypt.hashpw('admin'.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
        cursor.execute(
            "INSERT INTO users (user_id, name, email, password_hash, role) VALUES (%s, %s, %s, %s, %s)",
            (admin_id, "System Admin", admin_email, admin_hash, "admin")
        )
        conn.commit()
        print(f"Created Admin account: {admin_email} / pass: admin")
    else:
        print(f"Admin account {admin_email} already exists.")
    cursor.close()
    conn.close()

    # 5. Final Output
    print("-" * 30)
    print("Database seeding completed.")
    print(f"Unique Grids Processed: {len(unique_grids)}")
    print(f"Total Nodes Inserted: {nodes_inserted}")
    print("-" * 30)

if __name__ == "__main__":
    seed_database()
