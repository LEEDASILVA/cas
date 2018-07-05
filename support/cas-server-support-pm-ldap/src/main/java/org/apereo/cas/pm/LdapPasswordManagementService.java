package org.apereo.cas.pm;

import lombok.val;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apereo.cas.CipherExecutor;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;

import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LdapUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * This is {@link LdapPasswordManagementService}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
public class LdapPasswordManagementService extends BasePasswordManagementService {
    public LdapPasswordManagementService(final CipherExecutor<Serializable, String> cipherExecutor,
                                         final String issuer,
                                         final PasswordManagementProperties passwordManagementProperties) {
        super(passwordManagementProperties, cipherExecutor, issuer);
    }

    @Override
    public String findEmail(final String username) {
        try {
            val ldap = properties.getLdap();
            val filter = LdapUtils.newLdaptiveSearchFilter(ldap.getSearchFilter(),
                LdapUtils.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME,
                CollectionUtils.wrap(username));
            LOGGER.debug("Constructed LDAP filter [{}] to locate account email", filter);

            val factory = LdapUtils.newLdaptivePooledConnectionFactory(ldap);
            val response = LdapUtils.executeSearchOperation(factory, ldap.getBaseDn(), filter);
            LOGGER.debug("LDAP response to locate account email is [{}]", response);

            if (LdapUtils.containsResultEntry(response)) {
                val entry = response.getResult().getEntry();
                LOGGER.debug("Found LDAP entry [{}] to use for the account email", entry);

                val attributeName = properties.getReset().getMail().getAttributeName();
                val attr = entry.getAttribute(attributeName);
                if (attr != null) {
                    val email = attr.getStringValue();
                    LOGGER.debug("Found email address [{}] for user [{}]. Validating...", email, username);
                    if (EmailValidator.getInstance().isValid(email)) {
                        LOGGER.debug("Email address [{}] matches a valid email address", email);
                        return email;
                    }
                    LOGGER.error("Email [{}] is not a valid address", email);
                } else {
                    LOGGER.error("Could not locate an LDAP attribute [{}] for [{}] and base DN [{}]",
                        attributeName, filter.format(), ldap.getBaseDn());
                }
                return null;
            }
            LOGGER.error("Could not locate an LDAP entry for [{}] and base DN [{}]", filter.format(), ldap.getBaseDn());
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean changeInternal(final Credential credential, final PasswordChangeBean bean) {
        try {
            val ldap = properties.getLdap();
            val c = (UsernamePasswordCredential) credential;

            val filter = LdapUtils.newLdaptiveSearchFilter(ldap.getSearchFilter(),
                LdapUtils.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME,
                CollectionUtils.wrap(c.getId()));
            LOGGER.debug("Constructed LDAP filter [{}] to update account password", filter);

            val factory = LdapUtils.newLdaptivePooledConnectionFactory(ldap);
            val response = LdapUtils.executeSearchOperation(factory, ldap.getBaseDn(), filter);
            LOGGER.debug("LDAP response to update password is [{}]", response);

            if (LdapUtils.containsResultEntry(response)) {
                val dn = response.getResult().getEntry().getDn();
                LOGGER.debug("Updating account password for [{}]", dn);
                if (LdapUtils.executePasswordModifyOperation(dn, factory, c.getPassword(), bean.getPassword(),
                    properties.getLdap().getType())) {
                    LOGGER.debug("Successfully updated the account password for [{}]", dn);
                    return true;
                }
                LOGGER.error("Could not update the LDAP entry's password for [{}] and base DN [{}]", filter.format(), ldap.getBaseDn());
            } else {
                LOGGER.error("Could not locate an LDAP entry for [{}] and base DN [{}]", filter.format(), ldap.getBaseDn());
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Map<String, String> getSecurityQuestions(final String username) {
        val set = new HashMap<String, String>();
        try {
            val ldap = properties.getLdap();
            val filter = LdapUtils.newLdaptiveSearchFilter(ldap.getSearchFilter(),
                LdapUtils.LDAP_SEARCH_FILTER_DEFAULT_PARAM_NAME,
                CollectionUtils.wrap(username));
            LOGGER.debug("Constructed LDAP filter [{}] to locate security questions", filter);

            val factory = LdapUtils.newLdaptivePooledConnectionFactory(ldap);
            val response = LdapUtils.executeSearchOperation(factory, ldap.getBaseDn(), filter);
            LOGGER.debug("LDAP response for security questions [{}]", response);

            if (LdapUtils.containsResultEntry(response)) {
                val entry = response.getResult().getEntry();
                LOGGER.debug("Located LDAP entry [{}] in the response", entry);
                val qs = properties.getLdap().getSecurityQuestionsAttributes();
                LOGGER.debug("Security question attributes are defined to be [{}]", qs);

                qs.forEach((k, v) -> {
                    val q = entry.getAttribute(k);
                    val a = entry.getAttribute(v);
                    val value = q.getStringValue();
                    if (q != null && a != null && StringUtils.isNotBlank(value) && StringUtils.isNotBlank(a.getStringValue())) {
                        LOGGER.debug("Added security question [{}]", value);
                        set.put(value, value);
                    }
                });
            } else {
                LOGGER.debug("LDAP response did not contain a result for security questions");
            }
        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return set;
    }
}
