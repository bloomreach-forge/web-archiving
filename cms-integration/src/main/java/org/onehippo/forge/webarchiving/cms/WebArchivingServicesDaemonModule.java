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

package org.onehippo.forge.webarchiving.cms;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.webarchiving.cms.util.CmsUtils;
import org.onehippo.forge.webarchiving.cms.util.Discoverable;
import org.onehippo.forge.webarchiving.cms.util.LifeCycle;
import org.onehippo.forge.webarchiving.cms.util.ModuleSessionAware;
import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.error.WebArchivingException;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO It doesn't know until runtime which services are Discoverable
 * Registers services for the web archiving addon
 */
@ProvidesService(types = {
    WebArchiveManager.class
})
public class WebArchivingServicesDaemonModule extends AbstractReconfigurableDaemonModule {
    private static Logger log = LoggerFactory.getLogger(WebArchivingServicesDaemonModule.class);

    private static final String WEB_ARCHIVE_MANAGER_CONFIG_LOCATION = "archivemanager";
    private static final String CLASS_NAME = "className";
    private final Object configurationLock = new Object();
    private String environment;
    private WebArchiveManager webArchiveManager;

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
        shutdownService(webArchiveManager, WebArchiveManager.class);
    }

    private void initializeServices(final Node moduleConfig) throws RepositoryException, WebArchivingException {
        webArchiveManager = initializeService(getServiceConfigNode(moduleConfig, WEB_ARCHIVE_MANAGER_CONFIG_LOCATION), WebArchiveManager.class);
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

        if (service instanceof LifeCycle) {
            ((LifeCycle) service).initialize(config);
        }

        if (service instanceof Discoverable) {
            HippoServiceRegistry.registerService(service, serviceInterface);
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
            if (service instanceof LifeCycle) {
                ((LifeCycle) service).destroy();
            }

            if (service instanceof Discoverable) {
                HippoServiceRegistry.unregisterService(service, serviceInterface);
            }
        }
    }
}
