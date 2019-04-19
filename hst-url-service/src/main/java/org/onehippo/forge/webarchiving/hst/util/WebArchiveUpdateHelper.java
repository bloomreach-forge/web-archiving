/*
 * Copyright 2018-2019 BloomReach Inc. (http://www.bloomreach.com)
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

package org.onehippo.forge.webarchiving.hst.util;

import java.util.Calendar;
import java.util.List;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdate;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebArchiveUpdateHelper {

    private static final Logger log = LoggerFactory.getLogger(WebArchiveUpdateHelper.class);

    public static void sendArchiveUpdate(final String channelName, final String creator, final WebArchiveUpdateType eventType, final List<String> urls, String id){

        final WebArchiveManager webArchiveManager = HippoServiceRegistry.getService(WebArchiveManager.class);

        if (webArchiveManager == null) {
            log.error("A required service isn't registered: web-archive-service ({}). Skipping request of update for channel {}",
                    WebArchiveManager.class, channelName);
            return;
        }

        WebArchiveUpdate webArchiveUpdate = new WebArchiveUpdate();
        webArchiveUpdate.setCreator(creator);
        webArchiveUpdate.setType(eventType);
        webArchiveUpdate.setUrls(urls);
        webArchiveUpdate.setId(id);
        Calendar now = Calendar.getInstance();
        webArchiveUpdate.setCreated(now);

        try {
            webArchiveManager.requestUpdate(webArchiveUpdate);
        } catch (WebArchiveUpdateException e) {
            log.error("\n====================  Failed to request Web Archive update ====================\n{}" +
                    "========================================\n", webArchiveUpdate, e);
        }
    }
}
