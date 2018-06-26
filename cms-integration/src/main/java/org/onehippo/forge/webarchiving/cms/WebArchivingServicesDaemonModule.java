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
import org.onehippo.forge.webarchiving.common.api.WebArchiveManager;
import org.onehippo.forge.webarchiving.common.api.WebArchiveUpdateJobsManager;
import org.onehippo.forge.webarchiving.cms.util.ModuleSessionAware;
import org.onehippo.forge.webarchiving.cms.util.LifeCycle;
import org.onehippo.forge.webarchiving.cms.util.CmsUtils;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers services for the web archiving addon
 */
@ProvidesService(types = {
    WebArchiveUpdateJobsManager.class,
    WebArchiveManager.class
})
public class WebArchivingServicesDaemonModule extends AbstractReconfigurableDaemonModule {
    private static Logger log = LoggerFactory.getLogger(WebArchivingServicesDaemonModule.class);

    private static final String WEB_ARCHIVE_MANAGER_CONFIG_LOCATION = "archivemanager";
    private static final String WEB_ARCHIVE_UPDATE_JOBS_MANAGER_CONFIG_LOCATION = "updatesmanager";

    private static final String CLASS_NAME = "className";

    private final Object configurationLock = new Object();

    private String environment;

    private WebArchiveUpdateJobsManager webArchiveUpdateJobsManager;
    private WebArchiveManager webArchiveManager;

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        log.debug("Initialized {}...", WebArchivingServicesDaemonModule.class.getName());
    }

    @Override
    protected void doShutdown() {
        unregisterServices();
        log.debug("Shut down {}...", WebArchivingServicesDaemonModule.class.getName());
    }

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {

        this.environment = CmsUtils.getEnvironment(moduleConfig);
        log.info("Configuring {} for environment '{}'", WebArchivingServicesDaemonModule.class.getName(), environment);

        synchronized (configurationLock) {
            unregisterServices();
            registerServices(moduleConfig);
        }
    }

    private void unregisterServices() {
        unregisterService(webArchiveUpdateJobsManager, WebArchiveUpdateJobsManager.class);
        unregisterService(webArchiveManager, WebArchiveManager.class);
    }

    private void registerServices(final Node moduleConfig) {
        webArchiveUpdateJobsManager = registerService(moduleConfig, WEB_ARCHIVE_UPDATE_JOBS_MANAGER_CONFIG_LOCATION, WebArchiveUpdateJobsManager.class);
        webArchiveManager = registerService(moduleConfig, WEB_ARCHIVE_MANAGER_CONFIG_LOCATION, WebArchiveManager.class);
    }

    private <T> T registerService(final Node moduleConfig, final String configPath, final Class<T> serviceInterface) {

        String className = null;
        try {
            T service = HippoServiceRegistry.getService(serviceInterface);
            if (service != null) {
                log.info("Service {} already registered, skip registering from {}/{})", serviceInterface.getName(), moduleConfig.getPath(), configPath);
                return service;
            }

            if (moduleConfig.hasNode(configPath)) {
                final Node serviceConfig = moduleConfig.getNode(configPath);

                final Map<String, String> config = CmsUtils.getServiceConfiguration(serviceConfig, this.environment);
                log.debug("Got service configuration {} for environment {}", config, this.environment);

                className = config.get(CLASS_NAME);
                if (StringUtils.isNotBlank(className)) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends T> moduleClass = (Class<? extends T>) Class.forName(className);
                    service = moduleClass.getConstructor().newInstance();

                    if (service instanceof ModuleSessionAware) {
                        ((ModuleSessionAware) service).setModuleSession(moduleConfig.getSession());
                    }

                    if (service instanceof LifeCycle) {
                        ((LifeCycle) service).initialize(config);
                    }

                    HippoServiceRegistry.registerService(service, serviceInterface);
                    return service;
                } else {
                    log.warn("Property '{}' not present or empty at node {}/{}", CLASS_NAME, moduleConfig.getPath(), configPath);
                }
            } else {
                log.warn("Config node '{}' not found below {}", configPath, moduleConfig.getPath());
            }
        } catch (Exception e) {
            log.error(e.getClass().getSimpleName() + " while registering, initializing service "
                + serviceInterface.getSimpleName() + " from path " + configPath + ", className=" + className, e);
        }
        return null;
    }

    private <T> void unregisterService(T module, Class<T> serviceInterface) {
        if (module != null) {
            if (module instanceof LifeCycle) {
                ((LifeCycle) module).destroy();
            }

            HippoServiceRegistry.unregisterService(module, serviceInterface);
        }
    }
}
