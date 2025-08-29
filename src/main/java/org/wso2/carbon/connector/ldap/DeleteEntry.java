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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.wso2.integration.connector.core.AbstractConnectorOperation;
import org.wso2.integration.connector.core.ConnectException;

public class DeleteEntry extends AbstractConnectorOperation {

    @Override
    public void execute(MessageContext messageContext, String s, Boolean aBoolean) throws ConnectException {
        String dn = (String) getParameter(messageContext, LDAPConstants.DN);

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);
        OMElement message = factory.createOMElement(LDAPConstants.MESSAGE, ns);

        try {
            DirContext context = LDAPUtils.getDirectoryContext(messageContext);
            try {
                Attributes matchingAttributes = new BasicAttributes();
                //search for the existance of dn
                matchingAttributes.put(new BasicAttribute(LDAPConstants.DN));
                NamingEnumeration<SearchResult> searchResult = context.search(dn, matchingAttributes);
                try {
                    context.destroySubcontext(dn);
                    message.setText(LDAPConstants.SUCCESS);
                    result.addChild(message);
                    LDAPUtils.preparePayload(messageContext, result);
                } catch (NamingException e) {
                    log.error("Failed to delete ldap entry with dn = " + dn, e);
                    LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.DELETE_ENTRY_ERROR, e);
                    throw new SynapseException(e);
                }
            } catch (NamingException e) {
                LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.ENTRY_DOESNOT_EXISTS_ERROR,
                        e);
                throw new SynapseException(e);
            }
        } catch (NamingException e) {
            LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.INVALID_LDAP_CREDENTIALS, e);
            throw new SynapseException(e);
        }
    }
}
