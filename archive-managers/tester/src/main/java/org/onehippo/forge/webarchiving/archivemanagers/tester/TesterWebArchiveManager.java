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

package org.onehippo.forge.webarchiving.archivemanagers.tester;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.onehippo.forge.webarchiving.cms.util.Discoverable;
import org.onehippo.forge.webarchiving.cms.util.LifeCycle;
import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TesterWebArchiveManager implements WebArchiveManager, LifeCycle, Discoverable {
    private static final Logger log = LoggerFactory.getLogger(TesterWebArchiveManager.class);

    private int testerSleepTimeInSeconds = 10;
    private ExecutorService pool;

    @Override
    public synchronized void initialize(final Map<String, String> props) {
        final String sleepSeconds = props.get("tester.sleep.seconds");
        if (sleepSeconds != null) {
            try {
                testerSleepTimeInSeconds = Integer.parseInt(sleepSeconds);
            } catch (NumberFormatException nfe) {
                log.error("Initializing {}: cannot parse property tester.sleep.seconds='{}' into integer", this.getClass().getName(), sleepSeconds);
            }
        }
        log.info("Initialized {}. Simulating a Web Archive service with conf: sleep seconds: {}", this.getClass().getName(), testerSleepTimeInSeconds);

        pool = Executors.newCachedThreadPool();

    }

    @Override
    public void destroy() {
        log.debug("Destroying {}", this.getClass().getName());

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

    @Override
    public synchronized void requestUpdate(final WebArchiveUpdate update) throws WebArchiveUpdateException {

        pool.submit(() -> {
            log.info("\n====================   Received update: ====================\n{}\n\n" +
                "========================================\n\n", update);

            try {
                Thread.sleep(testerSleepTimeInSeconds * 1000);
            } catch (InterruptedException e) {
                log.warn("Can't even sleep for {} seconds", testerSleepTimeInSeconds);
            }

            if (update.hashCode() % 2 != 0) {
                log.error("\n====================  Failed to request Web Archive update ====================\n{}" +
                    "========================================\n", update);
            }
        });
    }

    @Override
    public String getArchiveManagerInfo() {
        return "Tester Web Archive Manager [" + this.getClass().getSimpleName() + "]";
    }
}
