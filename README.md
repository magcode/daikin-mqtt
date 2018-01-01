# daikin-mqtt
MQTT interface for Daikin AC devices.
Based on [Daikin Java interface](https://bitbucket.org/m-do/jdaikin) of Jonathan Giles.

*This software is WIP*

# Properties file
You need a properties file:

```
topic=home
refresh=60
mqttServer=tcp://192.168.0.1

daikin1.host=192.168.0.2
daikin1.type=wireless
daikin1.name=ac-room1

daikin2.host=192.168.0.2
daikin2.type=wireless
daikin2.name=ac-room2
```