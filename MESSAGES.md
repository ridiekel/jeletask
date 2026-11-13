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
| output_state                        |                           | Bit 0 = the thermostat's own heat/cool demand (seen on a MICROS+: 8 = idle, 9 = demand) |
| day_preset_temperature              |                           | Day preset temperature (see prosoft)             |
| night_at_heating_preset_temperature |                           | Night at heating temperature (see prosoft)       |
| night_at_cooling_preset_temperature |                           | Night at cooling temperature (see prosoft)       |
| eco_preset_offset                   |                           | ECO offset preset temperature (see prosoft)      |

Unknown preset, mode or fan speed values (e.g. `0`, sent by radiator zones without a fan) are ignored and logged
instead of aborting the whole state message; the attribute is then left out of the JSON.

| For actions  | Description                                                                         |
|--------------|-------------------------------------------------------------------------------------|
| TARGET       | Set target state, also pass the target_temperature value                            |
| DAY_PRESET   | Store a new day preset temperature, pass `day_preset_temperature`                   |
| NIGHT_PRESET | Store a new night (heating) preset temperature, pass `night_at_heating_preset_temperature` |
| ECO_OFFSET   | Store a new eco offset (eco = day − offset), pass `eco_preset_offset`               |

The preset actions only change the stored preset temperatures; they never switch a thermostat on.
Day/night are sent as 1 byte `(°C + 40) × 2`, so they must be between -40 and 87.5 °C; the eco offset must be between
0 and 25.5 °C, the most a report can hold. A value outside that range, or a missing value, is rejected and nothing is sent. The eco offset is sent in steps of 0.5 °C (`offset × 2`):
the DoIP manual (TDS15132) says `× 10`, but a MICROS+ stored 15.0 °C when sent 30 (reports do use `× 10`, unsigned). This was only measured
on a heating-only MICROS+: other central types may follow the manual, and negative offsets are untested.

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/sensor/9/set \
    -m '{"state":"ON","action":"DAY_PRESET","day_preset_temperature":21.5}'
```

| For state change            | Description                                 |
|-----------------------------|---------------------------------------------|
| ON                          | Turn on the device                          |
| OFF                         | Turn off the device                         |

| For preset change | Description                                                      |
|-------------------|------------------------------------------------------------------|
| DAY               | Change to day preset                                             |
| NIGHT             | Change to night preset                                           |
| ECO               | Change to eco preset                                             |
| FROST             | Change to frost protection preset                                |
| MANUAL            | Leave the presets: re-sends the current target temperature      |

Choosing a mode, fan speed or preset on an OFF thermostat (e.g. in Home Assistant) sends `ON` first and then the
chosen mode, fan speed or preset. `{"state":"ON"}` on its own turns the thermostat back on in its previous mode.

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

| Attribute       | Required | Description                                                                                  |
|-----------------|----------|----------------------------------------------------------------------------------------------|
| number          | yes      | A unique DISPLAYMESSAGE number                                                               |
| bus_numbers     | yes      | A list of BUS numbers (see below)                                                            |
| address_numbers | yes      | A list of ADDRESS numbers (see below)                                                        |
| description     | no       | Description for this config entry                                                            |
| message_type    | no       | `message` or `alarm`: the type of a display message that does not say. Without it: `alarm`  |
| message_beeps   | no       | 0 to 255: the number of beeps of a display message that does not say. Without it: 1          |
| ascii           | no       | `true`: 16 ASCII characters per line, `false`: 8 characters of any kind. For a display message that does not say. Without it: `false` |

`bus_numbers` and `address_numbers` are two comma separated lists of the same size.
They contain the Teletask bus number(s) and Teletask interface address(es) of the interface(s) on which to display the Message/Alarm

```json
"DISPLAYMESSAGE": [
  {
    "number": 1000,
    "bus_numbers": "1,1",
    "address_numbers": "13,5",
    "description": "Aurus living room + aurus basement",
    "message_type": "message",
    "message_beeps": 2,
    "ascii": true
  }
]
```

#### Sending a display message

Publish JSON to the set topic of the display:

```
<TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/<number>/set
```

| Field         | Description                                                                                                                                         |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| message_line1 | The first line                                                                                                                                      |
| message_line2 | The second line                                                                                                                                     |
| message       | Instead of the two lines: one text that the bridge spreads over both lines (see below). Not together with `message_line1` or `message_line2`       |
| ascii         | `true`: 16 ASCII characters per line. `false`: 8 characters per line, any character. Left out: the `ascii` of the display in the config, or else `false` |
| message_type  | `message` or `alarm`. Left out: the `message_type` of the display in the config, or else `alarm`                                                   |
| message_beeps | The number of beeps, 0 to 255; more is sent as 255, less as 0, with a warning in the log. Left out: the `message_beeps` of the display in the config, or else 1 |

`message` is spread over the lines like this:

1. If the text fits on line 1, it all goes there.
2. Otherwise line 1 ends at the last place that still fits where a line may break, and the rest goes to line 2:
   - at a space, which disappears;
   - after a hyphen `-`, a dash `–` `—` or a slash `/`, which stay on line 1.

   Not after a full stop or a comma: without a space after them they are usually part of a number, like `21.5` or
   `12,5`. A word longer than a line is split where the line ends.
3. What does not fit on line 2 is cut off, with a warning in the log.

The examples below are for display 1000 with the config above (`message`, 2 beeps, ascii). The box shows what the
display shows.

##### Two lines, each on its own

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message_line1":"DOORBELL","message_line2":"PLEASE OPEN!"}'
```

