package org.onehippo.forge.webarchiving.hst.util;

import java.util.Map;

import org.hippoecm.hst.core.container.HstContainerURL;

public class LocalHstContainerURL implements HstContainerURL {

    private final String host;
    private final int port;
    private final String contextPath;
    private final String requestPath;
    private final String resolvedMountPath;

    public LocalHstContainerURL(final String host, final int port, final String contextPath, final String requestPath, final String resolvedMountPath){
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
        this.requestPath = requestPath;
        this.resolvedMountPath = resolvedMountPath;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getHostName() {
        return host;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getRequestPath() {
        return requestPath;
    }

    @Override
    public int getPortNumber() {
        return port;
    }

    @Override
    public String getResolvedMountPath() {
        return resolvedMountPath;
    }

    @Override
    public String getPathInfo() {
        //TODO HARD CODED
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

}
