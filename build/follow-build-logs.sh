#!/usr/bin/env bash

# Copyright 2023-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# A simple convenience script to follow (tail) all log files generated 
# by the intermediate repository and product build scripts at once.

# Set the working directory to the containing "build" folder
cd "$(dirname "$(readlink -f "$0")")"

tail -F --retry \
  target/intermediate/build.log \
  target/products/build.log \
  target/documentation/build.log \
  target/unit-tests/build.log \
  2>/dev/null
