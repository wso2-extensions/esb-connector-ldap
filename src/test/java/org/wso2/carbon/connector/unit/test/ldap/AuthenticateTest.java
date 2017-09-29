package org.wso2.carbon.connector.unit.test.ldap;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.ldap.Authenticate;
import org.wso2.carbon.connector.ldap.LDAPConstants;

import java.util.*;

public class AuthenticateTest {
    private Authenticate authenticate;
    private TemplateContext templateContext;
    private MessageContext messageContext;
    private LdapServerSetup ldap;

    @BeforeMethod
    public void setUp() throws Exception {
        ldap = new LdapServerSetup();
        authenticate = new Authenticate();
        ldap.initializeProperties();
        if (ldap.useEmbeddedLDAP) {
            ldap.initializeEmbeddedLDAPServer();
        }
        messageContext = ldap.createMessageContext();
        templateContext = new TemplateContext("authenticate", null);
    }

    @Test
    public void testAuthenticate() throws Exception {
        ldap.createSampleEntity();

        try {
            messageContext.setProperty(LDAPConstants.PROVIDER_URL, "ldap://localhost:10389/");
            templateContext.getMappedValues().put(LDAPConstants.DN, "uid=john004,ou=People,dc=wso2,dc=com");
            templateContext.getMappedValues().put(LDAPConstants.PASSWORD, "12345");
            messageContext.setProperty(LDAPConstants.SECURE_CONNECTION, "false");
            messageContext.setProperty(LDAPConstants.DISABLE_SSL_CERT_CHECKING, "false");
            Stack functionStack = new Stack();
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            authenticate.connect(messageContext);
            Assert.assertEquals((messageContext.getEnvelope().getBody().getFirstElement()).getFirstElement().getText(),
                    "Success");

        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void wrongParameterConnectTest() throws Exception {
        ldap.createSampleEntity();

        try {
            messageContext.setProperty(LDAPConstants.PROVIDER_URL, "ldap://localhost:10389/");
            templateContext.getMappedValues().put(LDAPConstants.DN, "uid=john004,ou=People,dc=wso2,dc=com");
            templateContext.getMappedValues().put(LDAPConstants.PASSWORD, "1234");
            messageContext.setProperty(LDAPConstants.SECURE_CONNECTION, "false");
            messageContext.setProperty(LDAPConstants.DISABLE_SSL_CERT_CHECKING, "false");
            Stack functionStack = new Stack();
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            authenticate.connect(messageContext);
            Assert.assertEquals((messageContext.getEnvelope().getBody().getFirstElement()).getFirstElement().getText(),
                    "Fail");

        } finally {
            ldap.deleteSampleEntry();
        }

    }

    @AfterMethod
    protected void cleanup() {
        ldap.cleanup();
    }
}
