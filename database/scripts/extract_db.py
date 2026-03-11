import re

schema = []

for file in ["c:/xampp/htdocs/Project_mobile_app/landsnot_db.sql", "c:/xampp/htdocs/Project_mobile_app/update_db.sql"]:
    try:
        with open(file, 'r', encoding='utf-8') as f:
            content = f.read()
            # find all CREATE TABLE statements until the first semicolon
            matches = re.findall(r'(CREATE TABLE.*?\;)', content, re.DOTALL | re.IGNORECASE)
            # find all ALTER TABLE statements
            alters = re.findall(r'(ALTER TABLE.*?\;)', content, re.DOTALL | re.IGNORECASE)
            
            schema.extend(matches)
            schema.extend(alters)
    except Exception as e:
        print(f"Error reading {file}: {e}")

with open("c:/xampp/htdocs/Project_mobile_app/extracted_schema.sql", "w", encoding="utf-8") as f:
    f.write("\n\n".join(schema))
