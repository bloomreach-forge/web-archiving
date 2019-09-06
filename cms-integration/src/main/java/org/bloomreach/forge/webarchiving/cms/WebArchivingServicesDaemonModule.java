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

package org.bloomreach.forge.webarchiving.cms;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.bloomreach.forge.webarchiving.cms.util.CmsUtils;
import org.bloomreach.forge.webarchiving.cms.util.Discoverable;
import org.bloomreach.forge.webarchiving.cms.util.HSTServicesAwarePlatformManaged;
import org.bloomreach.forge.webarchiving.cms.util.ModuleSessionAware;
import org.bloomreach.forge.webarchiving.cms.util.PlatformManaged;
import org.bloomreach.forge.webarchiving.common.api.HstUrlService;
import org.bloomreach.forge.webarchiving.common.api.WebArchiveManager;
import org.bloomreach.forge.webarchiving.common.api.WebArchiveUpdateJobsManager;
import org.bloomreach.forge.webarchiving.common.error.WebArchivingException;
import org.hippoecm.hst.core.internal.PlatformModelAvailableService;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.cms7.services.ProxiedServiceTracker;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO It doesn't know until runtime which services are Discoverable Registers services for the web archiving addon
 */
@ProvidesService(types = {
        WebArchiveUpdateJobsManager.class,
        WebArchiveManager.class
})
public class WebArchivingServicesDaemonModule extends AbstractReconfigurableDaemonModule {
    private static Logger log = LoggerFactory.getLogger(WebArchivingServicesDaemonModule.class);

    private static final String WEB_ARCHIVE_MANAGER_CONFIG_LOCATION = "archivemanager";
    private static final String WEB_ARCHIVE_UPDATE_JOBS_MANAGER_CONFIG_LOCATION = "updatesmanager";
    private static final String WEB_ARCHIVE_HST_URL_SERVICE_CONFIG_LOCATION = "hsturlservice";

    private static final String CLASS_NAME = "className";

    private final Object configurationLock = new Object();

    private String environment;

    private WebArchiveUpdateJobsManager webArchiveUpdateJobsManager;
    private WebArchiveManager webArchiveManager;
    private HstUrlService hstUrlService;
    private Map<HSTServicesAwarePlatformManaged, ProxiedServiceTracker<PlatformModelAvailableService>> hstPlatformModelAvailableServiceTrackers = new HashMap<>();


    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        log.debug("Initialized {}...", WebArchivingServicesDaemonModule.class.getName());
    }

    @Override
    protected void doShutdown() {
        shutdownServices();
        log.debug("Shut down {}...", WebArchivingServicesDaemonModule.class.getName());
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {

        this.environment = CmsUtils.getEnvironment(moduleConfig);
        log.info("Configuring {} for environment '{}'", WebArchivingServicesDaemonModule.class.getName(), environment);

        synchronized (configurationLock) {
            shutdownServices();
            try {
                initializeServices(moduleConfig);
            } catch (Exception e) {
                log.error("Error while initializing web-archiving-addon services:", e);
            }
        }
    }

    private void shutdownServices() {
        shutdownService(webArchiveUpdateJobsManager, WebArchiveUpdateJobsManager.class);
        shutdownService(webArchiveManager, WebArchiveManager.class);
        shutdownService(hstUrlService, HstUrlService.class);
    }

    private void initializeServices(final Node moduleConfig) throws RepositoryException, WebArchivingException {
        webArchiveUpdateJobsManager = initializeService(getServiceConfigNode(moduleConfig, WEB_ARCHIVE_UPDATE_JOBS_MANAGER_CONFIG_LOCATION), WebArchiveUpdateJobsManager.class);
        webArchiveManager = initializeService(getServiceConfigNode(moduleConfig, WEB_ARCHIVE_MANAGER_CONFIG_LOCATION), WebArchiveManager.class);
        hstUrlService = initializeService(getServiceConfigNode(moduleConfig, WEB_ARCHIVE_HST_URL_SERVICE_CONFIG_LOCATION), HstUrlService.class);
    }

    private <T> T initializeService(final Node serviceConfigNode, final Class<T> serviceInterface) throws RepositoryException, WebArchivingException {
        Map<String, String> config = CmsUtils.getServiceConfiguration(serviceConfigNode, this.environment);
        log.debug("Got service configuration {} for environment {}", config, this.environment);

        String className = config.get(CLASS_NAME);
        if (StringUtils.isBlank(className)) {
            throw new WebArchivingException("Property '{}' not present or empty in configuration {}", CLASS_NAME, serviceConfigNode.getPath());
        }

        T service;
        try {
            final Class<? extends T> moduleClass = (Class<? extends T>) Class.forName(className);
            service = moduleClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new WebArchivingException(e);
        }

        if (service instanceof ModuleSessionAware) {
            ((ModuleSessionAware) service).setModuleSession(serviceConfigNode.getSession());
        }

        if (service instanceof HSTServicesAwarePlatformManaged) {
            ProxiedServiceTracker<PlatformModelAvailableService> hstPlatformModelAvailableServiceTracker = new ProxiedServiceTracker<PlatformModelAvailableService>() {
                @Override
                public void serviceRegistered(final ProxiedServiceHolder<PlatformModelAvailableService> serviceHolder) {
                    try {
                        ((HSTServicesAwarePlatformManaged) service).initialize(config);
                    } catch (WebArchivingException e) {
                        log.error("Error while initializing web-archiving-addon services:", e);
                    }
                }

                @Override
                public void serviceUnregistered(final ProxiedServiceHolder<PlatformModelAvailableService> serviceHolder) {
                    ((HSTServicesAwarePlatformManaged) service).destroy();
                }
            };

            HippoServiceRegistry.addTracker(hstPlatformModelAvailableServiceTracker, PlatformModelAvailableService.class);
            hstPlatformModelAvailableServiceTrackers.put((HSTServicesAwarePlatformManaged) service, hstPlatformModelAvailableServiceTracker);

        } else if (service instanceof PlatformManaged ) {
            ((PlatformManaged) service).initialize(config);
        }

        if (service instanceof Discoverable) {
            HippoServiceRegistry.register(service, serviceInterface);
        }
        return service;
    }


    private Node getServiceConfigNode(final Node moduleConfig, final String configPath) throws RepositoryException, WebArchivingException {
        if (moduleConfig.hasNode(configPath)) {
            return moduleConfig.getNode(configPath);
        } else {
            throw new WebArchivingException("Config node '{}' not found below {}", configPath, moduleConfig.getPath());
        }
    }

    private <T, S extends T> void shutdownService(S service, Class<T> serviceInterface) {
        if (service != null) {
            if (service instanceof HSTServicesAwarePlatformManaged) {
                HippoServiceRegistry.removeTracker(hstPlatformModelAvailableServiceTrackers.get(service), PlatformModelAvailableService.class);
            }

            if (service instanceof PlatformManaged) {
                ((PlatformManaged) service).destroy();
            }

            if (service instanceof Discoverable) {
                HippoServiceRegistry.unregister(service, serviceInterface);
            }
        }
    }
}
