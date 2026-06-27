# Qingzhou – One‑Stop Intelligent Management Platform

## Overview

Qingzhou (轻舟) is a lightweight Java‑based software development platform designed primarily for unified management and intelligent operations of heterogeneous business systems across multiple categories. Through an innovative model‑driven architecture, it enables developers to simply write plain JavaBeans and use declarative annotations to dynamically generate front‑end pages, completely eliminating tedious front‑end development work. This core advantage not only significantly reduces initial development costs and greatly improves delivery efficiency, but also fundamentally lowers system maintenance complexity, delivering substantial value through reduced upkeep costs and long‑term benefits. In addition, Qingzhou leverages auto‑discovery, plugin‑based extensibility, unified API specifications, front‑end/back‑end separation, and large‑model‑driven natural language interaction, empowering enterprises to manage various business systems in a standardized, cost‑effective, and highly flexible manner, while upgrading traditional manual operations to intent‑driven intelligent governance.

### Core Features and Capabilities

1. **Centralized Management**  
   By deploying a "Qingzhou Agent" on the server side, enterprises no longer need to configure separate management entry points for each business system. The agent automatically discovers and remotely registers various systems to the "Qingzhou Management Console." This mechanism greatly reduces operational complexity and avoids information silos and redundant efforts caused by decentralized management of multiple systems. At the same time, centralized management enables global monitoring, unified configuration, and batch operations, significantly enhancing the transparency and efficiency of enterprise IT governance and laying a solid foundation for large‑scale operations.

2. **Plugin‑Based Architecture**  
   Qingzhou does not hardcode management logic directly; instead, it dispatches plugins that conform to the Qingzhou API specification—called "Qingzhou Apps"—to perform specific actions. This plugin‑based design decouples the core framework from business implementations, allowing easy addition or removal of plugins when new management capabilities are needed or existing ones are upgraded. The significance lies in offering extremely high extensibility and flexibility: enterprises can combine management capabilities on demand, while third‑party developers are encouraged to contribute plugins, fostering an open ecosystem and effectively avoiding system refactoring costs caused by evolving business requirements.

3. **Unified Standards**  
   Qingzhou provides a set of standard development specifications for all business systems, covering interface definitions, user interaction, and integration patterns. This enables heterogeneous systems with diverse styles to present a consistent "language" and "look‑and‑feel" to administrators, achieving truly uniform management. The value lies in drastically reducing learning curves and operational errors—administrators no longer need to memorize different proprietary operations for each system. Moreover, standardized interfaces lay a robust foundation for automated and scripted operations, driving enterprises from human‑driven to rule‑based standardized operations.

4. **Front‑End / Back‑End Separation**  
   Qingzhou supports independent deployment of the front end and back end, with the front end able to be hosted separately on high‑performance servers such as Nginx. This architecture delivers three core benefits: first, the front end and back end can scale elastically independently, so high concurrent UI traffic does not consume back‑end computing resources; second, development and release are decoupled—the front end can iterate its UI independently while the back end focuses on services, improving team collaboration efficiency; third, standard API communication facilitates integration with unified authentication, API gateways, and other cloud‑native components, while also enabling UI‑level integration with third‑party systems.

5. **AI‑Driven Intelligent Operations**  
   Qingzhou integrates large language model capabilities, allowing administrators to use natural language to describe their intent (e.g., "Summarize the overall health status of the system") without needing to remember complex operation paths or scripts. The system automatically understands and executes the corresponding management logic. This represents a fundamental upgrade from traditional manual configuration and click‑based operations to intent‑driven management, dramatically lowering the operational barrier—even non‑technical personnel can participate in daily administration. More profoundly, AI can proactively recommend optimization strategies and predict anomalies based on historical data and context, transforming the management system from reactive to proactive intelligent governance, unleashing productivity and reducing human errors.

6. **Support for REST and MCP Multi‑Protocol Integration**  
   Qingzhou’s multi‑protocol integration is not a simple superposition but rather generates both REST and MCP access capabilities on the same business model:
    - **REST protocol**: targets traditional web front ends, mobile apps, and third‑party system integrations, meeting conventional HTTP API invocation needs;
    - **MCP protocol**: targets intelligent application scenarios such as large language models and AI agents, providing standardized tool‑call interfaces.  
      Developers only need to maintain a single set of JavaBean business models, which simultaneously support both "manual operation interfaces" and "AI‑driven intelligent calls"—truly enabling "write once, output via dual protocols." This greatly reduces maintenance costs associated with adapting systems across multiple ecosystems.

### Scope of Application

Given Qingzhou’s positioning as a platform for "centralized management and intelligent operations of heterogeneous systems," it is best suited for developing **cross‑system, management‑oriented, and standardized** enterprise management applications (i.e., "systems that manage business systems"):

1. **Unified IT Operations and Management Platforms**
    - Typical scenarios: multi‑cloud management platforms, hybrid cloud monitoring centers, automated operations platforms.
    - Fit: The "agent registration" mechanism enables automatic access across network nodes, and centralized monitoring with batch operations addresses the pain points of distributed operations.

2. **Heterogeneous Business System Aggregation Portals**
    - Typical scenarios: enterprise unified management portals (integrating ERP, CRM, OA, etc.), cross‑system data dashboards.
    - Fit: The "plugin" approach adapts to different underlying systems, while "unified APIs and UI specifications" mask heterogeneity to provide a consistent experience.

