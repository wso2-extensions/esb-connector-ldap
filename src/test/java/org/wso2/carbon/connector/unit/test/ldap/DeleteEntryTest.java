/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.connector.unit.test.ldap;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.template.TemplateContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.ldap.DeleteEntry;
import org.wso2.carbon.connector.ldap.Init;
import org.wso2.carbon.connector.ldap.LDAPConstants;

import java.util.Stack;

public class DeleteEntryTest {
    private DeleteEntry deleteEntry;
    private Init init;
    private TemplateContext templateContext;
    private MessageContext messageContext;
    private Stack functionStack;
    private LdapServerSetup ldap;

    @BeforeMethod
    public void setUp() throws Exception {
        ldap = new LdapServerSetup();
        deleteEntry = new DeleteEntry();
        init = new Init();
        ldap.initializeProperties();
        if (ldap.useEmbeddedLDAP) {
            ldap.initializeEmbeddedLDAPServer();
        }
        messageContext = ldap.createMessageContext();
        messageContext.setProperty(LDAPConstants.SECURE_CONNECTION, "false");
        messageContext.setProperty(LDAPConstants.DISABLE_SSL_CERT_CHECKING, "false");
        templateContext = new TemplateContext("authenticate", null);
        templateContext.getMappedValues().put(LDAPConstants.PROVIDER_URL, "ldap://localhost:10389/");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_PRINCIPAL, "cn=admin,dc=wso2,dc=com");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "19902");
        templateContext.getMappedValues().put(LDAPConstants.DN, "uid=john004,ou=People,dc=wso2,dc=com");
        functionStack = new Stack();
    }

    @Test
    public void testDeleteEntry() throws Exception {
        ldap.createSampleEntity();
        //deleting created entry
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        init.connect(messageContext);
        deleteEntry.connect(messageContext);
        Assert.assertEquals((messageContext.getEnvelope().getBody().getFirstElement()).getFirstElement().getText(),
                "Success");
    }

    @Test
    public void testDeleteEntryWithWrongDn() throws Exception {
        ldap.createSampleEntity();
        templateContext.getMappedValues().put(LDAPConstants.DN, "uid=wrong,ou=People,dc=wso2,dc=com");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        try {
            init.connect(messageContext);
            deleteEntry.connect(messageContext);
        } catch (SynapseException e) {
            String Message = "[LDAP: error code 32 - Unable to perform the search because base entry 'uid=wrong,"
                    + "ou=People,dc=wso2,dc=com' does not exist in the server.]";
            OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorMessage")).next()).getText(),
                    Message);
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                    Integer.toString(LDAPConstants.ErrorConstants.ENTRY_DOESNOT_EXISTS_ERROR));
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testDeleteEntryWithInvalidCredentials() throws Exception {
        ldap.createSampleEntity();
        try {
            templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "1902");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            try {
                deleteEntry.connect(messageContext);
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
