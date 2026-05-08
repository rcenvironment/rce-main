#!/usr/bin/env python3

# Copyright 2026 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

# This script generates Eclipse .launch files for running the JUnit tests in 
# each test project (*.tests), and a "launch group" for running all of them.
# It only generates non-OSGi .launch files at this time; this could be easily
# extended to OSGi tests.


import os.path
import sys

import jinja2

PATH_FROM_SCRIPT_TO_PROJECTS_ROOT = '../../..'
PATH_FROM_SCRIPT_TO_OUTPUT_DIR = '../../eclipse/testing/generated'

root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), PATH_FROM_SCRIPT_TO_PROJECTS_ROOT))
output_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), PATH_FROM_SCRIPT_TO_OUTPUT_DIR))


def clean_existing_launch_files():
    filenames = next(os.walk(output_dir))[2]
    cleaned = 0
    for filename in filenames:
        if filename.startswith('RCE - ') and filename.endswith('.launch'):
            os.unlink(os.path.join(output_dir, filename))
            cleaned += 1
    print('Cleaned %d existing launch files from %s' % (cleaned, output_dir))


def determine_list_of_test_bundles():
    raw_subdirs = next(os.walk(root_dir))[1]
    # TODO these are currently simply excluded; consider generating a separate launch group for them
    slow_test_bundles = [
        'de.rcenvironment.core.communication.tests',
        'de.rcenvironment.core.communication.uplink.tests',
        'de.rcenvironment.core.communication.transport.jms.tests',
    ]
    excluded_test_bundles = [
        'de.rcenvironment.components.excel.common.tests',  # cannot locate the DLL, which won't be fixed (deprecated)
        'de.rcenvironment.components.inputprovider.gui.tests',  # no test cases; to investigate
        'de.rcenvironment.core.component.executor.tests',  # no test cases; to investigate
        'de.rcenvironment.core.launcher.tests',  # restructuring WIP; recheck after finished
        'de.rcenvironment.core.remoteaccess.tests',  # manual tests (set up using Junit) only
    ]
    filtered_subdirs = [dir for dir in raw_subdirs
                        if dir.endswith('.tests')
                        and dir not in excluded_test_bundles
                        and dir not in slow_test_bundles
                        and os.path.isfile(os.path.join(root_dir, dir, '.project'))]
    return filtered_subdirs


def generate_single_launch_file(output_filepath, bundle_name):
    content = jinja2.Template('''\
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.jdt.junit.launchconfig">
    <booleanAttribute key="org.eclipse.debug.core.ATTR_FORCE_SYSTEM_CONSOLE_ENCODING" value="false"/>
    <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
        <listEntry value="/{{ TEST_BUNDLE_NAME }}"/>
    </listAttribute>
    <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
        <listEntry value="4"/>
    </listAttribute>
    <stringAttribute key="org.eclipse.jdt.junit.CONTAINER" value="={{ TEST_BUNDLE_NAME }}"/>
    <booleanAttribute key="org.eclipse.jdt.junit.KEEPRUNNING_ATTR" value="false"/>
    <stringAttribute key="org.eclipse.jdt.junit.TESTNAME" value=""/>
    <stringAttribute key="org.eclipse.jdt.junit.TEST_KIND" value="org.eclipse.jdt.junit.loader.junit4"/>
    <booleanAttribute key="org.eclipse.jdt.launching.ATTR_ATTR_USE_ARGFILE" value="false"/>
    <booleanAttribute key="org.eclipse.jdt.launching.ATTR_USE_CLASSPATH_ONLY_JAR" value="false"/>
    <stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value=""/>
    <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="{{ TEST_BUNDLE_NAME }}"/>
    <stringAttribute key="org.eclipse.jdt.launching.VM_ARGUMENTS" value="-ea"/>
</launchConfiguration>
''').render(TEST_BUNDLE_NAME=bundle_name)
    with open(output_filepath, 'w') as f:
        f.write(content)


def generate_launch_group_file(output_filepath, bundle_names):
    content = jinja2.Template('''\
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.debug.core.groups.GroupLaunchConfigurationType">
{% for SINGLE_LAUNCH_NAME in SINGLE_LAUNCH_NAMES %}
    <stringAttribute key="org.eclipse.debug.core.launchGroup.{{ loop.index0 }}.action" value="NONE"/>
    <booleanAttribute key="org.eclipse.debug.core.launchGroup.{{ loop.index0 }}.adoptIfRunning" value="false"/>
    <booleanAttribute key="org.eclipse.debug.core.launchGroup.{{ loop.index0 }}.enabled" value="true"/>
    <stringAttribute key="org.eclipse.debug.core.launchGroup.{{ loop.index0 }}.mode" value="inherit"/>
    <stringAttribute key="org.eclipse.debug.core.launchGroup.{{ loop.index0 }}.name" value="{{ SINGLE_LAUNCH_NAME }}"/>
{% endfor %}    
</launchConfiguration>
    ''').render(SINGLE_LAUNCH_NAMES=[define_single_launch_id(name) for name in bundle_names])

    with open(output_filepath, 'w') as f:
        f.write(content)


def execute_main():
    if len(sys.argv) != 1:
        print("This script does not expect any parameters")
        sys.exit(2)

    os.makedirs(output_dir, exist_ok=True)

    clean_existing_launch_files()

    print('Generating launch files in %s' % output_dir)
    test_bundle_names = determine_list_of_test_bundles()
    for bundle_name in test_bundle_names:
        single_bundle_launch_filename = define_single_launch_id(bundle_name) + '.launch'
        generate_single_launch_file(os.path.join(output_dir, single_bundle_launch_filename), bundle_name)

    launch_group_filename = 'RCE - run all non-OSGi JUnit tests.launch'
    generate_launch_group_file(os.path.join(output_dir, launch_group_filename), test_bundle_names)


def define_single_launch_id(bundle_name):
    return 'RCE - run non-OSGi JUnit tests in %s' % bundle_name


if __name__ == '__main__':
    execute_main()
