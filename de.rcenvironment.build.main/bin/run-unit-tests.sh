#!/usr/bin/env bash

# Copyright 2006-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This script runs the Eclipse/OSGi bundle tests in this repository,
# loading required libraries from the configured third-party p2 repository.


# set working directory to the .build.meta project
cd "$(dirname "$(readlink -f "$0")")/.."

# load settings and functions
. bin/include.sh

run_unit_tests
