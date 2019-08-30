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

package org.bloomreach.forge.webarchiving.archivemanagers.archiefweb;

import java.util.Map;

import org.bloomreach.forge.webarchiving.cms.util.Discoverable;
import org.bloomreach.forge.webarchiving.cms.util.LifeCycle;
import org.bloomreach.forge.webarchiving.common.api.WebArchiveManager;
import org.bloomreach.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArchiefWebArchiveManager implements WebArchiveManager, LifeCycle, Discoverable {
    private static final Logger log = LoggerFactory.getLogger(ArchiefWebArchiveManager.class);

    @Override
    public void initialize(final Map<String, String> props) throws WebArchiveUpdateException {
    }

    @Override
    public void destroy() {
        log.debug("Destroying {}", this.getClass().getName());
    }

    @Override
    public synchronized void requestUpdate(final WebArchiveUpdate update) throws WebArchiveUpdateException {

    }

    @Override
    public String getArchiveManagerInfo() {
        return this.getClass().getSimpleName();

    }
}