3. **IoT and Edge Device Management**
    - Typical scenarios: edge gateway management consoles, distributed IoT device monitoring and configuration systems.
    - Fit: The agent model naturally fits edge node access, and plugins are well‑suited to handle private protocols of diverse device types.

4. **Security and Compliance Auditing**
    - Typical scenarios: unified permission governance centers, global security policy inspection and distribution systems.
    - Fit: Centralized control facilitates unified policy distribution and auditing across systems, while standardized interfaces enable cross‑system compliance comparisons.

5. **Intent‑Driven Intelligent Operations**
    - Typical scenarios: AIOps intelligent assistants, natural‑language‑driven business configuration centers.
    - Fit: Leveraging the platform’s "AI + large model" strengths, complex cross‑system configuration and inspection tasks are converted into simple natural‑language commands.

> **Core takeaway**: The Qingzhou platform is not intended for developing specific production business systems (such as trading cores or order systems), but rather for building **"management systems"** – applications that exhibit the characteristics of "multi‑source heterogeneous access, centralized unified control, and intent‑driven intelligent operations" are its ideal use cases.

### Qingzhou Architecture Diagram

![Qingzhou Architecture Diagram](./docs/images/architecture.png)

## Quick Start

- Environment requirements: JDK 1.8+ and Maven 3.8+.
- Build: Run `mvn clean install -DskipTests` in the source root directory to obtain the binary product package (`/qingzhou/target/qingzhou`).
- Start: Go to the `bin` directory of the product package and execute the startup script (e.g., on Linux/macOS run `sh start.sh`).
- Access: After startup, open your browser and visit `http://localhost:7900/web` to access the Qingzhou visual web console.

## Features

### REST

### Remote Management

### Integration

## More Information

### Configuration Parameters

The configuration parameters file is located at `product-package/instances/default/conf/qingzhou.properties`. After modification, restart the Qingzhou instance for the changes to take effect.

### Directory Structure

The directory structure of the Qingzhou binary product package is as follows:

- `bin`: Executable programs directory.
    - `start.sh`: Startup script for Linux/Mac platforms. You can specify the instance to start by appending a space and the instance name (i.e., a subdirectory name under `instances`). The default is `default`.
    - `start.bat`: Startup script for Windows platforms. You can specify the instance to start by appending a space and the instance name (i.e., a subdirectory name under `instances`). The default is `default`.
    - `gen-cipher-key.sh`: Generates a random symmetric encryption key for the service `qingzhou.crypto.Crypto.getCipher(String key)`.
    - `gen-pair-key.sh`: Generates a random asymmetric public/private key pair for the service `qingzhou.crypto.Crypto.getPairCipher(String publicKey, String privateKey)`.
- `instances`: Instance data directories. You can create multiple copies for different purposes, e.g., development, testing, etc.
    - `default`: The default instance directory used at startup.
- `lib`: Contains the compiled binary `*.jar` files of Qingzhou source code.
    - `version*.zip`: Distribution zip package of the Qingzhou binary `*.jar` files. It is automatically extracted during startup; after extraction, this file is no longer needed and may be deleted or kept.
    - `version*` directory: Automatically generated during startup by extracting `version*.zip`. The `*.jar` files under this directory are loaded into memory.
    - **Important note**: At startup, if the `version*` directory already exists, its contents are compared with `version*.zip`. If they do not match, the directory is deleted and regenerated. To disable this behavior, move the `version*.zip` file out of this directory or rename it so that it no longer starts with `version`.

### Service Interfaces

The HTTP interfaces opened by Qingzhou services are as follows:

| Interface URI         | Description                                 |
|-----------------------|---------------------------------------------|
| /registry/register    | Register an application on a remote instance |
| /registry/refresh     | Refresh the communication key of a remote instance |
| /registry/invoke      | Execute a module operation of a specified application |
| /registry/instance    | Retrieve the list of registered instances    |
| /registry/app/list    | Retrieve the list of registered applications |
| /registry/app/info    | Retrieve detailed information of a specific application |
| /registry/app/model   | Retrieve module information of a specific application |
| /agent                | Execute an application operation on a remote instance |
| /web                  | Front‑end static resources of the management console |
| /ai/chat              | Natural language interaction for intelligent management |
| /ai/equip             | Prompt configuration supported by the intelligent management feature |

### Front‑End / Back‑End Separation

Qingzhou adopts a front‑end / back‑end separation architecture, supporting independent deployment of the two tiers.

Below is an example of deploying the Qingzhou front end independently on Nginx:

1. The front‑end static resources are located in the source directory `modules/qingzhou-web/src/main/resources/webapp`.
2. Add the following configuration to your Nginx configuration:
    ```nginx
    server {
        listen 8000;

        location /web {
            alias /modules/qingzhou-web/src/main/resources/webapp;
            index index.html;
            try_files $uri $uri/ /web/index.html;
        }

        location ^~ /registry/web/ {
            proxy_pass http://localhost:7900/registry/web/;
        }

        location ^~ /registry/invoke/ {
            proxy_pass http://localhost:7900/registry/invoke/;
        }

        location ^~ /ai/ {
            proxy_pass http://localhost:7900/ai/;
        }
    }
    ```
3. Start the Nginx service.
4. Visit `http://localhost:8000/web` to open the Qingzhou management console.

### Frequently Asked Questions

### Troubleshooting