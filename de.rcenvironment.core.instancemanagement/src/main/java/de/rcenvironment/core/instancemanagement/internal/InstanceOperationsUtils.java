/*
 * Copyright 2006-2025 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */

package de.rcenvironment.core.instancemanagement.internal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.rcenvironment.core.configuration.bootstrap.profile.CommonProfile;

/**
 * 
 * Utility class for {@link InstanceOperationsImpl}.
 *
 * @author David Scholz
 * @author Robert Mischke
 */
public final class InstanceOperationsUtils {

    /**
     * Timeout reached error message for acquiring lock file.
     */
    public static final String TIMEOUT_REACHED_MESSAGE =
        "Timeout reached while trying to acquire the lock, aborting startup of instance with id: %s.";

    /**
     * Error message if acquiring lock file fails.
     */
    public static final String UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON =
        "Unexpected error when trying to acquire a file lock on ";

    /**
     * 
     */
    public static final String IM_LOCK_FILE_ACCESS_PERMISSIONS = "rw";

    /**
     * Name of the file, which contains the installation id of a running profile.
     */
    public static final String INSTALLATION_ID_FILE_NAME = "installation";

    private static final String IM_LOCK_FILE_NAME = "instancemanagement.lock";

    /**
     * Name of the shutdown.dat file.
     */
    private static final String SHUTDOWN_FILE_NAME = "shutdown.dat";

    private static final String SLASH = "/";

    private static final Log sharedLog = LogFactory.getLog(InstanceOperationsUtils.class);

    private InstanceOperationsUtils() {}

    /**
     * 
     * Tries to acquire a lock on the IM lock file.
     * 
     * @param profile the profile to lock.
     * @param timeout the maximum time trying to acquire the lock.
     * @return <code>true</code> if locking was successful, else <code>false</code> is returned.
     * @throws IOException on failure.
     */
    public static boolean lockIMLockFile(final File profile, final long timeout) throws IOException {
        File lockfile = new File(profile.getAbsolutePath() + SLASH + IM_LOCK_FILE_NAME);
        lockfile.createNewFile();
        FileLock lock = null;

        if (!lockfile.isFile()) {
            throw new IOException("Lockfile isn't available.");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, IM_LOCK_FILE_ACCESS_PERMISSIONS)) {

            lock = randomAccessFile.getChannel().tryLock();

            if (lock == null) {

                final long timestamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
                final int maxWaitIterations = 20;

                while (timestamp - TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < (-timeout)) {

                    lock = randomAccessFile.getChannel().tryLock();

                    if (lock != null) {
                        return true;
                    }

                    Thread.sleep(maxWaitIterations);
                }
            } else {
                return true;
            }

        } catch (InterruptedException e) {
            throw new IOException("Unexpected error when trying to acquire a file lock on ");
        }

        return false;

    }

    /**
     * Tests whether the given profile directory is locked by a running instance.
     * 
     * @param profileDir the profile directory, as expected by the "--profile" parameter.
     * @return true if the directory is locked.
     * @throws IOException on failure.
     */
    public static boolean isProfileLocked(File profileDir) throws IOException {
        if (!profileDir.isDirectory()) {
            throw new IOException("Profile directory " + profileDir.getAbsolutePath() + " can not be created or is not a directory");
        }
        File lockfile = new File(profileDir, CommonProfile.PROFILE_DIR_LOCK_FILE_NAME);

        final int maxAttempts = 5;
        int attempt = 1;
        while (true) {
            try {
                // check this on every attempt in case the file was deleted concurrently
                if (!lockfile.isFile()) {
                    return false;
                }
                return testProfileLockOnce(lockfile);
            } catch (OverlappingFileLockException | IOException e) {
                if (attempt >= maxAttempts) {
                    throw new IOException("Failed to check the profile lock at " + lockfile.getAbsolutePath()
                        + " for " + attempt + " times; giving up", e);
                }
                LogFactory.getLog(InstanceOperationsUtils.class).debug(
                    "Failed to check the profile lock at " + lockfile.getAbsolutePath()
                        + ", most likely due to another thread performing the same check concurrently ("
                        + e.toString() + "); will retry");
            }
            attempt++;
        }
    }

    private static boolean testProfileLockOnce(File lockfile) throws IOException, OverlappingFileLockException {
        // try to get a lock on this file
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(lockfile, IM_LOCK_FILE_ACCESS_PERMISSIONS)) {
            final FileLock lock = randomAccessFile.getChannel().tryLock(); // can throw OverlappingFileLockException
            if (lock != null) {
                lock.release();
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            throw new IOException(UNEXPECTED_ERROR_WHEN_TRYING_TO_ACQUIRE_A_FILE_LOCK_ON + lockfile, e);
        }
    }

    /**
     * 
     * Simple watchdog for the shutdown file.
     * 
     * @param profilePath the path to the profile directory in which the shutdown file should be detected
     * @param timeoutMsec the maximum time to wait for the file to appear, in msec
     * @return <code>true</code> if shutdown file was found and is not empty, <code>false</code> otherwise
     * @throws IOException on failure.
     */
    public static boolean awaitShutdownFile(final Path profilePath, int timeoutMsec) throws IOException {
        Path shutdownFile = profilePath.resolve(CommonProfile.PROFILE_INTERNAL_DATA_SUBDIR).resolve(SHUTDOWN_FILE_NAME);

        final int existencePollingDelay = 500;
        final int fileHasContentPollingDelay = 200;

        long timeoutTimestamp = System.currentTimeMillis() + timeoutMsec;

        while (!Files.exists(shutdownFile)) {
            if (System.currentTimeMillis() >= timeoutTimestamp) {
                sharedLog.debug("Reached timeout while waiting for " + shutdownFile);
                return false;
            }
            try {
                Thread.sleep(existencePollingDelay);
            } catch (InterruptedException e) {
                sharedLog.debug("Interrupted while waiting for " + shutdownFile);
                return false;
            }
        }

        for (int i = 0; i < 2; i++) {
            if (Files.size(shutdownFile) != 0) {
                return true;
            }
            try {
                Thread.sleep(fileHasContentPollingDelay);
            } catch (InterruptedException e) {
                sharedLog.debug("Interrupted while waiting for " + shutdownFile);
                return false;
            }
        }

        // unusual case, log as waning
        sharedLog.warn("Found expected file " + shutdownFile + ", but it remained empty even after waiting");
        return false;
    }

    /**
     * 
     * Deletes the instance.lock file from the profile folder.
     * 
     * @param profileDir the profile directory.
     */
    public static void deleteInstanceLockFromProfileFolder(File profileDir) {
        for (File fileInProfileDir : profileDir.listFiles()) {
            if (fileInProfileDir.isFile()
                && CommonProfile.PROFILE_DIR_LOCK_FILE_NAME.equals(fileInProfileDir.getName())) {
                fileInProfileDir.delete();
                break;
            }
        }
    }

}
