#!/bin/bash

echo "Starting Border Router .."
cd ../rpl-border-router

make TARGET=cooja connect-router-cooja
