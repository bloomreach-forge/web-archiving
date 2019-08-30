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

package org.bloomreach.forge.webarchiving.cms.util;

import javax.jcr.Session;
import org.onehippo.repository.modules.DaemonModule;

/**
 * Services can implement this interface if access is needed to the JCR session provided by the initiator module.
 * The service initiator module (typically a {@link DaemonModule} implementation) manages this.
 */
public interface ModuleSessionAware {

    /**
     * Set the JCR session provided to the underlying (daemon) module.
     * @param moduleSession the JCR session provided to the underlying (daemon) module
     */
    void setModuleSession(Session moduleSession);
}
