-- 为容器健康检查创建用户, 本地访问, 无权限, 无密码
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
FLUSH PRIVILEGES;
