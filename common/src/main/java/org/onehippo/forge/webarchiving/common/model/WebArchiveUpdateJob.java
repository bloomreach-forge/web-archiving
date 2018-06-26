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

package org.onehippo.forge.webarchiving.common.model;

import java.io.Serializable;
import java.util.Calendar;

/**
 * A web archive update job containing exactly one {@link WebArchiveUpdate} object.
 * This class takes care of maintaining status for this update and scheduling related information.
 * The contained {@link WebArchiveUpdate} object acts as the payload of this job
 */
public class WebArchiveUpdateJob implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Web archive update identifier.
     */
    private String id;

    /**
     * The update creation date time.
     */
    private Calendar created;

    /**
     * The last modified date time of this update.
     */
    private Calendar lastModified;

    /**
     * Web archive update included in this update.
     */
    private WebArchiveUpdate webArchiveUpdate;

    /**
     * Web archive update job status.
     */
    private WebArchiveUpdateJobStatus status;

    /**
     * Number of attempt to submit this job
     */
    private long attempt = 0;

    public WebArchiveUpdateJob() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(final Calendar created) {
        this.created = created;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(final Calendar lastModified) {
        this.lastModified = lastModified;
    }

    public WebArchiveUpdate getWebArchiveUpdate() {
        return webArchiveUpdate;
    }

    public void setWebArchiveUpdate(final WebArchiveUpdate webArchiveUpdate) {
        this.webArchiveUpdate = webArchiveUpdate;
    }

    public WebArchiveUpdateJobStatus getStatus() {
        return status;
    }

    public void setStatus(final WebArchiveUpdateJobStatus status) {
        this.status = status;
    }

    public long getAttempt() {
        return attempt;
    }

    public void setAttempt(final long attempt) {
        this.attempt = attempt;
    }

    @Override
    public String toString() {
        return "WebArchiveUpdateJob{" +
            "id='" + id + '\'' +
            ", created=" + created.getTime() +
            ", lastModified=" + lastModified.getTime() +
            ", webArchiveUpdate=" + webArchiveUpdate +
            ", status=" + status +
            ", attempt=" + attempt +
            '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebArchiveUpdateJob)) {
            return false;
        }

        final WebArchiveUpdateJob that = (WebArchiveUpdateJob) o;

        if (attempt != that.attempt) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (created != null ? !created.equals(that.created) : that.created != null) {
            return false;
        }
        if (lastModified != null ? !lastModified.equals(that.lastModified) : that.lastModified != null) {
            return false;
        }
        if (webArchiveUpdate != null ? !webArchiveUpdate.equals(that.webArchiveUpdate) : that.webArchiveUpdate != null) {
            return false;
        }
        return status == that.status;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
        result = 31 * result + (webArchiveUpdate != null ? webArchiveUpdate.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (int) (attempt ^ (attempt >>> 32));
        return result;
    }
}
