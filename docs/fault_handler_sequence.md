# Configuring the LDAP Fault Handler Sequence

Following is a sample proxy service and fault handler sequence you can use as a starting point for handling faults when integrating with LDAP. Customize this sample to match your requirements.

**Sample Proxy**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<proxy xmlns="http://ws.apache.org/ns/synapse" name="ldap_sampleProxy" transports="https,http" statistics="disable" trace="disable" startOnLoad="true">
  <target>
    <inSequence onError="faultHandlerSeq">     
      <init>       
      </init>     
      <filter source="$axis2:HTTP_SC" regex="^[^2][0-9][0-9]">
            <then>
               <switch source="$axis2:HTTP_SC">
                  <case regex="401">
                  <!--Fill with your Error code value and expression-->
                     <property name="ERROR_CODE" value=""/>          
                     <property name="ERROR_MESSAGE" value | expression />
                  </case>
                  <case regex="422">
                     <property name="ERROR_CODE" value=""/>          
                     <property name="ERROR_MESSAGE" value | expression/>
                  </case>
                  <case regex="404">
                     <property name="ERROR_CODE" value=""/>          
                     <property name="ERROR_MESSAGE" value | expression/>
                  </case>
                  <case regex="403">
                     <property name="ERROR_CODE" value=""/>          
                     <property name="ERROR_MESSAGE" value | expression/>
                  </case>
                  <case regex="400">
                     <property name="ERROR_CODE" value=""/>          
                     <property name="ERROR_MESSAGE" value | expression/>
                  </case>
                  <case regex="500">
                     <property name="ERROR_CODE" value=""/> 
                    <property name="ERROR_MESSAGE" value | expression/>
                  </case>
                  <default>
                     <property name="ERROR_CODE" expression="$axis2:HTTP_SC"/>
                     <property name="ERROR_MESSAGE" value | expression/>
                  </default>
               </switch>
               <sequence key="faultHandlerSeq" />
            </then>
         </filter>
      <respond />
    </inSequence>
    <outSequence>
     <send></send>
    </outSequence>
  </target>
</proxy>
```
**faultHandlerSeq**
```xml
<sequence xmlns="http://ws.apache.org/ns/synapse" name="faultHandlerSeq">
<property xmlns:ns="http://org.apache.synapse/xsd" name="contentTypeValue" expression="get-property('transport', 'Content-Type')"></property>
   <filter xmlns:ns="http://org.apache.synapse/xsd" xpath="get-property('contentTypeValue') = 'application/json' or get-property('contentTypeValue') = 'text/json'">
      <then>        
          <payloadFactory media-type="json">
               <format> {"error_code":"$1", "error_message":"$2"}
          </format>
                  <args>
                     <arg expression="get-property('ERROR_CODE')" evaluator="xml"></arg>
                     <arg expression="get-property('ERROR_MESSAGE')" evaluator="xml"></arg>
                  </args>
               </payloadFactory>
               <property name="messageType" value="application/json" scope="axis2"></property>
        </then>
      </filter>
      <filter xmlns:ns="http://org.apache.synapse/xsd" xpath="get-property('contentTypeValue') = 'application/xml' or get-property('contentTypeValue') = 'text/xml'">
          <then>
          <payloadFactory media-type="xml">
                  <format>
                     <error_info>
                        <error_code>$1</error_code>
                        <error_message>$2</error_message>
                     </error_info>
                  </format>
                  <args>
                     <arg expression="get-property('ERROR_CODE')" evaluator="xml"></arg>
                     <arg expression="get-property('ERROR_MESSAGE')" evaluator="xml"></arg>
                  </args>
               </payloadFactory>
               <property name="messageType" value="text/xml" scope="axis2"></property>
        </then>                            
   </filter>
   <respond></respond>
</sequence> 
```