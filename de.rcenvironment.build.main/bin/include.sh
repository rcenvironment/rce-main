# Copyright 2023-2025 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke, Jan Flink

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

# validate that RCE_BUILD_TYPE contains a known value
case "$RCE_BUILD_TYPE" in
  snapshot|rc|release)
    ;;
  *)
    echo "Invalid value \"$RCE_BUILD_TYPE\" for RCE_BUILD_TYPE"
    exit 1
    ;;
esac

# set derived properties
MAVEN_SETTINGS="--batch-mode --show-version --errors --fail-at-end"
if [ ! -z "$MAVEN_REPOSITORY_LOCATION" ]; then
    MAVEN_SETTINGS="$MAVEN_SETTINGS -Dmaven.repo.local=$MAVEN_REPOSITORY_LOCATION"
fi

# note that this URI pattern for relative paths is not generally valid, but accepted by Tycho
RCE_THIRD_PARTY_REPOSITORY_URL="file:$RCE_THIRD_PARTY_REPOSITORY_LOCAL_SOURCE_RELATIVE_PATH"

# TODO determine git branch/status/commit and pass it to the build process?

# after this point, fail on undefined variables, too
set -u


#-----------------------
# Function Definitions
#-----------------------

create_zip_file() {
    FILENAME="$1"
    shift
    # we are using the JDK "jar" command here as it is known to be available, unlike the "zip"
    # command, which is, for example, not present by default in a git bash;
    # "-M" is short for "--no-manifest"
    jar -cMf "${FILENAME}" $@
}

fail_with_log_tail_if_non_zero() {
    EXIT_CODE="$1"
    LOG_FILE="$2"
    # log tail lines to print; arbitrary, can be adjusted or made a parameter
    TAIL_COUNT=80

    if [ $EXIT_CODE != 0 ]; then
        echo "Operation failed with exit code $EXIT_CODE; last lines of log file:"
        echo
        tail -n "$TAIL_COUNT" "$LOG_FILE"
        exit $EXIT_CODE
    fi
}

build_intermediate_repo() {
    # Generated output:
    # - target/intermediate/repository: the "intermediate" p2 repository
    #                                   to build products from
    # - target/intermediate/build.log:  the output of the Maven build process

    # delete previous output
    rm -rf target/intermediate
    mkdir -p target/intermediate

    # TODO validate presence of third-party repository build

    echo "Building RCE Bundles and Features (Intermediate Repository)"
    echo "  Build type:                    $RCE_BUILD_TYPE"
    echo "  Third-party repository:        $RCE_THIRD_PARTY_REPOSITORY_URL"
    # echo "  Collect JQA data:              $RCE_COLLECT_JQASSISTANT_DATA"

    BUILD_LOG_FILE="target/intermediate/build.log"
    echo "  Build Log File:                $(pwd)/$BUILD_LOG_FILE"

    set +e
    # TODO make help content generation optional?
    # TODO inject build timestamp from here for consistency between stages and reproducible builds
    mvn $MAVEN_SETTINGS \
    -f ../de.rcenvironment/maven/secondStage/pom.xml \
    -Drce.maven.buildScope=intermediateRepo \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    -Drce.maven.repositories.foundation.url="$RCE_THIRD_PARTY_REPOSITORY_URL" \
    clean package \
    >"$BUILD_LOG_FILE" 2>&1
    EXIT_CODE=$?
    set -e

    fail_with_log_tail_if_non_zero $EXIT_CODE "$BUILD_LOG_FILE"

    mv ../de.rcenvironment/target/de.rcenvironment.modules.repository.intermediate/repository \
       target/intermediate/

    # 6 lines for clean output in the BUILD SUCCESS case
    tail -n 6 "$BUILD_LOG_FILE"
    echo "Generated artifacts:"
    echo "  Intermediate p2 repository:    $(pwd)/target/intermediate/repository"
}


