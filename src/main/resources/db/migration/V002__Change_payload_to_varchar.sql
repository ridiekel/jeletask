ALTER TABLE mqtt_message_trace ADD COLUMN payload_temp VARCHAR(10000);

UPDATE mqtt_message_trace
SET payload_temp = CASE
                       WHEN LENGTH(payload) > 10000
                           THEN SUBSTRING(payload, 1, 9997) || '...'
                       ELSE payload
    END;

ALTER TABLE mqtt_message_trace DROP COLUMN payload;
ALTER TABLE mqtt_message_trace ALTER COLUMN payload_temp RENAME TO payload;
ALTER TABLE mqtt_message_trace ALTER COLUMN payload SET NOT NULL;

