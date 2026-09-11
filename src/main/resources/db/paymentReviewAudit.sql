CREATE TABLE IF NOT EXISTS paymentReviewAudit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    orderNo VARCHAR(20),
    adminUserId INT,
    action VARCHAR(50),
    outcome VARCHAR(50),
    transactionId VARCHAR(255),
    beforeJson LONGTEXT,
    afterJson LONGTEXT,
    adminNote TEXT,
    createdDateTime TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_paymentReviewAudit_orderNo (orderNo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
