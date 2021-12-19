# Jeletask

An open source java API for Teletask domotics.

It is the purpose to create an API for software developers or domotics enthusiasts, who are interested in generating their own control environment for the TELETASK domotics systems, so you can create your own user interface and connected solutions and services.

If you own a Teletask MICROS or MICROS+, you have access to the free (or paid in case of the MICROS+) DLL32 LIBRARY (TDS15132).  
However, if you're a java programmer like myself, you don't want to use a windows dll :-).

The API supports the MICROS+ (maybe also PICOS, but untested), but you'll have to buy a licence to be able to make TCP calls.

# Configuring

Create a configuration json file following this example.
At this time I can only test with MICROS_PLUS. If you have a PICOS, you can test with type: MICROS_PLUS.
If teletask has not changed their binary API, it should be compatible.

```json
{
  "type": "MICROS_PLUS",
  "componentsTypes": {
    "RELAY": [
      {
        "number": 1,
        "description": "Ceiling lights"
      },
      {
        "number": 23,
        "description": "Closet lights"
      }
    ],
    "SENSOR": [
      {
        "number": 1,
        "description": "Light Sensor",
        "type": "LIGHT"
      }
    ],
    "COND": [
      {
        "number": 1,
        "description": "Condition Example"
      }
    ],
    "FLAG": [
      {
        "number": 6,
        "description": "Flag Example"
      }
    ],
    "GENMOOD": [
      {
        "number": 1,
        "description": "All off"
      }
    ],
    "MOTOR": [
      {
        "number": 1,
        "description": "Blinds"
      }
    ],
    "LOCMOOD": [
      {
        "number": 1,
        "description": "Watch TV"
      }
    ],
    "DIMMER": [
      {
        "number": 1,
        "description": "Spots"
      }
    ]
  }
}
```

# Running

## Environment variables


| Variable                       | Type     | Default value | Description                                      |
|--------------------------------|----------|---------------|--------------------------------------------------|
| TELETASK_HOST                  | Required |               | The ip address or hostname of your central unit  |
| TELETASK_PORT                  | Required |               | The port of your central unit, probably 55957    |
| TELETASK_ID                    | Required |               | The id used in mqtt messages of the central unit |     
| TELETASK_MQTT_HOST             | Required |               | The host of your MQTT broker                     |     
| TELETASK_MQTT_PORT             | Optional | 1883          | The port of your MQTT broker                     |     
| TELETASK_MQTT_USERNAME         | Optional |               | The MQTT broker username                         |
| TELETASK_MQTT_PASSWORD         | Optional |               | The MQTT broker password                         |
| TELETASK_MQTT_CLIENTID         | Optional | teletask2mqtt | The client id used for connecting to MQTT        |
| TELETASK_MQTT_PREFIX           | Optional | teletask2mqtt | The MQTT message topic prefix                    |
| TELETASK_MQTT_DISCOVERY_PREFIX | Optional | homeassistant | The MQTT home assistant discovery prefix         |

## Docker run

You should be able to run using following minimal command:

```shell
docker run --name jeletask2mqtt \
  -v "<path_to_your_confg_json>:/teletask2mqtt/config.json" \
  -e TELETASK_HOST="<teletask_ip_address>" \
  -e TELETASK_PORT="<teletask_port>" \
  -e TELETASK_ID="<teletask_id>" \
  -e TELETASK_MQTT_HOST="<mqtt_host>" \
  ridiekel/jeletask2mqtt:latest
```

# Messages

When using mosquitto clients you should be able to easily test your config.

In ubuntu you can use:

```shell
sudo apt install -y mosquitto-clients
```

## Relay

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/relay/1/state
```

### Change the state

#### Turning on
```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/relay/1/set \
    -m "ON"
```

#### Turning off
```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/relay/1/set \
    -m "OFF"
```

## Local Mood

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/locmood/1/state
```

### Change the state

#### Turning on
```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/locmood/1/set \
    -m "ON"
```

#### Turning off
```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/locmood/1/set \
    -m "OFF"
```

## General Mood

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/genmood/1/state
```

### Change the state

#### Turning on
```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/genmood/1/set \
    -m "ON"
```

#### Turning off
```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/genmood/1/set \
    -m "OFF"
```

## Dimmer

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/state
```

### Change the state

#### Turning on

For turning on you can use `ON` (goes to 100%) of any integer value between `0` and `100`

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m "60"
```

#### Turning off

For turning off, you can use either `OFF` or `0`

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m "OFF"
```

## Motor

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/state
```

### Change the state

#### Controlling

You can send either `UP`, `DOWN` or `STOP` to the motor function.

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/set \
    -m "65"
```

## Flag

You can only listen to flags.

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/flag/1/state
```

## Sensor

You can only listen to sensor values.

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/sensor/1/state
```
