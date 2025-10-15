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
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/relay/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/relay/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/relay/1/set \
    -m '{"state":"OFF"}'
```

## Local Mood

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/locmood/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/locmood/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/locmood/1/set \
    -m '{"state":"OFF"}'
```

## General Mood

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/genmood/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/genmood/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/genmood/1/set \
    -m '{"state":"OFF"}'
```

## Timed Mood

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/timedmood/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/timedmood/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/timedmood/1/set \
    -m '{"state":"OFF"}'
```

## Dimmer

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/state
```

### Change the state

#### Turning on

For turning on you can use `ON` (goes to 100%), `OFF` (goes to 0%), `PREVIOUS_STATE` (goes to last %) or any integer value between `0` and `100`

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/set \
    -m '{"state":"ON"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/set \
    -m '{"state":"75"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/set \
    -m '{"state":"ON", "brightness": "60"}'
```

#### Turning off

For turning off, you can use either `OFF` or `0`

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/set \
    -m '{"state":"OFF"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/set \
    -m '{"state":"0"}'
```

or

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/dimmer/1/set \
    -m '{"brightness":"0"}'
```

## Motor

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/motor/1/state
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
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/motor/1/set \
    -m '{"state":"UP"}'
```

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/motor/1/set \
    -m '{"state":"STOP"}'
```

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/motor/1/set \
    -m '{"state":"DOWN"}'
```

#### Position

You can also send the motor to a position.
Position can be anything between `0` and `100`.

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/motor/1/set \
    -m '{"position": 25}'
```

## Flag

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/flag/1/state
```

### Change the state

#### Turning on

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/flag/1/set \
    -m '{"state":"ON"}'
```

#### Turning off

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/flag/1/set \
    -m '{"state":"OFF"}'
```

## Sensor

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/sensor/1/state
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

| Attribute                           | Possible Values           | Description                                      |
|-------------------------------------|---------------------------|--------------------------------------------------|
| state                               | ON/OFF                    |                                                  |
| action                              | ON/OFF                    |                                                  |
| current_temperature                 |                           | The current temperature                          |
| target_temperature                  |                           | The set target temperature                       |
| decimals                            |                           | How many decimals you want returned (rounded up) |
| preset                              | DAY/NIGHT/ECO             | The current preset                               |
| mode                                | AUTO/HEAT/COOL/VENT/DRY   | The current mode                                 |
| fanspeed                            | SPAUTO/SPLOW/SPMED/SPHIGH | The current fan speed                            |
| window_open                         | 0 = closed / 255 = open   | Window state (Untested)                          |
| swing_direction                     |                           | switch direction as value (Untested)             |
| output_state                        |                           | Untested/Unsure                                  |
| day_preset_temperature              |                           | Day preset temperature (see prosoft)             |
| night_at_heating_preset_temperature |                           | Night at heating temperature (see prosoft)       |
| night_at_cooling_preset_temperature |                           | Night at cooling temperature (see prosoft)       |
| eco_preset_offset                   |                           | ECO offset preset temperature (see prosoft)      |

| For actions | Description                                              |
|-------------|----------------------------------------------------------|
| TARGET      | Set target state, also pass the target_temperature value |

| For state change            | Description                                 |
|-----------------------------|---------------------------------------------|
| ON                          | Turn on the device                          |
| OFF                         | Turn off the device                         |

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

| For fan speed change | Description             |
|----------------------|-------------------------|
| AUTO                 | Set fan speed to auto   |
| LOW                  | Set fan speed to slow   |
| MEDIUM               | Set fan speed to medium |
| HIGH                 | Set fan speed to high   |

You can also set the target temperature by sending the desired temperature to the "target_temperature" attribute.

## Input (digital inputs)

### Listen to sensor events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/input/1/state
```

The following json attributes are provided on the /state MQTT topic:

| Attribute                   | Possible Values                                                | Description                                                                                                                                                                                      |
|-----------------------------|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| state                       | CLOSED<br/>OPEN<br/>LONG_PRESS<br/>SHORT_PRESS<br/>NOT_PRESSED | Button is being pressed<br/>Button press finished<br/>Button was pressed for a long time (configurable*)<br/>Button was pressed for a short time (configurable*)<br/>Button is no longer pressed |
| open_time                   | long                                                           | The time the pressing started (when state is LONG_PRESS or SHORT_PRESS)                                                                                                                          |
| close_time                  | long                                                           | The time the pressing ended (when state is LONG_PRESS or SHORT_PRESS)                                                                                                                            |
| long_press_config_in_millis | integer                                                        | The configured amount of time for long press to occur                                                                                                                                            |
| press_duration_millis       | integer                                                        | The actual amount of time the button was pressed (when state is LONG_PRESS or SHORT_PRESS)                                                                                                       |

&ast; Check your teletask config.
You should enable "Edge triggered" for this to work.

If you provide `"long_press_duration_millis" : <duration_in_millis>` in the config, the states will be either `NOT_PRESSED`, `SHORT_PRESS` or `LONG_PRESS`.

If you don't provide this config, the states will be `OPEN` (Not pressing the input) or `CLOSED` (Pressing the input).

## Timed function

### Listen to events

```
mosquitto_sub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/timedfnc/1/state
```

### Starting / stopping a timed function

#### Starting a timed function

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/timedfnc/1/set \
    -m '{"state":"ON"}'
```

#### Stopping a timed function

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/timedfnc/1/set \
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
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message_line1":"DOORBELL","message_line2":"PLEASE OPEN!", "message_beeps":"10","message_type="message"}'
```

Notes:

- Both lines each have maximum length of 16 chars. This is a Teletask limitation.
- message_type can either be "alarm" or "message".
