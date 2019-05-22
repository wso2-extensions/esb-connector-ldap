# Configuring LDAP Operations

[[Prerequisites]](#Prerequisites) [[Initializing the Connector]](#initializing-the-connector)

## Prerequisites

### Importing the LDAP Certificate

You can follow the following steps to import your LDAP certificate into wso2esb client’s keystore as follows:

1. To encrypt the connections, we'll need to configure a certificate authority (https://www.digitalocean.com/community/tutorials/how-to-encrypt-openldap-connections-using-starttls) and use it to sign the keys for the LDAP server.

2. Use the ESB Management Console or the following command to import that certificate into the EI client keystore. 
    ```
    keytool -importcert -file <certificate file> -keystore <EI>/repository/resources/security/client-truststore.jks -alias "LDAP"
    ```
3. Restart the server and deploy the LDAP configuration.

## Initializing the Connector

To use the LDAP connector, add the <ldap.init> element in your configuration before performing any other operation. This LDAP configuration authenticates with the LDAP server in order to gain access to perform various LDAP operations.
**init**
```xml
<ldap.init>
    <providerUrl>{$ctx:providerUrl}</providerUrl>
    <securityPrincipal>{$ctx:securityPrincipal}</securityPrincipal>
    <securityCredentials>{$ctx:securityCredentials}</securityCredentials>
    <secureConnection>{$ctx:secureConnection}</secureConnection>
    <disableSSLCertificateChecking>{$ctx:disableSSLCertificateChecking}</disableSSLCertificateChecking>
    <timeout>{$ctx:timeout}</timeout>
</ldap.init>
```
**Properties** 
* providerUrl : The URL of the LDAP server.
* securityPrincipal : The Distinguished Name(DN) of the admin of the LDAP Server.
* securityCredentials : The password of the LDAP admin.
* secureConnection : The boolean value for the secure connection.
* disableSSLCertificateChecking : The boolean value to check whether certificate enable or not.
* timeout : The read timeout in milliseconds for LDAP operations.

#### Ensuring secure data
For security purposes, you should store securityCredentials in the WSO2 secure vault and make reference to it by using an alias instead of hard-coding the actual value in the configuration file. For more information, see [Working with Passwords](https://docs.wso2.com/display/EI640/Working+with+Passwords+in+the+ESB+profile).

#### Re-using LDAP configurations
For best results, save the LDAP configuration as a local entry. You can then easily reference it with the configKey attribute in your LDAP operations. For example, if you saved the above <ldap.init> entry as a local entry named ldapConfig,  you could reference it from the deleteEntry operation as follows:

```xml
<ldap.deleteEntry configKey="ldapConfig">
    <dn>{$ctx:dn}</dn>
</ldap.deleteEntry>
```

Now that you have connected to LDAP, use the information in the following topics to perform various operations with the connector:

[Working with User Authentication in LDAP](user_auth.md)

[Working with CRUD operations in LDAP](crud_ops.md)
