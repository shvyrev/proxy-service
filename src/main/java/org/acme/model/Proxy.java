package org.acme.model;

import org.acme.utils.IdImpl;
import org.acme.utils.JsonImpl;

import static org.acme.utils.Utils.hashString;
import static org.acme.utils.Utils.nowMillis;

public class Proxy implements IdImpl, JsonImpl {
    public String host;
    public Integer port;
    public Long latency = Long.MAX_VALUE;
    public Long updatedAt;

    public static Proxy of(String host, int port) {
        final Proxy proxy = new Proxy();
        proxy.host = host;
        proxy.port = port;
        proxy.updatedAt = nowMillis();
        return proxy;
    }

    @Override
    public String id() {
        return hashString(host, port);
    }

    public Long lastUpdateTime(){
        return updatedAt;
    }

    public Long latency(){
        return latency;
    }
}
