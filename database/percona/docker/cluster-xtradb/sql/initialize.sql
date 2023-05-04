-- 为容器健康检查创建用户, 本地访问, SHOW STATUS 权限, 无密码
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
GRANT SELECT ON `mysql`.* TO 'health'@'localhost';

FLUSH PRIVILEGES;
