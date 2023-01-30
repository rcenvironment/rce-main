#!/usr/bin/env python3

# Copyright 2022 DLR, Germany
#
# SPDX-License-Identifier: EPL-1.0
#
# https://rcenvironment.de/
#
# Author: Robert Mischke

import os
import re
import sys

import jinja2
import yaml

# set to False if updating existing projects (overwriting files) is intended; True to require manual deletion (safer)
# TODO add a CLI flag for this (e.g. --overwrite)?
FAIL_ON_EXISTING_PROJECT_DIRECTORIES = False


class ModuleConfiguration:

    def __init__(self, yaml_section, yaml_root):
        self.module_id = yaml_section['id']
        self.module_name = yaml_section['name']

        # these sets are optional; absence is converted to an empty list for ease of rendering/handling
        self.build_process_project_includes = self.__expand_patterns_in_elements(
            yaml_section.get('build-process-project-includes', []))
        self.feature_to_feature_includes = self.__expand_patterns_in_elements(
            yaml_section.get('feature-to-feature-includes', []))
        self.feature_to_bundle_includes = self.__expand_patterns_in_elements(
            yaml_section.get('feature-to-bundle-includes', []))

        # global settings (at least for now)
        self.module_version = yaml_root['modules-version']
        self.parent_pom_id = yaml_root['parent-pom-id']
        self.parent_pom_version = yaml_root['parent-pom-version']
        self.parent_pom_relative_path = yaml_root['parent-pom-relative-path']

    def __expand_patterns_in_elements(self, input_list):
        result = []

        # x.y[.z] - note that only one occurrence per entry is supported for now
        optional_pattern = re.compile('^([^[]*)\\[([^]]+)]([^[]*)$')

        # x.y.(a|b|c) - note that only one occurrence per entry is supported for now
        multi_pattern = re.compile('^([^(]*)\\(([^)]+)\\)([^(]*)$')

        for element in input_list:
            m = optional_pattern.search(element)
            if m:
                result.append(m.group(1) + m.group(3))
                result.append(m.group(1) + m.group(2) + m.group(3))
                continue

            m = multi_pattern.search(element)
            if m:
                for option in m.group(2).split('|'):
                    result.append(m.group(1) + option + m.group(3))
                continue

            result.append(element)

        return result


# NOTE: in the triple-quoted templates below, "\t" is a workaround for PyCharm incorrectly
# checking the indentation whitespace as Python code, even though it's inside a string literal;
# see https://github.com/PyCQA/pycodestyle/issues/45

def set_up_project_directory(_path):
    if os.path.isdir(_path):
        if FAIL_ON_EXISTING_PROJECT_DIRECTORIES:
            print("ERROR: Project directory %r already exists" % _path)
            sys.exit(1)
        else:
            print("Note: Project directory %r already exists, files may be overwritten" % _path)
    else:
        os.mkdir(_path)
        print("Created project directory %r" % _path)


def generate_feature_xml(module: ModuleConfiguration):
    # note: this file uses a weird indentation as generated by Eclipse; we are keeping that so far
    return jinja2.Template('''\
<?xml version="1.0" encoding="UTF-8"?>
<feature
      id="{{ MODULE_ID }}.feature"
      label="{{ MODULE_NAME }} (Feature)"
      version="{{ VERSION }}.qualifier"
      provider-name="DLR">

   <copyright url="https://www.rcenvironment.de">
      Copyright 2022 DLR, Germany
   </copyright>

   <license url="https://www.eclipse.org/legal/epl-v10.html">
      Eclipse Public License - v 1.0
   </license>
{% for feature_include in FEATURE_TO_FEATURE_INCLUDES %}
   <includes
         id="{{ feature_include }}"
         version="0.0.0"/>
{% endfor %}
{%- for bundle_include in FEATURE_TO_BUNDLE_INCLUDES %}
   <plugin
         id="{{ bundle_include }}"
         download-size="0"
         install-size="0"
         version="0.0.0"
         unpack="false"/>
{% endfor %}
</feature>

''').render(
        MODULE_ID=module.module_id,
        MODULE_NAME=module.module_name,
        VERSION=module.module_version,
        FEATURE_TO_FEATURE_INCLUDES=module.feature_to_feature_includes,
        FEATURE_TO_BUNDLE_INCLUDES=module.feature_to_bundle_includes
    )


def generate_feature_project_project_file(module: ModuleConfiguration):
    # using tabs as generated by Eclipse
    return jinja2.Template('''\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
\t<name>{{ MODULE_ID }}.feature</name>
\t<comment></comment>
\t<projects>
\t</projects>
\t<buildSpec>
\t\t<buildCommand>
\t\t\t<name>org.eclipse.pde.FeatureBuilder</name>
\t\t\t<arguments>
\t\t\t</arguments>
\t\t</buildCommand>
\t</buildSpec>
\t<natures>
\t\t<nature>org.eclipse.pde.FeatureNature</nature>
\t</natures>
</projectDescription>

''').render(
        MODULE_ID=module.module_id
    )


