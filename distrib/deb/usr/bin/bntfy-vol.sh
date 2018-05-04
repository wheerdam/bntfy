#!/bin/bash

#
# notify host and port
#
HOST=127.0.0.1
PORT=25311

#
# channel to control
#
CHANNEL="Master"

#
# additional commands to run to unmute sound
#
UNMUTE_EXTRA="amixer -q set Headphone unmute; amixer -q set Speaker unmute"

MIXER="amixer -q set $CHANNEL"
GET_VOLUME="amixer get $CHANNEL | sed -n 's/.*\[\(.*\)%\].*/\1/p'"
GET_MUTE_STATUS="amixer get $CHANNEL | sed -n 's/.*\[\([onf]*\)\].*/\1/p'"
TOGGLE="$MIXER toggle; $UNMUTE_EXTRA" 
MUTE="$MIXER mute"
UNMUTE="$MIXER unmute; $UNMUTE_EXTRA"
if [ ! -f /tmp/bntfy-vol.value ]; then
	VALUE=`eval $GET_VOLUME`
	echo "$VALUE" > /tmp/bntfy-vol.value
else
	VALUE=`cat /tmp/bntfy-vol.value`
fi

setvol() {
    eval "$MIXER $1%"
	echo "$1" > /tmp/bntfy-vol.value
    if [ $1 -eq "0" ]; then
        eval $MUTE
    else
        eval $UNMUTE
    fi
}

if [ "$1" = "toggle" ]; then
    eval $TOGGLE
elif [ "$1" = "mute" ]; then
    eval $MUTE
elif [ "$1" = "unmute" ]; then
    eval $UNMUTE
elif [[ $1 == +* ]]; then
    ADD=${1:1}
    VALUE=$((VALUE+ADD))
    if [ $VALUE -gt 100 ]; then
        VALUE=100
    fi
    setvol $VALUE
elif [[ $1 == -* ]]; then
    SUB=${1:1}
    VALUE=$((VALUE-SUB))
    if [ $VALUE -lt 0 ]; then
        VALUE=0
    fi
    setvol $VALUE
elif [ "$1" = "update" ]; then
    # do nothing, just output to notify
    # we only need to catch this condition so it doesn't fall through to else
    :
else
    VALUE=$1
    setvol $VALUE
fi

# output to notify
STATUS=`eval $GET_MUTE_STATUS`
if [ "$STATUS" = "on" ]; then
    printf "bartitle:,$VALUE,20bbff,right"      > /dev/udp/$HOST/$PORT
    printf "tray:Volume: $VALUE,$VALUE,20bbff"  > /dev/udp/$HOST/$PORT
    printf "mute:off"                           > /dev/udp/$HOST/$PORT
else
    printf "text:MUTE,centered"     > /dev/udp/$HOST/$PORT
    printf "tray:Muted,100,ff0000"  > /dev/udp/$HOST/$PORT
    printf "mute:on"                > /dev/udp/$HOST/$PORT
fi
