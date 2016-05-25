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
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.core.ConnectException;

public class SearchEntry extends AbstractConnector {
	protected static Log log = LogFactory.getLog(SearchEntry.class);

	@Override
	public void connect(MessageContext messageContext) throws ConnectException {
		String objectClass = (String) getParameter(messageContext, LDAPConstants.OBJECT_CLASS);
		String filter = (String) getParameter(messageContext, LDAPConstants.FILTERS);
		String dn = (String) getParameter(messageContext, LDAPConstants.DN);
		String returnAttributes[] = ((String) getParameter(messageContext, LDAPConstants.ATTRIBUTES)).split(",");
		boolean onlyOneReference =
				Boolean.valueOf((String) getParameter(messageContext, LDAPConstants.ONLY_ONE_REFERENCE));

		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
		OMElement result = factory.createOMElement(LDAPConstants.RESULT, ns);

		try {
			DirContext context = LDAPUtils.getDirectoryContext(messageContext);

			String attrFilter = generateAttrFilter(filter);
			String searchFilter = generateSearchFilter(objectClass, attrFilter);
			NamingEnumeration<SearchResult> results = null;
			try {
				results = searchInUserBase(dn, searchFilter, returnAttributes, SearchControls.SUBTREE_SCOPE, context);
				SearchResult entityResult = null;

				if (!onlyOneReference) {
					if (results != null && results.hasMore()) {
						while (results.hasMore()) {
							entityResult = results.next();
							result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
						}
					}
				} else {
					entityResult = makeSureOnlyOneMatch(results);
					if (entityResult == null)
						throw new NamingException("Multiple objects for the searched target have been found. Try to " +
						                          "change onlyOneReference option");
					result.addChild(prepareNode(entityResult, factory, ns, returnAttributes));
				}

				LDAPUtils.preparePayload(messageContext, result);

				if (context != null) {
					context.close();
				}

			} catch (NamingException e) { //LDAP Errors are catched
				LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.SEARCH_ERROR, e);
				throw new SynapseException(e);
			}

		} catch (NamingException e) { //Authentication failures are catched
			LDAPUtils.handleErrorResponse(messageContext, LDAPConstants.ErrorConstants.INVALID_LDAP_CREDENTIALS, e);
			throw new SynapseException(e);
		}
	}

	private OMElement prepareNode(SearchResult entityResult, OMFactory factory, OMNamespace ns,
	                              String returnAttributes[]) throws NamingException {
		Attributes attributes = entityResult.getAttributes();
		Attribute attribute;
		OMElement entry = factory.createOMElement(LDAPConstants.ENTRY, ns);
		OMElement dnattr = factory.createOMElement(LDAPConstants.DN, ns);
		dnattr.setText(entityResult.getNameInNamespace());
		entry.addChild(dnattr);

		for (int i = 0; i < returnAttributes.length; i++) {
			attribute = attributes.get(returnAttributes[i]);
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

	private SearchResult makeSureOnlyOneMatch(NamingEnumeration<SearchResult> results) {
		SearchResult searchResult = null;

		if (results.hasMoreElements()) {
			searchResult = (SearchResult) results.nextElement();

			// Make sure there is not another item available, there should be only 1 match
			if (results.hasMoreElements()) {
				// Here the code has matched multiple objects for the searched target
				return null;
			}
		}
		return searchResult;
	}

	private NamingEnumeration<SearchResult> searchInUserBase(String dn, String searchFilter,
	                                                         String[] returningAttributes, int searchScope,
	                                                         DirContext rootContext) throws NamingException {
		String userBase = dn;
		SearchControls userSearchControl = new SearchControls();
		userSearchControl.setReturningAttributes(returningAttributes);
		userSearchControl.setSearchScope(searchScope);
		NamingEnumeration<SearchResult> userSearchResults;
		userSearchResults = rootContext.search(userBase, searchFilter, userSearchControl);
		return userSearchResults;

	}

	private String generateAttrFilter(String filter) {
		String attrFilter = "";
		if (filter != null && filter.trim().length() > 0 && !filter.trim().equals("null")) {
			String filterArray[] = filter.split(",");
			if (filterArray != null && filterArray.length > 0) {
				for (int i = 0; i < filterArray.length; i++) {
					attrFilter += "(";
					attrFilter += filterArray[i];
					attrFilter += ")";
				}
			}
		}
		return attrFilter;
	}

	private String generateSearchFilter(String objectClass, String attrFilter) {
		return "(&(objectClass=" + objectClass + ")" + attrFilter + ")";
	}
}
