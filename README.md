# LDAP EI Connector

The LDAP [Connector](https://docs.wso2.com/display/EI650/Working+with+Connectors) allows you to connect to any LDAP server through a simple web services interface and perform CRUD (Create, Read, Update, Delete) operations on LDAP entries. This connector uses the [JAVA JNDI APIs](https://directory.apache.org/api/user-guide.html) to connect to a required LDAP server.

## Compatibility

| Connector version  | Supported WSO2 ESB/EI version |
| ------------- | ------------- |
| [1.0.10](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.10) | EI 6.4.0, EI 6.5.0    |
| [1.0.9](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.9) | ESB 4.9.0, EI 6.5.0    |
| [1.0.8](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.8) | EI 6.5.0    |
| [1.0.7](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.7) | ESB 4.9.0, ESB 5.0.0, EI 6.1.1, EI 6.2.0, EI 6.3.0, EI 6.4.0    |
| [1.0.6](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.6) | ESB 4.9.0, ESB 5.0.0, EI 6.1.1, EI 6.2.0    |
| [1.0.5](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.5) | ESB 4.9.0, ESB 5.0.0, EI 6.1.1    |
| [1.0.4](https://github.com/wso2-extensions/esb-connector-ldap/tree/org.wso2.carbon.connector.ldap-1.0.4) | ESB 4.9.0, ESB 5.0.0, EI 6.1.1    |

## Getting started

#### Download and install the connector

1. Download the connector from the [WSO2 Store](https://store.wso2.com/store/assets/esbconnector/details/4ecf8dde-60f3-4e91-ba22-5f49a4e302f4) by clicking the Download Connector button.
2. Then you can follow this [Documentation](https://docs.wso2.com/display/EI650/Working+with+Connectors+via+the+Management+Console) to add and enable the connector via the Management Console in your EI instance.
3. For more information on using connectors and their operations in your EI configurations, see [Using a Connector](https://docs.wso2.com/display/EI650/Using+a+Connector).
4. If you want to work with connectors via EI tooling, see [Working with Connectors via Tooling](https://docs.wso2.com/display/EI650/Working+with+Connectors+via+Tooling).

#### Configuring the connector operations

To get started with LDAP connector and their operations, see [Configuring LDAP Operations](docs/config.md).

## Building From the Source

Follow the steps given below to build the LDAP connector from the source code:

1. Get a clone or download the source from [Github](https://github.com/wso2-extensions/esb-connector-ldap).
2. Run the following Maven command from the `esb-connector-ldap` directory: `mvn clean install`.
3. The LDAP connector zip file is created in the `esb-connector-ldap/target` directory

## How You Can Contribute

As an open source project, WSO2 extensions welcome contributions from the community.
Check the [issue tracker](https://github.com/wso2-extensions/esb-connector-ldap/issues) for open issues that interest you. We look forward to receiving your contributions.
