const fs = require('fs');

const inputFile = 'init_database_old.sql';
const outputFile = 'init_clean.sql';

console.log('Reading ' + inputFile);
let content = fs.readFileSync(inputFile, 'utf-8');
const lines = content.split('\n');

let skipping = false;

const skipAllTables = ['landslide_prediction', 'rainfall_records', 'rainfall_prediction_data', 'landslide_level', 'user_pins', 'user_pinned_locations', 'landslide_events'];
const noInsertTables = ['prediction_logs', 'user_locations', 'notifications', 'user_reports'];

const result = [];
for (let i = 0; i < lines.length; i++) {
    let line = lines[i];
    
    // Pattern checks to start skipping block
    let startSkip = false;
    
    let insertMatch = line.match(/^INSERT INTO `([^`]+)`/);
    if (insertMatch) {
        const table = insertMatch[1];
        if (skipAllTables.includes(table) || noInsertTables.includes(table)) {
            startSkip = true;
        }
    }
    
    let createMatch = line.match(/^CREATE TABLE (IF NOT EXISTS )?`([^`]+)`/);
    if (createMatch) {
        const table = createMatch[2];
        if (skipAllTables.includes(table)) {
            startSkip = true;
        }
    }
    
    let alterMatch = line.match(/^ALTER TABLE `([^`]+)`/);
    if (alterMatch) {
        const table = alterMatch[1];
        if (skipAllTables.includes(table)) {
            startSkip = true;
        }
    }
    
    // phpMyAdmin comments for disabled tables
    let indexMatch = line.match(/^-- Indexes for table `([^`]+)`/);
    if (indexMatch && skipAllTables.includes(indexMatch[1])) {
        // Strip the precedent '-- ' line
        if (result.length > 0 && result[result.length - 1].startsWith('--')) {
            result.pop();
        }
        startSkip = true;
    }
    
    let constraintMatch = line.match(/^-- Constraints for table `([^`]+)`/);
    if (constraintMatch && skipAllTables.includes(constraintMatch[1])) {
        if (result.length > 0 && result[result.length - 1].startsWith('--')) result.pop();
        startSkip = true;
    }
    
    // Explicitly delete orphaned constraints within non-skipped tables
    let orphanConstraintMatch = line.match(/^  ADD CONSTRAINT `[^`]+` FOREIGN KEY \(`[^`]+`\) REFERENCES `([^`]+)`/);
    if (orphanConstraintMatch && skipAllTables.includes(orphanConstraintMatch[1])) {
        // We do not want to add this line.
        // Also check if the line ends with a comma. If it does, we just drop it.
        // If it ends with a semicolon, and the previous line ends with a comma, we might need to fix the previous line's comma, 
        // but typically phpMyAdmin dumps have multiple ADD CONSTRAINT lines. We can just replace the previous line's comma with a semicolon if this was the last constraint.
        if (line.trim().endsWith(';')) {
            if (result.length > 0) {
               result[result.length - 1] = result[result.length - 1].replace(/,\s*$/, ';');
            }
        }
        continue;
    }
    
    let aiMatch = line.match(/^-- AUTO_INCREMENT for table `([^`]+)`/);
    if (aiMatch && skipAllTables.includes(aiMatch[1])) {
        if (result.length > 0 && result[result.length - 1].startsWith('--')) result.pop();
        startSkip = true;
    }

    if (startSkip) {
        skipping = true;
    }

    // Push line if we are not skipping
    if (!skipping) {
        result.push(line);
    }
    
    // If skipping ends at semicolon + optional space/carriage return
    if (skipping) {
        if (line.match(/;[\s\r]*$/)) {
            skipping = false;
        }
    }
}

fs.writeFileSync(outputFile, result.join('\n'));
console.log('Cleaned file saved to ' + outputFile + '. Size: ' + fs.statSync(outputFile).size + ' bytes');
