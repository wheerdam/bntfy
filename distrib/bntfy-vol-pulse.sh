#!/bin/bash

#
# notify host and port
#
HOST=127.0.0.1
PORT=25311

#
# channel to control
#
CHANNEL=`pacmd list-sinks | grep index | awk '{print $3}'`

#
# additional commands to run to unmute sound
#
UNMUTE_EXTRA=""

MIXER="pacmd set-sink-volume $CHANNEL"
GET_VOLUME="pacmd list-sinks | grep front-left: | awk '{print $5}' | sed -n 's/(.*)%/\1/p'"
GET_MUTE_STATUS="pacmd list-sinks | grep muted: | awk '{print $2}'"
MUTE="pacmd set-sink-mute $CHANNEL 1"
UNMUTE="pacmd set-sink-mute $CHANNEL 0"
if [ ! -f /tmp/bntfy-vol.value ]; then
	VALUE=`eval $GET_VOLUME`
	echo "$VALUE" > /tmp/bntfy-vol.value
else
	VALUE=`cat /tmp/bntfy-vol.value`
fi

setvol() {
	VOL=$(( $1 * 1000 ))
    eval "$MIXER $VOL"
	echo "$1" > /tmp/bntfy-vol.value
    if [ $1 -eq "0" ]; then
        eval $MUTE
    else
        eval $UNMUTE
    fi
}

if [ "$1" = "toggle" ]; then
	STATUS=`eval $GET_MUTE_STATUS | awk '{print $2}'`
	if [ "$STATUS" = "no" ]; then
		eval $MUTE
	else
		eval $UNMUTE
	fi
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
STATUS=`eval $GET_MUTE_STATUS | awk '{print $2}'`
if [ "$STATUS" = "no" ]; then
	COLOR="20bbff"
	if [ $VALUE -gt 65 ]; then
		COLOR="ffbb20"
	fi
    printf "bartitle:,$VALUE,$COLOR,right"      > /dev/udp/$HOST/$PORT
    printf "tray:Volume: $VALUE,$VALUE,$COLOR"  > /dev/udp/$HOST/$PORT
    printf "mute:off"                           > /dev/udp/$HOST/$PORT
else
	echo "muted"
    printf "text:MUTE,centered"     > /dev/udp/$HOST/$PORT
    printf "tray:Muted,100,ff0000"  > /dev/udp/$HOST/$PORT
    printf "mute:on"                > /dev/udp/$HOST/$PORT
fi
