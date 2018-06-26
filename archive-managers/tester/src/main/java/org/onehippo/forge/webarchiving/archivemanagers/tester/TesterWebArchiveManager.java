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

package org.onehippo.forge.webarchiving.archivemanagers.tester;

import java.util.Calendar;

import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TesterWebArchiveManager implements WebArchiveManager {
    private static final Logger log = LoggerFactory.getLogger(TesterWebArchiveManager.class);

    @Override
    public void requestUpdate(final WebArchiveUpdate update) throws WebArchiveUpdateException {
        if (Calendar.getInstance().get(Calendar.MINUTE) % 2 != 0) {
            throw new WebArchiveUpdateException("It's an odd minute, web archive is closed now");
        }
    }

    @Override
    public String getArchiveManagerInfo() {
        return "Tester Web Archive Manager [" + this.getClass().getSimpleName() +"]";
    }
}
