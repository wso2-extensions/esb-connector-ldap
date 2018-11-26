## Integration tests for WSO2 EI LDAP connector

### Pre-requisites:

 - Maven 3.x
 - Java 1.6 or above
 - org.wso2.esb.integration.integration-base is required. this test suite has been configured to download this automatically. however if its fail download following project and compile using mvn clean install command to update your local repository.
   https://github.com/wso2-extensions/esb-integration-base

### Tested Platform: 

 - Mac OSX 10.9.2
 - UBUNTU 16.04
 - WSO2 EI 6.4.0

 STEPS:

1. Make sure the WSO2 EI 6.4.0 zip file with available at "{LDAP_HOME}/repository/"

2. Follow the below mentioned steps for adding valid certificate to access LDAP server over SSL.

       	i)   To encrypt the connections, we'll need to configure a certificate authority (https://www.digitalocean.com/community/tutorials/how-to-encrypt-openldap-connections-using-starttls) and use it to sign the keys for the LDAP server.
       	ii)  Place the created certificate into {EI_Connector_Home}/repositiry/.

3. Integration Tests uses Embedded in-memory LDAP server in tests. So normally connector doesn't need an external LDAP server to run its tests.
    If you want to test the Connector with your LDAP server, do necessary changes to LDAP properties file at location
    "{LDAP_HOME}/src/test/resources/artifacts/EI/connector/config".

	    i)    providerUrl                       - URL of you LDAP server
    	ii)   securityPrincipal                 - Root user DN
    	iii)  securityCredentials               - Root user password
    	iv)   secureConnection                  - The boolean value for the secure connection.
    	v)    disableSSLCertificateChecking     - The boolean value to check whether certificate enable or not
    	vi)   onlyOneReference                  - The boolean value whether to guarantee or not only one reference
    	vii)  testUserId                        - The user ID
    	viii) ldapUserBase                      - User Base of the LDAP server
    	ix)   testUserId                        - Sample test user id
    	x)    baseDN                            - Base DN of the LDAP server
    	xi)   ldapPort                          - Port which Embedded LDAP server should be started. (Default 10389)
    	xii)  useEmbeddedLDAP                   - Use embedded LDAP server or outside ldap sever. If you want to use your LDAP server to test with the Connector, make this value - false

4. Navigate to "{LDAP_HOME}/" and run the following command.<br/>
     ``` $ mvn clean install -Dskip-tests=false```

**NOTE :**
If you are using Embedded LDAP mode in Integration Testing, please make sure that ldapPort you are assigning in config file is not used by any other application in your local machine.