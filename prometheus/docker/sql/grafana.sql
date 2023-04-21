-- 创建存储 Grafana 数据的数据库
CREATE DATABASE IF NOT EXISTS `grafana` DEFAULT CHARACTER SET utf8mb4;

-- 为 Grafana 数据库创建并授权用户
CREATE USER 'grafana'@'%' IDENTIFIED WITH mysql_native_password BY 'grafana';
GRANT ALL PRIVILEGES ON `grafana`.* TO 'grafana'@'%';

-- 为容器健康检查创建用户, 本地访问, 无权限, 无密码
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';

-- 为 mysqld_exporter 创建用户
CREATE USER 'mysqld_exporter'@'%' IDENTIFIED WITH mysql_native_password BY 'mysqld_exporter';
GRANT REPLICATION CLIENT, PROCESS ON *.* TO 'mysqld_exporter'@'%';
GRANT SELECT ON `performance_schema`.* TO 'mysqld_exporter'@'%';

FLUSH PRIVILEGES;
