# Jeletask

An open source java API for Teletask domotics.

This project is a fork from the xhibit version that became inactive (https://github.com/xhibit/Teletask-api).

It is the purpose to create an API for software developers or domotics enthusiasts, who are interested in generating their own control environment for the TELETASK domotics systems, so you can create your own user interface and connected solutions and services.

If you own a Teletask MICROS or MICROS+, you have access to the free (or paid in case of the MICROS+) DLL32 LIBRARY (TDS15132).  
However, if you're a java programmer like myself, you don't want to use a windows dll :-).

The API also supports the MICROS+, but you'll have to buy a licence to be able to make TCP calls.

For the MICROS you can buy a RS232 > LAN converter (TDS10118) so you can access the MICROS server through regular IP.

Started discussing the possibilities on the Teletask forum
...and ended up programming a java interface based on IP Sockets, exposed by a basic java API.

Initially only setting and getting RELAYS, MOTOR, GENMOOD, LOCMOOD, COND, FLAG is supported.

The purpose of this library is to actually be able to put a REST or other API on top of this.