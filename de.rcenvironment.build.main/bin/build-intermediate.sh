#!/usr/bin/env bash

# Copyright 2023 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This script builds the "intermediate" p2 repository from this repository's 
# sources and the configured third-party p2 repository.
#
# The "intermediate" repository contains most of the bundles and features to 
# build executable RCE products from, combining artifacts compiled as part of
# this operation as well as artifacts fetched from the third-party repository.


# Set working directory to the .build.meta project
cd "$(dirname "$(readlink -f "$0")")/.."

# Load settings and functions
. bin/include.sh

# Invoke imported functions
build_intermediate_repo
