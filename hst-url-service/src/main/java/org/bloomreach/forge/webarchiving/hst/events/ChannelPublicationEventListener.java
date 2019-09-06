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

package org.bloomreach.forge.webarchiving.hst.events;

import java.util.Calendar;

import org.bloomreach.forge.webarchiving.common.api.WebArchiveManager;
import org.bloomreach.forge.webarchiving.common.api.WebArchiveUpdateJobsManager;
import org.bloomreach.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdate;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateJob;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateJobStatus;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.bloomreach.forge.webarchiving.common.util.WebArchiveUpdateJobBuilder;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEventListenerRegistry;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelPublicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChannelPublicationEventListener.class);

    public void init() {
        ChannelEventListenerRegistry.get().register(this);
    }

    public void destroy() {
        ChannelEventListenerRegistry.get().unregister(this);
    }

    @Subscribe
    public void onEvent(ChannelEvent event) {
        if (event.getException() != null) {
            return;
        }

        final ChannelEventType type = event.getChannelEventType();
        final String projectId = event.getEditingMount().getChannel().getBranchId();

        if (ChannelEventType.PUBLISH != type) {
            log.debug("Skipping ChannelEvent '{}' because type is not equal to {}.", type, ChannelEventType.PUBLISH);
            return;
        } else if (projectId != null) {
            log.debug("Skipping ChannelEvent because publication is in context of project with ID: '{}'", projectId);
            return;
        }

        final String channelIdentifier = event.getEditingMount().getChannel().getName();
        final WebArchiveUpdateJobsManager webArchiveUpdateJobsManager = HippoServiceRegistry.getService(WebArchiveUpdateJobsManager.class);

        if (webArchiveUpdateJobsManager == null) {
            log.error("A required service isn't registered: web-archive-service ({}). Skipping request of update for channel {}",
                    WebArchiveUpdateJobsManager.class, webArchiveUpdateJobsManager, channelIdentifier);
            return;
        }
        final String creator = String.join(", ", event.getUserIds());

        WebArchiveUpdate webArchiveUpdate = new WebArchiveUpdate();
        webArchiveUpdate.setCreator(creator);
        webArchiveUpdate.setType(WebArchiveUpdateType.CHANNEL);
        webArchiveUpdate.setId(channelIdentifier);
        Calendar now = Calendar.getInstance();
        webArchiveUpdate.setCreated(now);

        WebArchiveUpdateJob job = WebArchiveUpdateJobBuilder.newJob()
            .setCreated(now)
            .setLastModified(now)
            .setStatus(WebArchiveUpdateJobStatus.QUEUED)
            .setWebArchiveUpdate(webArchiveUpdate)
            .build();
        try {
            webArchiveUpdateJobsManager.createWebArchiveUpdateJob(job);
        } catch (WebArchiveUpdateException e) {
            log.error("Failed to create Web Archive update job {}", job);
        }
    }
}
