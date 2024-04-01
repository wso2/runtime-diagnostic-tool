# Configuration Guide

### Server Configurations

Given table below is the description of the server configurations.

| Configuration | Description                                         |
| --- |-----------------------------------------------------|
| diagnostic_tool_enabled | Whether the diagnostic tool is enabled or not.      |
| deployment_toml_path | Path to the deployment.toml file in the WSO2 server |
| logs_directory | Path to the logs directory.                         |
| updates_config_path | Path to the updates config file.                    |
| diagnostic_log_file_path | Path to write the diagnostic log file.              |
| carbon_log_file_path | Path to the carbon error log (wso2error.log) file.  |
| process_id_path | Path to the process id file (wso2carbon.pid).       |
| server_name | Name of the WSO2 server.                            |
| server_version | Version of the WSO2 server.                         |

Given below is a sample configuration for the WSO2 Micro Integrator. 

```toml
[server_configuration]
diagnostic_tool_enabled = "true"
deployment_toml_path = "../conf/deployment.toml"
logs_directory = "../repository/logs"
updates_config_path = "../updates/config.json"
diagnostic_log_file_path = "logs/diagnostics.log"
carbon_log_file_path = "../repository/logs/wso2error.log"
process_id_path = "../wso2carbon.pid"
server_name = "WSO2 Micro Integrator"
server_version = "4.3.0"
```

### Action Executor Configurations

Currently, the tool supports the following action executors.

| Action Executor | Description                                                             |
| --- |-------------------------------------------------------------------------------------|
| ThreadDumper | Runs the jstack tool to take thread dump and writes the output a file.     |
| MemoryDumper | Takes a heap dump                                                          |
| OpenFileFinder | Finds the open files by the server process and writes the output a file. |
| Netstat | Dumps the network statistics of the server to a file.                           |
| ServerInfo | Dumps the server information such as name, verstion etc.                     |
| MetricsSnapshot | Takes a current snapshot of the Passthrough transport metrics in synapse|

#### ThreadDumper

| Configuration | Description                                         |
| --- |-----------------------------------------------------|
| count | Number of thread dumps to be taken.                 |
| delay | Delay between each thread dump in milliseconds.     |

Given below is a sample configuration for the ThreadDumper action executor.

```toml
[[action_executor_configuration]]
executor = "ThreadDumper"
count = "5"
delay = "2000"
```

#### MemoryDumper

Given below is a sample configuration for the MemoryDumper action executor.

```toml
[[action_executor_configuration]]
executor = "MemoryDumper"
```

#### OpenFileFinder

Given below is a sample configuration for the OpenFileFinder action executor.

```toml
[[action_executor_configuration]]
executor = "OpenFileFinder"
```

#### Netstat

Given below is a sample configuration for the Netstat action executor.

```toml
[[action_executor_configuration]]
executor = "Netstat"
command = "netstat -lt"
```

#### ServerInfo

Given below is a sample configuration for the ServerInfo action executor.

```toml
[[action_executor_configuration]]
executor = "ServerInfo"
```

#### MetricsSnapshot

Given below is a sample configuration for the MetricsSnapshot action executor.

```toml
[[action_executor_configuration]]
executor = "MetricsSnapshot"
```

### Watcher Configurations

Currently, the tool supports the following watchers.

| Watcher | Description                                                                |
| --- |----------------------------------------------------------------------------|
| cpu_watcher | Watches the CPU usage of the server.                                       |
| memory_watcher | Watches the memory usage of the server.                                    |
| log_watcher | Watches the logs for specific error patterns and triggers actions.         |
| traffic_analyzer | Analyzes the Passthrough server traffic and records in diagnostic log file |

#### cpu_watcher

| Configuration | Description                                                                            |
| --- |----------------------------------------------------------------------------------------|
| enabled | Whether the watcher is enabled or not.                                                 |
| threshold | The threshold value for the CPU usage.                                                 |
| attempts | The number of attempts before triggering the action executors (This resets every hour) |
| interval | The interval between each check in seconds.                                            |
| action_executors | The action executors to be triggered when the threshold is reached. (Comma separated)  |

Given below is a sample configuration for the cpu_watcher.

```toml
[cpu_watcher]
enabled = "true"
threshold = "20"
attempts = "2"
interval = "5"
action_executors = "ThreadDumper,MetricsSnapshot,ServerInfo"
```

#### memory_watcher

| Configuration    | Description                                                                           |
|------------------|---------------------------------------------------------------------------------------|
| enabled          | Whether the watcher is enabled or not.                                                |
| threshold        | The threshold value for the memory usage.                                             |
| attempts         | The number of attempts before triggering the action executors (This resets every hour)|
| interval         | The interval between each check in seconds.                                           |
| action_executors | The action executors to be triggered when the threshold is reached. (Comma separated) |

Given below is a sample configuration for the memory_watcher.

