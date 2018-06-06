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

package org.onehippo.forge.webarchiving.hst.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.HSTUrlService;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class HSTUrlServiceImpl implements HSTUrlService {
    private static final Logger log = LoggerFactory.getLogger(HSTUrlServiceImpl.class);

    public void registerService() {
        // Since multiple HST webapps are supported, we need to account for the 'contextPath' in the name to avoid
        // collisions
        HippoServiceRegistry.registerService(this, HSTUrlService.class/*, contextPath + "-" + HSTUrlServiceImpl.class.getName()*/);
    }

    public void unregisterService() {
        HippoServiceRegistry.unregisterService(this, HSTUrlService.class/*, contextPath + "-" + HSTUrlServiceImpl.class.getName()*/);
    }

    private String contextPath;
    private HstLinkCreator linkCreator;
    private HstRequestContextComponent hstRequestContextComponent;
    private HstURLFactory hstURLFactory;
    private HstManager hstManager;
    private HstSiteMapMatcher hstSiteMapMatcher;

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setLinkCreator(final HstLinkCreator linkCreator) {
        this.linkCreator = linkCreator;
    }

    public void setHstRequestContextComponent(final HstRequestContextComponent hstRequestContextComponent) {
        this.hstRequestContextComponent = hstRequestContextComponent;
    }

    public void setHstURLFactory(final HstURLFactory hstURLFactory) {
        this.hstURLFactory = hstURLFactory;
    }

    public void setHstManager(final HstManager hstManager) {
        this.hstManager = hstManager;
    }

    public void setHstSiteMapMatcher(final HstSiteMapMatcher hstSiteMapMatcher) {
        this.hstSiteMapMatcher = hstSiteMapMatcher;
    }

    public String[] getAllUrls(Node handleNode) throws WebArchiveUpdateException {
        try {
            HstMutableRequestContext requestContext = hstRequestContextComponent.create();
            ModifiableRequestContextProvider.set(requestContext);

            ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount("localhost", contextPath, "/");
            //ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchVirtualHost("localhost").matchMount(contextPath, "/");



            requestContext.setBaseURL(createLocalBaseContainerUrl(resolvedMount.getResolvedMountPath()));

            /*HstContainerURL containerUrl = createContainerUrl(null, "localhost", "/home", requestContext, null);1
            requestContext.setBaseURL(containerUrl);
            ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());*/

            requestContext.setResolvedMount(resolvedMount);


            requestContext.matchingFinished();
            requestContext.setURLFactory(hstURLFactory);
            requestContext.setSiteMapMatcher(hstSiteMapMatcher);

            List<HstLink> links = linkCreator.createAll(handleNode, requestContext, "localhost", null, true);
            List<HstLink> all = linkCreator.createAll(handleNode, requestContext, true);
            List<HstLink> allAvailableCanonicals = linkCreator.createAllAvailableCanonicals(handleNode, requestContext);


            List<String> urls = new ArrayList<>();
            urls.addAll(links.stream().map(link -> link.toUrlForm(requestContext, true)).collect(Collectors.toList()));
            urls.addAll(all.stream().map(link -> link.toUrlForm(requestContext, true)).collect(Collectors.toList()));
            urls.addAll(allAvailableCanonicals.stream().map(link -> link.toUrlForm(requestContext, true)).collect(Collectors.toList()));

            ModifiableRequestContextProvider.clear();

            return urls.toArray(new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private HstContainerURL createLocalBaseContainerUrl(final String resolvedMountPath) {
        return new HstContainerURL() {
            @Override
            public String getCharacterEncoding() {
                return "UTF-8";
            }

            @Override
            public String getHostName() {
                return "localhost";
            }

            @Override
            public String getContextPath() {
                return contextPath;
            }

            @Override
            public String getRequestPath() {
                return null;
            }

            @Override
            public int getPortNumber() {
                return 8080;
            }

            @Override
            public String getResolvedMountPath() {
                return resolvedMountPath;
            }

            @Override
            public String getPathInfo() {
                return "/";
            }

            @Override
            public String getActionWindowReferenceNamespace() {
                return null;
            }

            @Override
            public void setActionWindowReferenceNamespace(final String actionWindowReferenceNamespace) {

            }

            @Override
            public String getResourceWindowReferenceNamespace() {
                return null;
            }

            @Override
            public void setResourceWindowReferenceNamespace(final String resourceWindowReferenceNamespace) {

            }

            @Override
            public String getComponentRenderingWindowReferenceNamespace() {
                return null;
            }

            @Override
            public void setComponentRenderingWindowReferenceNamespace(final String componentRenderingWindowReferenceNamespace) {

            }

            @Override
            public String getResourceId() {
                return null;
            }

            @Override
            public void setResourceId(final String resourceId) {

            }

            @Override
            public void setParameter(final String name, final String value) {

            }

            @Override
            public void setParameter(final String name, final String[] values) {

            }

            @Override
            public void setParameters(final Map<String, String[]> parameters) {

            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return null;
            }

            @Override
            public String getParameter(final String name) {
                return null;
            }

            @Override
            public String[] getParameterValues(final String name) {
                return new String[0];
            }

            @Override
            public void setActionParameter(final String name, final String value) {

            }

            @Override
            public void setActionParameter(final String name, final String[] values) {

            }

            @Override
            public void setActionParameters(final Map<String, String[]> parameters) {

            }

            @Override
            public Map<String, String[]> getActionParameterMap() {
                return null;
            }
        };
    }



/*
    protected void setHstServletPath(final GenericHttpServletRequestWrapper request, final ResolvedMount resolvedMount) {
        if (resolvedMount.getMatchingIgnoredPrefix() != null) {
            request.setServletPath("/" + resolvedMount.getMatchingIgnoredPrefix() + resolvedMount.getResolvedMountPath());
        } else {
            request.setServletPath(resolvedMount.getResolvedMountPath());
        }
    }
*/

/*

    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        return vhosts.matchMount().matchSiteMapItem(url);
    }
*/


     /*   public String getHostGroupNameForCmsHost() {
        String hostGroupNameForCmsHost = (String) RequestContextProvider.get().getAttribute(HOST_GROUP_NAME_FOR_CMS_HOST);
        if (hostGroupNameForCmsHost == null) {
            throw new IllegalStateException("For cms rest request there should be a request context attr for '" + HOST_GROUP_NAME_FOR_CMS_HOST + "' " +
                "but wasn't found.");
        }
        return hostGroupNameForCmsHost;
    }*/


    public String[] getAllUrls(String documentId) throws WebArchiveUpdateException {
        return null;
    }



    /**
     * Returns the node with the given UUID using the session of the given request context.
     *
     * @param requestContext the request context
     * @param uuidParam      a UUID
     * @return the node with the given UUID, or null if no such node could be found.
     */
    static Node getNodeByUuid(final HstRequestContext requestContext, final String uuidParam) {
        if (uuidParam == null) {
            log.info("UUID is null, returning null", uuidParam);
            return null;
        }

        final String uuid = PathUtils.normalizePath(uuidParam);

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            log.info("Illegal UUID: '{}', returning null", uuidParam);
            return null;
        }

        try {
            return requestContext.getSession().getNodeByIdentifier(uuid);
        } catch (ItemNotFoundException e) {
            log.warn("Node not found: '{}', returning null", uuid);
        } catch (RepositoryException e) {
            log.warn("Error while fetching node with UUID '" + uuid + "', returning null", e);
        }
        return null;
    }


    /**
     * Returns the node with at the given (absolute) path, using the session of the given request context.
     *
     * @param requestContext the request context
     * @param path           an absolute path
     * @return the node at the given path, or null if no such node could be found.
     */
    static Node getNodeByPath(final HstRequestContext requestContext, final String path) {
        if (path == null) {
            log.info("Path is null, returning null", path);
            return null;
        }

        try {
            return requestContext.getSession().getNode(PathUtils.normalizePath(path));
        } catch (ItemNotFoundException e) {
            log.warn("Node not found: '{}', returning null", path);
        } catch (RepositoryException e) {
            log.warn("Error while fetching node at path '" + path + "', returning null", e);
        }
        return null;
    }

}
