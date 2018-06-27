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

package org.onehippo.forge.webarchiving.cms.util;

import java.util.Map;

import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.repository.modules.DaemonModule;
/**
 * Component module lifecycle abstraction.
 *
 * Services can implement this interface in order to initialize and destroy themselves on each lifecycle method call.
 * The service initiator module (typically a {@link DaemonModule} implementation) manages this lifecycle.
 */
public interface LifeCycle {

    /**
     * Do initialization using the {@code props}.
     * @param props properties
     * @throws WebArchiveUpdateException if any exception occurs
     */
    void initialize(Map<String, String> props) throws WebArchiveUpdateException;

    /**
     * Do destruction.
     */
    void destroy();

}
