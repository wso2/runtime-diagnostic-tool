/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org).
 *
 *   WSO2 LLC. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.diagnostics.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;

public class JmxtermExecutor {
    private static final Logger log = LogManager.getLogger(JmxtermExecutor.class);

    private final static String JAVA_JMXTERM_COMMAND =
        "java -jar " + System.getProperty(Constants.APP_HOME) + "/bin/jmxterm-cli.jar";

    public static int getAttributeValue(String type, String pid, String attribute) {

        String output = executeJmxTermAndRetrieveLastLine("echo \"get -s -b org.apache.synapse:Name=" + type +
                ",Type=PassThroughConnections " + attribute + "\" | " +
                JAVA_JMXTERM_COMMAND + " -n -l " + pid + " | tail -1");
        return getInt(output);
    }

    public static String getMessageSizes(String type, String pid, String attribute) {
        String[] output = executeJmxTermAndRetrieveLastNLines("echo \"get -s -b org.apache.synapse:Name=" + type +
                ",Type=PassThroughConnections " + attribute + "\" | " +
                JAVA_JMXTERM_COMMAND + " -n -l " + pid + " | tail -8", 8);
        StringBuilder outputString = new StringBuilder();
        for (String line : output) {
            outputString.append(line).append("\n");
        }
        return outputString.toString();
    }

    public static int getCpuUsage(String pid) {
        String output = executeJmxTermAndRetrieveLastLine("echo \"get -s -b java.lang:type=OperatingSystem " +
                "ProcessCpuLoad\" | " + JAVA_JMXTERM_COMMAND + " -n -l " + pid + " | tail -1");
        return getFloat(output) == -1 ? -1 : (int) (getFloat(output) * 100);
    }

    public static int getMemoryUsage(String pid) {
        String output = executeJmxTermAndRetrieveMaxAndUsed("echo \"get -s -b java.lang:type=Memory " +
                "HeapMemoryUsage\" | " + JAVA_JMXTERM_COMMAND + " -n -l " + pid + " | tail -6");
        return getInt(output);
    }

    private static int getInt(String output) {
        if (StringUtils.isEmpty(output)) {
            return -1;
        }
        return Integer.parseInt(output);
    }

    private static Float getFloat(String output) {
        if (StringUtils.isEmpty(output)) {
            return -1f;
        }
        return Float.parseFloat(output);
    }

    private static BufferedReader executeJmxTermCommand(String command) {
        String[] commands = new String[]{"/bin/sh", "-c", command};
        try {
            Process process = Runtime.getRuntime().exec(commands);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            return reader;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String executeJmxTermAndRetrieveMaxAndUsed(String command) {
        long max = 0;
        long used = 0;
        BufferedReader reader = executeJmxTermCommand(command);
        if (reader != null) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("max")) {
                        max = Long.parseLong(line.split("=")[1].trim().replace(";", ""));
                    } else if (line.contains("used")) {
                        used = Long.parseLong(line.split("=")[1].trim().replace(";", ""));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (max == 0) {
            return "";
        }
        // return percentage
        return String.valueOf((used * 100) / max);
    }

    private static String executeJmxTermAndRetrieveLastLine(String command) {
        String lastLine = "";
        BufferedReader reader = executeJmxTermCommand(command);
        if (reader != null) {
            try {
                lastLine = readLastLine(reader);
            } catch (Exception e) {
                log.error("Error while reading last line from JMXTerm output", e);
                return "";
            }
        }
        return lastLine;
    }

    private static String[] executeJmxTermAndRetrieveLastNLines(String command, int numberOfLines) {
        BufferedReader reader = executeJmxTermCommand(command);
        if (reader != null) {
            try {
                return readLastNLines(reader, numberOfLines);
            } catch (Exception e) {
                log.error("Error while reading last " + numberOfLines + " lines from JMXTerm output", e);
                return new String[0];
            }
        }
        return new String[0];
    }

    private static String[] readLastNLines(BufferedReader reader, int numberOfLines) throws Exception {
        Deque<String> lines = new ArrayDeque<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.addLast(line);
            if (lines.size() > numberOfLines) {
                lines.removeFirst();
            }
        }
        return lines.toArray(new String[0]);
    }

    private static String readLastLine(BufferedReader reader) throws Exception {
        String line, lastLine = "";
        while ((line = reader.readLine()) != null) {
             lastLine = line;
        }
        return lastLine;
    }
}
