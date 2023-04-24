-- 为容器健康检查创建用户, 本地访问, 无权限, 无密码
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';

-- 创建用于主从同步的用户, 具备同步权限, 允许从实例访问
CREATE USER 'replica'@'%' IDENTIFIED WITH mysql_native_password BY 'replica';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'replica'@'%';

FLUSH PRIVILEGES;
