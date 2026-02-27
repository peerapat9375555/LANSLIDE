-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 26, 2026 at 04:26 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `landsnot_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `emergency_services`
--

CREATE TABLE `emergency_services` (
  `service_id` char(36) NOT NULL,
  `img_url` varchar(255) DEFAULT NULL,
  `service_name` varchar(255) NOT NULL,
  `phone_number` varchar(20) NOT NULL,
  `district` varchar(100) DEFAULT NULL,
  `updated_by` char(36) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `landslide_events`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `landslide_level`
--

CREATE TABLE `landslide_level` (
  `level_id` int(11) NOT NULL,
  `level_name` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  `color_code` varchar(10) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

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

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `notification_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `event_id` char(36) DEFAULT NULL,
  `prediction_id` char(36) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `sent_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `is_read` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

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

-- --------------------------------------------------------

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

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `user_locations`
--

CREATE TABLE `user_locations` (
  `location_id` char(36) NOT NULL,
  `user_id` char(36) NOT NULL,
  `latitude` decimal(10,8) NOT NULL,
  `longitude` decimal(11,8) NOT NULL,
  `district` varchar(100) DEFAULT NULL,
  `location_name` varchar(255) DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `emergency_services`
--
ALTER TABLE `emergency_services`
  ADD PRIMARY KEY (`service_id`),
  ADD KEY `updated_by` (`updated_by`);

--
-- Indexes for table `landslide_events`
--
ALTER TABLE `landslide_events`
  ADD PRIMARY KEY (`event_id`),
  ADD KEY `verified_by` (`verified_by`),
  ADD KEY `prediction_id` (`prediction_id`);

--
-- Indexes for table `landslide_level`
--
ALTER TABLE `landslide_level`
  ADD PRIMARY KEY (`level_id`);

--
-- Indexes for table `landslide_prediction`
--
ALTER TABLE `landslide_prediction`
  ADD PRIMARY KEY (`prediction_id`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`notification_id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `event_id` (`event_id`),
  ADD KEY `prediction_id` (`prediction_id`);

--
-- Indexes for table `rainfall_prediction_data`
--
ALTER TABLE `rainfall_prediction_data`
  ADD PRIMARY KEY (`data_id`),
  ADD KEY `prediction_id` (`prediction_id`);

--
-- Indexes for table `rainfall_records`
--
ALTER TABLE `rainfall_records`
  ADD PRIMARY KEY (`record_id`),
  ADD KEY `location_id` (`location_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `user_locations`
--
ALTER TABLE `user_locations`
  ADD PRIMARY KEY (`location_id`),
  ADD KEY `user_id` (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `landslide_level`
--
ALTER TABLE `landslide_level`
  MODIFY `level_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `emergency_services`
--
ALTER TABLE `emergency_services`
  ADD CONSTRAINT `emergency_services_ibfk_1` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Constraints for table `landslide_events`
--
ALTER TABLE `landslide_events`
  ADD CONSTRAINT `landslide_events_ibfk_1` FOREIGN KEY (`verified_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `landslide_events_ibfk_2` FOREIGN KEY (`prediction_id`) REFERENCES `landslide_prediction` (`prediction_id`) ON DELETE SET NULL;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `notifications_ibfk_2` FOREIGN KEY (`event_id`) REFERENCES `landslide_events` (`event_id`) ON DELETE SET NULL,
  ADD CONSTRAINT `notifications_ibfk_3` FOREIGN KEY (`prediction_id`) REFERENCES `landslide_prediction` (`prediction_id`) ON DELETE SET NULL;

--
-- Constraints for table `rainfall_prediction_data`
--
ALTER TABLE `rainfall_prediction_data`
  ADD CONSTRAINT `rainfall_prediction_data_ibfk_1` FOREIGN KEY (`prediction_id`) REFERENCES `landslide_prediction` (`prediction_id`) ON DELETE CASCADE;

--
-- Constraints for table `rainfall_records`
--
ALTER TABLE `rainfall_records`
  ADD CONSTRAINT `rainfall_records_ibfk_1` FOREIGN KEY (`location_id`) REFERENCES `user_locations` (`location_id`) ON DELETE CASCADE;

--
-- Constraints for table `user_locations`
--
ALTER TABLE `user_locations`
  ADD CONSTRAINT `user_locations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
