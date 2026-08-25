CREATE TABLE IF NOT EXISTS beneficiary (
                                           id INT AUTO_INCREMENT PRIMARY KEY,
                                           full_name VARCHAR(100) NOT NULL,
    gov_id VARCHAR(20) NOT NULL,
    contact VARCHAR(10) NOT NULL,
    email VARCHAR(150) NOT NULL,
    age INT NOT NULL,
    address VARCHAR(255) NOT NULL,
    scheme_name VARCHAR(150) NOT NULL
    );