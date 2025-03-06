#!/usr/bin/env bash

# Copyright 2023-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Jan Flink, Robert Mischke

# During a regular build, the RCE documentation is generated and rendered as part of the product
# build process. This script builds it in isolation instead to allow faster iteration and testing.

# Set working directory to the containing "build" folder
cd "$(dirname "$(readlink -f "$0")")"

# Load settings and functions
. ../de.rcenvironment.build.main/bin/include.sh

build_documentation_only
