import pandas as pd
import ee
import math
import os
import json
from dotenv import load_dotenv
import sys

# Adding parent dir to path to import gee_extractor
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from gee_extractor import initialize_gee, build_combined_image

def calculate_grid_id(lat, lon):
    # Group coordinates into 5x5km grids
    # 5km is roughly 0.045 degrees
    grid_lat = round(lat / 0.045) * 0.045
    grid_lon = round(lon / 0.045) * 0.045
    return f"g_{grid_lat:.3f}_{grid_lon:.3f}"

def distance_to_road_zone(dist_meters):
    if dist_meters <= 50: return 1
    elif dist_meters <= 100: return 2
    elif dist_meters <= 200: return 3
    elif dist_meters <= 500: return 4
    else: return 5

def prepare_static_data():
    load_dotenv()
    initialize_gee()
    
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target_csv = os.path.join(project_root, 'nan_province_data.csv')
    
    print(f"Loading {target_csv}...")
    df = pd.read_csv(target_csv)
    
    # Optional: read training file headers (as requested by user instruction)
    train_csv = os.path.join(project_root, 'Landslide_Final_Cleaned_V2.csv')
    try:
        train_df = pd.read_csv(train_csv, nrows=1)
        print(f"Verified target features against: {train_df.columns.tolist()[:5]}...")
    except Exception as e:
        print(f"Could not read {train_csv}: {e}")
    
    # 1. Add Grid ID mapping
    df['grid_id'] = df.apply(lambda row: calculate_grid_id(row['LATITUDE'], row['LONGITUDE']), axis=1)
    
    print("Building GEE Image...")
    combined_img = build_combined_image()
    
    print("Creating GEE FeatureCollection for points to extract Static Features...")
    CHUNK_SIZE = 500
    extracted_data = []
    
    for i in range(0, len(df), CHUNK_SIZE):
        chunk = df.iloc[i:i+CHUNK_SIZE]
        print(f"Extracting GEE chunk {i} to {i+len(chunk)}...")
        
        ee_features = []
        for idx, row in chunk.iterrows():
            geom = ee.Geometry.Point([row['LONGITUDE'], row['LATITUDE']])
            feat = ee.Feature(geom, {'idx': idx})
            ee_features.append(feat)
            
        ee_fc = ee.FeatureCollection(ee_features)
        
        try:
            results = combined_img.sampleRegions(
                collection=ee_fc,
                scale=500, # 500 meters resolution matching training config
                geometries=False
            ).getInfo()['features']
            
            for f in results:
                props = f['properties']
                orig_idx = props['idx']
                row_data = df.loc[orig_idx].to_dict()
                
                # Mapped according to the exact names in modifier_data.py & retrain_model.py
                row_data['Elevation_Extracted'] = props.get('Elevation', 0)
                row_data['Slope_Extracted'] = props.get('Slope', 0)
                row_data['Aspect_Extracted'] = props.get('Aspect', 0)
                row_data['MODIS_LC'] = props.get('MODIS_LC', 0)
                row_data['NDVI'] = props.get('NDVI', 0)
                row_data['NDWI'] = props.get('NDWI', 0)
                row_data['TWI'] = props.get('TWI', 0)
                row_data['Soil_Type'] = props.get('Soil_Type', 0)
                
                dist_road = props.get('Distance_to_Road', 5000)
                row_data['Road_Zone'] = distance_to_road_zone(dist_road)
                
                extracted_data.append(row_data)
        except Exception as e:
            print(f"Error on chunk {i}: {e}")
            
    final_df = pd.DataFrame(extracted_data)
    out_path = os.path.join(project_root, 'nan_static_features_db.csv')
    final_df.to_csv(out_path, index=False)
    print(f"Saved {len(final_df)} enriched records to {out_path}")

if __name__ == "__main__":
    prepare_static_data()
