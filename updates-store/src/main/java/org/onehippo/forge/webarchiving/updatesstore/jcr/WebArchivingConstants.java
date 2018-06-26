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

package org.onehippo.forge.webarchiving.updatesstore.jcr;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Web archiving addon JCR job store constants.
 */
public final class WebArchivingConstants {

    /**
     * Web archiving addon repository namespace prefix.
     */
    static final String NS_WEB_ARCHIVING_ADDON = "webarchivingaddon:";

    static final String NT_WEB_ARCHIVE_UPDATE_CONTAINER = NS_WEB_ARCHIVING_ADDON + "updatejobsstore";
    static final String NT_WEB_ARCHIVE_UPDATE_JOB = NS_WEB_ARCHIVING_ADDON + "updatejob";
    static final String NT_WEB_ARCHIVE_UPDATE = NS_WEB_ARCHIVING_ADDON + "update";

    static final String PROP_ID = NS_WEB_ARCHIVING_ADDON + "id";
    static final String PROP_CREATED = NS_WEB_ARCHIVING_ADDON + "created";
    static final String PROP_LAST_MODIFIED = NS_WEB_ARCHIVING_ADDON + "lastmodified";
    static final String PROP_CREATOR = NS_WEB_ARCHIVING_ADDON + "creator";
    static final String PROP_ATTEMPT = NS_WEB_ARCHIVING_ADDON + "attempt";
    static final String PROP_STATUS = NS_WEB_ARCHIVING_ADDON + "status";
    static final String PROP_TYPE = NS_WEB_ARCHIVING_ADDON + "type";
    static final String PROP_URLS = NS_WEB_ARCHIVING_ADDON + "urls";

    //TODO Update this list
    static final Set<String> BUILTIN_PROP_NAMES =
        new HashSet<>(Arrays.asList(PROP_ID, PROP_STATUS, PROP_CREATED, PROP_LAST_MODIFIED, PROP_CREATOR));


    private WebArchivingConstants() {
    }
}
