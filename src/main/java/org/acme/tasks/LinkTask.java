package org.acme.tasks;

public class LinkTask extends TaskImpl {

    public LinkTask(String type, String url) {
        super(type, url);
    }

    public static LinkTask of(String type, String url) {
        return new LinkTask(type, url);
    }

    @Override
    public String toString() {
        return "LinkTask{" +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
