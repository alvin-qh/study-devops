-- 创建用户 test, 具备登录权限
CREATE ROLE test LOGIN ENCRYPTED PASSWORD 'test';

-- 创建数据库 test
CREATE DATABASE test;

-- 将 test 数据库的全部权限赋予用户 test
GRANT ALL PRIVILEGES ON DATABASE test TO test;