def generate_feature_project_pom(module: ModuleConfiguration):
    return jinja2.Template('''\
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
\txsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
\t<modelVersion>4.0.0</modelVersion>

\t<artifactId>{{ MODULE_ID }}.feature</artifactId>
\t<name>{{ MODULE_NAME }} (Feature)</name>
\t<version>{{ VERSION }}-SNAPSHOT</version>
\t<packaging>eclipse-feature</packaging>

\t<parent>
\t\t<groupId>de.rcenvironment</groupId>
\t\t<artifactId>{{ PARENT_POM_ID }}</artifactId>
\t\t<version>{{ PARENT_POM_VERSION }}</version>
\t\t<relativePath>{{ PARENT_POM_RELATIVE_PATH }}</relativePath>
\t</parent>

</project>

''').render(
        MODULE_ID=module.module_id,
        MODULE_NAME=module.module_name,
        VERSION=module.module_version,
        PARENT_POM_ID=module.parent_pom_id,
        PARENT_POM_VERSION=module.parent_pom_version,
        PARENT_POM_RELATIVE_PATH=module.parent_pom_relative_path
    )


def generate_feature_project_build_properties():
    return "bin.includes = feature.xml\n"


def generate_include_project_pom(module: ModuleConfiguration):
    return jinja2.Template('''\
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
\txsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
\t<modelVersion>4.0.0</modelVersion>

\t<artifactId>{{ MODULE_ID }}.include</artifactId>
\t<name>{{ MODULE_NAME }} (Maven Include Project)</name>
\t<version>{{ VERSION }}-SNAPSHOT</version>
\t<packaging>pom</packaging>

\t<parent>
\t\t<groupId>de.rcenvironment</groupId>
\t\t<artifactId>{{ PARENT_POM_ID }}</artifactId>
\t\t<version>{{ PARENT_POM_VERSION }}</version>
\t\t<relativePath>../{{ PARENT_POM_RELATIVE_PATH }}</relativePath>
\t</parent>

\t<modules>
\t\t<module>..</module> <!-- the module's own feature -->
{%- for project_include in BUILD_PROCESS_PROJECT_INCLUDES %}
\t\t<module>../../{{ project_include }}</module>
{%- endfor %}
\t</modules>

</project>

''').render(
        MODULE_ID=module.module_id,
        MODULE_NAME=module.module_name,
        VERSION=module.module_version,
        PARENT_POM_ID=module.parent_pom_id,
        PARENT_POM_VERSION=module.parent_pom_version,
        PARENT_POM_RELATIVE_PATH=module.parent_pom_relative_path,
        BUILD_PROCESS_PROJECT_INCLUDES=module.build_process_project_includes
    )


def generate_or_update_module_setup(module_setup, parent_dir):
    # create project directories if necessary
    feature_project_path = os.path.join(parent_dir, module_setup.module_id + '.feature')
    include_project_path = os.path.join(parent_dir, module_setup.module_id + '.feature/maven-include')
    set_up_project_directory(feature_project_path)
    set_up_project_directory(include_project_path)

    with open(os.path.join(feature_project_path, 'feature.xml'), 'w') as f:
        f.write(generate_feature_xml(module_setup))
    with open(os.path.join(feature_project_path, 'pom.xml'), 'w') as f:
        f.write(generate_feature_project_pom(module_setup))
    with open(os.path.join(feature_project_path, '.project'), 'w') as f:
        f.write(generate_feature_project_project_file(module_setup))
    with open(os.path.join(feature_project_path, 'build.properties'), 'w') as f:
        f.write(generate_feature_project_build_properties())
    with open(os.path.join(include_project_path, 'pom.xml'), 'w') as f:
        f.write(generate_include_project_pom(module_setup))


def determine_list_of_projects(parent_dir, _verbose=False):
    raw_subdirs = next(os.walk(parent_dir))[1]
    filtered_subdirs = set()

    for subdir in raw_subdirs:

        if subdir.startswith('.') \
                or subdir.startswith('org.eclipse.') \
                or subdir.startswith('de.rcenvironment.modules.') \
                or subdir.startswith('de.rcenvironment.build.') \
                or subdir == 'LICENSES':
            # ignore/skip these silently
            continue

        filtered_subdirs.add(subdir)

    return filtered_subdirs


def execute_main():
    if len(sys.argv) != 2:
        print("Usage: %s <target directory containing a modules-definition.yaml>" % os.path.basename(__file__))
        sys.exit(2)

    (_, parent_dir) = sys.argv

    parent_dir = os.path.normpath(parent_dir)
    if not os.path.isdir(parent_dir):
        print("Error: Parent directory %r does not exist" % parent_dir)
        sys.exit(1)

    yaml_root = yaml.safe_load(open(os.path.join(parent_dir, 'modules-definition.yaml')))

    unmatched_projects = determine_list_of_projects(parent_dir)

    for yaml_module_section in yaml_root['modules']:
        # extract the project parameters required for project configuration
        module_setup = ModuleConfiguration(yaml_module_section, yaml_root)

        generate_or_update_module_setup(module_setup, parent_dir)

        # match "build-process-project-includes" against the actual subdirectories
        for entry in module_setup.build_process_project_includes:
            if entry not in unmatched_projects:
                print(
                    'Warning: "build-process-project-includes" entry %r does not match an actual subdirectory' % entry)
        unmatched_projects.difference_update(module_setup.build_process_project_includes)

    if (len(unmatched_projects) != 0):
        print('\nSubdirectories in the target directory that were not matched '
              + 'by any "build-process-project-includes" clause (for verification):')
        for project in sorted(unmatched_projects):
            print('- %s' % project)


if __name__ == '__main__':
    execute_main()
