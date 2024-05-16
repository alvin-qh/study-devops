-- 为容器健康检查创建用户, 本地访问, SHOW STATUS 权限, 无密码
CREATE USER 'health'@'%' IDENTIFIED WITH mysql_native_password BY '';

-- 创建集群复制用户
CREATE USER 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl'@'%';

FLUSH PRIVILEGES;

-- 安装插件
INSTALL PLUGIN group_replication SONAME 'group_replication.so';
