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
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class SearchEntry extends AbstractConnector {
    protected static Log log = LogFactory.getLog(SearchEntry.class);

    @Override
    public void connect(MessageContext messageContext) throws ConnectException {
        String objectClass = (String) getParameter(messageContext, LDAPConstants.OBJECT_CLASS);
        String filter = (String) getParameter(messageContext, LDAPConstants.FILTERS);
        String dn = (String) getParameter(messageContext, LDAPConstants.DN);
        String returnAttributes[] = ((String) getParameter(messageContext, LDAPConstants.ATTRIBUTES)).split(",");

        int limit = 0;
        String searchLimit = (String) getParameter(messageContext, LDAPConstants.LIMIT);
        if (!StringUtils.isEmpty(searchLimit)) {
            try {
                limit = Integer.parseInt(searchLimit);
            } catch (NumberFormatException ex) {
                log.error("Invalid value specified for Search limit. Setting default limit value of 0 (unlimited)");
            }
        }

        boolean onlyOneReference = Boolean.valueOf(
                (String) getParameter(messageContext, LDAPConstants.ONLY_ONE_REFERENCE));
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE,
                                                   LDAPConstants.NAMESPACE);
        OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);
        try {
            log.info("::::: Getting Directory Context");
            DirContext context = LDAPUtils.getDirectoryContext(messageContext);
            log.info("::::: context obtained successfully");
            String attrFilter = generateAttrFilter(filter, messageContext);
            String searchFilter = generateSearchFilter(objectClass, attrFilter);
            try {
                NamingEnumeration<SearchResult> results = searchInUserBase(
                        dn, searchFilter, returnAttributes, SearchControls.SUBTREE_SCOPE, context, limit);
                SearchResult entityResult;
                if (!onlyOneReference) {
                    if (results != null && results.hasMore()) {
                        while (results.hasMoreElements()) {
                            entityResult = results.next();
                            Attributes attributes = entityResult.getAttributes();
                            if (attributes != null) {
                                Attribute attribute = attributes.get(LDAPConstants.OBJECT_GUID);
                                if (attribute != null) {

                                    Object attObject = attribute.get(0);
                                    final byte[] bytes = (byte[]) attObject;

                                    // https://community.oracle.com/thread/1157698
                                    // Represent objectGUID in UUID
                                    if (bytes.length == 16) {
                                        final ByteBuffer bb = ByteBuffer.wrap(swapBytes(bytes));
                                        String attr = new java.util.UUID(bb.getLong(), bb.getLong()).toString();
                                        entityResult.getAttributes().put(LDAPConstants.OBJECT_GUID, attr);
                                    }
                                }
                            }

                            result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
                        }
                    } else {

                        //throw new NamingException("No matching result or entity found for this search");
                        //LDAPUtils.preparePayload(messageContext, "No matching result or entity found for this search", LDAPConstants.RESULT_NOT_FOUND_ERROR_CODE);
                        log.info("No matching result or entity found for this search - "+ LDAPConstants.RESULT_NOT_FOUND_ERROR_CODE);
                    }
                } else {
                    entityResult = makeSureOnlyOneMatch(results);
                    if (entityResult == null) {
                        throw new NamingException("Multiple objects for the searched target have been found. Try to " +
                                                          "change onlyOneReference option");
                    } else {
                        result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
                    }
                }
                log.info("::::::::: Result = "+result);
                LDAPUtils.preparePayload(messageContext, result);
                if (context != null) {
                    context.close();
                }
            } catch (NamingException e) { //LDAP Errors are catched
                log.error("LDAP Errors are catched - ",e);
                LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.SEARCH_ERROR, e);
                throw new SynapseException(e);
            }
        } catch (NamingException e) { //Authentication failures are catched
            log.error("Authentication failures are catched - ",e);
            LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.INVALID_LDAP_CREDENTIALS, e);
            throw new SynapseException(e);
        }
    }

    private OMElement prepareNode(SearchResult entityResult, OMFactory factory, OMNamespace ns, String returnAttributes[])
            throws NamingException {
        Attributes attributes = entityResult.getAttributes();
        Attribute attribute;
        OMElement entry = factory.createOMElement(LDAPConstants.ENTRY, ns);
        OMElement dnattr = factory.createOMElement(LDAPConstants.DN, ns);
        dnattr.setText(entityResult.getNameInNamespace());
        entry.addChild(dnattr);

        for (int i = 0; i < returnAttributes.length; i++) {
            attribute = attributes.get(returnAttributes[i]);

            // Remove ";" from returnAttribute elements to prevent invalid xml generation
            if (returnAttributes[i].contains(";")) {
                String[] splitResult = returnAttributes[i].split("(?=;)");
                returnAttributes[i] = splitResult[0];
            }
            if (attribute != null) {
                NamingEnumeration ne = null;
                ne = attribute.getAll();
                while (ne.hasMoreElements()) {
                    String value = (String) ne.next();
                    OMElement attr = factory.createOMElement(returnAttributes[i], ns);
                    attr.setText(value);
                    entry.addChild(attr);
                }
            }
        }
        return entry;
    }

    private SearchResult makeSureOnlyOneMatch(NamingEnumeration<SearchResult> results) throws NamingException {
        SearchResult searchResult = null;

        if (results.hasMoreElements()) {
            searchResult = (SearchResult) results.nextElement();

            // Make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                // Here the code has matched multiple objects for the searched target
                return null;
            }
            return searchResult;
        } else {
            throw new NamingException("Could not find a matching entry for this search");
        }
    }

    /**
     * swap the bytes 0<->3, 1<->2,4<->5,6<->7 of the objectGUID byte array,
     * because objectGUID byte order is not big-endian
     *
     * @param bytes byte array needed to be swapped
     * @return swapped byte array
     */
    protected byte[] swapBytes(byte[] bytes) {
        // bytes[0] <-> bytes[3]
        byte swap = bytes[3];
        bytes[3] = bytes[0];
        bytes[0] = swap;
        // bytes[1] <-> bytes[2]
        swap = bytes[2];
        bytes[2] = bytes[1];
        bytes[1] = swap;
        // bytes[4] <-> bytes[5]
        swap = bytes[5];
        bytes[5] = bytes[4];
        bytes[4] = swap;
        // bytes[6] <-> bytes[7]
        swap = bytes[7];
        bytes[7] = bytes[6];
        bytes[6] = swap;
        return bytes;
    }

    private NamingEnumeration<SearchResult> searchInUserBase(String dn, String searchFilter,
                                                             String[] returningAttributes,
                                                             int searchScope, DirContext rootContext, int limit)
            throws NamingException {
        String userBase = dn;
        SearchControls userSearchControl = new SearchControls();
        userSearchControl.setReturningAttributes(returningAttributes);
        userSearchControl.setCountLimit(limit);
        userSearchControl.setSearchScope(searchScope);
        NamingEnumeration<SearchResult> userSearchResults;
        userSearchResults = rootContext.search(userBase, searchFilter, userSearchControl);
        return userSearchResults;

    }

    private String generateAttrFilter(String filter, MessageContext messageContext) {
        String attrFilter = "";
        try {
            JSONObject object = new JSONObject(filter);
            Iterator keys = object.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                attrFilter += "(";
                attrFilter += key + "=" + object.getString(key);
                attrFilter += ")";
            }
        } catch (JSONException e) {
            log.error("Error while passing the JSON object - "+ e);
            handleException("Error while passing the JSON object", e, messageContext);
        }
        return attrFilter;
    }

    private String generateSearchFilter(String objectClass, String attrFilter) {
        return "(&(objectClass=" + objectClass + ")" + attrFilter + ")";
    }
}
