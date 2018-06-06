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

package org.onehippo.forge.webarchiving.common.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Web archive update job status.
 *
 * Don't change the order in which the enum are declared. The {@link #compareTo(Enum)} method
 * uses this order. See enum specs.
 */
public enum WebArchiveUpdateJobStatus {

    QUEUED("Queued"), //queued for submission

    SUBMITTED("Submitted"),

    COMPLETED("Completed"), //can be deleted

    ABORTED("Aborted"), //must be resubmitted

    ERROR("Error"), //can be deleted

    UNDEFINED("Undefined"); //can be deleted

    public static final WebArchiveUpdateJobStatus[] ALL = new WebArchiveUpdateJobStatus[]{QUEUED, SUBMITTED, COMPLETED, ABORTED, ERROR, UNDEFINED};
    public static List<WebArchiveUpdateJobStatus> all() {
        return Arrays.asList(ALL);
    }

    /** Statuses that are in the category of 'pending' */
    public static final WebArchiveUpdateJobStatus[] CATEGORY_PENDING = new WebArchiveUpdateJobStatus[]{QUEUED, SUBMITTED, ABORTED};

    /** Statuses that are in the category of 'done' */
    public static final WebArchiveUpdateJobStatus[] CATEGORY_DONE = new WebArchiveUpdateJobStatus[]{COMPLETED, ERROR, UNDEFINED};

    public static List<WebArchiveUpdateJobStatus> categoryPending() {
        return Arrays.asList(CATEGORY_PENDING);
    }

    public static List<WebArchiveUpdateJobStatus> categoryDone() {
        return Arrays.asList(CATEGORY_DONE);
    }

    private String label;

    WebArchiveUpdateJobStatus(String label) {
        this.label = label;
    }

    public String getLabel(final Locale locale) {
        final ResourceBundle bundle = ResourceBundle.getBundle(WebArchiveUpdateJobStatus.class.getName(), locale);
        try {
            return bundle.getString(this.name().toLowerCase().concat(".label"));
        }
        catch (MissingResourceException mre) {
            return this.label;
        }
    }

    public String getHint(final Locale locale) {
        final ResourceBundle bundle = ResourceBundle.getBundle(WebArchiveUpdateJobStatus.class.getName(), locale);
        try {
            return bundle.getString(this.name().toLowerCase().concat(".hint"));
        }
        catch (MissingResourceException mre) {
            return this.label;
        }
    }

    @Override
    public String toString() {
        return this.label;
    }
}
