/*
 *  Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.connector.ldap.LDAPConstants;
import org.wso2.carbon.connector.ldap.SearchEntry;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

/**
 * Tests transparent handling of Active Directory's ranged attribute responses
 * (MS-ADTS range option, driven by the MaxValRange policy). AD range semantics
 * cannot be emulated by the in-memory test LDAP server, so the follow-up range
 * reads are served by a stubbed {@link DirContext}.
 */
public class RangedAttributeSearchTest {

    private static final String GROUP_DN = "cn=largeGroup,ou=Groups,dc=wso2,dc=com";

    /** Exposes the protected range-chasing and node-building members for testing. */
    private static class TestableSearchEntry extends SearchEntry {
        List<Object> collect(DirContext context, String dn, String baseName, Attribute firstRange)
                throws NamingException {
            return collectRangedValues(context, dn, baseName, firstRange);
        }

        OMElement buildNode(SearchResult searchResult, OMFactory factory, OMNamespace ns,
                            String[] returnAttributes, DirContext context) throws NamingException {
            Set<String> binaryAttributes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            binaryAttributes.add(LDAPConstants.OBJECT_GUID);
            return prepareNode(searchResult, factory, ns, returnAttributes, binaryAttributes, context);
        }
    }

    /**
     * Stub DirContext answering getAttributes(name, {requestedId}) from a canned map,
     * recording each requested attribute id. Unknown range requests get an empty result.
     */
    private DirContext stubContext(final Map<String, Attributes> responses, final List<String> requestedIds) {
        return (DirContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{DirContext.class}, (proxy, method, args) -> {
                    if ("getAttributes".equals(method.getName()) && args != null && args.length == 2
                            && args[1] instanceof String[]) {
                        String requestedId = ((String[]) args[1])[0];
                        requestedIds.add(requestedId);
                        Attributes response = responses.get(requestedId);
                        return response != null ? response : new BasicAttributes(true);
                    }
                    throw new UnsupportedOperationException("Unexpected call: " + method.getName());
                });
    }

    private Attribute rangedAttribute(String id, String... values) {
        BasicAttribute attribute = new BasicAttribute(id);
        for (String value : values) {
            attribute.add(value);
        }
        return attribute;
    }

    private Attributes singleAttribute(Attribute attribute) {
        Attributes attributes = new BasicAttributes(true);
        attributes.put(attribute);
        return attributes;
    }

    @Test
    public void testChasesAllRangesUntilTerminalForm() throws Exception {
        Map<String, Attributes> responses = new HashMap<>();
        responses.put("member;range=2-*", singleAttribute(rangedAttribute("member;range=2-3", "m2", "m3")));
        responses.put("member;range=4-*", singleAttribute(rangedAttribute("member;range=4-*", "m4")));
        List<String> requestedIds = new ArrayList<>();
        DirContext context = stubContext(responses, requestedIds);

        List<Object> values = new TestableSearchEntry().collect(context, GROUP_DN, "member",
                rangedAttribute("member;range=0-1", "m0", "m1"));

        Assert.assertEquals(values, Arrays.asList("m0", "m1", "m2", "m3", "m4"));
        Assert.assertEquals(requestedIds, Arrays.asList("member;range=2-*", "member;range=4-*"));
    }

    @Test
    public void testTerminalFirstFragmentNeedsNoFollowUp() throws Exception {
        List<String> requestedIds = new ArrayList<>();
        DirContext context = stubContext(new HashMap<>(), requestedIds);

        List<Object> values = new TestableSearchEntry().collect(context, GROUP_DN, "member",
                rangedAttribute("member;range=0-*", "m0", "m1"));

        Assert.assertEquals(values, Arrays.asList("m0", "m1"));
        Assert.assertTrue(requestedIds.isEmpty());
    }

    @Test
    public void testStopsWhenFollowUpReturnsNoRangedAttribute() throws Exception {
        // Server answers the follow-up with no ranged fragment: keep what was collected
        List<String> requestedIds = new ArrayList<>();
        DirContext context = stubContext(new HashMap<>(), requestedIds);

        List<Object> values = new TestableSearchEntry().collect(context, GROUP_DN, "member",
                rangedAttribute("member;range=0-1", "m0", "m1"));

        Assert.assertEquals(values, Arrays.asList("m0", "m1"));
        Assert.assertEquals(requestedIds, Arrays.asList("member;range=2-*"));
    }

