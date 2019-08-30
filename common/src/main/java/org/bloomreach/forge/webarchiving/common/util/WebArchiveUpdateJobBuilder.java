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

package org.bloomreach.forge.webarchiving.common.util;

import java.util.Calendar;

import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateJob;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateJobStatus;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdate;


public class WebArchiveUpdateJobBuilder {

    private WebArchiveUpdateJob webArchiveUpdateJob;

    private WebArchiveUpdateJobBuilder() {
        webArchiveUpdateJob = new WebArchiveUpdateJob();
    }

    public static WebArchiveUpdateJobBuilder newJob() {
        return new WebArchiveUpdateJobBuilder();
    }

    public WebArchiveUpdateJob build() {
        return this.webArchiveUpdateJob;
    }

    public WebArchiveUpdateJobBuilder setCreated(Calendar created) {
        this.webArchiveUpdateJob.setCreated(created);
        return this;
    }

    public WebArchiveUpdateJobBuilder setLastModified(Calendar created) {
        this.webArchiveUpdateJob.setLastModified(created);
        return this;
    }

    public WebArchiveUpdateJobBuilder setWebArchiveUpdate(WebArchiveUpdate webArchiveUpdate) {
        this.webArchiveUpdateJob.setWebArchiveUpdate(webArchiveUpdate);
        return this;
    }

    public WebArchiveUpdateJobBuilder setStatus(WebArchiveUpdateJobStatus status) {
        this.webArchiveUpdateJob.setStatus(status);
        return this;
    }
}
