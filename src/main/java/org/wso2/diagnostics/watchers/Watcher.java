package org.wso2.diagnostics.watchers;

import java.util.Map;

public interface Watcher {

    public void init(Map<String, Object> configMap);

    public void start();
}
