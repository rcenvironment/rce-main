#!/usr/bin/env bash

# Copyright 2006-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Jan Flink, Robert Mischke

# During a regular build, the RCE documentation is generated and rendered as part of the product
# build process. This script builds it in isolation instead to allow faster iteration and testing.

# set working directory to the .build.meta project
cd "$(dirname "$(readlink -f "$0")")/.."

# load settings and functions
. bin/include.sh

build_documentation_only
