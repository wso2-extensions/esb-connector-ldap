# Working with CRUD operations in LDAP

[[Overview]](#overview)  [[Operation details]](#operation-details)  [[Sample configuration]](#sample-configuration)

### Overview 

The following CRUD operations allow you to work with LDAP. Click an operation name to see details on how to use it.
For a sample proxy service that illustrates how to work with each operation, see [Sample configuration](#sample-configuration).

| Operation        | Description |
| ------------- |-------------|
| [addEntry](#creating-a-new-ldap-entry)    | Creates a new LDAP entry in the LDAP server. |
| [searchEntry](#searching-a-ldap-entry)      | Performs a search for one or more LDAP entities with the specified search keys.  |
| [updateEntry](#updating-a-ldap-entry)    | Updates an existing LDAP entry in the LDAP server. |
| [deleteEntry](#deleting-a-ldap-entry)    | Deletes an existing LDAP entry from the LDAP server.    |

### Operation details

This section provides more details on each of the operations.

#### Creating a new LDAP entry

The addEntry operation creates a new LDAP entry in the LDAP server.

**addEntry**
```xml
<ldap.addEntry>
    <objectClass>{$ctx:objectClass}</objectClass>
    <dn>{$ctx:dn}</dn>
    <attributes>{$ctx:attributes}</attributes>
</ldap.addEntry>
```

**Properties**
* objectClass : The object class of the new entry.
* dn : The distinguished name of the new entry. This should be a unique DN that does not already exist in the LDAP server.
* attributes : The other attributes of the entry other than the DN. These attributes should be specified as comma separated key-value pairs.

**Sample request**

Following is a sample request that can be handled by the addEntry operation.

```json
{ 
   "providerUrl":"ldap://localhost:10389/",
   "securityPrincipal":"cn=admin,dc=wso2,dc=com",
   "securityCredentials":"comadmin",
   "secureConnection":"false",
   "disableSSLCertificateChecking":"false",
   "application":"ldap",
   "operation":"createEntity",
   "content":{ 
      "objectClass":"inetOrgPerson",
      "dn":"uid=testDim20,ou=staff,dc=wso2,dc=com",
      "attributes":{ 
         "mail":"testDim1s22c@wso2.com",
         "userPassword":"12345",
         "sn":"dim",
         "cn":"dim",
         "manager":"cn=dimuthuu,ou=Groups,dc=example,dc=com"
      }
   }
}
```

**Sample response**

Given below is a sample response for the addEntry operation.

```json
{"result":{"message":"Success"}}
```
**Related LDAP documentation**
https://directory.apache.org/api/user-guide/2.4-adding.html

#### Searching a LDAP entry

The searchEntry operation performs a search for one or more LDAP entities based on the specified search keys.

**searchEntry**
```xml
<ldap.searchEntry>
    <objectClass>{$ctx:objectClass}</objectClass>
    <dn>{$ctx:dn}</dn>
    <filters>{$ctx:filters}</filters>
    <attributes>{$ctx:attributes}</attributes>
    <onlyOneReference>{$ctx:onlyOneReference}</onlyOneReference>
    <limit>1000</limit>
</ldap.searchEntry>
```

**Properties**
* objectClass : The object class of the entry you need to search.
* filters : The keywords to use in the search. The parameters should be in JSON format as follow:
  ```json 
  "filters":{ "uid":"john", "mail":"testDim2@gmail.com"}
  ```
* dn : The distinguished name of the entry you need to search.
* attributes : The attributes of the LDAP entry that should be included in the search result. 
* onlyOneReference : Boolean value whether to guarantee or not only one reference.
* limit : This allows you to set a limit on the number of search results. If this property is not defined the maximum no of search results will be returned.

**Sample request**

Following is a sample request that can be handled by the searchEntry operation.

```json
{
   "providerUrl":"ldap://server.example.com",
   "securityPrincipal":"cn=admin,dc=example,dc=com",
   "securityCredentials":"admin",
   "secureConnection":"false",
   "disableSSLCertificateChecking":"false",
   "application":"ldap",
    "operation":"searchEntity",
    "content":{
        "dn":"ou=sales,dc=example,dc=com",
        "objectClass":"inetOrgPerson",
        "attributes":"mail,uid,givenName,manager,objectGUID",
      "filters":{
          "manager":"cn=sales-group,ou=sales,dc=example,dc=com","uid":"rajjaz"},
        "onlyOneReference":"false"
    }
}
```

**Sample response**

Given below is a sample response for the searchEntry operation.

```json
{
   "result":{
      "entry":{
         "dn":"uid=test_001,ou=staff,dc=example,dc=com",
         "mail":"test_001@wso2.com",
         "uid":"test_001"
      }
   }
}
```
**Related LDAP documentation**
https://directory.apache.org/api/user-guide/2.3-searching.html

#### Updating a LDAP entry

The updateEntry operation updates an existing LDAP entry in the LDAP server based on the specified changes.

**updateEntry**
```xml
<ldap.updateEntry>
    <mode>{$ctx:mode}</mode>
    <dn>{$ctx:dn}</dn>
    <attributes>{$ctx:attributes}</attributes>
</ldap.updateEntry>
```

**Properties**
* mode : The mode of the update operation. Possible values are as follows:
    * replace : Replaces an existing attribute with the new attribute that is specified.
    * add : Adds a new attributes
    * remove : Removes an existing attribute.
* dn : The distinguished name of the entry to be updated.
* attributes : Attributes of the entry to be updated. The attributes to be updated should be specified as comma separated key-value pairs.

**Sample request**

Following is a sample request that can be handled by the updateEntry operation.

```json
{
    "providerUrl":"ldap://localhost:10389/",
    "securityPrincipal":"cn=admin,dc=wso2,dc=com",
    "securityCredentials":"comadmin",
    "secureConnection":"false",
    "disableSSLCertificateChecking":"false",
    "application": "ldap",
    "operation": "updateEntity",
    "content":{
        "mode":"replace",
        "dn":"uid=testDim20,ou=staff,dc=wso2,dc=com",
        "attributes":{ 
         "mail":"testDim1s22c@wso2.com",
         "userPassword":"12345",
         "sn":"dim",
         "cn":"dim",
         "manager":"cn=dimuthuu,ou=Groups,dc=example,dc=com"
      }
    }
}
```

**Sample response**

Given below is a sample response for the updateEntry operation.

```json
{"result":{"message":"Success"}}
```
**Related LDAP documentation**
https://directory.apache.org/api/user-guide/2.6-modifying.html

#### Deleting a LDAP entry

The deleteEntry operation deletes an existing LDAP entry from the LDAP server.

**deleteEntry**
```xml
<ldap.deleteEntry>
    <dn>{$ctx:dn}</dn>
</ldap.deleteEntry>
```

**Properties**
* dn : The distinguished name of the entry to be deleted.

**Sample request**

Following is a sample request that can be handled by the deleteEntry operation.

```json
{
    "providerUrl":"ldap://localhost:10389/",
    "securityPrincipal":"cn=admin,dc=wso2,dc=com",
    "securityCredentials":"comadmin",
    "secureConnection":"false",
    "disableSSLCertificateChecking":"false",
    "application": "ldap",
    "operation":"deleteEntity",
    "content":{
        "dn":"uid=testDim20,ou=staff,dc=wso2,dc=com"
    }
}
```

**Sample response**

Given below is a sample response for the deleteEntry operation.

```json
{"result":{"message":"Success"}}
```
**Related LDAP documentation**
https://directory.apache.org/api/user-guide/2.5-deleting.html

### Sample configuration

Following example illustrates how to connect to LDAP with the init operation and addEntry operation.

1. Create a sample proxy as below :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<proxy xmlns="http://ws.apache.org/ns/synapse" name="addEntry" transports="https,http" statistics="disable" trace="disable" startOnLoad="true">
   <target>
      <inSequence>
         <property name="objectClass" expression="json-eval($.content.objectClass)" />
         <property name="dn" expression="json-eval($.content.dn)" />
         <property name="attributes" expression="json-eval($.content.attributes)" />
         <property name="providerUrl" expression="json-eval($.providerUrl)" />
         <property name="securityPrincipal" expression="json-eval($.securityPrincipal)" />
         <property name="securityCredentials" expression="json-eval($.securityCredentials)" />
         <property name="secureConnection" expression="json-eval($.secureConnection)" />
         <property name="disableSSLCertificateChecking" expression="json-eval($.disableSSLCertificateChecking)" />
         <property name="timeout" expression="json-eval($.timeout)" />
         <ldap.init>
            <providerUrl>{get-property('providerUrl')}</providerUrl>
            <securityPrincipal>{get-property('securityPrincipal')}</securityPrincipal>
            <securityCredentials>{get-property('securityCredentials')}</securityCredentials>
            <secureConnection>{get-property('secureConnection')}</secureConnection>
            <disableSSLCertificateChecking>{get-property('disableSSLCertificateChecking')}</disableSSLCertificateChecking>
            <timeout>{get-property('timeout')}</timeout>
         </ldap.init>
         <ldap.addEntry>
            <objectClass>{get-property('objectClass')}</objectClass>
            <dn>{get-property('dn')}</dn>
            <attributes>{get-property('attributes')}</attributes>
         </ldap.addEntry>
         <respond />
      </inSequence>
      <faultSequence>
         <respond />
      </faultSequence>
   </target>
   <description />
</proxy>
```
2. Create an json file named addEntry.json and copy the configurations given below to it:

```json
{ 
   "providerUrl":"ldap://localhost:10389/",
   "securityPrincipal":"cn=admin,dc=wso2,dc=com",
   "securityCredentials":"comadmin",
   "secureConnection":"false",
   "disableSSLCertificateChecking":"false",
   "application":"ldap",
   "operation":"createEntity",
   "content":{ 
      "objectClass":"inetOrgPerson",
      "dn":"uid=testDim20,ou=staff,dc=wso2,dc=com",
      "attributes":{ 
         "mail":"testDim1s22c@wso2.com",
         "userPassword":"12345",
         "sn":"dim",
         "cn":"dim",
         "manager":"cn=dimuthuu,ou=Groups,dc=example,dc=com"
      }
   }
}
```

3. Replace the credentials with your values.

4. Execute the following curl command:

```bash
curl http://localhost:8280/services/addEntry -H "Content-Type: application/json" -d @addEntry.json
```
5. LDAP returns an json response similar to the one shown below:
 
```json
{"result":{"message":"Success"}}
```