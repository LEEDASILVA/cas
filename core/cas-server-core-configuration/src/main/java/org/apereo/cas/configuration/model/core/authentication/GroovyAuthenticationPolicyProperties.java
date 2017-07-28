package org.apereo.cas.configuration.model.core.authentication;

import java.io.Serializable;

/**
 * This is {@link GroovyAuthenticationPolicyProperties}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
public class GroovyAuthenticationPolicyProperties implements Serializable {
    /**
     * Path to the groovy script to execute.
     */
    private String script;

    public String getScript() {
        return script;
    }

    public void setScript(final String script) {
        this.script = script;
    }
}
