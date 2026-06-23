# Qingzhou One-Stop Intelligent Management Platform

## Overview

Qingzhou is a lightweight Java-based software development platform primarily designed for centralized unified management
and intelligent operations of heterogeneous business systems across multiple categories. Through automatic agent
registration, plug-in based extensibility, unified API specifications, front-end/back-end separation, and large
model-driven natural language interaction, it enables enterprises to remotely manage, configure, and operate various
business systems in a standardized, low-cost, and highly flexible manner, upgrading traditional manual operations to
intent-driven intelligent governance.

### Core Features and Capabilities

1. **Centralized Management**  
   By deploying the "Qingzhou Agent" on the server side, enterprises no longer need to configure separate management
   entry points for each business system. The agent automatically discovers and remotely registers various systems to
   the "Qingzhou Management Console." This mechanism greatly reduces operational complexity and avoids information silos
   and redundant effort caused by decentralized management of multiple systems. At the same time, centralized management
   enables global monitoring, unified configuration, and batch operations, significantly improving the transparency and
   efficiency of enterprise IT governance and laying the foundation for large-scale operations.

2. **Plug-in Architecture**  
   The Qingzhou Agent does not hardcode governance logic directly. Instead, it executes specific operations by
   dispatching "Qingzhou Apps" (plug-ins) that comply with the Qingzhou API specifications. This plug-in design
   decouples the core framework from business implementations. When adding or upgrading governance capabilities for a
   business system, only the corresponding plug-in needs to be added or removed, without modifying the agent core. The
   significance lies in providing extremely high extensibility and flexibility: enterprises can combine governance
   capabilities as needed, while third-party developers are supported in contributing plug-ins, fostering an open
   ecosystem and effectively avoiding system reconstruction costs caused by business changes.

3. **Unified Specifications**  
   Qingzhou APIs provide a set of standard development specifications for all business systems, covering interface
   definitions, interaction patterns, and integration modes. This enables multiple originally heterogeneous and
   disparate systems to present a consistent "language" and "look and feel" to administrators, achieving truly
   consistent management. The value lies in drastically reducing learning costs and error rates – administrators no
   longer need to familiarize themselves with the proprietary operation methods of each different system. Meanwhile,
   standardized interfaces provide a solid foundation for automated and scripted operations, driving enterprises from "
   human-driven" to "rule-driven" standardized operations systems.

4. **Front-End/Back-End Separation**  
   Supports independent deployment of the front end (e.g., management UI) and back end (business logic and data
   services), with the front end capable of being hosted separately on high-performance web servers like Nginx. This
   architecture delivers two core benefits: first, the front end and back end can scale elastically independently to
   handle different load scenarios – for example, high-concurrency UI access does not consume back-end computing
   resources; second, development and release are decoupled – front-end teams can iterate the UI independently while
   back-end teams focus on service stability, improving team collaboration efficiency. Additionally, front-end static
   assets can leverage CDN acceleration to further enhance user access experience globally.

