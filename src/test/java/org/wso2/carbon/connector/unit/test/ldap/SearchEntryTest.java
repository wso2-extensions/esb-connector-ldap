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
import org.wso2.carbon.connector.ldap.SearchEntry;
import org.wso2.carbon.connector.ldap.Init;
import org.wso2.carbon.connector.ldap.LDAPConstants;

import java.util.Stack;

public class SearchEntryTest {
    private SearchEntry searchEntry;
    private Init init;
    private TemplateContext templateContext;
    private MessageContext messageContext;
    private Stack functionStack;
    private LdapServerSetup ldap;

    @BeforeMethod
    public void setUp() throws Exception {
        ldap = new LdapServerSetup();
        searchEntry = new SearchEntry();
        init = new Init();
        ldap.initializeProperties();
        if (ldap.useEmbeddedLDAP) {
            ldap.initializeEmbeddedLDAPServer();
        }
        messageContext = ldap.createMessageContext();
        messageContext.setProperty(LDAPConstants.SECURE_CONNECTION, "false");
        messageContext.setProperty(LDAPConstants.DISABLE_SSL_CERT_CHECKING, "false");
        templateContext = new TemplateContext("search", null);
        templateContext.getMappedValues().put(LDAPConstants.PROVIDER_URL, "ldap://localhost:10389/");
        templateContext.getMappedValues().put(LDAPConstants.OBJECT_CLASS, "inetOrgPerson");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_PRINCIPAL, "cn=admin,dc=wso2,dc=com");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "19902");
        templateContext.getMappedValues().put(LDAPConstants.ATTRIBUTES, "mail,uid,givenName");
        templateContext.getMappedValues().put(LDAPConstants.DN, "ou=People,dc=wso2,dc=com");
        templateContext.getMappedValues().put(LDAPConstants.ONLY_ONE_REFERENCE, "false");
        templateContext.getMappedValues().put(LDAPConstants.FILTERS, "{\n" + "      \"uid\": \"john004\"\n" + "    }");
        functionStack = new Stack();
    }

    @Test
    public void testSearchEntry() throws Exception {
        ldap.createSampleEntity();
        try {
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            searchEntry.connect(messageContext);
            OMElement result = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertNotNull(result);
            OMElement entry = result.getFirstElement();
            Assert.assertNotNull(entry);
            String userId = ((OMElement) (entry.getChildrenWithLocalName("uid")).next()).getText();
            Assert.assertEquals(userId, ldap.testUserId);
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testSearchEntryWithMultipleSearch() throws Exception {
        ldap.createSampleEntity();
        try {
            templateContext.getMappedValues().put(LDAPConstants.ONLY_ONE_REFERENCE, "true");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            searchEntry.connect(messageContext);
            OMElement result = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertNotNull(result);
            OMElement entry = result.getFirstElement();
            Assert.assertNotNull(entry);
            String userId = ((OMElement) (entry.getChildrenWithLocalName("uid")).next()).getText();
            Assert.assertEquals(userId, ldap.testUserId);
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testSearchEntryWithMultipleSearchAndInvalidObjectClass() throws Exception {
        ldap.createSampleEntity();
        try {
            templateContext.getMappedValues().put(LDAPConstants.ONLY_ONE_REFERENCE, "true");
            templateContext.getMappedValues().put(LDAPConstants.OBJECT_CLASS, "");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            try {
                searchEntry.connect(messageContext);

            } catch (SynapseException e) {
                String Message = "Multiple objects for the searched target have been found. Try to change "
                        + "onlyOneReference option";
                OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
                Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorMessage")).next()).getText(),
                        Message);
                Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                        Integer.toString(LDAPConstants.ErrorConstants.SEARCH_ERROR));
            }
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testSearchEntryWithInvalidCredentials() throws Exception {
        ldap.createSampleEntity();
        try {
            templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "1902");
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            try {
                searchEntry.connect(messageContext);
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