```toml
[memory_watcher]
enabled = "true"
threshold = "30"
attempts = "2"
interval = "5"
action_executors = "ThreadDumper,MetricsSnapshot,ServerInfo"
```

#### log_watcher

| Configuration | Description                                                                           |
| --- |---------------------------------------------------------------------------------------|
| enabled | Whether the watcher is enabled or not.                                                |
| interval | The interval between each check in seconds.                                           |

Given below is a sample configuration for the log_watcher.

```toml
[log_watcher]
enabled = "true"
interval = "0.1" 
```

### Log error patterns

| Configuration | Description                                                                                                                                                                                                                                                                         |
| --- |-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| regex | Directory to store the zip files                                                                                                                                                                                                                                                    |
| executors | The action executors to be triggered when the threshold is reached. (Comma separated)                                                                                                                                                                                               |
| reload_time | Continuous error log which matches the regex patter won't be processed again and again unless the reload_time has elapsed. An error log which matches a certain regex pattern will only be processed after the reload time interval where a similar error log was processed before. |

Given below is a sample configuration.

```toml
[[log_pattern]]
regex = "(.*)org.apache.synapse.transport.passthru(.*)"
executors = "MetricsSnapshot,Netstat,OpenFileFinder,ThreadDumper,ServerInfo"
reload_time = "30"
```

### Traffic Analyzer Configurations

| Configuration | Description                                                                           |
| --- |---------------------------------------------------------------------------------------|
| last_second_requests_enabled | Whether the last second requests watcher is enabled or not.                           |
| last_second_requests_windows_size | The window size for the last second requests watcher.                                  |
| last_second_requests_delay | The delay for the last second requests watcher.                                        |
| last_second_requests_interval | The interval for the last second requests watcher.                                     |
| last_fifteen_seconds_requests_enabled | Whether the last fifteen seconds requests watcher is enabled or not.                 |
| last_fifteen_seconds_requests_window_size | The window size for the last fifteen seconds requests watcher.                        |
| last_fifteen_seconds_requests_delay | The delay for the last fifteen seconds requests watcher.                              |
| last_fifteen_seconds_requests_interval | The interval for the last fifteen seconds requests watcher.                           |
| last_minutes_requests_enabled | Whether the last minutes requests watcher is enabled or not.                           |
| last_minutes_requests_window_size | The window size for the last minutes requests watcher.                                  |
| last_minutes_requests_delay | The delay for the last minutes requests watcher.                                        |
| last_minutes_requests_interval | The interval for the last minutes requests watcher.                                     |
| notify_interval | The interval for the traffic analyzer to notify the user.                               |

Given below is a sample configuration for the traffic analyzer.

```toml
[traffic_analyzer]
last_second_requests_enabled = "false"
last_second_requests_windows_size = "300"
last_second_requests_delay = "60"
last_second_requests_interval = "1"
last_fifteen_seconds_requests_enabled = "true"
last_fifteen_seconds_requests_window_size = "100"
last_fifteen_seconds_requests_delay = "4"
last_fifteen_seconds_requests_interval = "15"
last_minutes_requests_enabled = "true"
last_minutes_requests_window_size = "100"
last_minutes_requests_delay = "1"
last_minutes_requests_interval = "60"
notify_interval = "300"
```

### Post Action Executors

#### Zip File Configurations

| Configuration | Description                                                                                       |
| --- |---------------------------------------------------------------------------------------------------|
| output_directory | Directory to store the zip files                                                                  |
| max_count | Maximum number of zip files to maintain. When the count exceeds, the older files will be deleted. |

Given below is a sample configuration.

```toml
[zip_file_configuration]
output_directory = "data"
max_count = "50"
```

#### FTP Configurations

| Configuration | Description                                                                                       |
| --- |---------------------------------------------------------------------------------------------------|
| enabled | Whether the FTP is enabled or not.                                                                |
| host | The FTP host.                                                                                     |
| port | The FTP port.                                                                                     |
| username | The FTP username.                                                                                 |
| password | The FTP password.                                                                                 |
| directory | The FTP directory.                                                                                |

Given below is a sample configuration.

```toml
[ftp_configuration]
enabled = "true"
host = "ftp.example.com"
port = "21"
username = "user"
password = "password"
directory = "diagnostics"
```

#### SFTP Configurations

| Configuration | Description                                                                                       |
| --- |---------------------------------------------------------------------------------------------------|
| enabled | Whether the SFTP is enabled or not.                                                               |
| host | The SFTP host.                                                                                    |
| port | The SFTP port.                                                                                    |
| username | The SFTP username.                                                                                |
| password | The SFTP password.                                                                                |
| directory | The SFTP directory.                                                                               |

Given below is a sample configuration.

```toml
[sftp_configuration]
enabled = "true"
host = "sftp.example.com"
port = "22"
username = "user"
password = "password"
directory = "diagnostics"
```

### Log4j2 Configurations

The log4j2.properties file can be used to configure the logging level of the tool. The default log level is set to INFO. The log4j2.properties file can be found in the `conf` directory.
