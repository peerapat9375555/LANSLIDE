-- Unused tables archived on Wed 03/11/2026

--
-- Table structure for table `landslide_level`
--

CREATE TABLE `landslide_level` (
  `level_id` int(11) NOT NULL,
  `level_name` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  `color_code` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure for table `landslide_prediction`
--

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

--
-- Table structure for table `rainfall_prediction_data`
--

CREATE TABLE `rainfall_prediction_data` (
  `data_id` char(36) NOT NULL,
  `prediction_id` char(36) NOT NULL,
  `forecast_amount` float DEFAULT NULL,
  `accumulated_hours` int(11) DEFAULT NULL,
  `confidence_level` float DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Table structure for table `rainfall_records`
--

CREATE TABLE `rainfall_records` (
  `record_id` char(36) NOT NULL,
  `location_id` char(36) DEFAULT NULL,
  `district` varchar(100) DEFAULT NULL,
  `rainfall_amount` float NOT NULL,
  `recorded_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
