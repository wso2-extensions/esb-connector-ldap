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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.CompositeName;
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
import org.wso2.carbon.connector.core.AbstractConnector;

public class SearchEntry extends AbstractConnector {
    protected static Log log = LogFactory.getLog(SearchEntry.class);

    // Active Directory returns attributes larger than MaxValRange under a ranged name
    // (MS-ADTS range option), e.g. member;range=0-1499, terminated by member;range=<n>-*
    private static final Pattern RANGED_ATTRIBUTE_PATTERN =
            Pattern.compile("(.+);range=(\\d+)-(\\d+|\\*)", Pattern.CASE_INSENSITIVE);

    @Override
    public void connect(MessageContext messageContext) {
        String objectClass = (String) getParameter(messageContext, LDAPConstants.OBJECT_CLASS);
        String filter = (String) getParameter(messageContext, LDAPConstants.FILTERS);
        String dn = (String) getParameter(messageContext, LDAPConstants.DN);
        String[] returnAttributes = {};
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
        Set<String> binaryAttributes = parseBinaryAttributes(
                LDAPUtils.lookupContextParams(messageContext, LDAPConstants.BINARY_ATTRIBUTES));

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
                if (pageSize <= 0) {
                    handleException("pageSize must be a positive integer, got: " + pageSize, messageContext);
                    return;
                }

                LdapContext ldapContext = LDAPUtils.getLdapContext(messageContext);
                // Need this when there are range chased attributes
                DirContext context = LDAPUtils.getDirectoryContext(messageContext);
                try {
                    byte[] cookie = null;
                    boolean hasResults = false;
                    int totalCollected = 0;
                    boolean limitReached = false;
                    do {
                        ldapContext.setRequestControls(new Control[]{
                                new PagedResultsControl(pageSize, cookie, Control.CRITICAL)
                        });
                        NamingEnumeration<SearchResult> results = searchInUserBase(dn, searchFilter,
                                returnAttributes, searchScope, ldapContext, 0);
                        if (results != null) {
                            while (results.hasMoreElements()) {
                                hasResults = true;
                                SearchResult entityResult = results.next();
                                processObjectGuid(entityResult);
                                result.addChild(prepareNode(entityResult, factory, ns, returnAttributes, binaryAttributes, context));
                                totalCollected++;
                                if (limit > 0 && totalCollected >= limit) {
                                    limitReached = true;
                                    break;
                                }
                            }
                        }
                        cookie = extractPagedCookie(ldapContext);
                    } while (!limitReached && cookie != null && cookie.length > 0);

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
                    try { context.close(); } catch (NamingException ignored) {}
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
                                result.addChild(prepareNode(entityResult, factory, ns, returnAttributes,
                                        binaryAttributes, context));
                            }
                        } else if (!allowEmptySearchResult) {
                            throw new NamingException("No matching result or entity found for this search");
                        }
                    } else {
                        SearchResult entityResult = makeSureOnlyOneMatch(results, allowEmptySearchResult);
                        processObjectGuid(entityResult);
                        result.addChild(prepareNode(entityResult, factory, ns, returnAttributes,
                                binaryAttributes, context));
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

    protected OMElement prepareNode(SearchResult entityResult, OMFactory factory, OMNamespace ns,
                                    String[] returnAttributes, Set<String> binaryAttributes,
                                    DirContext context) throws NamingException {
        Attributes attributes = entityResult.getAttributes();
        Attribute attribute;
        OMElement entry = factory.createOMElement(LDAPConstants.ENTRY, ns);
        OMElement dnattr = factory.createOMElement(LDAPConstants.DN, ns);
        dnattr.setText(entityResult.getNameInNamespace());
        entry.addChild(dnattr);
        if (returnAttributes.length == 0) {
            Set<String> chasedRangedAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            NamingEnumeration<String> ids = attributes.getIDs();
            while (ids.hasMore()) {
                String id = ids.next();
                Matcher rangeMatcher = RANGED_ATTRIBUTE_PATTERN.matcher(id);
                if (rangeMatcher.matches()) {
                    String baseName = rangeMatcher.group(1);
                    if (chasedRangedAttributes.add(baseName)) {
                        addAttributeElements(entry, factory, ns, baseName, binaryAttributes,
                                collectRangedValues(context, entityResult.getNameInNamespace(),
                                        baseName, attributes.get(id)));
                    }
                    continue;
                }
                Attribute attr = attributes.get(id);
                NamingEnumeration ne = attr.getAll();
                while (ne.hasMoreElements()) {
                    Object element = ne.next();
                    OMElement omElement = factory.createOMElement(id, ns);
                    omElement.setText(getAttributeValue(element, id, binaryAttributes));
                    entry.addChild(omElement);
                }
            }
        } else {
            for (int i = 0; i < returnAttributes.length; i++) {
                String requestedAttribute = returnAttributes[i];
                attribute = attributes.get(requestedAttribute);

                // Remove ";" from returnAttribute elements to prevent invalid xml generation
                String baseName = requestedAttribute;
                if (requestedAttribute.contains(";")) {
                    baseName = requestedAttribute.split("(?=;)")[0];
                }
                if ((attribute == null || attribute.size() == 0) && requestedAttribute.equals(baseName)) {
                    // AD serves attributes larger than MaxValRange under a ranged name, leaving the
                    // plain one absent or empty; chase the ranges so the caller gets the full set
                    Attribute rangedAttribute = findRangedAttribute(attributes, baseName);
                    if (rangedAttribute != null) {
                        addAttributeElements(entry, factory, ns, baseName, binaryAttributes,
                                collectRangedValues(context, entityResult.getNameInNamespace(),
                                        baseName, rangedAttribute));
                        continue;
                    }
                }
                if (attribute != null) {
                    NamingEnumeration ne = attribute.getAll();
                    while (ne.hasMoreElements()) {
                        Object element = ne.next();
                        OMElement attr = factory.createOMElement(baseName, ns);
                        attr.setText(getAttributeValue(element, baseName, binaryAttributes));
                        entry.addChild(attr);
                    }
                }
            }
        }
        return entry;
    }

    private void addAttributeElements(OMElement entry, OMFactory factory, OMNamespace ns, String attributeName,
                                      Set<String> binaryAttributes, List<Object> values) {
        // Strip attribute options (e.g. "member;binary") so the element name stays a valid xml name
        int optionIndex = attributeName.indexOf(';');
        String elementName = optionIndex >= 0 ? attributeName.substring(0, optionIndex) : attributeName;
        for (Object value : values) {
            OMElement attr = factory.createOMElement(elementName, ns);
            attr.setText(getAttributeValue(value, attributeName, binaryAttributes));
            entry.addChild(attr);
        }
    }

    private Attribute findRangedAttribute(Attributes attributes, String baseName) throws NamingException {
        NamingEnumeration<String> ids = attributes.getIDs();
        while (ids.hasMore()) {
            String id = ids.next();
            Matcher matcher = RANGED_ATTRIBUTE_PATTERN.matcher(id);
            if (matcher.matches() && matcher.group(1).equalsIgnoreCase(baseName)) {
                return attributes.get(id);
            }
        }
        return null;
    }

    /**
     * Collects every value of an attribute that Active Directory has split into ranged
     * fragments (MS-ADTS range option). Starting from the fragment the search returned,
     * requests {@code <name>;range=<next>-*} against the entry until the server answers
     * with the terminal {@code <name>;range=<n>-*} form.
     *
     * @param context    directory context used for the follow-up range reads
     * @param entryDn    full DN of the entry the attribute belongs to
     * @param baseName   attribute name without the range option
     * @param firstRange the ranged fragment returned by the original search
     * @return all values of the attribute, in range order
     */
    protected List<Object> collectRangedValues(DirContext context, String entryDn, String baseName,
                                               Attribute firstRange) throws NamingException {
        List<Object> values = new ArrayList<>();
        Attribute current = firstRange;
        long nextStart = 0;
        while (current != null) {
            NamingEnumeration<?> valueEnum = current.getAll();
            while (valueEnum.hasMoreElements()) {
                values.add(valueEnum.next());
            }
            Matcher matcher = RANGED_ATTRIBUTE_PATTERN.matcher(current.getID());
            if (!matcher.matches() || "*".equals(matcher.group(3))) {
                // Terminal form <name>;range=<n>-* : all values have been returned
                break;
            }
            long rangeEnd = Long.parseLong(matcher.group(3));
            if (rangeEnd + 1 <= nextStart) {
                // Server did not advance the range; stop instead of looping forever
                break;
            }
            nextStart = rangeEnd + 1;
            Attributes nextAttributes = context.getAttributes(new CompositeName().add(entryDn),
                    new String[]{baseName + ";range=" + nextStart + "-*"});
            current = findRangedAttribute(nextAttributes, baseName);
        }
        return values;
    }

    private Set<String> parseBinaryAttributes(String binaryAttributes) {
        Set<String> binaryAttributeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        binaryAttributeSet.add(LDAPConstants.OBJECT_GUID);
        if (StringUtils.isNotBlank(binaryAttributes)) {
            binaryAttributeSet.addAll(Arrays.asList(binaryAttributes.trim().split("\\s+")));
        }
        return binaryAttributeSet;
    }

    private String getAttributeValue(Object element, String attributeName, Set<String> binaryAttributes) {
        String elementType = element.getClass().toString();
        if (elementType.equals("class java.lang.String")) {
            return (String) element;
        }
        if (elementType.equals("class [B")) {
            // Strip attribute options (e.g. "userCertificate;binary") before matching configured names
            // Only attributes defined in binaryAttributes will be base64 encoded
            int optionIndex = attributeName.indexOf(';');
            String name = optionIndex >= 0 ? attributeName.substring(0, optionIndex) : attributeName;
            if (binaryAttributes.contains(name)) {
                return Base64.getEncoder().encodeToString((byte[]) element);
            }
            return new String((byte[]) element);
        }
        return "";
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
            handleException("Error while passing the JSON object", e, messageContext);
        }
        if (objectClass != null && !objectClass.isEmpty()) {
            return "(&(objectClass=" + objectClass + ")" + attrFilter + ")";
        } else {
            return attrFilter;
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
