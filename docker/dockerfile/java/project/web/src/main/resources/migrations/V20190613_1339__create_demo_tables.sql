CREATE TABLE docker_demo
(
    id     BIGINT AUTO_INCREMENT,
    name   VARCHAR(100) NOT NULL,
    remark VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_name(name)
);
