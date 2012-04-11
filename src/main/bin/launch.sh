#!/bin/sh

PRO_CMD=$1
LOG_FILE=$2

$PRO_CMD > $LOG_FILE 2>&1 &
echo $!