build_products_from_repo() {
    # Generated output:
    # - target/products/zip:        the zipped product builds and the update site
    # - target/products/updatesite: the unpacked product p2 repository ("update site")
    # - target/products/build.log:  the output of the Maven build process

    # delete previous output
    rm -rf target/products
    mkdir -p target/products

    # TODO validate presence of intermediate repository build

    echo "Assembling RCE (Executable Product)"
    echo "  Build type:                    $RCE_BUILD_TYPE"
    # echo "  Documentation build mode:  $RCE_DOCUMENTATION_BUILD_MODE"

    BUILD_LOG_FILE="target/products/build.log"
    echo "  Build Log File:                $(pwd)/$BUILD_LOG_FILE"

    # TODO apply RCE_DOCUMENTATION_BUILD_MODE; for now, it is always built

    set +e
    # This build stage currently includes documentation, branding, filesets,
    # and the "versioninfo" bundle
    mvn $MAVEN_SETTINGS \
    -f ../de.rcenvironment/maven/secondStage/pom.xml \
    -Drce.maven.buildScope=product.usingIntermediateRepo \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    -Drce.maven.repositories.foundation.url="file:target/intermediate/repository" \
    -Drce.maven.assembleProducts \
    -Drce.maven.createProductArchives \
    clean package \
    >"$BUILD_LOG_FILE" 2>&1
    EXIT_CODE=$?
    set -e

    fail_with_log_tail_if_non_zero $EXIT_CODE "$BUILD_LOG_FILE"

    OUTPUT_DIR="$(pwd)/target/products"

    # enter the Tycho product output directory
    cd ../de.rcenvironment/target/de.rcenvironment.modules.repository.mainProduct/

    # copy one of the VERSION files to the top level of the output directory
    cp repository/VERSION "${OUTPUT_DIR}/VERSION"
    # using the VERSION file's content, define the zip file names
    ZIP_FILES_BASENAME="rce-$(<repository/VERSION)"

    # move and rename product zips
    mv products/de.rcenvironment.products.rce.default-linux.gtk.x86_64.zip \
      "${OUTPUT_DIR}/${ZIP_FILES_BASENAME}-standard-linux.x86_64.zip"
    mv products/de.rcenvironment.products.rce.default-win32.win32.x86_64.zip \
      "${OUTPUT_DIR}/${ZIP_FILES_BASENAME}-standard-win32.x86_64.zip"
    # move and rename the unpacked update site repository
    mv repository "${OUTPUT_DIR}/updatesite"
    cd - >/dev/null

    # create a zip file of the repository in the output directory
    cd "${OUTPUT_DIR}/updatesite"
    # we are using the JDK "jar" command here as it is known to be available, unlike the "zip"
    # command, which is, for example, not present by default in a git bash; -M = "--no-manifest"
    create_zip_file "${OUTPUT_DIR}/${ZIP_FILES_BASENAME}-updatesite.zip" *
    cd - >/dev/null

    # 6 lines for clean output in the BUILD SUCCESS case
    tail -n 6 "$BUILD_LOG_FILE"
    echo "Generated artifacts:"
    echo "  Product and update site zip files:   $(pwd)/target/products/*.zip"
    echo "  Product update p2 repository:        $(pwd)/target/products/updatesite"
}

run_unit_tests() {
    # Generated output:
    # - target/unit-tests/reports/xml:  JUnit XML report files
    # - target/unit-tests/build.log:    the output of the Maven build process
    #
    # Note: The report files are also collected if the Maven run returns a non-zero exit code.

    # delete previous output
    rm -rf "target/unit-tests"
    mkdir -p "target/unit-tests"

    echo "Running Unit Tests (Build Scope '$RCE_UNIT_TESTS_BUILD_SCOPE')"

    BUILD_LOG_FILE="target/unit-tests/build.log"
    echo "  Build Log File:                $(pwd)/$BUILD_LOG_FILE"

    # TODO consider compiling and running tests against the intermediate repository
    # TODO consider making the test scope more configurable, e.g. per bundle

    set +e
    mvn $MAVEN_SETTINGS \
    -f ../de.rcenvironment/maven/secondStage/pom.xml \
    -Drce.maven.buildScope="$RCE_UNIT_TESTS_BUILD_SCOPE" \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    -Drce.maven.repositories.foundation.url="$RCE_THIRD_PARTY_REPOSITORY_URL" \
    -Drce.maven.skipDocumentation \
    -P !generateHelpFromDocbook \
    clean verify \
    >"$BUILD_LOG_FILE" 2>&1
    EXIT_CODE=$?
    set -e

    # collect individual XML report files
    mkdir -p target/unit-tests/reports/xml
    find .. -name '*.xml' -path '*/target/surefire-reports/*' \
      -exec cp {} target/unit-tests/reports/xml/ \;

    # a first quick-and-dirty check for test errors, but better than nothing
    # TODO generate a proper overview/report from these files
    echo "Checking for test errors (preliminary approach)"
    grep "<error" target/unit-tests/reports/xml/* || true

    fail_with_log_tail_if_non_zero $EXIT_CODE "$BUILD_LOG_FILE"

    # 6 lines for clean output in the BUILD SUCCESS case
    tail -n 6 "$BUILD_LOG_FILE"
    echo "Generated artifacts:"
    echo "  JUnit test result XML files:   $(pwd)/target/unit-tests/reports/xml"
}

build_documentation_only() {
    # Generated output:
    # - target/documentation/pdf:       the generated PDF documentation
    # - target/documentation/build.log: the output of the Maven build process

    # delete previous output
    rm -rf target/documentation
    mkdir -p target/documentation/pdf/{linux,windows}

    echo "Rendering RCE Documentation"

    BUILD_LOG_FILE="target/documentation/build.log"
    echo "  Build Log File:                $(pwd)/$BUILD_LOG_FILE"

    set +e
    mvn $MAVEN_SETTINGS \
    -f ../de.rcenvironment.documentation.core/pom.xml \
    -Drce.maven.buildType="$RCE_BUILD_TYPE" \
    clean generate-resources prepare-package \
    >"$BUILD_LOG_FILE" 2>&1
    EXIT_CODE=$?
    set -e

    fail_with_log_tail_if_non_zero $EXIT_CODE "$BUILD_LOG_FILE"

    mv -t target/documentation/pdf/linux/ \
      ../de.rcenvironment.documentation.core/target/docbkx/pdf/linux/*.pdf
    mv -t target/documentation/pdf/windows/ \
      ../de.rcenvironment.documentation.core/target/docbkx/pdf/windows/*.pdf

    # 6 lines for clean output in the BUILD SUCCESS case
    tail -n 6 "$BUILD_LOG_FILE"
    echo "  Documentation PDFs:             $(pwd)/target/documentation/pdf"

}
