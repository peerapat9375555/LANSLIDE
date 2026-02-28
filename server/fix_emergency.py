import mysql.connector

conn = mysql.connector.connect(
    host='localhost', user='root', password=''
)
cur = conn.cursor()

# Search ALL databases for emergency_services table
cur.execute("""
    SELECT TABLE_SCHEMA, TABLE_NAME 
    FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_NAME = 'emergency_services'
""")
rows = cur.fetchall()
print("Found emergency_services in:")
for r in rows:
    print(f"  Database: {r[0]}, Table: {r[1]}")

if not rows:
    print("  NOT FOUND in any database!")

# Also list all tables in landsnot_db
print("\nAll tables in landsnot_db:")
cur.execute("SHOW TABLES FROM landsnot_db")
for t in cur.fetchall():
    print(f"  {t[0]}")

cur.close()
conn.close()