```
┌────────────────┐
│DOORBELL        │
│PLEASE OPEN!    │
└────────────────┘
```

Only line 1 is fine too: `{"message_line1":"DOORBELL"}` leaves line 2 empty.

##### One text, spread over both lines

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message":"Someone is at the front door"}'
```

```
┌────────────────┐
│Someone is at   │
│the front door  │
└────────────────┘
```

"Someone is at the" would be 17 characters, so line 1 ends at the space before "the".

##### A text with hyphens or slashes

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message":"Garage-door-opener open"}'
```

```
┌────────────────┐
│Garage-door-    │
│opener open     │
└────────────────┘
```

On a display without ascii (8 characters per line), `{"message":"Bad/Keuken"}` shows:

```
┌────────┐
│Bad/    │
│Keuken  │
└────────┘
```

##### A text that does not fit

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message":"Someone is at the front door, please open it"}'
```

```
┌────────────────┐
│Someone is at   │
│the front door, │
└────────────────┘
```

and in the log:

```
Display message 1000 line 2 'the front door, please open it' has 30 characters, the display shows 16 with ascii: cut to 'the front door, '
```

The same warning appears for `message_line1` or `message_line2` when they are too long.

##### An alarm, while the display defaults to a message

What the message says wins over the defaults of the display:

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message":"WATER LEAK BASEMENT","message_type":"alarm","message_beeps":10}'
```

```
┌────────────────┐
│WATER LEAK      │
│BASEMENT        │
└────────────────┘
```

##### Without a sound

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message":"Good night","message_beeps":0}'
```

##### Characters outside ASCII

With `"ascii": false` a line holds 8 characters, but any character (UTF-16), such as é or ü. With `"ascii": true` they
show as `?`.

```
mosquitto_pub -h <TELETASK_MQTT_HOST> -p <TELETASK_MQTT_PORT> \
    -t <TELETASK_MQTT_PREFIX>/<TELETASK_CENTRAL_ID>/displaymessage/1000/set \
    -m '{"message":"Café is open","ascii":false}'
```

```
┌────────┐
│Café is │
│open    │
└────────┘
```

##### Not accepted

`message` together with `message_line1` or `message_line2` is refused, because it is not clear which text to show.
Nothing is sent to the display, and the log says why:

```
MQTT topic '.../displaymessage/1000/set' could not change state to: {"message":"Doorbell","message_line2":"Open up"} -- A display message has either a 'message' or 'message_line1' and 'message_line2', not both
```

Notes:

- A display shows at most 16 (ascii) or 8 characters per line. This is a Teletask limitation.
- The same message twice is shown twice: a display has no state, so nothing is dropped as unchanged.

#### From Home Assistant

Every display is a `notify` entity in Home Assistant, e.g. `notify.teletask_<TELETASK_CENTRAL_ID>_displaymessage_1000`.
Its `notify.send_message` action sends the text as `message`, so the bridge spreads it over the two lines as described
above. Home Assistant only passes the text, so the message type, ascii and the beeps come from `message_type`, `ascii`
and `message_beeps` of the display in the config. A `title` is not shown: a display has no room for it.

A message from a script or the developer tools:

```yaml
action: notify.send_message
target:
  entity_id: notify.teletask_1_displaymessage_1000
data:
  message: "Someone is at the front door"
```

An automation that shows the doorbell on the displays, with the time:

```yaml
alias: Doorbell on the displays
triggers:
  - trigger: state
    entity_id: sensor.teletask_1_input_42
    to: "CLOSED"
actions:
  - action: notify.send_message
    target:
      entity_id: notify.teletask_1_displaymessage_1000
    data:
      message: "Doorbell {{ now().strftime('%H:%M') }}"
```

Several displays at once:

```yaml
action: notify.send_message
target:
  entity_id:
    - notify.teletask_1_displaymessage_1000
    - notify.teletask_1_displaymessage_1001
data:
  message: "Dinner is ready"
```

For an alarm, other beeps or each line on its own from Home Assistant, publish the JSON yourself:

```yaml
action: mqtt.publish
data:
  topic: teletask2mqtt/1/displaymessage/1000/set
  payload: >-
    {"message_line1": "WATER LEAK", "message_line2": "BASEMENT", "message_type": "alarm", "message_beeps": 10}
```
