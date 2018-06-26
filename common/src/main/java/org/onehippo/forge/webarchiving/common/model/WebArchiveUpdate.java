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
import java.util.LinkedList;
import java.util.List;

/**
 * A web archive update contains all the information needed by an Archive manager to execute an archive update
 */
public class WebArchiveUpdate implements Serializable {

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
     * Creator user name.
     */
    private String creator;

    /**
     * URLs modified due to this update
     */
    private List<String> urls = new LinkedList<>();

    private WebArchiveUpdateType type;

    public WebArchiveUpdate() {
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(final List<String> urls) {
        this.urls = urls;
    }

    public WebArchiveUpdateType getType() {
        return type;
    }

    public void setType(final WebArchiveUpdateType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "WebArchiveUpdate{" +
            "id='" + id + '\'' +
            ", created=" + created.getTime() +
            ", creator='" + creator + '\'' +
            ", urls=" + urls +
            ", type=" + type +
            '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebArchiveUpdate)) {
            return false;
        }

        final WebArchiveUpdate that = (WebArchiveUpdate) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (created != null ? !created.equals(that.created) : that.created != null) {
            return false;
        }
        if (creator != null ? !creator.equals(that.creator) : that.creator != null) {
            return false;
        }
        if (urls != null ? !urls.equals(that.urls) : that.urls != null) {
            return false;
        }
        return type == that.type;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (creator != null ? creator.hashCode() : 0);
        result = 31 * result + (urls != null ? urls.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
