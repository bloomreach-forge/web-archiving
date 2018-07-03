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

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent.ChannelEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    @AllowConcurrentEvents
    public void onEvent(ChannelEvent event) {
        if (event.getException() != null) {
            return;
        }

        final ChannelEventType type = event.getChannelEventType();
        if (type != ChannelEventType.DISCARD && type != ChannelEventType.PUBLISH) {
            log.debug("Skipping ChannelEvent '{}' because type is not equal to {} or {}.", type,
                ChannelEventType.PUBLISH, ChannelEventType.DISCARD);
            return;
        }


        String homeLink = "(ERROR)";
        try {
            HstRequestContext context = event.getRequestContext();
            HstLinkCreator creator = context.getHstLinkCreator();
            Mount mount = context.getResolvedMount().getMount();
            HstLink link = creator.create("/", mount);
            homeLink = link.toUrlForm(context, true);
        } catch (Exception e) {
            String s = "";
        }

        log.error("\n(NOT AN ERROR) Channel: {}\nevent: {}\nispreview: {}\ncontextpath: {}\nhomepage: {}\nmountpath: {}\nrootFQLink (as string): {}\nuser: {}",
            event.getEditingPreviewSite().getName(),
            event.getChannelEventType(),
            event.getEditingMount().isPreview(),
            event.getEditingMount().getContextPath(),
            event.getEditingMount().getHomePage(),
            event.getEditingMount().getMountPath(),
            homeLink,
            event.getUserIds());

    }

}
