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

package org.bloomreach.forge.webarchiving.updatesstore.jcr;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.search.query.OrderClause;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.cms7.services.search.query.TypedQuery;
import org.onehippo.cms7.services.search.query.WhereClause;
import org.onehippo.cms7.services.search.query.constraint.AndConstraint;
import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.OrConstraint;
import org.onehippo.cms7.services.search.query.constraint.TextConstraint;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.onehippo.cms7.services.search.service.SearchServiceFactory;
import org.bloomreach.forge.webarchiving.cms.util.Discoverable;
import org.bloomreach.forge.webarchiving.cms.util.LifeCycle;
import org.bloomreach.forge.webarchiving.cms.util.ModuleSessionAware;
import org.bloomreach.forge.webarchiving.common.api.WebArchiveUpdateJobsManager;
import org.bloomreach.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdate;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateJob;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateJobStatus;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link WebArchiveUpdateJobsManager} implementation which stores data into JCR repository.
 */
public class JcrWebArchiveUpdateJobsManager implements WebArchiveUpdateJobsManager, ModuleSessionAware, LifeCycle, Discoverable {

    private static Logger log = LoggerFactory.getLogger(JcrWebArchiveUpdateJobsManager.class);

    protected static final String DEFAULT_WEB_ARCHIVE_UPDATE_JOBSTORE_LOCATION = WebArchivingConstants.NS_WEB_ARCHIVING_ADDON + "updatesstore";
    protected static final String CONFIG_PROP_BATCH_SIZE = "batchSize";
    protected static final int DEFAULT_BATCH_SIZE = 100;

    private static final Object mutex = new Object();
    protected Session moduleSession;
    protected long batchSize = DEFAULT_BATCH_SIZE;

    @Override
    public synchronized void initialize(final Map<String, String> props) throws WebArchiveUpdateException {
        String value = props.get(CONFIG_PROP_BATCH_SIZE);
        try {
            batchSize = Long.parseLong(value);
            if (batchSize < DEFAULT_BATCH_SIZE) {
                batchSize = DEFAULT_BATCH_SIZE;
            }
        } catch (NumberFormatException e) {
            log.warn("Incorrect number '{}'. Setting to default '{}'", value, DEFAULT_BATCH_SIZE);
            batchSize = DEFAULT_BATCH_SIZE;
        }
    }

    @Override
    public void destroy() {
        log.debug("Destroying {}", this.getClass().getName());
    }

    @Override
    public void setModuleSession(Session session) {
        this.moduleSession = session;
    }

