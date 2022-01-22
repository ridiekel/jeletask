# Jeletask

An open source java API for Teletask domotics.

It is the purpose to create an API for software developers or domotics enthusiasts, who are interested in generating their own control environment for the TELETASK domotics systems, so you can create your own user interface and connected solutions and services.

If you own a Teletask MICROS or MICROS+, you have access to the free (or paid in case of the MICROS+) DLL32 LIBRARY (TDS15132).  
However, if you're a java programmer like myself, you don't want to use a windows dll :-).

The API supports the MICROS+ (maybe also PICOS, but untested), but you'll have to buy a licence to be able to make TCP calls.

You can find the latest docker images at: https://hub.docker.com/r/ridiekel/jeletask2mqtt

# Configuring

Create a configuration json file following this example.
At this time I can only test with MICROS_PLUS. Please log an issue when you are having trouble with the other types of central unit. 
If teletask has not changed their binary API, it should be compatible.

The ```type``` Can be either ```PICOS```, ```NANOS```, ```MICROS```, ```MICROS_PLUS```
```json
{
  "type": "MICROS_PLUS",
  "componentsTypes": {
    "RELAY": [
      {
        "number": 1,
        "description": "Power outlet",
        "type": "switch" 
      },
      {
        "number": 23,
        "description": "Living Room - Closet lights"
      },
      {
        "number": 36,
        "description": "Living room - ceiling lights"
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
| TELETASK_MQTT_USERNAME         | Optional | <empty>       | The MQTT broker username                         |
| TELETASK_MQTT_PASSWORD         | Optional | <empty>       | The MQTT broker password                         |
| TELETASK_MQTT_CLIENTID         | Optional | teletask2mqtt | The client id used for connecting to MQTT        |
| TELETASK_MQTT_PREFIX           | Optional | teletask2mqtt | The MQTT message topic prefix                    |
| TELETASK_MQTT_DISCOVERY_PREFIX | Optional | homeassistant | The MQTT home assistant discovery prefix         |
| TELETASK_LOG_HACONFIG_ENABLED  | Optional | false         | (Advanced) Log the homeassistant config messages |
| TELETASK_LOG_TOPIC_ENABLED     | Optional | false         | (Advanced) Add the topic name to the log message |

## Docker run

### Versions

Different versions (tags) are provided.
Depending on your needs, you will need a different version (tag)

| Tag           | Description                                                                                                                                 |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| latest-amd64  | The latest version of the application. Built for amd64 arhitecture.                                                                         |
| latest-arm64  | The latest version of the application. Built for arm64 architecture llike raspberry pi 4.                                                   |
| latest-native | The latest version, but built with spring boot native. This is only available for amd64, but should boot a lot faster on that architecture. |


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

# HomeAssistant

Auto configuration should work with relays. 
Other types are not yet supported, work in progress.
Pleas log an issue when having trouble with auto configuration in HA.

The bridge creates 1 device with id ```teletask-<TELETASK_ID>```, and adds entities with the following entity id pattern: ```light.teletask_<TELETASK_ID>_<FUNCTION_TYPE>_<COMPONENT_NUMBER>```, which should be unique for your installation.

## Additional config

### Relay

The default type of a relay is ```light```, but can be overridden. 
Possible values: https://www.home-assistant.io/docs/mqtt/discovery/#lighting

```json
...
"RELAY": [
  {
    "number": 1,
    "description": "Power outlet",
    "type": "switch" 
  },
  {
    "number": 23,
    "description": "Living Room - Closet lights"
  },
  {
    "number": 36,
    "description": "Living room - ceiling lights"
  }
]
...
```

## Config message

The config message is published to topic: ```<TELETASK_MQTT_DISCOVERY_PREFIX>/<HA_COMPONENT_TYPE>/<TELETASK_ID>/<FUNCTION_TYPE>_<COMPONENT_NUMBER>/config```

```json
{
  "device": {
    "identifiers": [
      "<TELETASK_ID>"
    ],
    "manufacturer": "teletask",
    "name": "teletask-<TELETASK_ID>",
    "model": "<Your model type>"
  },
  "~": "<TELETASK_MQTT_PREFIX>/<TELETASK_ID>/relay/1",
  "state_topic": "~/state",
  "unique_id": "<TELETASK_ID>-<FUNCTION_TYPE>-<COMPONENT_NUMBER>",
  "name": "<COMPONENT_DESCRIPTION>",
  "command_topic": "~/set"
}
```
## Example

Topic: ```homeassistant/light/my_teletask/relay_36/config```

```json
{
  "device": {
    "identifiers": [
      "my_teletask"
    ],
    "manufacturer": "teletask",
    "name": "teletask-my_teletask",
    "model": "Micros Plus"
  },
  "~": "teletask2mqtt/my_teletask/relay/1",
  "state_topic": "~/state",
  "unique_id": "my_teletask-relay-36",
  "name": "Living room - ceiling lights",
  "command_topic": "~/set"
}
```
