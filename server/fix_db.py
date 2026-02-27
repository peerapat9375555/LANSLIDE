import mysql.connector

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'landsnot_db',
}

try:
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    # Temporarily disable foreign key checks
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0;")
    
    # Drop tables to allow clean recreation
    tables_to_drop = [
        "user_pinned_locations",
        "prediction_logs",
        "static_nodes",
        "rain_grids"
    ]
    
    for table in tables_to_drop:
        try:
            cursor.execute(f"DROP TABLE IF EXISTS {table};")
            print(f"Dropped {table}.")
        except Exception as e:
            print(f"Error dropping {table}: {e}")
            
    # Remove fk constraint from notifications if exists
    try:
        cursor.execute("ALTER TABLE notifications DROP FOREIGN KEY fk_notifications_prediction_log;")
        print("Dropped fk_notifications_prediction_log from notifications.")
    except Exception as e:
        print(f"Constraint might not exist: {e}")
        
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1;")
    conn.commit()
    print("Cleanup successful.")
    
except Exception as e:
    print(f"Database error: {e}")
finally:
    if 'conn' in locals() and conn.is_connected():
        cursor.close()
        conn.close()
