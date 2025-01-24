#!/usr/bin/env bash

# Copyright 2006-2024 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke, Jan Flink

# This script builds the executable RCE "products" (Eclipse terminology) 
# from this repository's sources and the configured third-party p2 
# repository in a single invocation.


# set working directory to the .build.meta project
cd "$(dirname "$(readlink -f "$0")")/.."

# load settings and functions
. bin/include.sh

generateDocumentation
