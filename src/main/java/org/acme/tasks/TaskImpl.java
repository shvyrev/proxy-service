package org.acme.tasks;

import org.acme.utils.IdImpl;
import org.acme.utils.JsonImpl;

import static org.acme.utils.Utils.hashString;

public class TaskImpl implements IdImpl, JsonImpl {
    public String type;
    public String url;

    public TaskImpl(String type, String url) {
        this.type = type;
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String id() {
        return hashString(type, url);
    }

}
