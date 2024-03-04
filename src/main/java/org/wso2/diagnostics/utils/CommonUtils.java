package org.wso2.diagnostics.utils;

import java.util.Map;

public class CommonUtils {

    public static int getIntegerValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (int) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return defaultValue;
    }

    public static boolean getBooleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    public static String[] getActionExecutors(String watcherType) {
        String actionExecutors =
                (String) ConfigMapHolder.getInstance().getConfigMap().get(watcherType + ".action_executors");
        String[] actionExecutorArray = null;
        if (actionExecutors != null) {
            actionExecutorArray = actionExecutors.split(",");
        }
        return actionExecutorArray;
    }
}
