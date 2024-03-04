# Analysis

### Data

The zip files are generated in the `<WSO2_HOME>/diagnostics-tool/data` directory. The zip file is named as `<processId>-<timestamp>.zip`. The zip file contains the following files:

- **deployment.toml**: The deployment.toml file of the server which contains the configurations.
- **diagnostics.log**: The log file of the diagnostics tool which contains logs related to traffic pattern to the server. The logs are explained in the [Diagnostics Log](#diagnostics-log) section.
- **log.txt**: The log line that triggered the Action Executors.
- **logs.zip**: The log directory of the server in zip format. This may contain the heap dump file if it is generated.
- **lsof-output.txt** [Optional]: The output of the lsof command which contains the open files by the server process during the error time.
- **netstat-output.txt** [Optional]: The output of the netstat command which contains the network statistics of the server during the error time.
- **server-info.txt**: The server information such as name, version etc.
- **thread-dump-<no>-<timestamp>.txt**: The thread dump of the server taken during different time intervals during the error time.
- **metrics-snapshot.txt**: The current snapshot of the Passthrough transport metrics in synapse runtime.

### Diagnostics Log

The diagnostics log contains the following logs:

#### Memory Watcher
The memory watcher logs are prefixed with `[MemoryWatcher]`. 

Eg: `MemoryWatcher Heap usage is above threshold. Heap usage: 87, Retry count: " + count`.

Here, the log indicates that the heap usage is above the threshold and the heap usage is 87%. The MemoryWatcher retries a couple of times before executing the Action Executors.

#### CPU Watcher
The CPU watcher logs are prefixed with `[CPUWatcher]`.

Eg: `CPUWatcher CPU usage is above threshold. CPU usage: 91, Retry count: " + count`.

Here, the log indicates that the CPU usage is above the threshold and the CPU usage is 91%. The CPUWatcher retries a couple of times before executing the Action Executors.

#### Traffic Analyzer
The traffic analyzer logs are prefixed with `[TrafficAnalyzer]`.

Eg: `TrafficAnalyzer Attribute Last15SecondRequests of type http-listener increased more than the threshold, old value: 2, new value: 227, threshold: 115.22752880979914`.

Here, the log indicates that the Last15SecondRequests attribute of type http-listener increased more than the threshold. The old value is 2, the new value is 227 and the threshold is 115.22752880979914. The threshold is calculated based on the standard deviation of a Simple Moving Average window.

#### Log Watcher

Following are examples of log watcher logs:

```
[Interpreter] Executing the action executors for the log line matching the regex pattern (.*)org.apache.synapse.transport.passthru(.*)
ServerInfo [INFO] ServerInfo executed successfully.
OpenFileFinder [INFO] OpenFileFinder executed successfully.
Netstat [INFO] Netstat executed successfully
ZipFileExecutor [INFO] Zipping the folder at /Users/user/wso2mi-4.3.0/diagnostics-tool/temp/2024-03-01_14:21:06.743
ZipFileExecutor [INFO] Diagnosis Dumped in :/Users/user/wso2mi-4.3.0/diagnostics-tool/data/96970_2024-03-01_14:21:06.743.zip
ThreadDumper [INFO] Thread dump execution is completed for 96970, thread dump count: 5, delay: 2000ms
MetricsSnapshot [INFO] MetricsSnapshot executed successfully.
ZipFileExecutor [INFO] Zipping the folder at /Users/user/wso2mi-4.3.0/diagnostics-tool/temp/2024-03-01_14:21:06.838
ZipFileExecutor [INFO] Diagnosis Dumped in :/Users/user/wso2mi-4.3.0/diagnostics-tool/data/96970_2024-03-01_14:21:06.838.zip
```

Here, the log indicates that the log line matching the regex pattern `(.*)org.apache.synapse.transport.passthru(.*)` is found. The ServerInfo, OpenFileFinder, Netstat, MetricsSnapshot, and ThreadDumper Action Executors are executed. The executors are executed in parallel and the zip file is generated with the output at the end.
