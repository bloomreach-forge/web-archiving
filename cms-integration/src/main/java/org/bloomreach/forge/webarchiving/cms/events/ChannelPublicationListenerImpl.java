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

package org.bloomreach.forge.webarchiving.cms.events;

import java.util.Map;

import org.bloomreach.forge.webarchiving.cms.util.PlatformManaged;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.eventbus.HippoEventListenerRegistry;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.bloomreach.forge.webarchiving.common.api.ChannelPublicationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO import org.onehippo.repository.events.PersistedHippoEventsService;

/**
 * Implementation of {@link ChannelPublicationListener}
 */
public class ChannelPublicationListenerImpl implements ChannelPublicationListener, PlatformManaged {
    private static Logger log = LoggerFactory.getLogger(ChannelPublicationListenerImpl.class);

    @Override
    public synchronized void initialize(Map<String, String> props) {
        log.debug("Initializing {}, registering to Hippo Event Bus", this.getClass().getName());
        HippoEventListenerRegistry.get().register(this);
    }

    @Override
    public void destroy() {
        log.debug("Destroying {}", this.getClass().getName());
        HippoEventListenerRegistry.get().unregister(this);
    }

    public String getEventCategory() {
        return "channel-manager";
    }

    /*public boolean onlyNewEvents() {
        return true;
    }*/

    /**
     * Listen for channel publication events
     */
    //@Override
    @Subscribe
    public void onHippoEvent(final HippoEvent hippoEvent) {

        log.debug("Received event = {}", hippoEvent);

    }
}
