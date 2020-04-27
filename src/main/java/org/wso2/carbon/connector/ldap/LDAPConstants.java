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

public class LDAPConstants {
    public static final String SECURE_CONNECTION = "secureConnection";
    public static final String DISABLE_SSL_CERT_CHECKING = "disableSSLCertificateChecking";
    public static final String PROVIDER_URL = "providerUrl";
    public static final String SECURITY_PRINCIPAL = "securityPrincipal";
    public static final String SECURITY_CREDENTIALS = "securityCredentials";
    public static final String CONNECTOR_NAMESPACE = "http://org.wso2.esbconnectors.ldap";
    public static final String OBJECT_CLASS = "objectClass";
    public static final String OBJECT_GUID = "objectGUID";
    public static final String LIMIT = "limit";
    public static final String ATTRIBUTES = "attributes";
    public static final String DN = "dn";
    public static final String SCOPE = "scope";
    public static final String RESULT = "result";
    public static final String MESSAGE = "message";
    public static final String SUCCESS = "Success";
    public static final String PASSWORD = "password";
    public static final String FAIL = "Fail";
    public static final String SSL = "ssl";
    public static final String TLS = "TLS";
    public static final String NAMESPACE = "ns";
    public static final String ERROR = "error";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_CODE = "errorCode";
    public static final String ONLY_ONE_REFERENCE = "onlyOneReference";
    public static final String FILTERS = "filters";
    public static final String ENTRY = "entry";
    public static final String MODE = "mode";
    public static final String REPLACE = "replace";
    public static final String ADD = "add";
    public static final String REMOVE = "remove";
    public static final String COM_SUN_JNDI_LDAP_LDAPCTXFACTORY =
            "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String JAVA_NAMING_LDAP_FACTORY_SOCKET = "java.naming.ldap.factory.socket";
    public static final String ORG_WSO2_CARBON_CONNECTOR_SECURITY_MYSSLSOCKETFACTORY =
            "org.wso2.carbon.connector.security.MySSLSocketFactory";
    public static final String JAVA_NAMING_LDAP_ATTRIBUTE_BINARY =
            "java.naming.ldap.attributes.binary";
    public static final String COM_SUN_JNDI_LDAP_CONNECT_POOL = "com.sun.jndi.ldap.connect.pool";
    public static final String COM_SUN_JNDI_LDAP_CONNECT_POOL_PROTOCOL = "com.sun.jndi.ldap.connect.pool.protocol";
    public static final String COM_SUN_JNDI_LDAP_CONNECT_POOL_INITSIZE = "com.sun.jndi.ldap.connect.pool.initsize";
    public static final String COM_SUN_JNDI_LDAP_CONNECT_POOL_MAXSIZE = "com.sun.jndi.ldap.connect.pool.maxsize";
    public static final String CONNECTION_POOLING_ENABLED = "connectionPoolingEnabled";
    public static final String CONNECTION_POOLING_PROTOCOL = "connectionPoolingProtocol";
    public static final String CONNECTION_POOLING_INIT_SIZE = "connectionPoolingInitSize";
    public static final String CONNECTION_POOLING_MAX_SIZE = "connectionPoolingMaxSize";
    public static final String COM_JAVA_JNDI_LDAP_READ_TIMEOUT = "com.sun.jndi.ldap.read.timeout";
    public static final String TIMEOUT = "timeout";

    public static final class ErrorConstants {
        public static final int SEARCH_ERROR = 7000001;
        public static final int INVALID_LDAP_CREDENTIALS = 7000002;
        public static final int ADD_ENTRY_ERROR = 7000003;
        public static final int UPDATE_ENTRY_ERROR = 7000004;
        public static final int DELETE_ENTRY_ERROR = 7000005;
        public static final int ENTRY_DOESNOT_EXISTS_ERROR = 7000006;

    }
}
