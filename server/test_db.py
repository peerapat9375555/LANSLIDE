import mysql.connector
import sys

try:
    print("Testing DB Connection...")
    c = mysql.connector.connect(host='127.0.0.1', user='root', password='', database='landsnot_db', connection_timeout=5)
    print("Connected!")
    cur = c.cursor()
    cur.execute("SELECT COUNT(*) FROM static_nodes")
    row = cur.fetchone()
    print(f"static_nodes count: {row[0]}")
    c.close()
    sys.exit(0)
except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
