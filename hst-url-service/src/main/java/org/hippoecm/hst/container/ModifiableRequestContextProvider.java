package org.hippoecm.hst.container;

import org.hippoecm.hst.core.request.HstRequestContext;

public final class ModifiableRequestContextProvider {
    private ModifiableRequestContextProvider() {
    }

    public static HstRequestContext get() {
        return RequestContextProvider.get();
    }

    public static void set(HstRequestContext requestContext) {
        RequestContextProvider.set(requestContext);
    }

    public static void clear() {
        RequestContextProvider.clear();
    }
}