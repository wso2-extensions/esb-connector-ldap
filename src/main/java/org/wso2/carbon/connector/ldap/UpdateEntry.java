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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.integration.connector.core.AbstractConnectorOperation;
import org.wso2.integration.connector.core.ConnectException;

import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Iterator;

public class UpdateEntry extends AbstractConnectorOperation {

    @Override
    public void execute(MessageContext messageContext, String s, Boolean aBoolean) throws ConnectException {
        String attributesString = (String) getParameter(messageContext, LDAPConstants.ATTRIBUTES);
        String dn = (String) getParameter(messageContext, LDAPConstants.DN);
        String mode = (String) getParameter(messageContext, LDAPConstants.MODE);

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);
        OMElement message = factory.createOMElement(LDAPConstants.MESSAGE, ns);

        try {
            DirContext context = LDAPUtils.getDirectoryContext(messageContext);

            Attributes entry = new BasicAttributes();
            if (StringUtils.isNotEmpty(attributesString)) {
                JSONObject object = new JSONObject(LDAPUtils.createJsonObjectString(attributesString.trim()));
                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String val = object.getString(key);
                    Attribute newAttr = new BasicAttribute(key);
                    newAttr.add(val);
                    entry.put(newAttr);
                }
            }

            try {
                if (mode.equals(LDAPConstants.REPLACE)) {
                    context.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, entry);
                } else if (mode.equals(LDAPConstants.ADD)) {
                    context.modifyAttributes(dn, DirContext.ADD_ATTRIBUTE, entry);
                } else if (mode.equals(LDAPConstants.REMOVE)) {
                    context.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE, entry);
                }
                message.setText(LDAPConstants.SUCCESS);
                result.addChild(message);
                LDAPUtils.preparePayload(messageContext, result);
            } catch (NamingException e) { // LDAP Errors are catched
                LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.UPDATE_ENTRY_ERROR, e);
                throw new SynapseException(e);
            }
        } catch (NamingException e) { //Authentication failures are catched
            LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.INVALID_LDAP_CREDENTIALS, e);
            throw new SynapseException(e);
        } catch (JSONException e) {
            handleException("Error while passing the JSON object", e, messageContext);
        }
    }
}
