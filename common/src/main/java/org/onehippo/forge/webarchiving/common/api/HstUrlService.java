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

package org.onehippo.forge.webarchiving.common.api;

import javax.jcr.Node;

import org.onehippo.cms7.services.SingletonService;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;

/**
 * Interface that is responsible for providing url information about documents in the CMS
 */
@SingletonService
public interface HstUrlService {

    /**
     * Return all public Urls a document is visible under
     *
     * @param handleNode the {@link Node} object of the document handle
     * @return all public urls as an array of Strings
     * @throws WebArchiveUpdateException if an exception occurs
     */
    String[] getAllUrls(Node handleNode) throws WebArchiveUpdateException;


}
