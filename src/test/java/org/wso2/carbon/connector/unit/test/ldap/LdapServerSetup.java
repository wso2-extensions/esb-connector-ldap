package org.wso2.carbon.connector.unit.test.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DN;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.MessageContext;

import javax.naming.Context;
import javax.naming.directory.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.Hashtable;

public class LdapServerSetup {

    private String userBase = null;
    protected String testUserId = null;
    private String providerUrl = null;
    private String securityPrincipal = null;
    private String securityCredentials = null;
    private String baseDN = null;
    private int ldapPort = 0;
    protected boolean useEmbeddedLDAP = true;

    private InMemoryDirectoryServer ldapServer;

    public void initializeProperties() {
        userBase = "ou=People,dc=wso2,dc=com";
        testUserId = "john004";
        providerUrl = "ldap://localhost:10389/";
        securityPrincipal = "cn=admin,dc=wso2,dc=com";
        securityCredentials = "19902";
        baseDN = "dc=wso2,dc=com";
        ldapPort = 10389;
        useEmbeddedLDAP = true;
    }

    public void initializeEmbeddedLDAPServer() throws Exception {
        InMemoryListenerConfig inMemoryListenerConfig = InMemoryListenerConfig.createLDAPConfig("default", ldapPort);
        InMemoryDirectoryServerConfig directoryServerConfig = new InMemoryDirectoryServerConfig(new DN(baseDN));
        directoryServerConfig.setListenerConfigs(inMemoryListenerConfig);
        directoryServerConfig.addAdditionalBindCredentials(securityPrincipal, securityCredentials);
        ldapServer = new InMemoryDirectoryServer(directoryServerConfig);

        ldapServer.startListening();

        com.unboundid.ldap.sdk.Entry wso2Entry = new com.unboundid.ldap.sdk.Entry(baseDN);
        wso2Entry.addAttribute("objectClass", "dcObject");
        wso2Entry.addAttribute("objectClass", "organizationalUnit");
        wso2Entry.addAttribute("ou", "WSO2");
        wso2Entry.addAttribute("dc", "WSO2");

        ldapServer.add(wso2Entry);

        com.unboundid.ldap.sdk.Entry entry = new com.unboundid.ldap.sdk.Entry(userBase);
        entry.addAttribute("objectClass", "organizationalUnit");
        ldapServer.add(entry);
    }

    public void deleteSampleEntry() throws Exception {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
        env.put(Context.SECURITY_CREDENTIALS, securityCredentials);

        DirContext ctx = new InitialDirContext(env);
        String dn = "uid=" + testUserId + "," + userBase;
        ctx.destroySubcontext(dn);
    }

    public static OMElement createOMElement(String xml) {
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xml));
            StAXOMBuilder builder = new StAXOMBuilder(reader);
            return builder.getDocumentElement();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    protected MessageContext createMessageContext() throws AxisFault {
        org.apache.axis2.context.MessageContext mc = new org.apache.axis2.context.MessageContext();
        SynapseConfiguration config = new SynapseConfiguration();
        SynapseEnvironment env = new Axis2SynapseEnvironment(config);
        MessageContext messageContext = new Axis2MessageContext(mc, config, env);
        org.apache.axiom.soap.SOAPEnvelope envelope = OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
        OMDocument omDoc = OMAbstractFactory.getSOAP11Factory().createOMDocument();
        omDoc.addChild(envelope);
        envelope.getBody().addChild(createOMElement("<a>test</a>"));
        messageContext.setEnvelope(envelope);
        return messageContext;
    }

    public void createSampleEntity() throws Exception {

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        env.put(Context.PROVIDER_URL, providerUrl);
        env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
        env.put(Context.SECURITY_CREDENTIALS, securityCredentials);

        DirContext ctx = new InitialDirContext(env);
        Attributes entry = new BasicAttributes();
        Attribute obClassAttr = new BasicAttribute("objectClass");
        obClassAttr.add("inetOrgPerson");
        entry.put(obClassAttr);

        Attribute mailAttr = new BasicAttribute("mail");
        mailAttr.add(testUserId + "@wso2.com");
        entry.put(mailAttr);

        Attribute passAttr = new BasicAttribute("userPassword");
        passAttr.add("12345");
        entry.put(passAttr);

        Attribute snAttr = new BasicAttribute("sn");
        snAttr.add("dim");
        entry.put(snAttr);

        Attribute cnAttr = new BasicAttribute("cn");
        cnAttr.add("dim");
        entry.put(cnAttr);

        String dn = "uid=" + testUserId + "," + userBase;

        ctx.createSubcontext(dn, entry);
    }

    protected void cleanup() {
        if (ldapServer != null) {
            ldapServer.shutDown(true);
        }
        ldapServer = null;
    }
}
