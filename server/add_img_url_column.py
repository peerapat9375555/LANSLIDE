"""
Migration script: เพิ่มคอลัมน์ img_url ในตาราง emergency_services
รัน: python add_img_url_column.py
"""
import mysql.connector

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'landsnot_db',
}

def run():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    try:
        # ตรวจสอบว่า column img_url มีอยู่แล้วหรือไม่
        cursor.execute("""
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = 'landsnot_db'
              AND TABLE_NAME = 'emergency_services'
              AND COLUMN_NAME = 'img_url'
        """)
        count = cursor.fetchone()[0]
        if count == 0:
            cursor.execute("""
                ALTER TABLE emergency_services
                ADD COLUMN img_url VARCHAR(512) NULL DEFAULT NULL
            """)
            conn.commit()
            print("[OK] Added img_url column successfully")
        else:
            print("[INFO] img_url column already exists, nothing to do")
    except Exception as e:
        conn.rollback()
        print(f"[ERROR] {e}")
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    run()
