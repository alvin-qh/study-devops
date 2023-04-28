-- 为容器健康检查创建用户, 本地访问, 无权限, 无密码
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';

-- 创建用于主从同步的用户, 具备同步权限, 允许从实例访问
CREATE USER 'replica'@'%' IDENTIFIED WITH mysql_native_password BY 'replica';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replica'@'%';

-- 为 ProxySQL 创建用户
-- 代理 SQL 用户
CREATE USER 'proxysql'@'%' IDENTIFIED WITH mysql_native_password BY 'proxysql';
GRANT ALL PRIVILEGES ON *.* TO 'proxysql'@'%';

-- 监控用户
CREATE USER 'monitor'@'%' IDENTIFIED WITH mysql_native_password BY 'monitor';
GRANT SELECT, SUPER, PROCESS, SHOW DATABASES, REPLICATION CLIENT, REPLICATION SLAVE ON *.* TO 'monitor'@'%';

FLUSH PRIVILEGES;