    @Override
    public String createWebArchiveUpdateJob(WebArchiveUpdateJob webArchiveUpdateJob) throws WebArchiveUpdateException {
        String updateJobId;
        Session session = null;
        try {
            WebArchiveUpdate webArchiveUpdate = webArchiveUpdateJob.getWebArchiveUpdate();
            if (webArchiveUpdate == null) {
                throw new WebArchiveUpdateException("Job does not contain an update {}", webArchiveUpdateJob);
            }

            session = getSession();
            Node jobNode = createJobNode(session);
            bindWebArchiveUpdateJobNode(jobNode, webArchiveUpdateJob);

            updateJobId = jobNode.getName();
            webArchiveUpdateJob.setId(updateJobId);

            createWebArchiveUpdate(jobNode, webArchiveUpdate);

        } catch (RepositoryException e) {
            refreshSession(session);
            throw new WebArchiveUpdateException(e, "Error while creating job {}", webArchiveUpdateJob);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return updateJobId;
    }

    @Override
    public WebArchiveUpdateJob getWebArchiveUpdateJobById(String webArchiveUpdateJobId) throws WebArchiveUpdateException {
        WebArchiveUpdateJob job = new WebArchiveUpdateJob();
        Session session = null;
        try {
            session = getSession();
            Node jobNode = getJobsStoreNode(session).getNode(webArchiveUpdateJobId);
            mapWebArchiveJob(jobNode, job);
        } catch (RepositoryException e) {
            throw new WebArchiveUpdateException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return job;
    }

    @Override
    public void updateWebArchiveUpdateJob(WebArchiveUpdateJob webArchiveUpdateJob) throws WebArchiveUpdateException {
        if (StringUtils.isBlank(webArchiveUpdateJob.getId())) {
            throw new WebArchiveUpdateException("No identifier in job {}", webArchiveUpdateJob);
        }

        Session session = null;
        try {
            session = getSession();
            Node jobNode = getJobsStoreNode(session).getNode(webArchiveUpdateJob.getId());

            if (!jobNode.isNodeType("mix:referenceable")) {
                jobNode.addMixin("mix:referenceable");
            }

            bindWebArchiveUpdateJobNode(jobNode, webArchiveUpdateJob);
            session.save();

        } catch (RepositoryException e) {
            refreshSession(session);
            throw new WebArchiveUpdateException(e, "Error while creating job {}", webArchiveUpdateJob);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public void deleteWebArchiveUpdateJobs(WebArchiveUpdateJob... webArchiveUpdateJobs) throws WebArchiveUpdateException {
        int batchCount = 0;
        int totalJobsDeleted = 0;
        final StringBuilder infoText = new StringBuilder("Deleted web archive update jobs:\n");
        Session session = null;

        try {
            session = getSession();
            for (WebArchiveUpdateJob job : webArchiveUpdateJobs) {
                try {
                    Node node = getJobsStoreNode(session).getNode(job.getId());
                    log.debug("Removing node {}", node.getPath());
                    node.remove();

                    infoText.append("  ").append(job.getId()).append("\n");
                    totalJobsDeleted++;
                    batchCount++;
                } catch (RepositoryException e2) {
                    log.error("Error while deleting job {}, message={}", job.getId(), e2.getMessage());
                }

                if (batchCount >= batchSize) {
                    log.debug("Saving a batch of {} after a total of {} deletions", batchCount, totalJobsDeleted);
                    batchCount = 0;
                    session.save();

                    // pause for the save to be propagated (cluster, Lucene)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            // last batch save
            if (session.hasPendingChanges()) {
                session.save();
            }
            if (totalJobsDeleted > 0) {
                log.info(infoText.toString());
            }
        } catch (RepositoryException e) {
            refreshSession(session);
            throw new WebArchiveUpdateException(e,
                "Error while deleting job {}",
                Arrays.stream(webArchiveUpdateJobs).map(WebArchiveUpdateJob::getId).collect(Collectors.toList()));
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public List<WebArchiveUpdateJob> searchForWebArchiveUpdateJobs(List<WebArchiveUpdateJobStatus> statuses, Map<String, String> searchFilters, int offset,
                                                                   int limit, String orderByPropertyHint, boolean ascending) throws WebArchiveUpdateException {
        final SearchServiceFactory searchServiceFactory = HippoServiceRegistry.getService(SearchServiceFactory.class);
        if (searchServiceFactory == null) {
            throw new SearchServiceException("Cannot find service by name " + SearchServiceFactory.class.getName());
        }

        List<WebArchiveUpdateJob> jobs = new LinkedList<>();

        if (limit > 0) {
            Session session = null;
            try {
                session = getSession();
                final SearchService searchService = searchServiceFactory.createSearchService(session);
                Query query = createSearchQuery(searchService, WebArchivingConstants.NT_WEB_ARCHIVE_UPDATE_JOB, statuses, searchFilters);

                if (StringUtils.isNotBlank(orderByPropertyHint)) {
                    query = query.orderBy(orderByPropertyHint);
                    query = ascending ? ((OrderClause) query).ascending() : ((OrderClause) query).descending();
                } else {
                    query = query.orderBy(WebArchivingConstants.PROP_LAST_MODIFIED).descending();
                }

                if (offset > 0) {
                    query = query.offsetBy(offset);
                }
                if (limit > 0) {
                    query = query.limitTo(limit);
                }

                QueryResult result = searchService.search(query);
                for (final HitIterator hits = result.getHits(); hits.hasNext(); ) {
                    final Hit hit = hits.next();
                    final String uuid = hit.getSearchDocument().getContentId().toIdentifier();
                    final WebArchiveUpdateJob job = getWebArchiveUpdateJobByUuid(uuid, session);
                    jobs.add(job);
                }
            } catch (SearchServiceException | RepositoryException e) {
                throw new WebArchiveUpdateException(e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
        return jobs;
    }

    protected Node createJobNode(final Session session) throws RepositoryException {
        Node updateJobsContainerNode = getJobsStoreNode(session);

        String jobNodeName = "job_" + System.currentTimeMillis();
        Random rand = new Random();
        while (updateJobsContainerNode.hasNode(jobNodeName)) {
            jobNodeName += "_" + rand.nextInt(10);
        }
        Node jobNode = updateJobsContainerNode.addNode(jobNodeName, WebArchivingConstants.NT_WEB_ARCHIVE_UPDATE_JOB);

        if (!jobNode.isNodeType("mix:referenceable")) {
            jobNode.addMixin("mix:referenceable");
        }
        return jobNode;
    }

    protected void bindWebArchiveUpdateJobNode(final Node jobNode, final WebArchiveUpdateJob job) throws RepositoryException {
        jobNode.setProperty(WebArchivingConstants.PROP_STATUS, job.getStatus() != null ? job.getStatus().name() : WebArchiveUpdateJobStatus.UNDEFINED.name());
        jobNode.setProperty(WebArchivingConstants.PROP_ATTEMPT, job.getAttempt());
        jobNode.setProperty(WebArchivingConstants.PROP_CREATED, job.getCreated());
        jobNode.setProperty(WebArchivingConstants.PROP_LAST_MODIFIED, job.getLastModified());
    }

    protected void createWebArchiveUpdate(final Node jobNode, final WebArchiveUpdate update) throws RepositoryException {
        Node updateNode = jobNode.addNode(WebArchivingConstants.NS_WEB_ARCHIVING_ADDON + "update", WebArchivingConstants.NT_WEB_ARCHIVE_UPDATE);

        if (!updateNode.isNodeType("mix:referenceable")) {
            updateNode.addMixin("mix:referenceable");
        }

        bindWebArchiveUpdateNode(updateNode, update);

        updateNode.getSession().save();
        String jobItemId = updateNode.getIdentifier();
        update.setId(jobItemId);
    }

    protected void bindWebArchiveUpdateNode(final Node updateNode, final WebArchiveUpdate update) throws RepositoryException {
        updateNode.setProperty(WebArchivingConstants.PROP_CREATED, update.getCreated());
        updateNode.setProperty(WebArchivingConstants.PROP_CREATOR, StringUtils.defaultIfEmpty(update.getCreator(), ""));
        updateNode.setProperty(WebArchivingConstants.PROP_ID, StringUtils.defaultIfEmpty(update.getId(), ""));
        updateNode.setProperty(WebArchivingConstants.PROP_TYPE, update.getType() != null ? update.getType().name() : WebArchiveUpdateType.UNDEFINED.name());
        updateNode.setProperty(WebArchivingConstants.PROP_URLS, update.getUrls().toArray(new String[0]));
    }

    protected WebArchiveUpdateJob getWebArchiveUpdateJobByUuid(final String jobUuid, final Session session) throws RepositoryException {
        Node jobNode = session.getNodeByIdentifier(jobUuid);
        WebArchiveUpdateJob job = new WebArchiveUpdateJob();
        mapWebArchiveJob(jobNode, job);
        return job;
    }

    protected void mapWebArchiveJob(final Node jobNode, WebArchiveUpdateJob job) throws RepositoryException {
        job.setId(jobNode.getName());
        job.setCreated(JcrUtils.getDateProperty(jobNode, WebArchivingConstants.PROP_CREATED, null));
        job.setLastModified(JcrUtils.getDateProperty(jobNode, WebArchivingConstants.PROP_LAST_MODIFIED, null));
        job.setStatus(mapStatus(jobNode, WebArchivingConstants.PROP_STATUS));
        job.setAttempt(JcrUtils.getLongProperty(jobNode, WebArchivingConstants.PROP_ATTEMPT, 0L));

        Node updateNode = jobNode.getNode(WebArchivingConstants.NS_WEB_ARCHIVING_ADDON + "update");
        if (updateNode.isNodeType(WebArchivingConstants.NT_WEB_ARCHIVE_UPDATE)) {
            WebArchiveUpdate update = new WebArchiveUpdate();
            update.setId(JcrUtils.getStringProperty(updateNode, WebArchivingConstants.PROP_ID, null));
            update.setCreated(JcrUtils.getDateProperty(updateNode, WebArchivingConstants.PROP_CREATED, null));
            update.setCreator(JcrUtils.getStringProperty(updateNode, WebArchivingConstants.PROP_CREATOR, null));
            update.setType(mapType(updateNode, WebArchivingConstants.PROP_TYPE));
            update.setUrls(Arrays.asList(JcrUtils.getMultipleStringProperty(updateNode, WebArchivingConstants.PROP_URLS, null)));
            job.setWebArchiveUpdate(update);
        }
    }

    protected Query createSearchQuery(final SearchService searchService, final String webArchiveUpdateJobNodeType, final List<WebArchiveUpdateJobStatus> statuses,
                                      final Map<String, String> searchFilters) throws RepositoryException {

        TypedQuery typedQuery = searchService.createQuery().ofType(webArchiveUpdateJobNodeType);
        WhereClause where = null;

        // combine any statuses with an OR
        if (statuses != null && !statuses.isEmpty()) {

            Constraint statusConstraint;

            if (statuses.size() == 1) {
                statusConstraint = QueryUtils.text(WebArchivingConstants.PROP_STATUS).isEqualTo(statuses.get(0).name());
            } else {
                OrConstraint or = null;
                for (WebArchiveUpdateJobStatus status : statuses) {
                    final TextConstraint textConstraint = QueryUtils.text(WebArchivingConstants.PROP_STATUS).isEqualTo(status.name());
                    if (or == null) {
                        or = QueryUtils.either(textConstraint);
                    } else {
                        or = or.or(textConstraint);
                    }
                }
                statusConstraint = or;
            }
            log.debug("Adding 'where' status constraint {}", statusConstraint);
            where = typedQuery.where(statusConstraint);
        }

        if (searchFilters != null && !searchFilters.isEmpty()) {
            Constraint filterConstraint = null;
            AndConstraint and = null;
            for (String attrName : searchFilters.keySet()) {

                String propName = WebArchivingConstants.NS_WEB_ARCHIVING_ADDON + attrName.toLowerCase();
                if (!WebArchivingConstants.BUILTIN_JOB_PROP_NAMES.contains(propName)) {
                    propName = WebArchivingConstants.NS_WEB_ARCHIVING_ADDON + attrName;
                }

                final String value = searchFilters.get(attrName);
                Constraint constraint = QueryUtils.text(propName).isEqualTo(value);
                if (searchFilters.size() == 1) {
                    filterConstraint = constraint;
                } else {
                    if (and == null) {
                        and = QueryUtils.both(constraint);
                    } else {
                        and = and.and(constraint);
                    }
                    filterConstraint = and;
                }
            }

            if (where == null) {
                log.debug("Adding 'where' filter constraint for {} filters: {}", searchFilters.size(), filterConstraint);
                return typedQuery.where(filterConstraint);
            } else {
                log.debug("ANDing existing 'where' status constraint with filter constraint for {} filters: {}", searchFilters.size(), filterConstraint);
                return where.and(filterConstraint);
            }
        }

        if (where != null) {
            return where;
        }

        log.debug("Returning typed query without 'where' clause: {}", typedQuery);
        return typedQuery;
    }

    protected void refreshSession(final Session session) {
        try {
            if (session != null) {
                session.refresh(false);
            }
        } catch (RepositoryException e) {
            log.error("Failed to refresh.", e);
        }
    }

    protected Session getSession() throws RepositoryException {
        if (moduleSession != null) {
            return moduleSession.impersonate(getSystemCredentials());
        }
        throw new RepositoryException("ModuleSession is null");
    }

    protected Credentials getSystemCredentials() {
        return new SimpleCredentials("system", new char[]{});
    }

    protected Node getJobsStoreNode(final Session session) throws RepositoryException {
        Node rootNode = session.getRootNode();
        Node updatesStoreNode;
        synchronized (mutex) {
            if (!rootNode.hasNode(DEFAULT_WEB_ARCHIVE_UPDATE_JOBSTORE_LOCATION)) {
                updatesStoreNode = rootNode.addNode(DEFAULT_WEB_ARCHIVE_UPDATE_JOBSTORE_LOCATION, WebArchivingConstants.NT_WEB_ARCHIVE_UPDATE_JOBS_CONTAINER);
            } else {
                updatesStoreNode = rootNode.getNode(DEFAULT_WEB_ARCHIVE_UPDATE_JOBSTORE_LOCATION);
            }
        }
        return updatesStoreNode;
    }

    protected WebArchiveUpdateJobStatus mapStatus(final Node node, final String statusProperty) throws RepositoryException {
        WebArchiveUpdateJobStatus status = WebArchiveUpdateJobStatus.UNDEFINED;
        try {
            status = WebArchiveUpdateJobStatus.valueOf(JcrUtils.getStringProperty(node, statusProperty, ""));
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid Status value", e);
            } else {
                log.warn("Invalid Status value: {}", e.toString());
            }
        }
        return status;
    }

    protected WebArchiveUpdateType mapType(final Node node, final String typeProperty) throws RepositoryException {
        WebArchiveUpdateType type = WebArchiveUpdateType.UNDEFINED;
        try {
            type = WebArchiveUpdateType.valueOf(JcrUtils.getStringProperty(node, typeProperty, ""));
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.warn("Invalid Type value", e);
            } else {
                log.warn("Invalid Type value: {}", e.toString());
            }
        }
        return type;
    }

}
