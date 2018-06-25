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
import java.util.Calendar;
import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.common.api.HstUrlService;
import org.onehippo.forge.webarchiving.common.api.WebArchiveUpdateJobsManager;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdate;
import org.onehippo.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.onehippo.repository.documentworkflow.task.AbstractDocumentTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("HippoSystemOutCallsInspection")
public class WebArchivingTask extends AbstractDocumentTask {

    private static final Logger log = LoggerFactory.getLogger(WebArchivingTask.class);

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        try {

            //5) When you need link rewriting in cms, call HippoServiceRegistry#getServices(LinkCreatorService.class) and ask every service for the links (every hst webapp will do the linkrewriting)
            final HstUrlService hstUrlService = HippoServiceRegistry.getService(HstUrlService.class);

            List<String> urls = Arrays.asList(
                hstUrlService.getAllUrls(getDocumentHandle().getHandle()));


            // TranslationError.getErrorMessage("ee?", locale);

            final WebArchiveUpdateJobsManager webArchiveUpdateJobsManager = HippoServiceRegistry.getService(WebArchiveUpdateJobsManager.class);
            if (webArchiveUpdateJobsManager == null) {
                //TODO Email admin, log erros
                //throw new WebArchiveUpdateException("WebArchiveUpdateJobsManager class not registered");
            } else {
                //TODO HstUrlService.getAllUrls() System.out.println("\n>>>>>>>> Getting all URLs for " + getDocumentHandle().getHandle().getPath());
                WebArchiveUpdate webArchiveUpdate = new WebArchiveUpdate();
               /* try {
                    webArchiveUpdateJobsManager.storeWebArchiveUpdateJob(webArchiveUpdate, false);
                } catch (WebArchiveUpdateException e) {
                    e.printStackTrace();
                }*/
            }


            WebArchiveUpdate webArchiveUpdate = new WebArchiveUpdate();
            webArchiveUpdate.setCreator(getWorkflowContext().getUserIdentity());
            webArchiveUpdate.setType(WebArchiveUpdateType.DOCUMENT);
            webArchiveUpdate.setUrls(urls);
            webArchiveUpdate.setCreated(Calendar.getInstance());
            //webArchiveUpdate.set


/*
        final DocumentAdvancedService documentService = RequestCycle.get() == null ?
            createRestProxy(DocumentAdvancedService.class) :
            createSecureRestProxy(DocumentAdvancedService.class);

        List<String> urls = documentService.getUrls(getDocumentHandle().getHandle().getIdentifier());
        if (!urls.isEmpty()) {
            for (String url : urls) {
                System.out.println(">>>>>>>>>>>>>>> " + url);
            }
        } else {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> No urls found");
        }*/
        } catch (Exception e) {
            //TODO EMAIL
        }

        return null;

    }

/*

    public <T> T createRestProxy(final Class<T> restServiceApiClass) {
        T cxfProxy = JAXRSClientFactory.create("http://127.0.0.1:8080/site/_archiving", restServiceApiClass, getProviders(null));
        return cxfProxy;
    }


    private static final String HEADER_CMS_CONTEXT_SERVICE_ID = "X-CMS-CS-ID";
    private static final String HEADER_CMS_SESSION_CONTEXT_ID = "X-CMS-SC-ID";
    private static final String CMSREST_CMSHOST_HEADER = "X-CMSREST-CMSHOST";


    public <T> T createSecureRestProxy(final Class<T> restServiceApiClass) {
        T clientProxy = JAXRSClientFactory.create("http://127.0.0.1:8080/site/_archiving", restServiceApiClass, getProviders(null));

        HttpServletRequest httpServletRequest = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
        HttpSession httpSession = httpServletRequest.getSession();
        CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);
        if (cmsSessionContext == null) {
            CmsInternalCmsContextService cmsContextService = (CmsInternalCmsContextService) HippoServiceRegistry.getService(CmsContextService.class);
            cmsSessionContext = cmsContextService.create(httpSession);
            CmsSessionUtil.populateCmsSessionContext(cmsContextService, cmsSessionContext, (PluginUserSession) UserSession.get());
        }
        // The accept method is called to solve an issue as the REST call was sent with 'text/plain' as an accept header
        // which caused problems matching with the relevant JAXRS resource
        final Client client = WebClient.client(clientProxy);
        client.header(HEADER_CMS_CONTEXT_SERVICE_ID, cmsSessionContext.getCmsContextServiceId())
            .header(HEADER_CMS_SESSION_CONTEXT_ID, cmsSessionContext.getId())
            .header(CMSREST_CMSHOST_HEADER, RequestUtils.getFarthestRequestHost(httpServletRequest))
            .accept(MediaType.WILDCARD_TYPE);

        // Enabling CXF logging from client-side
        ClientConfiguration config = WebClient.getConfig(client);
        config.getInInterceptors().add(new RestProxyLoggingInInterceptor());
        config.getOutInterceptors().add(new RestProxyLoggingOutInterceptor());

        // default time out is 60000 ms;

        return clientProxy;
    }

    protected List<Object> getProviders(final List<Object> additionalProviders) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(defaultJJJProvider);
        if (additionalProviders != null) {
            providers.addAll(additionalProviders);
        }
        return providers;
    }

    private static final JacksonJaxbJsonProvider defaultJJJProvider = new JacksonJaxbJsonProvider() {{
        setMapper(new ObjectMapper() {{
            enableDefaultTypingAsProperty(DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class");
            registerModule(new SimpleModule("CmsRestJacksonJsonModule", Version.unknownVersion()) {{
                addDeserializer(Annotation.class, new AnnotationJsonDeserializer());
            }});
        }});
    }};
*/


}


//DocumentUrlService documentUrlService = HippoServiceRegistry.getService(DocumentUrlService.class);
//System.out.println(documentUrlService.getUrl(getDocumentHandle().getHandle()));

        /*
        List<Callable<List<Blueprint>>> restProxyJobs = new ArrayList<>();
        for (final IRestProxyService restProxyService : restProxyServices.values()) {
            final BlueprintService blueprintService = restProxyService.createSecureRestProxy(BlueprintService.class);
            restProxyJobs.add(new Callable<List<Blueprint>>() {
                @Override
                public List<Blueprint> call() throws Exception {
                    return blueprintService.getBlueprints().getBlueprints();
                }
            });
        }

        final List<Future<List<Blueprint>>> futures = submitJobs(restProxyJobs);
        for (Future<List<Blueprint>> future : futures) {
            try {
                for (Blueprint blueprint : future.get()) {
                    blueprints.put(blueprint.getId(), blueprint);
                }
            } catch (InterruptedException | ExecutionException e) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to load blueprint for one or more rest proxies.", e);
                } else{
                    log.warn("Failed to load blueprint for one or more rest proxies : {}", e.toString());
                }
            }
        }
         */

