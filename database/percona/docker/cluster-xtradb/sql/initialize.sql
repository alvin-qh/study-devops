-- 为容器健康检查创建用户, 本地访问, SHOW STATUS 权限, 无密码
CREATE USER 'health'@'%' IDENTIFIED WITH mysql_native_password BY '';

FLUSH PRIVILEGES;
