# Working with User Authentication in LDAP

[[  Overview ]](#overview)  [[ Operation details ]](#operation-details)  [[  Sample configuration  ]](#sample-configuration)

### Overview 

LDAP authentication is a major requirement in most LDAP based applications. The  authenticate operation simplifies the LDAP authentication mechanism. This operation authenticates the provided Distinguished Name(DN) and password against the LDAP server, and returns either a success or failure response depending on whether the authentication was successful or not.

**authenticate**
```xml
<ldap.authenticate>
    <dn>{$ctx:dn}</dn>
    <password>{$ctx:password}</password>
</ldap.authenticate>
```

**Properties**
* dn : The distinguished name of the user.
* password : The password of the user.

**Sample request**

Following is a sample request that can be handled by the authenticate operation.

```json
{
    "providerUrl":"ldap://localhost:10389/",
    "securityPrincipal":"cn=admin,dc=wso2,dc=com",
    "securityCredentials":"comadmin",
    "secureConnection":"false",
    "disableSSLCertificateChecking":"false",
    "application": "ldap",
    "operation":"authenticate",
    "content":{
        "dn":"uid=testDim20,ou=staff,dc=wso2,dc=com",
        "password":"12345"
    }
}
```

**Sample response**

Given below is a sample success response for the authenticate operation.

**Authentication success response**
```xml
<Response xmlns="http://localhost/services/ldap">
    <result>
        <message>Success</message>
    </result>
</Response>
```

**Authentication failure response**
```xml
<Response xmlns="http://localhost/services/ldap">
    <result>
        <message>Fail</message>
    </result>
</Response>
```

#### Error codes
This section describes the connector error codes and their meanings.


| Error Code  | Description |
| ------------- | ------------- |
| 7000001 | An error occurred while searching a LDAP entry.    |
| 7000002 | LDAP root user's credentials are invalid.    |
| 7000003 | An error occurred while adding a new LDAP entry.    |
| 7000004 | An error occurred while updating an existing LDAP entry.    |
| 7000005 | An error occurred while deleting a LDAP entry.    |
| 7000006 | The LDAP entry that is required to perform the operation does not exist.    |

**Sample error response**
```xml
<Fault xmlns="http://localhost/services/ldap">
    <error>
        <errorCode>700000X</errorCode>
        <errorMessage>Error Message</errorMessage>
    </error>
</Fault>
```

**Related LDAP documentation**

### Sample configuration

Following example illustrates how to connect to LDAP with the init operation and search operation.

1. Create a sample proxy as below :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<proxy xmlns="http://ws.apache.org/ns/synapse" name="authenticate" transports="https,http" statistics="disable" trace="disable" startOnLoad="true">
   <target>
      <inSequence>
         <property name="objectClass" expression="json-eval($.content.objectClass)" />
         <property name="attributes" expression="json-eval($.content.attributes)" />
         <property name="providerUrl" expression="json-eval($.providerUrl)" />
         <property name="securityPrincipal" expression="json-eval($.securityPrincipal)" />
         <property name="securityCredentials" expression="json-eval($.securityCredentials)" />
         <property name="secureConnection" expression="json-eval($.secureConnection)" />
         <property name="disableSSLCertificateChecking" expression="json-eval($.disableSSLCertificateChecking)" />
         <property name="dn" expression="json-eval($.content.dn)" />
         <property name="password" expression="json-eval($.content.password)" />
         <ldap.init>
            <providerUrl>{$ctx:providerUrl}</providerUrl>
            <securityPrincipal>{$ctx:securityPrincipal}</securityPrincipal>
            <securityCredentials>{$ctx:securityCredentials}</securityCredentials>
            <secureConnection>{$ctx:secureConnection}</secureConnection>
            <disableSSLCertificateChecking>{$ctx:disableSSLCertificateChecking}</disableSSLCertificateChecking>
         </ldap.init>
         <ldap.authenticate>
            <dn>{$ctx:dn}</dn>
            <password>{$ctx:password}</password>
         </ldap.authenticate>
         <respond />
      </inSequence>
      <outSequence>
         <send />
      </outSequence>
   </target>
   <description />
</proxy>
```

2. Create a json file named authenticate.json and copy the configurations given below to it:

```json
{
    "providerUrl":"ldap://localhost:10389/",
    "securityPrincipal":"cn=admin,dc=wso2,dc=com",
    "securityCredentials":"comadmin",
    "secureConnection":"false",
    "disableSSLCertificateChecking":"false",
    "application": "ldap",
    "operation":"authenticate",
    "content":{
        "dn":"uid=testDim20,ou=staff,dc=wso2,dc=com",
        "password":"12345"
    }
}
```
3. Replace the credentials with your values.

4. Execute the following curl command:

```bash
curl http://localhost:8280/services/authenticate -H "Content-Type: application/json" -d @authenticate.json
```

5. LDAP returns a json response similar to the one shown below:
 
```xml
<Response xmlns="http://localhost/services/ldap">
    <result>
        <message>Success</message>
    </result>
</Response>
```
