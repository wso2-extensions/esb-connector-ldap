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
import org.wso2.carbon.connector.ldap.AddEntry;
import org.wso2.carbon.connector.ldap.Init;
import org.wso2.carbon.connector.ldap.LDAPConstants;

import java.util.*;

public class AddEntryTest {
    private AddEntry addEntry;
    private Init init;
    private TemplateContext templateContext;
    private MessageContext messageContext;
    private Stack functionStack;
    private LdapServerSetup ldap;

    @BeforeMethod
    public void setUp() throws Exception {
        ldap = new LdapServerSetup();
        addEntry = new AddEntry();
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
        templateContext.getMappedValues().put(LDAPConstants.OBJECT_CLASS, "inetOrgPerson");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_PRINCIPAL, "cn=admin,dc=wso2,dc=com");
        templateContext.getMappedValues().put(LDAPConstants.SECURITY_CREDENTIALS, "19902");
        templateContext.getMappedValues().put(LDAPConstants.ATTRIBUTES,
                "{\n" + "      \"mail\": \"testDim1s22sc@wso2.com\",\n" + "      \"userPassword\": \"12345\",\n"
                        + "      \"sn\": \"dim\",\n" + "      \"cn\": \"dim\"\n" + "    }");
        templateContext.getMappedValues().put(LDAPConstants.DN, "uid=john004,ou=People,dc=wso2,dc=com");
        functionStack = new Stack();
    }

    @Test(description = "Add Entry with valid Parameters")
    public void testAddEntry() throws Exception {
        try {
            functionStack.push(templateContext);
            messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
            init.connect(messageContext);
            addEntry.connect(messageContext);
            Assert.assertEquals((messageContext.getEnvelope().getBody().getFirstElement()).getFirstElement().getText(),
                    "Success");
        } finally {
            ldap.deleteSampleEntry();
        }
    }

    @Test
    public void testAddEntryWithMissingDn() throws Exception {
        templateContext.getMappedValues().put(LDAPConstants.DN, "");
        Stack functionStack = new Stack();
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        init.connect(messageContext);
        try {
            addEntry.connect(messageContext);
        } catch (SynapseException e) {
            String Message = "[LDAP: error code 68 - Unable to add an entry with the null DN.]";
            OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorMessage")).next()).getText(),
                    Message);
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                    Integer.toString(LDAPConstants.ErrorConstants.ADD_ENTRY_ERROR));
        }
    }

    @Test
    public void testAddEntryWithWrongUserBase() throws Exception {
        templateContext.getMappedValues().put(LDAPConstants.DN, "uid=john004,ou=example,dc=example");
        Stack functionStack = new Stack();
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        init.connect(messageContext);
        try {
            addEntry.connect(messageContext);
        } catch (SynapseException e) {
            //String a= LDAPConstants.ErrorConstants.ADD_ENTRY_ERROR ;
            String Message = "[LDAP: error code 32 - Unable to add entry 'uid=john004,ou=example,dc=example' "
                    + "because its parent entry 'ou=example,dc=example' does not exist in the server.]";
            OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorMessage")).next()).getText(),
                    Message);
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                    Integer.toString(LDAPConstants.ErrorConstants.ADD_ENTRY_ERROR));

        }
    }

    @Test
    public void testAddEntryWithWrongObjectClass() throws Exception {
        templateContext.getMappedValues().put(LDAPConstants.OBJECT_CLASS, "wrongObjectClass");
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        init.connect(messageContext);
        try {
            addEntry.connect(messageContext);
        } catch (SynapseException e) {
            OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                    Integer.toString(LDAPConstants.ErrorConstants.ADD_ENTRY_ERROR));
        }
    }

    @Test
    public void testAddEntryWithoutMandatoryAttributes() throws Exception {
        templateContext.getMappedValues().put(LDAPConstants.ATTRIBUTES,
                "{\n" + "      \"mail\": \"testDim1s22sc@wso2.com\",\n" + "      \"userPassword\": \"12345\",\n"
                        + "      \"sn\": \"dim\",\n" + "    }");
        Stack functionStack = new Stack();
        functionStack.push(templateContext);
        messageContext.setProperty("_SYNAPSE_FUNCTION_STACK", functionStack);
        init.connect(messageContext);
        try {
            addEntry.connect(messageContext);
        } catch (SynapseException e) {
            String Message = "[LDAP: error code 65 - Unable to add entry 'uid=john004,ou=People,dc=wso2,dc=com'"
                    + " because it violates the provided schema:  The entry is missing required attribute cn.]";
            OMElement error = messageContext.getEnvelope().getBody().getFirstElement();
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorMessage")).next()).getText(),
                    Message);
            Assert.assertEquals(((OMElement) (error.getChildrenWithLocalName("errorCode")).next()).getText(),
                    Integer.toString(LDAPConstants.ErrorConstants.ADD_ENTRY_ERROR));
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
                addEntry.connect(messageContext);
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
