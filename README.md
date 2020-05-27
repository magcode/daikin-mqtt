# daikin-mqtt
MQTT interface for Daikin AC devices. 
Based on the (fork of) [Daikin Java interface](https://bitbucket.org/m-do/jdaikin) of Jonathan Giles.

It aims to be compliant to the [Homie convention](https://homieiot.github.io).

This software has only been tested with the Daikin **BRP069A42** adapter. Others may work. Please test and let me know.

# Properties file
You need a `daikin.properties` file:

```
rootTopic=home                    # the mqtt root topic
refresh=60                        # number of seconds for MQTT status updates. Do not go below 60!
mqttServer=tcp://192.168.0.1      # IP or hostname of your mqtt broker

daikin1.host=192.168.0.2          # IP adress of your first Daikin Wifi adapter
daikin1.type=wireless             # only "wireless" is supported
daikin1.name=ac-room1             # a name for the Daikin device, used in the MQTT topic

daikin2.host=192.168.0.3
daikin2.type=wireless
daikin2.name=ac-room2
```

Notes
* You can define as many controllers as you want.
* Use the IP address of your controller. Host names do not work!

# Running it
It can be simply run with

`java -jar /var/javaapps/daikin/daikin-mqtt-1.0.0-jar-with-dependencies.jar`

Don't forget to put the `daikin.properties` right beside the jar file.

On Linux I recommend to use a service like this:

```
[Unit]
Description=daikin
After=syslog.target
Wants=network-online.target
After=network-online.target
[Service]
User=daikin
ExecStart=/usr/bin/java -XX:-UsePerfData -jar /var/javaapps/daikin/daikin-mqtt-1.0.0-jar-with-dependencies.jar
SuccessExitStatus=143
WorkingDirectory=/var/javaapps/daikin
[Install]
WantedBy=multi-user.target
```

You can keep this service running, even when powering off your Daikin AC device completely. Once it comes back online it will be reported via MQTT (`$state=ready`, compare with [Homie Device Lifecycle](https://homieiot.github.io/specification/#device-lifecycle)).

# MQTT structure

Following the homie convention the interface will post the following messages every 5 minutes.

```
home/ac-room1/$localip (the ip of the wifi adapter)
home/ac-room1/$homie (homie version, "3.0.0")
home/ac-room1/$name ("Aircondition - <name as given in the properties file>")
home/ac-room1/$fw/name ("daikin-mqtt")
home/ac-room1/$fw/version (current version of the MQTT interface)
home/ac-room1/$nodes ("aircon")
home/ac-room1/$implementation ("daikin-mqtt")
home/ac-room1/$stats/interval "600"
home/ac-room1/aircon/$name ("Aircondition")
home/ac-room1/aircon/$type ("Aircondition")
home/ac-room1/aircon/$properties ("targettemp,otemp,intemp,fan,fandirection,mode,power")
home/ac-room1/aircon/targettemp/$settable ("true")
home/ac-room1/aircon/fan/$settable ("true")
home/ac-room1/aircon/fandirection/$settable ("true")
home/ac-room1/aircon/mode/$settable ("true")
home/ac-room1/aircon/power/$settable ("true")
```


If online, the device will send the following messages every xx seconds (as configured in `daikin.properties`):

```
home/ac-room1/$stats/uptime (seconds since the adapter is reachable)
home/ac-room1/$state ("ready")
home/ac-room1/aircon/power (power status)
home/ac-room1/aircon/mode (mode, e.g. "Cool")
home/ac-room1/aircon/targettemp (target temperature)
home/ac-room1/aircon/intemp (inside temperature)
home/ac-room1/aircon/otemp (outside temperature)
home/ac-room1/aircon/fan (fan mode)
home/ac-room1/aircon/fandirection (fan direction)
```

In case the device gets offline the following messages are sent every xx seconds:

```
home/ac-room1/$stats/uptime 0
home/ac-room1/$state lost
home/ac-room1/aircon/mode None
home/ac-room1/aircon/power false
home/ac-room1/aircon/otemp 0
home/ac-room1/aircon/intemp 0
home/ac-room1/aircon/targettemp 0
home/ac-room1/aircon/fan None
home/ac-room1/aircon/fandirection None
```

The following topics can be used to send messages:

```
home/ac-room1/aircon/targettemp/$set
home/ac-room1/aircon/fan/$set
home/ac-room1/aircon/fandirection/$set
home/ac-room1/aircon/mode/$set

```

Notes
* `otemp` will go to `0` in case the device is not cooling or heating.
* You actually don't need to use `power` to turn the AC on or off. Sending `mode=Cool` or `mode=None` does the job as well.
* Possible values for `mode`, `fandirection` etc. can be found [here](https://bitbucket.org/m-do/jdaikin/src/default/src/main/java/net/jonathangiles/daikin/enums/) 

# Openhab integration example

## Things
```
Thing mqtt:topic:ac-room1 "Aircondition room 1" (mqtt:broker:mosquitto) {
    Channels:
        Type string : mode "Mode" [ stateTopic="home/ac-room1/aircon/mode", commandTopic="home/ac-room1/aircon/mode/set"]
        Type number : intemp "Inside temp" [ stateTopic="home/ac-room1/aircon/intemp"] 
        Type number : outtemp "Outside temp" [ stateTopic="home/ac-room1/aircon/otemp"] 
        Type number : targettemp "Target temp" [ stateTopic="home/ac-room1/aircon/targettemp"] 
        Type switch : power "Power" [ stateTopic="home/ac-room1/aircon/power", commandTopic="home/ac-room1/aircon/power/set", on="true", off="false"]
        Type switch : online "Online" [ stateTopic="home/ac-room1/$state", on="ready", off="lost"]
}
```
## Items
```
Number acRoom1TempTarget  "AC Room 1 target temp [%.1f °C]" { channel="mqtt:topic:ac-room1:targettemp" }
Number acRoom1TempCur  "AC Room 1 current temp [%.1f °C]" { channel="mqtt:topic:ac-room1:intemp" }
Number acRoom1TempOt  "AC Room 1 temp outside [%.1f °C]" { channel="mqtt:topic:ac-room1:outtemp" }
String acRoom1Mode  "AC Room 1 Mode" { channel="mqtt:topic:ac-room1:mode" }
Switch acRoom1Power "AC Room 1 Power" { channel="mqtt:topic:ac-room1:power" }
Switch acRoom1Online "AC Room 1 Online" { channel="mqtt:topic:ac-room1:online" }
```
