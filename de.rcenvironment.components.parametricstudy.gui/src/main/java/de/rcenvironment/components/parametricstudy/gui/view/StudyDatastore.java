/*
 * Copyright 2006-2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.components.parametricstudy.gui.view;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.rcenvironment.components.parametricstudy.common.Dimension;
import de.rcenvironment.components.parametricstudy.common.Measure;
import de.rcenvironment.components.parametricstudy.common.ParametricStudyService;
import de.rcenvironment.components.parametricstudy.common.Study;
import de.rcenvironment.components.parametricstudy.common.StudyDataset;
import de.rcenvironment.components.parametricstudy.common.StudyPublisher;
import de.rcenvironment.components.parametricstudy.common.StudyReceiver;
import de.rcenvironment.components.parametricstudy.common.StudyStructure;
import de.rcenvironment.core.communication.common.ResolvableNodeId;

/**
 * Study holding values generated by the {@link ParametricStudyComponent}.
 * @author Christian Weiss
 */
public final class StudyDatastore extends Study {

    private static final long serialVersionUID = 990775937058384209L;

    // needed to hold reference otherwise it is can not be called back
    private static DatasetNotificationSubscriber notificationSubscriber;

    private final List<StudyDataset> datasets = Collections.synchronizedList(new LinkedList<StudyDataset>());

    private final List<StudyDatasetAddListener> listeners = new LinkedList<StudyDatasetAddListener>();

    private final Map<String, Double> minValues = new HashMap<String, Double>();

    private final Map<String, Double> maxValues = new HashMap<String, Double>();
    
    public StudyDatastore(final String identifier, final String title, final StudyStructure structure) {
        super(identifier, title, structure);
    }

    /**
     * @param dataset new values.
     */
    public void addDataset(final StudyDataset dataset) {
        datasets.add(dataset);
        fireDatasetAdd(dataset);
        for (final Dimension dimension : getStructure().getDimensions()) {
            adjustMinMaxRange(dataset, dimension.getName());
        }
        for (final Measure measure : getStructure().getMeasures()) {
            adjustMinMaxRange(dataset, measure.getName());
        }
    }

    private void adjustMinMaxRange(final StudyDataset dataset,
            final String key) {
        final Serializable value = dataset.getValue(key);
        if (value instanceof Number) {
            final Double doubleValue = ((Number) value).doubleValue();
            if (minValues.get(key) == null
                    || minValues.get(key) > doubleValue) {
                minValues.put(key, doubleValue);
            }
            if (maxValues.get(key) == null
                    || maxValues.get(key) < doubleValue) {
                maxValues.put(key, doubleValue);
            }
        }
    }

    public Collection<StudyDataset> getDatasets() {
        return Collections.unmodifiableCollection(datasets);
    }

    public int getDatasetCount() {
        return datasets.size();
    }

    /**
     * @param key of the relevant value chain.
     * @return the minimum value of the chain.
     */
    public Double getMinValue(final String key) {
        return minValues.get(key);
    }

    /**
     * @param key of the relevant value chain.
     * @return the maximum value of the chain.
     */
    public Double getMaxValue(final String key) {
        return maxValues.get(key);
    }

    /**
     * Connects a {@link StudyReceiver} to the {@link StudyPublisher}.
     * @param identifier the unique identifier
     * @param platform the platform to receive updates from
     * @param parametricStudyService instance of {@link ParametricStudyService}
     * @return created {@link StudyDatastore}.
     */
    public static StudyDatastore connect(final String identifier, final ResolvableNodeId platform,
        ParametricStudyService parametricStudyService) {
        final StudyReceiver receiver = parametricStudyService.createReceiver(identifier, platform);
        if (receiver == null) {
            return null;
        }
        final StudyDatastore datastore = new StudyDatastore(identifier,
                receiver.getStudy().getTitle(), receiver.getStudy().getStructure());
        notificationSubscriber = new DatasetNotificationSubscriber(datastore);
        receiver.setNotificationSubscriber(notificationSubscriber);
        receiver.initialize();
        return datastore;
    }

    /**
     * @param listener for {@link StudyDataset}to add.
     */
    public void addDatasetAddListener(final StudyDatasetAddListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener for {@link StudyDataset} to remove.
     */
    public void removeDatasetAddListener(final StudyDatasetAddListener listener) {
        listeners.remove(listener);
    }

    private void fireDatasetAdd(final StudyDataset dataset) {
        final StudyDatasetAddListener[] listenersArray = listeners.toArray(new StudyDatasetAddListener[0]);
        for (final StudyDatasetAddListener listener : listenersArray) {
            try {
                listener.handleStudyDatasetAdd(dataset);
            } catch (RuntimeException e) {
                e = null; // ignore
            }
        }
    }

    /**
     * Needs to be implemented by classes which are interested in {@link StudyDataset}.
     * @author Doreen Seider
     */
    public interface StudyDatasetAddListener {

        /**
         * @param dataset the new {@link StudyDataset}.
         */
        void handleStudyDatasetAdd(final StudyDataset dataset);

    }

}
