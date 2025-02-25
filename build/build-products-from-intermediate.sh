#!/usr/bin/env bash

# Copyright 2023-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This script builds the executable RCE "products" (Eclipse terminology) from
# a previously built "intermediate" p2 repository.


# Set working directory to the containing "build" folder
cd "$(dirname "$(readlink -f "$0")")"

# Load settings and functions
. ../de.rcenvironment.build.main/bin/include.sh

build_products_from_repo
