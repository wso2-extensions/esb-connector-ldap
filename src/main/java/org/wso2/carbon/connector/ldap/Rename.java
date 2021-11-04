/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.carbon.connector.ldap;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.connector.core.AbstractConnector;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

public class Rename extends AbstractConnector {

    @Override
    public void connect(MessageContext messageContext) {

        String oldName = (String) getParameter(messageContext, LDAPConstants.OLD_NAME);
        String newName = (String) getParameter(messageContext, LDAPConstants.NEW_NAME);

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);
        OMElement message = factory.createOMElement(LDAPConstants.MESSAGE, ns);

        try {
            DirContext context = LDAPUtils.getDirectoryContext(messageContext);
            context.rename(oldName, newName);
            message.setText(LDAPConstants.SUCCESS);
            result.addChild(message);
            LDAPUtils.preparePayload(messageContext, result);
        } catch (NamingException e) {
            log.error("Failed to update ldap Common Name (CN) ", e);
            LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.RENAME_ERROR, e);
            throw new SynapseException(e);
        }
    }
}
