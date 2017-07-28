package org.apereo.cas.configuration.model.core.authentication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This is {@link StubPrincipalAttributesProperties}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
public class StubPrincipalAttributesProperties implements Serializable {

    /**
     * Static attributes that need to be mapped to a hardcoded value belong here.
     * The structure follows a key-value pair where key is the attribute name
     * and value is the attribute value. The key is the attribute fetched
     * from the source and the value is the attribute name CAS should
     * use for virtual renames.
     */
    private Map<String, String> attributes = new HashMap();

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(final Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
