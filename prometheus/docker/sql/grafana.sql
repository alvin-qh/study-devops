-- 创建存储 Grafana 数据的数据库
CREATE DATABASE IF NOT EXISTS `grafana` DEFAULT CHARACTER SET utf8mb4;

-- 创建用户
CREATE USER 'grafana'@'%' IDENTIFIED WITH mysql_native_password BY 'grafana';

-- 为 Grafana 数据库授权用户
GRANT ALL PRIVILEGES ON `grafana`.* TO `grafana`@'%' WITH GRANT OPTION;
