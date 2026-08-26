CREATE TABLE IF NOT EXISTS beneficiary (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           full_name VARCHAR(100) NOT NULL,
    gov_id VARCHAR(20) NOT NULL UNIQUE,
    contact VARCHAR(15) NOT NULL,
    email VARCHAR(150) NOT NULL,
    age INT NOT NULL,
    address VARCHAR(255) NOT NULL,
    scheme_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );