CREATE TABLE `emergency_services` (
  `service_id` char(36) NOT NULL,
  `img_url` varchar(255) DEFAULT NULL,
  `service_name` varchar(255) NOT NULL,
  `phone_number` varchar(20) NOT NULL,
  `district` varchar(100) DEFAULT NULL,
  `updated_by` char(36) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `landslide_events` (
  `event_id` char(36) NOT NULL,
  `district` varchar(100) DEFAULT NULL,
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `occurred_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `level_name` varchar(50) DEFAULT NULL,
  `verified` tinyint(1) DEFAULT 0,
  `verified_by` char(36) DEFAULT NULL,
  `prediction_id` char(36) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `landslide_level` (
  `level_id` int(11) NOT NULL,
  `level_name` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  `color_code` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `landslide_prediction` (
  `prediction_id` char(36) NOT NULL,
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `risk_score` float NOT NULL,
  `risk_level` varchar(50) DEFAULT NULL,
  `confidence` float DEFAULT NULL,
  `model_version` varchar(50) DEFAULT NULL,
  `analyzed_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notifications` (
  `notification_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `event_id` char(36) DEFAULT NULL,
  `prediction_id` char(36) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `sent_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_read` tinyint(1) DEFAULT 0,
  `log_id` char(36) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `prediction_logs` (
  `log_id` char(36) NOT NULL,
  `node_id` int(11) NOT NULL,
  `risk_level` varchar(50) NOT NULL,
  `probability` float NOT NULL,
  `status` varchar(20) DEFAULT 'pending',
  `features_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`features_json`)),
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rainfall_prediction_data` (
  `data_id` char(36) NOT NULL,
  `prediction_id` char(36) NOT NULL,
  `forecast_amount` float DEFAULT NULL,
  `accumulated_hours` int(11) DEFAULT NULL,
  `confidence_level` float DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rainfall_records` (
  `record_id` char(36) NOT NULL,
  `location_id` char(36) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `rainfall_amount` float NOT NULL,
  `recorded_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rain_grids` (
  `grid_id` varchar(50) NOT NULL,
  `center_lat` decimal(10,8) NOT NULL,
  `center_long` decimal(11,8) NOT NULL,
  `rain_values_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`rain_values_json`)),
  `last_updated` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `static_nodes` (
  `node_id` int(11) NOT NULL,
  `grid_id` varchar(50) DEFAULT NULL,
  `latitude` decimal(10,8) NOT NULL,
  `longitude` decimal(11,8) NOT NULL,
  `elevation_extracted` float DEFAULT NULL,
  `slope_extracted` float DEFAULT NULL,
  `aspect_extracted` float DEFAULT NULL,
  `modis_lc` float DEFAULT NULL,
  `ndvi` float DEFAULT NULL,
  `ndwi` float DEFAULT NULL,
  `twi` float DEFAULT NULL,
  `soil_type` float DEFAULT NULL,
  `road_zone` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `users` (
  `user_id` char(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(50) DEFAULT 'user',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_locations` (
  `location_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `latitude` decimal(10,8) NOT NULL,
  `longitude` decimal(11,8) NOT NULL,
  `district` varchar(100) DEFAULT NULL,
  `tambon` varchar(255) DEFAULT NULL,
  `location_name` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_pinned_locations` (
  `pin_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `latitude` decimal(10,8) NOT NULL,
  `longitude` decimal(11,8) NOT NULL,
  `label` varchar(255) DEFAULT NULL,
  `nearest_node_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_reports` (
  `report_id` varchar(36) NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `img_url` text DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `status` varchar(20) DEFAULT 'pending',
  `completed_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

ALTER TABLE `emergency_services`
  ADD PRIMARY KEY (`service_id`),
  ADD KEY `updated_by` (`updated_by`);

ALTER TABLE `landslide_events`
  ADD PRIMARY KEY (`event_id`),
  ADD KEY `verified_by` (`verified_by`),
  ADD KEY `prediction_id` (`prediction_id`);

ALTER TABLE `landslide_level`
  ADD PRIMARY KEY (`level_id`);

ALTER TABLE `landslide_prediction`
  ADD PRIMARY KEY (`prediction_id`);

ALTER TABLE `notifications`
  ADD PRIMARY KEY (`notification_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `event_id` (`event_id`),
  ADD KEY `prediction_id` (`prediction_id`),
  ADD KEY `fk_notifications_prediction_log` (`log_id`);

ALTER TABLE `prediction_logs`
  ADD PRIMARY KEY (`log_id`),
  ADD KEY `node_id` (`node_id`);

ALTER TABLE `rainfall_prediction_data`
  ADD PRIMARY KEY (`data_id`),
  ADD KEY `prediction_id` (`prediction_id`);

ALTER TABLE `rainfall_records`
  ADD PRIMARY KEY (`record_id`),
  ADD KEY `location_id` (`location_id`);

ALTER TABLE `rain_grids`
  ADD PRIMARY KEY (`grid_id`);

ALTER TABLE `static_nodes`
  ADD PRIMARY KEY (`node_id`),
  ADD KEY `grid_id` (`grid_id`);

ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`);

ALTER TABLE `user_locations`
  ADD PRIMARY KEY (`location_id`),
  ADD KEY `user_id` (`user_id`);

ALTER TABLE `user_pinned_locations`
  ADD PRIMARY KEY (`pin_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `nearest_node_id` (`nearest_node_id`);

ALTER TABLE `user_reports`
  ADD PRIMARY KEY (`report_id`);

ALTER TABLE `landslide_level`
  MODIFY `level_id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `static_nodes`
  MODIFY `node_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2728;

ALTER TABLE `emergency_services`
  ADD CONSTRAINT `emergency_services_ibfk_1` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

ALTER TABLE `landslide_events`
  ADD CONSTRAINT `landslide_events_ibfk_1` FOREIGN KEY (`verified_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `landslide_events_ibfk_2` FOREIGN KEY (`prediction_id`) REFERENCES `landslide_prediction` (`prediction_id`) ON DELETE SET NULL;

ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notifications_prediction_log` FOREIGN KEY (`log_id`) REFERENCES `prediction_logs` (`log_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `notifications_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `landslide_events` (`event_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `notifications_ibfk_3` FOREIGN KEY (`prediction_id`) REFERENCES `landslide_prediction` (`prediction_id`) ON DELETE SET NULL;

ALTER TABLE `prediction_logs`
  ADD CONSTRAINT `prediction_logs_ibfk_1` FOREIGN KEY (`node_id`) REFERENCES `static_nodes` (`node_id`) ON DELETE CASCADE;

ALTER TABLE `rainfall_prediction_data`
  ADD CONSTRAINT `rainfall_prediction_data_ibfk_1` FOREIGN KEY (`prediction_id`) REFERENCES `landslide_prediction` (`prediction_id`) ON DELETE CASCADE;

ALTER TABLE `rainfall_records`
  ADD CONSTRAINT `rainfall_records_ibfk_1` FOREIGN KEY (`location_id`) REFERENCES `user_locations` (`location_id`) ON DELETE CASCADE;

ALTER TABLE `static_nodes`
  ADD CONSTRAINT `static_nodes_ibfk_1` FOREIGN KEY (`grid_id`) REFERENCES `rain_grids` (`grid_id`) ON DELETE SET NULL;

ALTER TABLE `user_locations`
  ADD CONSTRAINT `user_locations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

ALTER TABLE `user_pinned_locations`
  ADD CONSTRAINT `user_pinned_locations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `user_pinned_locations_ibfk_2` FOREIGN KEY (`nearest_node_id`) REFERENCES `static_nodes` (`node_id`) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `rain_grids` (
  `grid_id` VARCHAR(50) PRIMARY KEY,
  `center_lat` DECIMAL(10,8) NOT NULL,
  `center_long` DECIMAL(11,8) NOT NULL,
  `rain_values_json` JSON NOT NULL,
  `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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

ALTER TABLE `notifications` ADD COLUMN IF NOT EXISTS `log_id` CHAR(36) DEFAULT NULL;

ALTER TABLE `notifications` ADD CONSTRAINT `fk_notifications_prediction_log` FOREIGN KEY (`log_id`) REFERENCES `prediction_logs`(`log_id`) ON DELETE SET NULL;