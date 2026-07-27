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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.integration.connector.core.AbstractConnectorOperation;
import org.wso2.integration.connector.core.ConnectException;

public class SearchEntry extends AbstractConnectorOperation {
    protected static Log log = LogFactory.getLog(SearchEntry.class);

    @Override
    public void execute(MessageContext messageContext, String s, Boolean aBoolean) throws ConnectException {
        String objectClass = (String) getParameter(messageContext, LDAPConstants.OBJECT_CLASS);
        String filter = (String) getParameter(messageContext, LDAPConstants.FILTERS);
        String dn = (String) getParameter(messageContext, LDAPConstants.DN);
        String[] returnAttributes = new String[0];
        String returnAttributesValue = (String) getParameter(messageContext, LDAPConstants.ATTRIBUTES);
        if (!(returnAttributesValue == null || returnAttributesValue.isEmpty())) {
            returnAttributes = returnAttributesValue.split(",");
        }
        String scope = (String) getParameter(messageContext, LDAPConstants.SCOPE);
        int searchScope = getSearchScope(scope);
        int limit = 0;
        String searchLimit = (String) getParameter(messageContext, LDAPConstants.LIMIT);
        if (!StringUtils.isEmpty(searchLimit)) {
            try {
                limit = Integer.parseInt(searchLimit);
            } catch (NumberFormatException ex) {
                log.error("Invalid value specified for Search limit. Setting default limit value of 0 (unlimited)");
            }
        }
        boolean onlyOneReference = Boolean.parseBoolean(
                (String) getParameter(messageContext, LDAPConstants.ONLY_ONE_REFERENCE));
        boolean allowEmptySearchResult = Boolean.parseBoolean(
                (String) getParameter(messageContext, LDAPConstants.ALLOW_EMPTY_SEARCH_RESULT));
        String pageSizeStr = (String) getParameter(messageContext, LDAPConstants.PAGE_SIZE);

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);

