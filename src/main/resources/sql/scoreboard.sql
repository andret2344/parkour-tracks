SELECT MIN(duration) OVER (PARTITION BY uuid, parkour) AS player_time,
       MIN(duration) OVER (PARTITION BY parkour) AS parkour_time,
       MAX(date) OVER (PARTITION BY uuid, parkour) AS last_run,
       COUNT(*) OVER (PARTITION BY uuid, parkour) AS count
FROM ats_parkour_records
WHERE uuid = ?
LIMIT 1;
