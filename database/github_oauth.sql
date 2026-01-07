ALTER TABLE `users` ADD COLUMN `github_id` VARCHAR(50) NULL AFTER `password`;

ALTER TABLE `users` MODIFY COLUMN `password` VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE `users` MODIFY COLUMN `password` VARCHAR(255) NULL DEFAULT '';

CREATE INDEX `idx_github_id` ON `users` (`github_id`);
