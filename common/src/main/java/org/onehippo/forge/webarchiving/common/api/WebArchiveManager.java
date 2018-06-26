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

package org.onehippo.forge.webarchiving.common.api;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdate;

/**
 * Interface for an external web archive service component.
 */
@SingletonService
public interface WebArchiveManager {

    /**
     * Requests a {@link WebArchiveUpdate} from a Web Archive System
     *
     * @param update a web archive update.
     * @throws WebArchiveUpdateException if an error occurs
     */
    void requestUpdate(WebArchiveUpdate update) throws WebArchiveUpdateException;

    /**
     * Gets information for the underlying archive manager implementation.
     * @return information for the underlying archive manager implementation.
     */
    String getArchiveManagerInfo();
}
