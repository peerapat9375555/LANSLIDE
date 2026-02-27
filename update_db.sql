-- Phase 1: Database Refinement (6-Table Structure)

-- 1. Table rain_grids (Stores 5x5km rain data)
CREATE TABLE IF NOT EXISTS `rain_grids` (
  `grid_id` VARCHAR(50) PRIMARY KEY,
  `center_lat` DECIMAL(10,8) NOT NULL,
  `center_long` DECIMAL(11,8) NOT NULL,
  `rain_values_json` JSON NOT NULL,
  `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 2. Table static_nodes (Stores geographic points and features)
CREATE TABLE IF NOT EXISTS `static_nodes` (
  `node_id` INT AUTO_INCREMENT PRIMARY KEY,
  `grid_id` VARCHAR(50) DEFAULT NULL,
  `latitude` DECIMAL(10,8) NOT NULL,
  `longitude` DECIMAL(11,8) NOT NULL,
  `elevation_extracted` FLOAT DEFAULT NULL,
  `slope_extracted` FLOAT DEFAULT NULL,
  `aspect_extracted` FLOAT DEFAULT NULL,
  `modis_lc` FLOAT DEFAULT NULL,
  `ndvi` FLOAT DEFAULT NULL,
  `ndwi` FLOAT DEFAULT NULL,
  `twi` FLOAT DEFAULT NULL,
  `soil_type` FLOAT DEFAULT NULL,
  `road_zone` FLOAT DEFAULT NULL,
  FOREIGN KEY (`grid_id`) REFERENCES `rain_grids`(`grid_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3. Table user_pinned_locations (Stores user interest points)
CREATE TABLE IF NOT EXISTS `user_pinned_locations` (
  `pin_id` CHAR(36) PRIMARY KEY,
  `user_id` CHAR(36) NOT NULL,
  `latitude` DECIMAL(10,8) NOT NULL,
  `longitude` DECIMAL(11,8) NOT NULL,
  `label` VARCHAR(255) DEFAULT NULL,
  `nearest_node_id` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`nearest_node_id`) REFERENCES `static_nodes`(`node_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 4. Table prediction_logs (Stores historical prediction results)
CREATE TABLE IF NOT EXISTS `prediction_logs` (
  `log_id` CHAR(36) PRIMARY KEY,
  `node_id` INT NOT NULL,
  `risk_level` VARCHAR(50) NOT NULL,
  `probability` FLOAT NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `features_json` JSON DEFAULT NULL,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`node_id`) REFERENCES `static_nodes`(`node_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 5. Notifications table update (Tracing exactly which prediction triggered the alert)
-- Add log_id to existing notifications table
ALTER TABLE `notifications` ADD COLUMN IF NOT EXISTS `log_id` CHAR(36) DEFAULT NULL;
ALTER TABLE `notifications` ADD CONSTRAINT `fk_notifications_prediction_log` FOREIGN KEY (`log_id`) REFERENCES `prediction_logs`(`log_id`) ON DELETE SET NULL;
