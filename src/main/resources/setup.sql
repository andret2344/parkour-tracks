CREATE TABLE IF NOT EXISTS ats_parkour_records
(
    id       INT PRIMARY KEY AUTO_INCREMENT,
    date     DATETIME,
    uuid     VARCHAR(64),
    parkour  VARCHAR(64),
    duration DECIMAL(8, 2)
);
