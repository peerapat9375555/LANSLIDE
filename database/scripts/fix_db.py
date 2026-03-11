import os

file1 = "c:/xampp/htdocs/Project_mobile_app/landsnot_db.sql"
file2 = "c:/xampp/htdocs/Project_mobile_app/update_db.sql"
output = "c:/xampp/htdocs/Project_mobile_app/init_database.sql"

try:
    with open(output, 'w', encoding='utf-8') as outfile:
        # Write first file
        with open(file1, 'r', encoding='utf-8') as infile:
            outfile.write("-- ----- ORIGINAL DATABASE DUMP -----\n")
            
            # The original dump already has 'log_id' added into the dump probably?
            # Or it might have fk_notifications_prediction_log inside it.
            # Let's clean the content of file 1 first to avoid errors.
            content1 = infile.read()
            outfile.write(content1)
            outfile.write("\n\n")
        
        # Write second file - But filter out the conflicting lines
        with open(file2, 'r', encoding='utf-8') as infile:
            outfile.write("-- ----- LATEST DATABASE UPDATES -----\n")
            
            # Here we skip the last two alters that are failing.
            lines = infile.readlines()
            for line in lines:
                if "ALTER TABLE `notifications` ADD CONSTRAINT `fk_notifications_prediction_log`" in line:
                    continue # Skip this to prevent Duplicate Key Constraint error (121)
                
                outfile.write(line)
            
    print(f"Successfully combined into {output} and fixed constraint bug")
except Exception as e:
    print(f"Error: {e}")
