/*
 * Copyright 2018 BloomReach Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.webarchiving.cms;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.api.WebArchiveUpdateJobsManager;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdate;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateJob;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateJobStatus.ACKNOWLEDGED;
import static org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateJobStatus.ERROR;
import static org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateJobStatus.QUEUED;
import static org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateJobStatus.UNDEFINED;

/**
 * A {@link RepositoryJob} implementation that processes completed and pending {@link WebArchiveUpdateJob} items, using
 * the underlying {@link WebArchiveUpdateJobsManager} and {@link WebArchiveManager}registry services
 */
public class WebArchiveUpdatesProcessor implements RepositoryJob {

    private static final Logger log = LoggerFactory.getLogger(WebArchiveUpdatesProcessor.class);

    private static final String CONFIG_DAYS_TO_LIVE = "daysToLive";
    private static final String CONFIG_SEARCH_LIMIT = "searchLimit";

    private static final int DEFAULT_DAYS_TO_LIVE = 365;
    private static final int DEFAULT_SEARCH_LIMIT = 1000;

    private long daysToLive = DEFAULT_DAYS_TO_LIVE;
    private long searchLimit = DEFAULT_SEARCH_LIMIT;

    private /*TODO final*/ ExecutorService pool;
    private WebArchiveUpdateJobsManager updateJobsManager;
    private WebArchiveManager webArchiveManager;

    public WebArchiveUpdatesProcessor() {
        System.out.println("Constructor and pool was null? " + (pool == null));
        if (pool == null) {
            pool = Executors.newCachedThreadPool(); //TODO CHECK LIFECYCLE OF SCHEDULER
        }

    }

    @Override
    public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
        final Session session = context.createSystemSession();
        try {
            doConfigure(context);
            processCompletedJobs();
            processPendingJobs();

        } catch (WebArchiveUpdateException e) {
            log.error("Error while processing web archive jobs", e);
        } finally {
            session.logout();
        }
    }

    protected void processCompletedJobs() throws RepositoryException, WebArchiveUpdateException {
        final List<WebArchiveUpdateJob> completedJobs = updateJobsManager.getCompletedWebArchiveUpdateJobs((int) searchLimit);

        //We collect all items eligible for deletion before deleting them because we want to to batch saves
        final List<WebArchiveUpdateJob> jobDeletions = new ArrayList<>();

        jobDeletions.addAll(completedJobs.stream().filter(job -> job.getStatus() == ACKNOWLEDGED).collect(Collectors.toList()));

        List<WebArchiveUpdateJob> failedJobs = completedJobs.stream().filter(job -> job.getStatus() == ERROR).collect(Collectors.toList());
        logFailedJobs(failedJobs);
        jobDeletions.addAll(failedJobs);

        List<WebArchiveUpdateJob> undefinedJobs = completedJobs.stream().filter(job -> job.getStatus() == UNDEFINED).collect(Collectors.toList());
        logFailedJobs(undefinedJobs);
        jobDeletions.addAll(undefinedJobs);

        try {
            updateJobsManager.deleteWebArchiveUpdateJobs(jobDeletions.toArray(new WebArchiveUpdateJob[0]));
        } catch (WebArchiveUpdateException e) {
            log.error("Error deleting web archive update jobs", e);
        }

    }

    private void processPendingJobs() throws WebArchiveUpdateException {
        final List<WebArchiveUpdateJob> pendingJobs = updateJobsManager.getPendingWebArchiveUpdateJobs((int) searchLimit);
        pendingJobs.stream()
            .filter(job -> job.getStatus() == QUEUED)
            .forEach(updateJob ->
                pool.submit(() -> {
                    WebArchiveUpdate update = updateJob.getWebArchiveUpdate();
                    try {
                        webArchiveManager.requestUpdate(update);
                        updateJob.setStatus(ACKNOWLEDGED);
                    } catch (WebArchiveUpdateException e) {
                        log.info("Error processing job:" + updateJob.toString(), e);
                        updateJob.setStatus(ERROR);
                    } finally {
                        try {
                            updateJobsManager.updateWebArchiveUpdateJob(updateJob);
                        } catch (WebArchiveUpdateException e2) {
                            log.error("Error while updating WebArchiveUpdate job:" + updateJob.toString(), e2);
                        }
                    }
                }));
    }

    protected void doConfigure(final RepositoryJobExecutionContext context) throws WebArchiveUpdateException {
        daysToLive = getNumber(context, CONFIG_DAYS_TO_LIVE, DEFAULT_DAYS_TO_LIVE);
        if (daysToLive <= 0) {
            throw new WebArchiveUpdateException("Web archive updates processor DISABLED because daysToLive is set to a negative number {}.", daysToLive);
        }

        updateJobsManager = HippoServiceRegistry.getService(WebArchiveUpdateJobsManager.class);
        if (updateJobsManager == null) {
            throw new WebArchiveUpdateException("WebArchiveUpdateJobsManager class is not registered");
        }

        webArchiveManager = HippoServiceRegistry.getService(WebArchiveManager.class);
        if (webArchiveManager == null) {
            throw new WebArchiveUpdateException("WebArchiveManager class is not registered");
        }

        searchLimit = getNumber(context, CONFIG_SEARCH_LIMIT, DEFAULT_SEARCH_LIMIT);
        if (searchLimit < 1) {
            searchLimit = DEFAULT_SEARCH_LIMIT;
        }
    }


    protected void logFailedJobs(final List<WebArchiveUpdateJob> failedJobs) {
        failedJobs.forEach(job -> log.error("Job has failed and has been removed from queue {}", job));
    }

    //TODO What is the lifecycle of the repo scheduler jobs?
    public synchronized void destroy() {
        if (pool != null) {
            pool.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                        log.error("Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
        log.debug("Destroyed {}, shut down thread pool", this.getClass().getName());
    }


    protected static long getNumber(final RepositoryJobExecutionContext context, final String attributeName, final long defaultValue) {
        long number;
        final String value = context.getAttribute(attributeName);
        try {
            if (StringUtils.isBlank(value)) {
                log.warn("Incorrect number '{}'. Setting to default '{}'", value, defaultValue);
                return defaultValue;
            }
            number = Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Incorrect number '{}'. Setting to default '{}'", value, defaultValue);
            number = defaultValue;
        }
        return number;
    }
}
