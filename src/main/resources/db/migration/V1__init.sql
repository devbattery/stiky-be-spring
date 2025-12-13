CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(255),
    role VARCHAR(50),
    provider VARCHAR(50),
    provider_id VARCHAR(255)
);