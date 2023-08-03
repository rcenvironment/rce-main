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
    # - target/intermediate/repository: the "intermediate" p2 repository 
    #                                   to build products from
    # - target/intermediate/build.log:  the output of the Maven build process
    
    # delete previous output
    rm -rf target/intermediate
    mkdir -p target/intermediate

    echo "Building RCE Bundles and Features (Intermediate Repository)"
    echo "  Build type:              $RCE_BUILD_TYPE"
    echo "  Third-party repository:  $RCE_THIRD_PARTY_REPOSITORY_URL"
    # echo "  Collect JQA data:        $RCE_COLLECT_JQASSISTANT_DATA"

    BUILD_LOG_FILE="target/intermediate/build.log"
    echo "Capturing log output in $(pwd)/$BUILD_LOG_FILE"

    set +e
    # TODO make help content generation optional?
    # TODO inject build timestamp from here for consistency between stages and reproducible builds
    mvn -B -f ../de.rcenvironment/maven/secondStage/pom.xml \
    -Drce.maven.buildScope=intermediateRepo \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    -Drce.maven.repositories.foundation.url="$RCE_THIRD_PARTY_REPOSITORY_URL" \
    clean package \
    >"$BUILD_LOG_FILE" 2>&1
    EXIT_CODE=$?
    set -e

    if [ $EXIT_CODE != 0 ]; then
        echo "Build failed with exit code $EXIT_CODE; last lines of log file:"
        # line count is arbitrary; adjust as needed
        tail -n 80 "$BUILD_LOG_FILE"
        exit $EXIT_CODE
    fi
    
    mv ../de.rcenvironment/target/de.rcenvironment.modules.repository.intermediate/repository \
       target/intermediate/

    # 6 lines for clean output in the BUILD SUCCESS case
    tail -n 6 "$BUILD_LOG_FILE"
}


# TODO not working yet as the intermediate repository must be updated/fixed (#16808)
build_products_from_repo() {
    # Generated output:
    # - target/products/zip:        the zipped executable product builds
    # - target/products/repository: the product p2 repository ("update site")
    # - target/products/build.log:  the output of the Maven build process

    # delete previous output
    rm -rf target/products
    mkdir -p target/products

    echo "Assembling RCE (Executable Product)"
    echo "  Build type:                $RCE_BUILD_TYPE"
    # echo "  Documentation build mode:  $RCE_DOCUMENTATION_BUILD_MODE"

    BUILD_LOG_FILE="target/products/build.log"
    echo "Capturing log output in $(pwd)/$BUILD_LOG_FILE"

    # TODO apply RCE_DOCUMENTATION_BUILD_MODE; for now, it is always built

    set +e
    # This build stage currently includes documentation, branding, filesets,
    # and the "versioninfo" bundle
    mvn -B -f ../de.rcenvironment/maven/secondStage/pom.xml \
    -Drce.maven.buildScope=product.usingIntermediateRepo \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    -Drce.maven.repositories.foundation.url="file:target/intermediate/repository" \
    -Drce.maven.assembleProducts \
    -Drce.maven.createProductArchives \
    clean package \
    >"$BUILD_LOG_FILE" 2>&1
    EXIT_CODE=$?
    set -e
    
    if [ $EXIT_CODE != 0 ]; then
        echo "Build failed with exit code $EXIT_CODE; last lines of log file:"
        # line count is arbitrary; adjust as needed
        tail -n 80 "$BUILD_LOG_FILE"
        exit $EXIT_CODE
    fi
    
    mkdir target/products/zip
    mv ../de.rcenvironment/target/de.rcenvironment.modules.repository.mainProduct/products/*.zip \
       target/products/zip
    mv ../de.rcenvironment/target/de.rcenvironment.modules.repository.mainProduct/repository \
       target/products/

    # 6 lines for clean output in the BUILD SUCCESS case
    tail -n 6 "$BUILD_LOG_FILE"
}
