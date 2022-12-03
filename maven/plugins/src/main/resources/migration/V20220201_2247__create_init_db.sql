-- 员工表
CREATE TABLE IF NOT EXISTS `employee` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `title` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`)
);

-- 部门表
CREATE TABLE IF NOT EXISTS `department` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL,
    `parent_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`)
);

-- 员工部门关系表
CREATE TABLE IF NOT EXISTS `department_employee` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `employee_id` BIGINT NOT NULL,
    `department_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`)
);
