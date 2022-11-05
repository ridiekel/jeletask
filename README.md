# Jeletask

An open source java API for Teletask domotics.

It is the purpose to create an API for software developers or domotics enthusiasts, who are interested in generating their own control environment for the TELETASK domotics systems, so you can create your own user interface and connected solutions and services.

If you own a Teletask MICROS+, you need the (paid) DLL32 LIBRARY (TDS15132).  
However, if you're a java programmer like myself, you don't want to use a windows dll :-).

Teletask documentation on how their API works can be found here: https://teletask.be/media/3109/tds15132-library.pdf

The program supports the MICROS+ (maybe also NANOS/PICOS, but untested), but you'll have to buy a licence to be able to make TCP calls.

You can find the latest docker images at: https://hub.docker.com/r/ridiekel/jeletask2mqtt

# Configuring

Create a configuration json file following this example.
At this time I can only test with MICROS_PLUS. Please log an issue when you are having trouble with the other types of central unit.
If teletask has not changed their binary API, it should be compatible.

The ```type``` Can be either ```PICOS```, ```NANOS```, ```MICROS_PLUS```

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
      },
      {
        "number": 2,
        "description": "General Analog Sensor 1",
        "type": "GAS",
        "gas_type": "4-20ma",
        "gas_min": 0,
        "gas_max": 14,
        "decimals": 2
      },
      {
        "number": 3,
        "description": "Temperature Sensor",
        "type": "TEMPERATURE",
        "ha_unit_of_measurement": "°C"
      },
      {
        "number": 4,
        "description": "HVAC / AC",
        "type": "TEMPERATURECONTROL"
      },
      {
        "number": 5,
        "description": "Aurus wall switch in the attic",
        "type": "TEMPERATURECONTROL"
      },
      {
        "number": 6,
        "description": "Pulse counter energy Airco",
        "type": "PULSECOUNTER"
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
    "TIMEDMOOD": [
      {
        "number": 1,
        "description": "Outdoor light"
      }
    ],
    "DIMMER": [
      {
        "number": 1,
        "description": "Spots"
      }
    ],
    "INPUT": [
      {
        "number": 42,
        "description": "State of TDS12117 input nr 3"
      }
    ],
    "TIMEDFNC": [
      {
        "number": 3,
        "description": "Timed function nr 3 (pulse for garage door)"
      }
    ],
    "DISPLAYMESSAGE": [
      {
        "number": 1000,
        "bus_numbers": "1,1",
        "address_numbers": "13,5",
        "description": "Aurus living room + aurus basement"
      }
    ]
  }
}
```

# Running

## Environment variables

| Variable                                         | Type     | Default value | Description                                                                                                                                                       |
|--------------------------------------------------|----------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| TELETASK_HOST                                    | Required |               | The ip address or hostname of your central unit                                                                                                                   |
| TELETASK_PORT                                    | Required |               | The port of your central unit, probably 55957                                                                                                                     |
| TELETASK_ID                                      | Required |               | The id used in mqtt messages of the central unit                                                                                                                  |     
| TELETASK_MQTT_HOST                               | Required |               | The host of your MQTT broker                                                                                                                                      |     
| TELETASK_MQTT_PORT                               | Optional | 1883          | The port of your MQTT broker                                                                                                                                      |     
| TELETASK_MQTT_USERNAME                           | Optional | <empty>       | The MQTT broker username                                                                                                                                          |
| TELETASK_MQTT_PASSWORD                           | Optional | <empty>       | The MQTT broker password                                                                                                                                          |
| TELETASK_MQTT_CLIENTID                           | Optional | teletask2mqtt | The client id used for connecting to MQTT                                                                                                                         |
| TELETASK_MQTT_PREFIX                             | Optional | teletask2mqtt | The MQTT message topic prefix                                                                                                                                     |
| TELETASK_MQTT_RETAINED                           | Optional | false         | Indicates whether or not the messages should be retained by the broker                                                                                            |
| TELETASK_MQTT_DISCOVERY_PREFIX                   | Optional | homeassistant | The MQTT home assistant discovery prefix                                                                                                                          |
| TELETASK_PUBLISH_MOTOR_POSITION                  | Optional | true          | Publish the position of the motor                                                                                                                                 |
| TELETASK_PUBLISH_MOTOR_POSITION_INTERVAL         | Optional | 250           | How frequently the position is published when the motor is running (in milliseconds).                                                                             |
| TELETASK_LOG_HACONFIG_ENABLED                    | Optional | false         | (Advanced) Log the homeassistant config messages                                                                                                                  |
| TELETASK_LOG_TOPIC_ENABLED                       | Optional | false         | (Advanced) Add the topic name to the log message                                                                                                                  |
| LOGGING_LEVEL_IO_GITHUB_RIDIEKEL_JELETASK_MQTT   | Optional | INFO          | (Advanced) log level of messages going from/to teletask central unit. <br/>Can be ERROR (least log messages), WARN, INFO, DEBUG or TRACE (extensive log messages) |
| LOGGING_LEVEL_IO_GITHUB_RIDIEKEL_JELETASK_CLIENT | Optional | INFO          | (Advanced) log level of messages going from/to mqtt. <br/>Can be ERROR (least log messages), WARN, INFO, DEBUG or TRACE (extensive log messages)                  |

## Docker run

### Versions

Different versions (tags) are provided.
Depending on your needs, you will need a different version (tag)

| Tag           | Description                                                                                                                                 |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| latest-amd64  | The latest version of the application. Built for amd64 arhitecture.                                                                         |
| latest-arm64  | The latest version of the application. Built for arm64 architecture llike raspberry pi 4.                                                   |
| latest-native | The latest version, but built with spring boot native. This is only available for amd64, but should boot a lot faster on that architecture. |

### Command line

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

### Docker compose

```yaml
version: '3.8'
services:
  mqtt:
    image: eclipse-mosquitto
    restart: unless-stopped
    volumes:
      - $HOME/.jeletask/mosquitto/data:/mosquitto/data
      - $HOME/.jeletask/mosquitto/logs:/mosquitto/logs
    ports:
      - "1883:1883"
    networks:
      - jeletask
    command: "mosquitto -c /mosquitto-no-auth.conf" #Needed for unauthenticated mqtt broker with listen address 0.0.0.0

  jeletask2mqtt:
    image: ridiekel/jeletask2mqtt:latest-native
    restart: unless-stopped
    volumes:
      - $HOME/.jeletask/teletask2mqtt/config.json:/teletask2mqtt/config.json
    environment:
      TELETASK_HOST: 192.168.0.123
      TELETASK_PORT: 55957
      TELETASK_ID: my_teletask
      TELETASK_MQTT_HOST: mqtt
    depends_on:
      - mqtt
    networks:
      - jeletask

