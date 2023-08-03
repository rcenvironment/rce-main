# Copyright 2023 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This file is included from all main operation scripts, and provides most
# of the actual functionality. The current working dir is expected to be the 
# .build.main project, which is also the reference point for relative paths.


#------------------------
# Common Initialization 
#------------------------

# abort on non-zero return values
set -e

# load static configuration
CONFIG_PATH="config"
. "$CONFIG_PATH/defaults.conf"
. "$CONFIG_PATH/branch.conf"

# load optional override properties
if [ -f "$CONFIG_PATH/overrides.conf" ]; then
  . "$CONFIG_PATH/overrides.conf"
fi

# set derived properties

case "$RCE_USE_LOCAL_THIRD_PARTY_REPOSITORY" in
    "no")
        RCE_THIRD_PARTY_REPOSITORY_URL="$RCE_THIRD_PARTY_REPOSITORY_REMOTE_MODE_ROOT_URL/$RCE_THIRD_PARTY_REPOSITORY_REFERENCE"
        ;;
    "yes")
        # this URI pattern for relative paths is not generally valid but accepted by Tycho
        RCE_THIRD_PARTY_REPOSITORY_URL="file:$RCE_THIRD_PARTY_REPOSITORY_LOCAL_MODE_RELATIVE_PATH"
        ;;
    *)
        echo "Invalid RCE_USE_LOCAL_THIRD_PARTY_REPOSITORY setting: $RCE_USE_LOCAL_THIRD_PARTY_REPOSITORY"
        exit 1
esac

# TODO determine git branch/status/commit and pass it to the build process?

# set up the common build log directory
mkdir -p target/logs

# after this point, fail on undefined variables, too
set -u


#-----------------------
# Function Definitions
#-----------------------

build_intermediate_repo() {
    # Generated output:
    # - target/intermediate-repo: the "intermediate" p2 repository to build products from

    rm -rf target/intermediate-repo

    echo "Building RCE Bundles and Features (Intermediate Repository)"
    echo "  Build type:              $RCE_BUILD_TYPE"
    echo "  Third-party repository:  $RCE_THIRD_PARTY_REPOSITORY_URL"
    # echo "  Collect JQA data:        $RCE_COLLECT_JQASSISTANT_DATA"

    echo "Capturing log output in $(pwd)/target/logs/intermediate-repo-build.txt"

    # TODO make help content generation optional?
    # TODO inject build timestamp from here for consistency between stages and reproducible builds
    mvn -B -f ../de.rcenvironment/maven/secondStage/pom.xml \
    -Drce.maven.buildScope=intermediateRepo \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    -Drce.maven.repositories.foundation.url="$RCE_THIRD_PARTY_REPOSITORY_URL" \
    clean package \
    >target/logs/intermediate-repo-build.txt 2>&1
    
    mkdir -p target/intermediate-repo
    mv ../de.rcenvironment/target/de.rcenvironment.modules.repository.intermediate/repository \
       target/intermediate-repo/

    # TODO 6 lines are fitting for the BUILD SUCCESS case; check behavior on errors
    tail -n 6 target/logs/intermediate-repo-build.txt
}
