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
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.HSTUrlService;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hippoecm.hst.core.container.AbstractHttpsSchemeValve.HTTP_SCHEME;



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
            HstContainerURL containerUrl = createContainerUrl(null, "localhost", "/home", requestContext, null);
            requestContext.setBaseURL(containerUrl);
            ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
            requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
            requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
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

    protected HstContainerURL createContainerUrl(final String scheme,
                                                 final String hostAndPort,
                                                 final String pathInfo,
                                                 final HstMutableRequestContext requestContext,
                                                 final String queryString) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        GenericHttpServletRequestWrapper containerRequest;
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            String host = hostAndPort.split(":")[0];
            if (hostAndPort.split(":").length > 1) {
                int port = Integer.parseInt(hostAndPort.split(":")[1]);
                request.setLocalPort(port);
                request.setServerPort(port);
            }

            if (scheme == null) {
                request.setScheme(HTTP_SCHEME);
            } else {
                request.setScheme(scheme);
            }
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            setRequestInfo(request, "/site", pathInfo);
            if (queryString != null) {
                request.setQueryString(queryString);
            }
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }

        requestContext.setServletRequest(containerRequest);
        requestContext.setServletResponse(response);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
            containerRequest.getContextPath(), HstRequestUtils.getRequestPath(containerRequest));

        setHstServletPath(containerRequest, mount);
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }


    protected void setRequestInfo(final MockHttpServletRequest request,
                                  final String contextPath,
                                  final String pathInfo) {
        request.setPathInfo(pathInfo);
        request.setContextPath(contextPath);
        request.setRequestURI(contextPath + request.getServletPath() + pathInfo);
    }

    protected void setHstServletPath(final GenericHttpServletRequestWrapper request, final ResolvedMount resolvedMount) {
        if (resolvedMount.getMatchingIgnoredPrefix() != null) {
            request.setServletPath("/" + resolvedMount.getMatchingIgnoredPrefix() + resolvedMount.getResolvedMountPath());
        } else {
            request.setServletPath(resolvedMount.getResolvedMountPath());
        }
    }


    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        return vhosts.matchSiteMapItem(url);
    }


     /*   public String getHostGroupNameForCmsHost() {
        String hostGroupNameForCmsHost = (String) RequestContextProvider.get().getAttribute(HOST_GROUP_NAME_FOR_CMS_HOST);
        if (hostGroupNameForCmsHost == null) {
            throw new IllegalStateException("For cms rest request there should be a request context attr for '" + HOST_GROUP_NAME_FOR_CMS_HOST + "' " +
                "but wasn't found.");
        }
        return hostGroupNameForCmsHost;
    }*/


    /*@Override
    public Map<String, Channel> getChannels(final String hostname) {
        try {
            VirtualHosts virtualHosts = hstManager.getVirtualHosts();
            final ResolvedVirtualHost resolvedVirtualHost = virtualHosts.matchVirtualHost(hostname);
            if (resolvedVirtualHost == null) {
                log.warn("Host '{}' is not a known host in the HST configuration. If it is a CMS host, make sure you " +
                        "add that host name to the correct '{}' node.", hostname, NODETYPE_HST_VIRTUALHOSTGROUP);
                return Collections.emptyMap();
            }
            return virtualHosts.getChannels(resolvedVirtualHost.getVirtualHost().getHostGroupName());
        } catch (ContainerException e) {
            throw new IllegalStateException("Could not load channels.", e);
        } catch (MatchException e) {
            throw new IllegalArgumentException(String.format("Could not match hostname '%s'.", hostname), e);
        }
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
