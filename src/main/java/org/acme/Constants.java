package org.acme;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;

import java.util.regex.Pattern;

public class Constants {
    public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:72.0) Gecko/20100101 Firefox/72.0";
    public static final String HREF = "href";
    public static final String SRC = "src";
    public static final String DEFAULT_LANGUAGE = "de";
    public static final int TIMEOUT = 2000;
    public static final int SSL_PORT = 443;
    public static final String UTF_8 = "utf-8";
    public static final String CONTENT_TYPE = "application/octet-stream";
    public static final int MAX_TWITTER_MESSAGE_LENGTH = 140;
    public static final String LANGUAGE_ERROR_TEXT_PATTERN = "language : %s. Not contains in : %s.";
    public static final String URL = "url";
    public static final String TYPE = "type";
    public static final String BLOOM_FILTER_NAME = "bloom-filter-name";
    public static final DeliveryOptions DELIVERY_OPTIONS = new DeliveryOptions().setSendTimeout(30000);
    public static final String SUCCESS_DEPLOYMENT_TEMPLATE = "Deployment id : %s";
    public static final String ERROR_DEPLOYMENT_MMESSAGE = "error.";
    public static final String EMPTY_STRING = "";
    public static final JsonArray EMPTY_JSON_ARRAY = new JsonArray();
    public static final String DEFAULT_BUCKET_NAME = "bucket";
    public static final String HTML_EXTENSION = ".html";

    public static final Pattern IP_PORT_PATTERN = Pattern.compile("\\d*\\.\\d*\\.\\d*\\.\\d*\\:\\d+");
    public static final Pattern SCRIPT_DATA_PATTERN = Pattern.compile("data:\\[(.*?)\\],");
//    public static final Pattern SCRIPT_DATA_PATTERN = Pattern.compile("\\{(.*?)\\}");
//    public static final Pattern SCRIPT_DATA_PATTERN = Pattern.compile("data:([^],]*)],");
}
