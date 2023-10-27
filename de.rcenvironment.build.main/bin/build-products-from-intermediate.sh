#!/usr/bin/env bash

# Copyright 2023 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This script builds the executable RCE "products" (Eclipse terminology) from
# a previously built "intermediate" p2 repository.


# set working directory to the .build.meta project
cd "$(dirname "$(readlink -f "$0")")/.."

# load settings and functions
. bin/include.sh

build_products_from_repo
