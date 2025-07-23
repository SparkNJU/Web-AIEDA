USE ai_eda;
DROP TABLE IF EXISTS users;


CREATE TABLE users (
    uid INT AUTO_INCREMENT PRIMARY KEY ,
    username VARCHAR(50),
    phone VARCHAR(15) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    role INT NOT NULL DEFAULT 1
);

INSERT INTO users (username, phone, password, description, role) VALUES
('admin', '1234567890', '123456', 'Administrator', 1);
