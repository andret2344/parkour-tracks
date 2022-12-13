SELECT uuid, duration
FROM ats_parkour_records
WHERE parkour = ?
ORDER BY duration
LIMIT ?