5. **AI-Driven Intelligent Governance**  
   After integrating large model capabilities, administrators no longer need to memorize complex operation paths or
   scripts. They simply describe their intent in natural language (e.g., "adjust the timeout threshold of the order
   system to 30 seconds"), and the system automatically understands and executes the corresponding governance logic.
   This achieves a fundamental upgrade from traditional manual configuration and click-based operations to intent-driven
   management, greatly lowering operational barriers and even enabling non-technical personnel to participate in daily
   management. The deeper value is that AI can proactively recommend optimization strategies and predict anomalies based
   on historical data and context, shifting the governance system from passive response to proactive intelligent
   stewardship, freeing up productivity and reducing human errors.

### Applicability

Based on Qingzhou's positioning of "centralized governance and intelligent operations for heterogeneous systems," it is
most suitable for developing enterprise-level management applications that are **cross-system, governance-heavy, and
standardization-oriented** – i.e., "systems that manage business systems":

1. **Unified IT Operations & Governance**
    - Typical scenarios: multi-cloud management platforms, hybrid cloud monitoring centers, automated operations
      platforms.
    - Fit: leverages "agent registration" to automatically connect across network nodes, and centralized monitoring with
      batch operations solves the pain points of decentralized operations.

2. **Heterogeneous Business System Aggregation Portals**
    - Typical scenarios: enterprise unified management portals (integrating ERP, CRM, OA, etc.), cross-system data
      dashboards.
    - Fit: uses "plug-ins" to adapt to different underlying systems, and employs "unified APIs and UI specifications" to
      mask heterogeneity and deliver a consistent experience.

3. **IoT and Edge Device Management**
    - Typical scenarios: edge gateway consoles, distributed IoT device monitoring and configuration systems.
    - Fit: the agent mechanism naturally suits edge node access, and plug-ins are suitable for handling proprietary
      protocols of diverse device types.

4. **Security and Compliance Auditing**
    - Typical scenarios: unified permission governance centers, global security policy inspection and distribution
      systems.
    - Fit: centralized governance facilitates unified global policy distribution and auditing, while standardized
      interfaces enable cross-system compliance comparison.

5. **Intent-Driven Intelligent Operations**
    - Typical scenarios: AIOps intelligent operations assistants, natural language-driven business configuration
      centers.
    - Fit: leverages the platform's "AI + large model" advantages to transform complex cross-system configuration and
      inspection tasks into simple natural-language interactive commands.

> **Core conclusion**: The Qingzhou platform is not meant for developing specific production business systems (such as
> transaction cores or order systems). Instead, it is meant for developing **"management systems" – systems that manage
other systems**. Any application featuring "multi-source heterogeneous access, centralized unified governance, and
> intent-based intelligent operations" is an ideal fit.

### Qingzhou Architecture Diagram

![Qingzhou Architecture Diagram](./docs/images/architecture.png)

## Quick Start

- Environment requirements: JDK 1.8+ and Maven 3.8+.
- Build: In the source root directory, run `mvn clean install -DskipTests` to obtain the binary product package (
  `/qingzhou/target/qingzhou`).
- Startup: Go to the `bin` directory of the product package and execute the startup script (e.g., `sh start.sh` on
  Linux/macOS).
- Access: After startup, open `http://localhost:7900/web` in a browser to access the Qingzhou visual web management
  console.

## Features

### REST

### Remote Management

### Integration

## More Information

### Configuration Parameters

The configuration file is located at `product-package/instances/default/conf/qingzhou.properties`. After modification,
restart the Qingzhou instance for changes to take effect.

### Directory Structure

The directory structure of the Qingzhou binary product package is as follows:

- `bin`: Executable programs directory.
    - `start.sh`: Startup script for Linux/Mac; you can append a space and an instance name to specify the instance to
      start (i.e., a subdirectory name under `instances`), default is `default`.
    - `start.bat`: Startup script for Windows; you can append a space and an instance name to specify the instance to
      start (i.e., a subdirectory name under `instances`), default is `default`.
    - `gen-cipher-key.sh`: Generates a random symmetric encryption key for the service
      `qingzhou.crypto.Crypto.getCipher(String key)`.
    - `gen-pair-key.sh`: Generates a random asymmetric public/private key pair for the service
      `qingzhou.crypto.Crypto.getPairCipher(String publicKey, String privateKey)`.
- `instances`: Instance data directory; you can duplicate multiple instances for different purposes, e.g., development,
  testing, etc.
    - `default`: The default instance directory.
- `lib`: Contains compiled binary `*.jar` files from the source code.
    - `version*.zip`: Distribution zip package containing Qingzhou binary `*.jar` files; it is automatically extracted
      at startup. After extraction, this file is no longer needed and can be deleted or kept.
    - `version*` directory: Automatically generated at startup from the `version*.zip` extraction; the `*.jar` files
      under this directory are loaded into memory.
    - Important note: At startup, if the `version*` directory already exists, it will be compared with `version*.zip`;
      if inconsistent, the directory will be deleted and regenerated. To disable this behavior, move `version*.zip` out
      of this directory or rename it so it does not start with `version`.

### Service Endpoints

The service endpoints (HTTP) exposed by Qingzhou are as follows:

| Endpoint URI          | Description                                              |
|-----------------------|----------------------------------------------------------|
| `/registry/register`  | Register an application on a remote instance             |
| `/registry/refresh`   | Refresh the communication key of a remote instance       |
| `/registry/invoke`    | Invoke a module operation of a specified application     |
| `/registry/instance`  | Get the list of registered instances                     |
| `/registry/app/list`  | Get the list of registered applications                  |
| `/registry/app/info`  | Get detailed information of a specific application       |
| `/registry/app/model` | Get module information of a specific application         |
| `/agent`              | Execute operations on a remote instance                  |
| `/web`                | Management console front-end static assets               |
| `/ai/chat`            | Natural language interaction for intelligent governance  |
| `/ai/equip`           | Prompt configuration supported by intelligent governance |

### Front-End/Back-End Separation

Qingzhou adopts a front-end/back-end separation architecture, supporting independent deployment of the front end and
back end.

Below is an example of deploying the Qingzhou front end independently on Nginx:

1. The front-end static assets are located in the source directory `modules/qingzhou-web/src/main/resources/webapp`.
2. Add the following configuration to Nginx:
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
4. Access `http://localhost:8000/web` to open the Qingzhou management console.

### Frequently Asked Questions

### Troubleshooting