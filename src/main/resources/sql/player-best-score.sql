/*
 * Copyright Andret (c) 2018. Copying and modifying allowed only keeping git link reference.
 */

SELECT MIN(duration) OVER (PARTITION BY uuid, parkour)                                  AS player_time,
       (SELECT MIN(duration) FROM ats_parkour_records P1 WHERE P1.parkour = P2.parkour) AS parkour_time,
       MAX(date) OVER (PARTITION BY uuid, parkour)                                      AS last_run,
       COUNT(*) OVER (PARTITION BY uuid, parkour)                                       AS count
FROM ats_parkour_records P2
WHERE uuid = ?
  AND parkour = ?
LIMIT 1;
