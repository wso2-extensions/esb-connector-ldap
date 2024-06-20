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
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPBody;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.nhttp.NhttpConstants;

public class LDAPUtils {
    private static final OMFactory fac = OMAbstractFactory.getOMFactory();
    protected static Log log = LogFactory.getLog(LDAPUtils.class);
    private static OMNamespace ns = fac.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);

    protected static DirContext getDirectoryContext(MessageContext messageContext)
            throws NamingException {
        String providerUrl = LDAPUtils.lookupContextParams(messageContext, LDAPConstants.PROVIDER_URL);
        String securityPrincipal = LDAPUtils.lookupContextParams(messageContext, LDAPConstants.SECURITY_PRINCIPAL);
        String securityCredentials = LDAPUtils.lookupContextParams(messageContext, LDAPConstants.SECURITY_CREDENTIALS);
        boolean secureConnection = Boolean
                .valueOf(LDAPUtils.lookupContextParams(messageContext, LDAPConstants.SECURE_CONNECTION));
        boolean disableSSLCertificateChecking = Boolean.valueOf(LDAPUtils.lookupContextParams(
                messageContext, LDAPConstants.DISABLE_SSL_CERT_CHECKING));
        String timeout = LDAPUtils.lookupContextParams(messageContext, LDAPConstants.TIMEOUT);
        boolean connectionPoolingEnabled = Boolean
                .valueOf(LDAPUtils.lookupContextParams(messageContext, LDAPConstants.CONNECTION_POOLING_ENABLED));
        String connectionPoolingProtocol = LDAPUtils
                .lookupContextParams(messageContext, LDAPConstants.CONNECTION_POOLING_PROTOCOL);
        String connectionPoolingInitSize = LDAPUtils
                .lookupContextParams(messageContext, LDAPConstants.CONNECTION_POOLING_INIT_SIZE);
        String connectionPoolingMaxSize = LDAPUtils
                .lookupContextParams(messageContext, LDAPConstants.CONNECTION_POOLING_MAX_SIZE);

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, LDAPConstants.COM_SUN_JNDI_LDAP_LDAPCTXFACTORY);
        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(LDAPConstants.JAVA_NAMING_LDAP_ATTRIBUTE_BINARY, LDAPConstants.OBJECT_GUID);

        // Fallback to anonymous binding when no credentials are provided
        if (StringUtils.isNotEmpty(securityPrincipal)) {
            env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
            env.put(Context.SECURITY_CREDENTIALS, securityCredentials);
        }

        if(StringUtils.isNotEmpty(timeout)) {
            env.put(LDAPConstants.COM_JAVA_JNDI_LDAP_READ_TIMEOUT, timeout);
        }
        if (secureConnection) {
            env.put(Context.SECURITY_PROTOCOL, LDAPConstants.SSL);
        }
        if (disableSSLCertificateChecking) {
            env.put(LDAPConstants.JAVA_NAMING_LDAP_FACTORY_SOCKET,
                    LDAPConstants.ORG_WSO2_CARBON_CONNECTOR_SECURITY_MYSSLSOCKETFACTORY);
        }
        env.put(LDAPConstants.COM_SUN_JNDI_LDAP_CONNECT_POOL, String.valueOf(connectionPoolingEnabled));
        if (connectionPoolingEnabled) {
            if (StringUtils.isNotEmpty(connectionPoolingProtocol)) {
                env.put(LDAPConstants.COM_SUN_JNDI_LDAP_CONNECT_POOL_PROTOCOL, connectionPoolingProtocol);
            }
            if (StringUtils.isNotEmpty(connectionPoolingInitSize)) {
                env.put(LDAPConstants.COM_SUN_JNDI_LDAP_CONNECT_POOL_INITSIZE, connectionPoolingInitSize);
            }
            if (StringUtils.isNotEmpty(connectionPoolingMaxSize)) {
                env.put(LDAPConstants.COM_SUN_JNDI_LDAP_CONNECT_POOL_MAXSIZE, connectionPoolingMaxSize);
            }
        }

        DirContext ctx = null;
        ctx = new InitialDirContext(env);
        return ctx;
    }

    public static String lookupContextParams(MessageContext ctxt, String paramName) {
        return (String) ctxt.getProperty(paramName);
    }

    public static void storeAdminLoginDatails(MessageContext ctxt, String url, String principal,
                                              String password) {
        ctxt.setProperty(LDAPConstants.PROVIDER_URL, url);
        ctxt.setProperty(LDAPConstants.SECURITY_PRINCIPAL, principal);
        ctxt.setProperty(LDAPConstants.SECURITY_CREDENTIALS, password);
    }

    public static void setConnectionPoolingParameters(MessageContext ctxt, String poolingEnabled, String protocol,
            String initSize, String maxSize) {
        ctxt.setProperty(LDAPConstants.CONNECTION_POOLING_ENABLED, poolingEnabled);
        ctxt.setProperty(LDAPConstants.CONNECTION_POOLING_PROTOCOL, protocol);
        ctxt.setProperty(LDAPConstants.CONNECTION_POOLING_INIT_SIZE, initSize);
        ctxt.setProperty(LDAPConstants.CONNECTION_POOLING_MAX_SIZE, maxSize);
    }

    public static void preparePayload(MessageContext messageContext, OMElement element) {
        SOAPBody soapBody = messageContext.getEnvelope().getBody();
        for (Iterator itr = soapBody.getChildElements(); itr.hasNext(); ) {
            OMElement child = (OMElement) itr.next();
            child.detach();
        }
        soapBody.addChild(element);
    }

    public static void preparePayload(MessageContext messageContext, Exception e, int errorCode) {
        OMElement omElement = fac.createOMElement(LDAPConstants.ERROR, ns);
        OMElement message = fac.createOMElement(LDAPConstants.ERROR_MESSAGE, ns);
        OMElement code = fac.createOMElement(LDAPConstants.ERROR_CODE, ns);
        message.addChild(fac.createOMText(omElement, e.getMessage()));
        code.addChild(fac.createOMText(omElement, errorCode + ""));
        omElement.addChild(message);
        omElement.addChild(code);
        preparePayload(messageContext, omElement);
    }

    public static void handleErrorResponse(MessageContext messageContext, int errorCode,
                                           Exception e) {
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) messageContext).getAxis2MessageContext();
        String errorMessage = e.getMessage();
        axis2MessageContext.setProperty(NhttpConstants.HTTP_SC, 500);
        messageContext.setProperty(SynapseConstants.ERROR_CODE, errorCode); // This doesn't work
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);
        messageContext.setFaultResponse(true);
        preparePayload(messageContext, e, errorCode);
    }
}
