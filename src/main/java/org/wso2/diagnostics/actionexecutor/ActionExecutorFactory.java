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

package org.wso2.diagnostics.actionexecutor;

/**
 * Factory used to create various Executors instance by their class name.
 */
public class ActionExecutorFactory {
    /**
     * This Method used to create Executor objects.
     *
     * @param executorType the executor type.
     * @return PostExecutor
     */
    public static ActionExecutor getActionExecutor(String executorType) {

        try {
            String classNameShell = "org.wso2.diagnostics.actionexecutor.";
            Object tempObject = Class.forName(classNameShell + executorType).getConstructor().newInstance();

            return (ActionExecutor) tempObject;
        } catch (Exception e) {
            // try loading the class from the classpath
            try {
                Object tempObject = Class.forName(executorType).getConstructor().newInstance();
                return (ActionExecutor) tempObject;
            } catch (Exception e1) {
                // ignore
            }
        }
        return null;
    }
}
