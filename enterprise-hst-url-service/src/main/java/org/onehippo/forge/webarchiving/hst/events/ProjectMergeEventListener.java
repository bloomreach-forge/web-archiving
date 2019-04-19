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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.onehippo.forge.webarchiving.common.api.HstUrlService;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.onehippo.forge.webarchiving.common.util.WebArchiveUpdateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onehippo.cms7.services.wpm.project.model.ReintegrationResult;
import com.onehippo.cms7.services.wpm.wpm.observation.WpmEvent;

public class ProjectMergeEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProjectMergeEventListener.class);
    private static final String PROJECTS_LOCATION = "/hippowpm:hippowpm/hippowpm:projects/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void init() {
        HippoServiceRegistry.registerService(this, HippoEventBus.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregisterService(this, HippoEventBus.class);
    }

    @Subscribe
    public void handleEvent(HippoEvent hippoEvent) {
        Repository repository = HstServices
                .getComponentManager()
                .getComponent(Repository.class.getName());
        Credentials configCred = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".hstconfigreader");
        Session session = null;
        try {
            session = repository.login(configCred);
        } catch (RepositoryException e) {
            log.error("Error while trying to login to repository.", e);
        }
        if (session != null) {
            if (hippoEvent.category().equals("hippo-addon-wpm") && hippoEvent.action().equals("reintegrateProject")) {
                WpmEvent event = new WpmEvent(hippoEvent);
                Node projectNode = null;
                try {
                    projectNode = session.getNode(PROJECTS_LOCATION + event.projectId());
                } catch (RepositoryException e) {
                    log.error("Couldn't retrieve node for path: " + PROJECTS_LOCATION + event.projectId());
                }
                try {
                    if (projectNode != null && projectNode.hasProperty("hippowpm:jsonReintegrationResult")) {
                        String jsonResult = projectNode.getProperty("hippowpm:jsonReintegrationResult").getString();
                        ReintegrationResult result = null;
                        try {
                            result = objectMapper.readValue(jsonResult, ReintegrationResult.class);
                        } catch (IOException e) {
                            log.error("Error while trying to parse/map json ReintegrationResult. ", e);
                        }

                        if (result != null) {
                            result.getChannels().forEach(
                                    (mountId, mergeResult) -> {
                                        HstManager hstManager = HstServices.getComponentManager().getComponent("org.hippoecm.hst.configuration.model.HstManager");
                                        try {
                                            Mount mount = hstManager.getVirtualHosts().getMountByIdentifier(mountId);
                                            WebArchiveUpdateHelper.sendArchiveUpdate(mount.getChannel().getName(), (String) hippoEvent.get("user"), WebArchiveUpdateType.CHANNEL, Collections.emptyList(), mount.getChannel().getName());
                                        } catch (ContainerException e) {
                                            log.error("Couldn't retrieve virtual hosts from HstManager.", e);
                                        }
                                    }
                            );
                            final Session finalSession = session;
                            final HstUrlService hstUrlService = HippoServiceRegistry.getService(HstUrlService.class);
                            result.getDocuments().forEach(
                                    (uuid, documentReintegrationResult) -> {
                                        try {
                                            Node handle = finalSession.getNodeByIdentifier(uuid);
                                            List<String> urls = Arrays.asList(hstUrlService.getAllUrls(handle));
                                            if (!urls.isEmpty()) {
                                                WebArchiveUpdateHelper.sendArchiveUpdate(handle.getPath(), (String) hippoEvent.get("user"), WebArchiveUpdateType.DOCUMENT, urls, handle.getPath());
                                            }
                                        } catch (RepositoryException | WebArchiveUpdateException e) {
                                            log.error("Error occurred while trying to send an update for a document as a result of project reintegration. ", e);
                                        }
                                    }
                            );
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Couldn't read or property hippowpm:jsonReintegrationResult doesn't exist from: " + projectNode);
                }
            }
            session.logout();
        }
    }
}
