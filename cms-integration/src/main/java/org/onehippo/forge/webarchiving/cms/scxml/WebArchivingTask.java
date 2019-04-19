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

package org.onehippo.forge.webarchiving.cms.scxml;


import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.HstUrlService;
import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.onehippo.forge.webarchiving.common.util.WebArchiveUpdateHelper;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebArchivingTask extends AbstractDocumentTask {

    private static final Logger log = LoggerFactory.getLogger(WebArchivingTask.class);

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        Node handle = getDocumentHandle().getHandle();

        //TODO, wip, For link rewriting in cms, use HippoServiceRegistry#getServices(LinkCreatorService.class) and ask every service for the links (every hst webapp will do the linkrewriting)
        final HstUrlService hstUrlService = HippoServiceRegistry.getService(HstUrlService.class);
        final WebArchiveManager webArchiveManager = HippoServiceRegistry.getService(WebArchiveManager.class);

        if (hstUrlService == null || webArchiveManager == null) {
            log.error("Not all required services are registered, url-service ({}): {}, web-archive-service ({}): {}. Skipping creation of job for {}",
                HstUrlService.class, hstUrlService, WebArchiveManager.class, webArchiveManager, handle.getPath());
            return null;
        }

        List<String> urls = null;
        try {
            urls = Arrays.asList(hstUrlService.getAllUrls(handle));
        } catch (WebArchiveUpdateException e) {
            log.error("Failed to create Web Archive update job for {}", handle.getPath(), e);
            return null;
        }

        if (urls.isEmpty()) {
            log.info("No public urls for document {}, skipping creation of job", handle.getPath());
            return null;
        }

        WebArchiveUpdateHelper.sendArchiveUpdate(handle.getPath(), getWorkflowContext().getUserIdentity(), WebArchiveUpdateType.DOCUMENT, urls, handle.getPath());
        return null;
    }
}

