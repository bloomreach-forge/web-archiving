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

package org.onehippo.forge.webarchiving.hst.events;

import java.util.Collections;

import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.onehippo.forge.webarchiving.common.util.WebArchiveUpdateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class ChannelPublicationEventListener implements ComponentManagerAware {

    private static final Logger log = LoggerFactory.getLogger(ChannelPublicationEventListener.class);

    private ComponentManager componentManager;

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void init() {
        componentManager.registerEventSubscriber(this);
    }

    public void destroy() {
        componentManager.unregisterEventSubscriber(this);
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
        }
        else if (projectId != null){
            log.debug("Skipping ChannelEvent because publication is in context of project with ID: '{}'",projectId);
            return;
        }

        final String channelIdentifier = event.getEditingMount().getChannel().getName();
        final String creator = String.join(", ",event.getUserIds());

        WebArchiveUpdateHelper.sendArchiveUpdate(channelIdentifier, creator, WebArchiveUpdateType.CHANNEL, Collections.emptyList(), channelIdentifier);
    }
}
