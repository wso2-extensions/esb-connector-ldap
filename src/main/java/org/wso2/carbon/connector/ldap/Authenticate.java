/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.connector.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

public class Authenticate extends AbstractConnector {

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String providerUrl =
                LDAPUtils.lookupContextParams(messageContext, LDAPConstants.PROVIDER_URL);
        String dn = (String) getParameter(messageContext, LDAPConstants.DN);
        String password = (String) getParameter(messageContext, LDAPConstants.PASSWORD);
        boolean secureConnection = Boolean.valueOf(
                LDAPUtils.lookupContextParams(messageContext, LDAPConstants.SECURE_CONNECTION));
        boolean disableSSLCertificateChecking = Boolean.valueOf(LDAPUtils.lookupContextParams(
                messageContext, LDAPConstants.DISABLE_SSL_CERT_CHECKING));

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE,
                LDAPConstants.NAMESPACE);
        OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);
        OMElement message = factory.createOMElement(LDAPConstants.MESSAGE, ns);

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAPConstants.COM_SUN_JNDI_LDAP_LDAPCTXFACTORY);
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        if (secureConnection) {
            env.put(Context.SECURITY_PROTOCOL, LDAPConstants.SSL);
        }
        if (disableSSLCertificateChecking) {
            env.put(LDAPConstants.JAVA_NAMING_LDAP_FACTORY_SOCKET,
                    LDAPConstants.ORG_WSO2_CARBON_CONNECTOR_SECURITY_MYSSLSOCKETFACTORY);
        }

        boolean logged = false;
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            message.setText(LDAPConstants.SUCCESS);
            result.addChild(message);
            LDAPUtils.preparePayload(messageContext, result);
        } catch (NamingException e) {
            message.setText(LDAPConstants.FAIL);
            result.addChild(message);
            LDAPUtils.preparePayload(messageContext, result);
        }
    }
}
