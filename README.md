# WSO2 Runtime Diagnostics Tool

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2mi-orange)](https://stackoverflow.com/tags/wso2-am/)
[![stackoverflow](https://img.shields.io/badge/stackoverflow-wso2am-orange)](https://stackoverflow.com/tags/wso2-micro-integrator/)

---

## Features
- Lightweight and easy-to-use tool for generating diagnostic details.
- Simplified data collection process to minimize user involvement.
- Preemptive data collection for certain types of issues, such as OOM errors.
- Capability to capture significant changes in Passthrough metrics for better insight into specific issues.

## High Level Architecture
![architecture.png](docs%2Farchitecture.png)

### Components

There are four main components in the tool:
- **Memory Watcher**: Monitors the memory usage of the server and executes the configured Action Executors when the memory usage exceeds the threshold.
- **CPU Watcher**: Monitors the CPU usage of the server and executes the configured Action Executors when the CPU usage exceeds the threshold.
- **Traffic Analyzer**: Monitors the traffic of the server and generates logs when the traffic pattern suddenly changes significantly.
- **Log Watcher**: Monitors the error logs of the server and executes the configured Action Executors when the log pattern matches the configured pattern.

### Action Executors

Action Executors are the components that are executed when a watcher component triggers an alert. The following are the available Action Executors:

| Action Executor | Description                                                             |
| --- |-------------------------------------------------------------------------------------|
| ThreadDumper | Runs the jstack tool to take thread dump and writes the output a file.     |
| MemoryDumper | Takes a heap dump                                                          |
| OpenFileFinder | Finds the open files by the server process and writes the output a file. |
| Netstat | Dumps the network statistics of the server to a file.                           |
| ServerInfo | Dumps the server information such as name, verstion etc.                     |
| MetricsSnapshot | Takes a current snapshot of the Passthrough transport metrics in synapse|

## Prerequisites

- Java Development Kit (JDK) 11 or later
- WSO2 product server (Currently supported for WSO2 API Manager and WSO2 Micro Integrator)
- Unix-based system (Linux, Mac OS, etc.)

## Usage

Please refer to the [configuration guide](./docs/config.md) for detailed instructions on how to configure the tool.
The output of the tool can be analyzed using the [analysis guide](./docs/analysis.md).

## Limitations
- The tool is currently supported in Unix-based systems only. Some of the Action Executors rely on Unix-based commands.
- The tool needs to be run on JDK environment. The tool is not supported in JRE environment. Some of the Action Executors rely on JDK tools.

## Issues and support
Please report issues at [WSO2 Diagnostic Tool Issues](https://github.com/wso2/diagnostics-tool/issues).

## Contributing to the project

As an open source project, we welcome contributions from the community. To contribute to the project you can send a pull request to the master branch. The `master` branch holds the latest unreleased source code. Before sending a pull request please make sure that your changes are compatible with the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0).

## License

This project is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).

---

(c) 2024, [WSO2 LLC](http://www.wso2.org/). All Rights Reserved.
