#!/usr/bin/env bash

# Copyright 2023-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# A convenience script that copies the overrides template file to 
# the proper override file location.

cd "$(dirname "$(readlink -f "$0")")"

cp config/overrides.template.conf config/overrides.conf
