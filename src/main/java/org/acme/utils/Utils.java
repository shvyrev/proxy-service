package org.acme.utils;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Utils {

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> not(Predicate<? super T> target) {
        Objects.requireNonNull(target);
        return (Predicate<T>)target.negate();
    }

    public static Predicate<String> urlPredicate() {
        return s -> {
            try {
                new URL(s);
                return true;
            } catch (MalformedURLException e) {
                return false;
            }
        };
    }

    public static String fileExtension(String fileName) {
        return (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) ?
                fileName.substring(fileName.lastIndexOf(".") + 1) : "";
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String hashFile(String filePath) throws IOException {
        return Files.asByteSource(Paths.get(filePath).toFile()).hash(Hashing.murmur3_32()).toString();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String hashString(String value) {
        return Hashing.murmur3_32().newHasher().putString(value, StandardCharsets.UTF_8).hash().toString();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String hashString(Object ... objects) {
        return hashString(String.join(",", Stream.of(objects).map(Object::toString).toArray(String[]::new)));
    }

    public static String rndUUID(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static Function<Element, String> attrMapper(String src) {
        return element -> element.attr(src);
    }

    public static List<String> clean(List<String> list) {
        return list.stream().filter(not(s -> s.replace(" ", "").isEmpty())).collect(toList());
    }

    public static String hashedFileName(String filePath, String fileName) throws IOException {
        final String extension = fileExtension(fileName);
        final String shortFileName = hashFile(filePath);
        return shortFileName + (extension.length() == 0 ? "" : "." + extension);
    }

    public static <V> Handler<AsyncResult<V>> promiseHandler(Promise<V> promise) {
        return event -> {
            if (event.succeeded()) {
                promise.complete(event.result());
            } else {
                promise.fail(event.cause());
            }
        };
    }

    public static  <V> Handler<AsyncResult<Message<V>>> messageHandler(Promise<V> promise) {
        return event -> {
            if (event.succeeded()) {
                promise.complete(event.result().body());
            } else {
                promise.fail(event.cause());
            }
        };
    }

    public static Void throwableVoidHandler(Throwable throwable) {
        throwable.printStackTrace();
        return null;
    }

    private static final Logger log = LoggerFactory.getLogger( Utils.class );

    public static <V> V throwableHandler(Throwable throwable) {
//        log.info(" $ throwableHandler : " + throwable.getMessage());
        throwable.printStackTrace();
        return null;
    }

    public static <V> V throwableHandlerSilent(Throwable throwable) {
        return null;
    }

    public static <V> Handler<AsyncResult<V>> messageHandler(Message<String> message) {
        return res -> {
            if (res.succeeded()) {
                message.reply(res.result());
            } else {
                res.cause().printStackTrace();
                message.fail(-1, res.cause().getLocalizedMessage());
            }
        };
    }

    public static Date nowDate(){
        return java.sql.Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static long nowMillis(){
        return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static <T, V> Future<V> compStage(T value, Function<T, V> func) {
        final Promise<V> promise = Promise.promise();
        promise.complete(func.apply(value));
        return promise.future();
    }

}
