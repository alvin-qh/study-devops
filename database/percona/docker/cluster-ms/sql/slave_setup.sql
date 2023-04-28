-- 为容器健康检查创建用户, 本地访问, 无权限, 无密码
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';

-- 为 ProxySQL 创建用户
-- 代理 SQL 用户
CREATE USER 'proxysql'@'%' IDENTIFIED WITH mysql_native_password BY 'proxysql';
GRANT ALL PRIVILEGES ON *.* TO 'proxysql'@'%';

-- 监控用户
CREATE USER 'monitor'@'%' IDENTIFIED WITH mysql_native_password BY 'monitor';
GRANT SELECT, SUPER, PROCESS, SHOW DATABASES, REPLICATION CLIENT, REPLICATION SLAVE ON *.* TO 'monitor'@'%';

FLUSH PRIVILEGES;