    @Test
    public void testStopsWhenServerDoesNotAdvanceRange() throws Exception {
        // A misbehaving server repeating the same fragment must not loop forever
        Map<String, Attributes> responses = new HashMap<>();
        responses.put("member;range=2-*", singleAttribute(rangedAttribute("member;range=0-1", "m0", "m1")));
        List<String> requestedIds = new ArrayList<>();
        DirContext context = stubContext(responses, requestedIds);

        List<Object> values = new TestableSearchEntry().collect(context, GROUP_DN, "member",
                rangedAttribute("member;range=0-1", "m0", "m1"));

        Assert.assertEquals(requestedIds, Arrays.asList("member;range=2-*"));
        Assert.assertEquals(values.size(), 4);
    }

    @Test
    public void testPreparedNodePrefersRangedValuesOverEmptyPlainAttribute() throws Exception {
        // AD returns an empty plain attribute alongside the populated ranged fragment;
        // the merged ranged values must win and appear under the plain attribute name
        Attributes attributes = new BasicAttributes(true);
        attributes.put(new BasicAttribute("member"));
        attributes.put(rangedAttribute("member;range=0-1", "m0", "m1"));
        attributes.put(rangedAttribute("cn", "largeGroup"));
        SearchResult searchResult = new SearchResult(GROUP_DN, null, attributes);
        searchResult.setNameInNamespace(GROUP_DN);

        Map<String, Attributes> responses = new HashMap<>();
        responses.put("member;range=2-*", singleAttribute(rangedAttribute("member;range=2-*", "m2")));
        DirContext context = stubContext(responses, new ArrayList<>());

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement entry = new TestableSearchEntry().buildNode(searchResult, factory, ns,
                new String[]{"member", "cn"}, context);

        Assert.assertEquals(collectTexts(entry, "member"), Arrays.asList("m0", "m1", "m2"));
        Assert.assertEquals(collectTexts(entry, "cn"), Arrays.asList("largeGroup"));
    }

    @Test
    public void testPreparedNodeMergesRangedValuesWhenAllAttributesRequested() throws Exception {
        Attributes attributes = new BasicAttributes(true);
        attributes.put(rangedAttribute("member;range=0-1", "m0", "m1"));
        attributes.put(rangedAttribute("cn", "largeGroup"));
        SearchResult searchResult = new SearchResult(GROUP_DN, null, attributes);
        searchResult.setNameInNamespace(GROUP_DN);

        Map<String, Attributes> responses = new HashMap<>();
        responses.put("member;range=2-*", singleAttribute(rangedAttribute("member;range=2-*", "m2")));
        DirContext context = stubContext(responses, new ArrayList<>());

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement entry = new TestableSearchEntry().buildNode(searchResult, factory, ns,
                new String[]{}, context);

        Assert.assertEquals(collectTexts(entry, "member"), Arrays.asList("m0", "m1", "m2"));
        Assert.assertEquals(collectTexts(entry, "cn"), Arrays.asList("largeGroup"));
    }

    @Test
    public void testExplicitRangeRequestKeepsSingleWindowBehaviour() throws Exception {
        // A caller asking for an explicit window must get exactly that window, no chasing
        Attributes attributes = new BasicAttributes(true);
        attributes.put(rangedAttribute("member;range=0-1", "m0", "m1"));
        SearchResult searchResult = new SearchResult(GROUP_DN, null, attributes);
        searchResult.setNameInNamespace(GROUP_DN);

        List<String> requestedIds = new ArrayList<>();
        DirContext context = stubContext(new HashMap<>(), requestedIds);

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace(LDAPConstants.CONNECTOR_NAMESPACE, LDAPConstants.NAMESPACE);
        OMElement entry = new TestableSearchEntry().buildNode(searchResult, factory, ns,
                new String[]{"member;range=0-1"}, context);

        Assert.assertEquals(collectTexts(entry, "member"), Arrays.asList("m0", "m1"));
        Assert.assertTrue(requestedIds.isEmpty());
    }

    private List<String> collectTexts(OMElement entry, String localName) {
        List<String> texts = new ArrayList<>();
        Iterator<?> children = entry.getChildrenWithLocalName(localName);
        while (children.hasNext()) {
            texts.add(((OMElement) children.next()).getText());
        }
        return texts;
    }
}