        try {
            String searchFilter = generateSearchFilter(objectClass, filter, messageContext);

            if (!StringUtils.isEmpty(pageSizeStr)) {
                // RFC 2696: fetch all pages on a single connection and return the full result set.
                // Paging is internal — it exists to satisfy server-side size limits (e.g. AD's 1000
                // entry cap) without requiring the caller to manage stateful cookies across calls.
                int pageSize;
                try {
                    pageSize = Integer.parseInt(pageSizeStr);
                } catch (NumberFormatException ex) {
                    handleException("Invalid value for pageSize: " + pageSizeStr, ex, messageContext);
                    return;
                }

                LdapContext ldapContext = LDAPUtils.getLdapContext(messageContext);
                try {
                    byte[] cookie = null;
                    boolean hasResults = false;
                    do {
                        ldapContext.setRequestControls(new Control[]{
                                new PagedResultsControl(pageSize, cookie, Control.CRITICAL)
                        });
                        NamingEnumeration<SearchResult> results = searchInUserBase(dn, searchFilter,
                                returnAttributes, searchScope, ldapContext, limit);
                        if (results != null) {
                            while (results.hasMoreElements()) {
                                hasResults = true;
                                SearchResult entityResult = results.next();
                                processObjectGuid(entityResult);
                                result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
                            }
                        }
                        cookie = extractPagedCookie(ldapContext);
                    } while (cookie != null && cookie.length > 0);

                    if (!hasResults && !allowEmptySearchResult) {
                        throw new NamingException("No matching result or entity found for this search");
                    }
                    LDAPUtils.preparePayload(messageContext, result);
                } catch (NamingException e) {
                    LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.SEARCH_ERROR, e);
                    throw new SynapseException(e);
                } catch (IOException e) {
                    LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.SEARCH_ERROR, e);
                    throw new SynapseException(e);
                } finally {
                    try { ldapContext.close(); } catch (NamingException ignored) {}
                }
            } else {
                // Standard non-paged search
                DirContext context = LDAPUtils.getDirectoryContext(messageContext);
                try {
                    NamingEnumeration<SearchResult> results = searchInUserBase(dn, searchFilter,
                            returnAttributes, searchScope, context, limit);
                    if (!onlyOneReference) {
                        if (results != null && results.hasMore()) {
                            while (results.hasMoreElements()) {
                                SearchResult entityResult = results.next();
                                processObjectGuid(entityResult);
                                result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
                            }
                        } else if (!allowEmptySearchResult) {
                            throw new NamingException("No matching result or entity found for this search");
                        }
                    } else {
                        SearchResult entityResult = makeSureOnlyOneMatch(results, allowEmptySearchResult);
                        result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
                    }
                    LDAPUtils.preparePayload(messageContext, result);
                } catch (NamingException e) {
                    LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.SEARCH_ERROR, e);
                    throw new SynapseException(e);
                } finally {
                    try { context.close(); } catch (NamingException ignored) {}
                }
            }
        } catch (NamingException e) { // Authentication failures from getLdapContext/getDirectoryContext
            LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.INVALID_LDAP_CREDENTIALS, e);
            throw new SynapseException(e);
        }
    }

    private void processObjectGuid(SearchResult entityResult) throws NamingException {
        Attributes attributes = entityResult.getAttributes();
        if (attributes != null) {
            Attribute attribute = attributes.get(LDAPConstants.OBJECT_GUID);
            if (attribute != null) {
                Object attObject = attribute.get(0);
                final byte[] bytes = (byte[]) attObject;
                // https://community.oracle.com/thread/1157698 — objectGUID bytes are not big-endian
                if (bytes.length == 16) {
                    final ByteBuffer bb = ByteBuffer.wrap(swapBytes(bytes));
                    String attr = new java.util.UUID(bb.getLong(), bb.getLong()).toString();
                    entityResult.getAttributes().put(LDAPConstants.OBJECT_GUID, attr);
                }
            }
        }
    }

    private byte[] extractPagedCookie(LdapContext ldapContext) throws NamingException {
        Control[] responseControls = ldapContext.getResponseControls();
        if (responseControls != null) {
            for (Control control : responseControls) {
                if (control instanceof PagedResultsResponseControl) {
                    return ((PagedResultsResponseControl) control).getCookie();
                }
            }
        }
        return null;
    }

    private OMElement prepareNode(SearchResult entityResult, OMFactory factory, OMNamespace ns,
                                  String[] returnAttributes) throws NamingException {
        Attributes attributes = entityResult.getAttributes();
        Attribute attribute;
        OMElement entry = factory.createOMElement(LDAPConstants.ENTRY, ns);
        OMElement dnattr = factory.createOMElement(LDAPConstants.DN, ns);
        dnattr.setText(entityResult.getNameInNamespace());
        entry.addChild(dnattr);
        if (returnAttributes.length == 0) {
            NamingEnumeration<String> ids = attributes.getIDs();
            while (ids.hasMore()) {
                String id = ids.next();
                Attribute attr = attributes.get(id);
                NamingEnumeration ne = attr.getAll();
                while (ne.hasMoreElements()) {
                    Object element = ne.next();
                    String elementType = element.getClass().toString();
                    String value = "";
                    if (elementType.equals("class java.lang.String")) {
                        value = element.toString();
                    } else if (elementType.equals("class [B")) {
                        Attribute attributeValue = attributes.get(id);
                        value = new String((byte[]) attributeValue.get());
                    }
                    OMElement omElement = factory.createOMElement(id, ns);
                    omElement.setText(value);
                    entry.addChild(omElement);
                }
            }
        } else {
            for (int i = 0; i < returnAttributes.length; i++) {
                attribute = attributes.get(returnAttributes[i]);

                // Remove ";" from returnAttribute elements to prevent invalid xml generation
                if (returnAttributes[i].contains(";")) {
                    String[] splitResult = returnAttributes[i].split("(?=;)");
                    returnAttributes[i] = splitResult[0];
                }
                if (attribute != null) {
                    NamingEnumeration ne = attribute.getAll();
                    while (ne.hasMoreElements()) {
                        Object element = ne.next();
                        String elementType = element.getClass().toString();
                        String value = "";
                        if (elementType.equals("class java.lang.String")) {
                            value = (String) element.toString();
                        } else if(elementType.equals("class [B")) {
                            Attribute attributeValue = attributes.get(returnAttributes[i]);
                            value = new String((byte[]) attributeValue.get());
                        }
                        OMElement attr = factory.createOMElement(returnAttributes[i], ns);
                        attr.setText(value);
                        entry.addChild(attr);
                    }
                }
            }
        }
        return entry;
    }

    private SearchResult makeSureOnlyOneMatch(NamingEnumeration<SearchResult> results,
                                              boolean allowEmptySearchResult) throws NamingException {
        SearchResult searchResult = null;

        if (results.hasMoreElements()) {
            searchResult = (SearchResult) results.nextElement();

            // Make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                // Here the code has matched multiple objects for the searched target
                throw new NamingException("Multiple objects for the searched target have been found. Try to " +
                        "change onlyOneReference option");
            }
            return searchResult;
        } else {
            if (!allowEmptySearchResult) {
                throw new NamingException("Could not find a matching entry for this search");
            }
            return null;
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
        SearchControls userSearchControl = new SearchControls();
        if (returningAttributes.length > 0) {
            userSearchControl.setReturningAttributes(returningAttributes);
        }
        userSearchControl.setCountLimit(limit);
        userSearchControl.setSearchScope(searchScope);
        return rootContext.search(dn, searchFilter, userSearchControl);

    }

    private String generateSearchFilter(String objectClass, String filter, MessageContext messageContext) {
        StringBuilder attrFilter = new StringBuilder();
        try {
            JSONObject object = new JSONObject(LDAPUtils.createJsonObjectString(filter.trim()));
            Iterator keys = object.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                attrFilter.append("(");
                attrFilter.append(key).append("=").append(object.getString(key));
                attrFilter.append(")");
            }
        } catch (JSONException e) {
            handleException("Error while passing the JSON object", e, messageContext);
        }
        if (objectClass != null && !objectClass.isEmpty()) {
            return "(&(objectClass=" + objectClass + ")" + attrFilter + ")";
        } else {
            return attrFilter.toString();
        }
    }

    private int getSearchScope(String scope) {
        int searchScope = 2;
        if(scope != null && !scope.isEmpty()) {
            if (scope.equalsIgnoreCase("OBJECT")) {
                searchScope = 0;
            } else if (scope.equalsIgnoreCase("ONE_LEVEL")) {
                searchScope = 1;
            }
        }
        return searchScope;
    }
}
