#!/usr/bin/env bash

# Copyright 2023-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This script runs the Eclipse/OSGi bundle tests in this repository,
# loading required libraries from the configured third-party p2 repository.
#
# The default invocation runs the standard tests (non-extended, default scope).
# If you specify any parameters, the first must be the test type filter:
# "-d" for default tests, "-e" for extended. The optional second parameter can 
# select a non-standard build scope (see /de.rcenvironment/maven/buildScopes).


# Set working directory to the containing "build" folder
cd "$(dirname "$(readlink -f "$0")")"

# Load settings and functions
. ../de.rcenvironment.build.main/bin/include.sh

if [ $# -eq 0 ]; then
  run_unit_tests "-d"
else
  run_unit_tests $@
fi

# Determine crude total test and error counts from the XML files
TEST_COUNT=$(cd target/unit-tests/reports/xml/ ; grep "<testcase " * | wc -l)
FAILURE_COUNT=$(cd target/unit-tests/reports/xml/ ; grep -E "<(error|failure)" * | wc -l)
OUTCOME_TEXT="Detected ${FAILURE_COUNT} test failure(s) out of ${TEST_COUNT}"

echo $OUTCOME_TEXT
# also append it to the test build log
echo $OUTCOME_TEXT >>target/unit-tests/build.log

if [ $FAILURE_COUNT -eq 0 ]; then
  exit 0
else
  exit 2
fi
