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

/**
 * Web archive update type.
 *
 * Don't change the order in which the enum are declared. The {@link #compareTo(Enum)} method
 * uses this order. See enum specs.
 */
public enum WebArchiveUpdateType {

    CHANNEL, //update operates on whole channel
    DOCUMENT; //update only operates on document urls

    public static final WebArchiveUpdateType[] ALL = new WebArchiveUpdateType[]{CHANNEL, DOCUMENT};
    public static List<WebArchiveUpdateType> all() {
        return Arrays.asList(ALL);
    }
}
