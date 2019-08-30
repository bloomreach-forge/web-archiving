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
import java.util.stream.Collectors;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.HstUrlService;
import org.onehippo.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.onehippo.forge.webarchiving.hst.util.LocalHstContainerURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HstUrlServiceImpl implements HstUrlService {
    private static final Logger log = LoggerFactory.getLogger(HstUrlServiceImpl.class);

    private String contextPath;
    private HstLinkCreator linkCreator;
    private HstRequestContextComponent hstRequestContextComponent;
    private HstURLFactory hstURLFactory;
    private HstManager hstManager;
    private String host;
    private int port;
    private String requestPath;


    public String[] getAllUrls(Node handleNode) throws WebArchiveUpdateException {
        List<String> urls = new ArrayList<>();
        try {
            if(!handleNode.isNodeType(HippoNodeType.NT_HANDLE)){
                log.warn("Service called for a non handle node");
            } else {
                HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
                HstModel hstModel = hstModelRegistry.getHstModel(contextPath);
                HstLinkCreator linkCreator = hstModel.getHstLinkCreator();

                HstMutableRequestContext requestContext = hstRequestContextComponent.create();
                ModifiableRequestContextProvider.set(requestContext);
                ResolvedMount resolvedMount = hstManager.getVirtualHosts().matchMount(host, contextPath, requestPath);
                requestContext.setBaseURL(new LocalHstContainerURL(host, port, contextPath, requestPath, resolvedMount.getResolvedMountPath()));
                requestContext.setResolvedMount(resolvedMount);
                requestContext.matchingFinished();
                requestContext.setURLFactory(hstURLFactory);

                //List<HstLink> links = linkCreator.createAll(handleNode, requestContext, host, null, true);
                List<HstLink> all = linkCreator.createAll(handleNode, requestContext, true);
                //List<HstLink> allAvailableCanonicals = linkCreator.createAllAvailableCanonicals(handleNode, requestContext);

                //urls.addAll(links.stream().map(link -> link.toUrlForm(requestContext, true)).collect(Collectors.toList()));
                urls.addAll(all.stream().map(link -> link.toUrlForm(requestContext, true)).collect(Collectors.toList()));
                //urls.addAll(allAvailableCanonicals.stream().map(link -> link.toUrlForm(requestContext, true)).collect(Collectors.toList()));

                ModifiableRequestContextProvider.clear();
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new WebArchiveUpdateException(e);
        }
        return urls.toArray(new String[0]);
    }

    public void registerService() {
        //TODO Since multiple HST webapps are supported, we need to account for the 'contextPath' in the name to avoid collisions
        HippoServiceRegistry.register(this, HstUrlService.class);
    }

    public void unregisterService() {
        //TODO Since multiple HST webapps are supported, we need to account for the 'contextPath' in the name to avoid collisions
        HippoServiceRegistry.unregister(this, HstUrlService.class);
    }


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

    public void setHost(final String host) {
        this.host = host;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setRequestPath(final String requestPath) {
        this.requestPath = requestPath;
    }
}
