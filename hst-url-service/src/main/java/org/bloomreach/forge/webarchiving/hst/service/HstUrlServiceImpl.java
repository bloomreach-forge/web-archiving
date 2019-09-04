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

package org.bloomreach.forge.webarchiving.hst.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.bloomreach.forge.webarchiving.cms.util.Discoverable;
import org.bloomreach.forge.webarchiving.cms.util.HSTServicesAwarePlatformManaged;
import org.bloomreach.forge.webarchiving.cms.util.ModuleSessionAware;
import org.bloomreach.forge.webarchiving.common.api.HstUrlService;
import org.bloomreach.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.tool.ContentBeansTool;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HstUrlServiceImpl implements HstUrlService, ModuleSessionAware, HSTServicesAwarePlatformManaged, Discoverable {
    private static final Logger log = LoggerFactory.getLogger(HstUrlServiceImpl.class);

    protected static final String CONFIG_PROP_ENVIRONMENT = "environment";
    private String environment;

    protected Session systemSession;
    protected Session liveUserSession;
    private ObjectConverter objectConverter;


    @Override
    public synchronized void initialize(final Map<String, String> props) throws WebArchiveUpdateException {
        environment = props.get(CONFIG_PROP_ENVIRONMENT);
        if (environment == null) {
            throw new WebArchiveUpdateException("Property 'environment' is required, for example 'environment=prod'");
        }
        if (systemSession == null) {
            throw new WebArchiveUpdateException("ModuleSession is null");
        }

        final ComponentManager componentManager = HstServices.getComponentManager();
        // get a non-pooled live user so use '.delegating'
        final Credentials liveUserCredentials = componentManager.getComponent(Credentials.class.getName() + ".default.delegating");
        if (liveUserCredentials == null) {
            throw new IllegalStateException("Platform webapp should have live user credentials and a repository Spring bean.");
        }

        try {
            liveUserSession = systemSession.impersonate(liveUserCredentials);
        } catch (Exception e) {
            throw new WebArchiveUpdateException("Cannot login a repository user with live user credentials of the platform webapp.", e);
        }

        if (liveUserSession == null) {
            throw new WebArchiveUpdateException("Expected a live user session from the platform webapp.");
        }


        final ContentBeansTool contentBeansTool = componentManager.getComponent(ContentBeansTool.class);
        objectConverter = contentBeansTool.getObjectConverter();

        if (objectConverter == null) {
            throw new IllegalStateException("Expected an object converter from the platform webapp.");
        }
    }

    @Override
    public void setModuleSession(Session session) throws RepositoryException {
        this.systemSession = session;
    }

    @Override
    public void destroy() {
        log.debug("Destroying {}", this.getClass().getName());
        if (liveUserSession != null) {
            liveUserSession.logout();
        }
    }

    public String[] getAllUrls(Node handleNode) throws WebArchiveUpdateException {
        List<String> urls = new ArrayList<>();
        try {
            if (!handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                log.warn("Service called for a non handle node, aborting");
                return null;
            }

            final HippoBean bean = (HippoBean) objectConverter.getObject(handleNode);
            if (bean == null) {
                log.debug("Could not create a bean for '{}'", handleNode.getPath());
                return null;
            }
            if (!HippoDocumentBean.class.isAssignableFrom(bean.getClass())) {
                log.warn("Expected a HippoDocumentBean but was '{}'", bean.getClass());
                return null;
            }

            final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
            if (hstModelRegistry == null) {
                log.info("Cannot create URLs without hstModelRegistry");
                return null;
            }

            final HstModel platformModel = hstModelRegistry.getHstModel(this.getClass().getClassLoader());
            final HstLinkCreator linkCreator = platformModel.getHstLinkCreator();
            if (linkCreator == null) {
                log.info("Cannot create URLs without link creator");
                return null;
            }

            final Mount anyMount = platformModel.getVirtualHosts().getMountsByHostGroup(environment).get(0);
            final List<HstLink> hstLinks = linkCreator.createAll(handleNode, anyMount, environment, "live", true);

            urls.addAll(hstLinks.stream()
                    .filter(link -> link.getMount().getChannel() != null)
                    .map(this::getFullyQualifiedURL)
                    .collect(Collectors.toList()));

        } catch (Exception e) {
            e.printStackTrace();
            throw new WebArchiveUpdateException(e);
        }
        return urls.toArray(new String[0]);
    }

    private String getFullyQualifiedURL(final HstLink hstLink) {
        Mount linkMount = hstLink.getMount();
        final HstSiteMapItem hstSiteMapItem = hstLink.getHstSiteMapItem();
        return (hstSiteMapItem == null || StringUtils.isBlank(hstLink.getPath())) ?
                createMountURL(linkMount) :
                createMountURL(linkMount) + "/" + hstLink.getPath();
    }


    private String createMountURL(final Mount mount) {
        String url = mount.getScheme() + "://" + mount.getVirtualHost().getHostName();
        if (StringUtils.isNotBlank(mount.getContextPath())) {
            url = url + mount.getContextPath();
        }
        if (StringUtils.isNotBlank(mount.getMountPath())) {
            url = url + mount.getMountPath();
        }
        return url;
    }
}
