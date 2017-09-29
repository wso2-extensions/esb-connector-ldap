package org.wso2.carbon.connector.unit.test.ldap;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.ldap.Init;
import org.wso2.carbon.connector.ldap.LDAPConstants;
import org.wso2.carbon.connector.ldap.UpdateEntry;

import java.util.Stack;

public class UpdateEntryTest {
    private UpdateEntry updateEntry;
    private Init init;
    private TemplateContext templateContext;
    private MessageContext messageContext;
    private Stack functionStack;
    private LdapServerSetup ldap;

    @BeforeMethod
    public void setUp() throws Exception {
        ldap = new LdapServerSetup();
        updateEntry = new UpdateEntry();
        init = new Init();
        ldap.initializeProperties();
        if (ldap.useEmbeddedLDAP) {
            ldap.initializeEmbeddedLDAPServer();
        }
        messageContext = ldap.createMessageContext();
        messageContext.setProperty(LDAPConstants.SECURE_CONNECTION, "false");
        messageContext.setProperty(LDAPConstants.DISABLE_SSL_CERT_CHECKING, "false");
        templateContext = new TemplateContext("update", null);
        templateContext.getMappedValues().put(LDAPConstants.PROVIDER_URL, "ldap://localhost:10389/");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_PRINCIPAL, "cn=admin,dc=wso2,dc=com");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "19902");
        templateContext.getMappedValues().put(LDAPConstants.DN, "uid=john004,ou=People,dc=wso2,dc=com");
        templateContext.getMappedValues().put(LDAPConstants.ATTRIBUTES,
                "{\n" + "      \"mail\": \"testDim1s22c@wso2.com\",\n" + "      \"userPassword\": \"12345\",\n"
                        + "      \"sn\": \"dim\",\n" + "      \"cn\": \"dim\"\n" + "    }");
        templateContext.getMappedValues().put(LDAPConstants.MODE, "replace");
        functionStack = new Stack();
    }

    @Test
    public void testupdateEntryWithReplace() throws Exception {
        ldap.createSampleEntity();
        try {
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            updateEntry.connect(messageContext);
            Assert.assertEquals((messageContext.getEnvelope().getBody().
                    getFirstElement()).getFirstElement().getText(), "Success");
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testupdateEntryWithAddAndRemove() throws Exception {
        ldap.createSampleEntity();
        try {
            //Update entry with add an attribute
            templateContext.getMappedValues()
                    .put(LDAPConstants.ATTRIBUTES, "{\n" + "      \"mail\": \"testDim1s22c@wso2.com\"  }");
            templateContext.getMappedValues().put(LDAPConstants.MODE, "add");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            updateEntry.connect(messageContext);
            Assert.assertEquals((messageContext.getEnvelope().getBody().
                    getFirstElement()).getFirstElement().getText(), "Success");

            //Update entry with delete an attribute
            templateContext.getMappedValues()
                    .put(LDAPConstants.ATTRIBUTES, "{\n" + "      \"mail\": \"testDim1s22c@wso2.com\"  }");
            templateContext.getMappedValues().put(LDAPConstants.MODE, "remove");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            updateEntry.connect(messageContext);
            Assert.assertEquals((messageContext.getEnvelope().getBody().
                    getFirstElement()).getFirstElement().getText(), "Success");

        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testUpdateEntryWithWrongParameters() throws Exception {
        ldap.createSampleEntity();
        try {
            templateContext.getMappedValues().put(LDAPConstants.DN, "uid=testDim22,ou=People,dc=wso2,dc=com");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            try {
                updateEntry.connect(messageContext);
            } catch (Exception e) {
                String Message =
                        "[LDAP: error code 32 - Unable to modify entry 'uid=testDim22,ou=People,dc=wso2,dc=com'"
                                + " because it does not exist in the server.]";
                OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
                Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorMessage")).next()).getText(),
                        Message);
                Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                        Integer.toString(LDAPConstants.ErrorConstants.UPDATE_ENTRY_ERROR));
            }
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testUpdateEntryWithInvalidCredentials() throws Exception {
        ldap.createSampleEntity();
        try {
            templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "1902");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            try {
                updateEntry.connect(messageContext);
            } catch (SynapseException e) {
                OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
                Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                        Integer.toString(LDAPConstants.ErrorConstants.INVALID_LDAP_CREDENTIALS));
            }
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @AfterMethod
    protected void cleanup() {
        ldap.cleanup();
    }
}
