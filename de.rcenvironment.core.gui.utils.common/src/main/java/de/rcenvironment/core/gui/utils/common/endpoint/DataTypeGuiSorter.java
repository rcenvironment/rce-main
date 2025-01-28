/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.gui.utils.common.endpoint;

import java.util.Comparator;

import de.rcenvironment.core.datamodel.api.DataType;

/**
 * Utility class for common sorting of data types in the GUI.
 *
 * @author Jan Flink
 */
public final class DataTypeGuiSorter {

    private DataTypeGuiSorter() {}

    public static Comparator<DataType> getComparator() {
        return Comparator.comparing(DataType::getDisplayName);
    }

    public static DataType getDefaultSelection() {
        return DataType.Float;
    }
}
