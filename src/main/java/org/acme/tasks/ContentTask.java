package org.acme.tasks;

public class ContentTask extends TaskImpl {

    public ContentTask(String type, String url) {
        super(type, url);
    }

    public static ContentTask of(String type, String url) {
        return new ContentTask(type, url);
    }

    @Override
    public String toString() {
        return "ContentTask{" +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