networks:
  jeletask:
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
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/relay/1/set \
    -m '{"state":"OFF"}'
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
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/locmood/1/set \
    -m '{"state":"OFF"}'
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
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/genmood/1/set \
    -m '{"state":"OFF"}'
```

## Timed Mood

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/timedmood/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/timedmood/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/timedmood/1/set \
    -m '{"state":"OFF"}'
```

## Dimmer

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/state
```

### Change the state

#### Turning on

For turning on you can use `ON` (goes to 100%), `OFF` (goes to 0%), `PREVIOUS_STATE` (goes to last %) or any integer value between `0` and `100`

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m '{"state":"ON"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m '{"state":"75"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m '{"state":"ON", "brightness": "60"}'
```

#### Turning off

For turning off, you can use either `OFF` or `0`

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m '{"state":"OFF"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m '{"state":"0"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/dimmer/1/set \
    -m '{"brightness":"0"}'
```

## Motor

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/state
```

The following json attributes are provided on the /state MQTT topic:

| Attribute         | Possible Values           | Description                                                                                                                                                                                     |
|-------------------|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| state             | ON<br/>OFF                | Running<br/>Not running                                                                                                                                                                         |
| last_direction    | UP/DOWN                   | Last running direction                                                                                                                                                                          |
| protection        | 0<br/>1<br/>2<br/>3<br/>4 | No protection defined<br/>On, and the motor is controlled by the protection<br/>On, but the motor is not controlled by the protection<br/>On, but overruled by user<br/>protection switched OFF |
| position          | 0 - 100                   | If state is ON, the position we're running to                                                                                                                                                   |
| current_position  | 0 - 100                   | The current position                                                                                                                                                                            |
| seconds_to_finish |                           | Time, in seconds, it'll take to get to position                                                                                                                                                 |

### Change the state

#### Controlling

You can send either `UP`, `DOWN` or `STOP` to the motor function.

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/set \
    -m '{"state":"UP"}'
```

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/set \
    -m '{"state":"STOP"}'
```

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/set \
    -m '{"state":"DOWN"}'
```

#### Position

You can also send the motor to a position.
Position can be anything between `0` and `100`.

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/motor/1/set \
    -m '{"position": 25}'
```

## Flag

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/flag/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/flag/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/flag/1/set \
    -m '{"state":"OFF"}'
```

## Sensor

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/sensor/1/state
```

The following sensor types are currently supported:

| Type                | Description                                                        |
|---------------------|--------------------------------------------------------------------|
| TEMPERATURE         | Teletask temperature sensor (TDS12250, TDS12251). Value is in °C.  |
| TEMPERATURECONTROL  | Teletask temperature controllabe sensor (Aurus OLED, HVAC, ...)    |
| HUMIDITY            | Teletask humidity sensor (TDS12260). Value is in %.                |
| LIGHT               | Teletask light sensor (TDS12270). Value is in Lux.                 |
| GAS                 | Teletask General Analog Sensor.                                    |
| PULSECOUNTER        | Teletask pulse counter (returns current reading + total).          |

#### HA MQTT config parameters

The following additional Home Assistant related parameters are available:

| Config parameter       | Description                                                                                                                                            |
|------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| ha_unit_of_measurement | To specify a custom 'unit_of_measurement' in HA auto discovery. For example: "°C"                                                                      |
| ha_modes               | To specify a custom "modes" in HA auto discovery. Default: "auto,off,cool,heat,dry,fan_only"                                                           |
| hatype                 | We try to guess the type of the component based on the type in teletask, but sometimes you will want to override this.<br/>You can use hatype for this |

#### More details for: GAS (General Analog Sensor)

This type of sensor has 3 additional config parameters:

| Config parameter | Description                                                                  |
|------------------|------------------------------------------------------------------------------|
| gas_type         | One of the 4 possible signal options: "4-20ma", "0-20ma", "0-10V" or "5-10V" |
| gas_min          | The "Min" value (see PROSOFT configuration)                                  |
| gas_max          | The "Max" value (see PROSOFT configuration)                                  |
| decimals         | How many decimals you want returned (rounded up)                             |

#### More details for: TEMPERATURECONTROL

Use TEMPERATURECONTROL for any temperature controllable 'sensor' like an AC (HVAC) or an Aurus OLED wall switch

The following json attributes are provided on the /state MQTT topic:

| Attribute                           | Possible Values           | Description                                       |
|-------------------------------------|---------------------------|---------------------------------------------------|
| state                               | ON/OFF                    |                                                   |
| current_temperature                 |                           | The current temperature                           |
| target_temperature                  |                           | The set target temperature                        |
| decimals                            |                           | How many decimals you want returned (rounded up)  |
| preset                              | DAY/NIGHT/ECO             | The current preset                                |
| mode                                | AUTO/HEAT/COOL/VENT/DRY   | The current mode                                  |
| fanspeed                            | SPAUTO/SPLOW/SPMED/SPHIGH | The current fan speed                             |
| window_open                         | 0 = closed / 255 = open   | Window state (Untested)                           |
| swing_direction                     |                           | switch direction as value (Untested)              |
| output_state                        |                           | Untested/Unsure                                   |
| day_preset_temperature              |                           | Day preset temperature (see prosoft)              |
| night_at_heating_preset_temperature |                           | Night at heating temperature (see prosoft)        |
| night_at_cooling_preset_temperature |                           | Night at cooling temperature (see prosoft)        |
| eco_preset_offset                   |                           | ECO offset preset temperature (see prosoft)       |

The following "state" attribute commands are supported on the /set MQTT topic:

| For state change            | Description                                 |
|-----------------------------|---------------------------------------------|
| ON                          | Turn on the device                          |
| OFF                         | Turn off the device                         |
| ONOFF                       | Toggle on or off the device                 |
| UP                          | Set target temperature up 0.5°C             |
| DOWN                        | Set target temperature down 0.5°C           |

| For preset change | Description            |
|-------------------|------------------------|
| DAY               | Change to day preset   |
| NIGHT             | Change to night preset |
| ECO               | Change to eco preset   |

| For operation mode change | Description                    |
|---------------------------|--------------------------------|
| AUTO                      | Set operating mode to auto     |
| HEAT                      | Set operating mode to heat     |
| COOL                      | Set operating mode to cool     |
| VENT                      | Set operating mode to vent     |
| DRY                       | Set operating mode to dry      |
| MODE                      | Toggle between available modes |

| For fan speed change   | Description                    |
|------------------------|--------------------------------|
| SPAUTO                 | Set fan speed to auto          |
| SPLOW                  | Set fan speed to slow          |
| SPMED                  | Set fan speed to medium        |
| SPHIGH                 | Set fan speed to high          |
| SPEED                  | Toggle between available modes |

You can also set the target temperature by sending the desired temperature to the "target_temperature" attribute.

## Input (digital inputs)

### Listen to sensor events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/input/1/state
```

The following json attributes are provided on the /state MQTT topic:

| Attribute         | Possible Values | Description                              |
|-------------------|-----------------|------------------------------------------|
| state             | OPEN<br/>CLOSED | Button is pressed<br/>Button is released |

## Timed function

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/timedfnc/1/state
```

### Starting / stopping a timed function

#### Starting a timed function

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/timedfnc/1/set \
    -m '{"state":"ON"}'
```

#### Stopping a timed function

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/timedfnc/1/set \
    -m '{"state":"OFF"}'
```

## Display message

You can only send a display message. Listening for events is not supported.

DISPLAYMESSAGE config attribute parameters:

| Attribute       | Description                           |
|-----------------|---------------------------------------|
| number          | A unique DISPLAYMESSAGE number        |
| bus_numbers     | A list of BUS numbers (see below)     |
| address_numbers | A list of ADDRESS numbers (see below) |
| description     | Description for this config entry     |

`bus_numbers` and `addres_numbers` are two comma separated lists of the same size.
They contain the Teletask bus number(s) and Teletask interface address(es) of the interface(s) on which to display the Message/Alarm

#### Sending a display message

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_ID>/displaymessage/1000/set \
    -m '{"message_line1":"DOORBELL","message_line2":"PLEASE OPEN!", "message_beeps":"10","message_type="message"}'
```

Notes:

- Both lines each have maximum length of 16 chars. This is a Teletask limitation.
- message_type can either be "alarm" or "message".

# HomeAssistant

Auto configuration should work with relays, dimmers, motors, sensors, timed functions, flags, general/local/timed moods, conditions and digital inputs.
Other types are not yet supported, work in progress.
Pleas log an issue when having trouble with auto configuration in HA.

The bridge creates 1 device with id ```teletask-<TELETASK_ID>```, and adds entities with the following entity id pattern: ```light.teletask_<TELETASK_ID>_<FUNCTION_TYPE>_<COMPONENT_NUMBER>```, which should be unique for your installation.

Examples:

```
switch.teletask_my_teletask_relay_1
light.teletask_my_teletask_relay_36
```

## Additional HA config

### Relay

The default type of a relay is ```light```, but can be overridden.
Possible values: https://www.home-assistant.io/docs/mqtt/discovery/#lighting

```json
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
