#!/bin/bash

# This sets up the source directory for use with the Android SDK tools from
# the command line.


# Run from the root directory of the project

android update project \
    -p . \
    -n cpuspy \
    -t android-14
