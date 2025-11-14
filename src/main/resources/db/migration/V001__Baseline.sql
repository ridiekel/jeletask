CREATE TABLE IF NOT EXISTS mqtt_message_trace (
    id UUID PRIMARY KEY,
    topic VARCHAR(512) NOT NULL,
    payload CLOB NOT NULL,
    direction VARCHAR(50) NOT NULL,
    qos INTEGER,
    retained BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_mqtt_trace_topic_created ON mqtt_message_trace(topic, created_at);
CREATE INDEX IF NOT EXISTS idx_mqtt_trace_direction_created ON mqtt_message_trace(direction, created_at);
CREATE INDEX IF NOT EXISTS idx_mqtt_trace_created ON mqtt_message_trace(created_at);
