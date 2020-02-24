package org.acme;

import org.acme.model.Proxy;
import org.acme.utils.IdImpl;
import org.acme.utils.JsonImpl;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class Cache {

    @Inject
    EmbeddedCacheManager cacheManager;

    private static final Logger log = LoggerFactory.getLogger( Cache.class );

    public <V extends IdImpl> V addIfNotExist(V value){
        return getCache(value).putIfAbsent(value.id(), value);
    }

    public <V extends IdImpl> void upsert(V value) {
        getCache(value).put(value.id(), value);
    }

    public <V extends JsonImpl> long size(String field, String value, Class<V> tClass) {
        return getCache(tClass).values().stream()
                .filter(o -> ((JsonImpl)o).toJson().getString(field, "").equalsIgnoreCase(value))
                .count();
    }

    public int size(Class tClass) {
        return getCache(tClass).size();
    }

    private <V extends IdImpl> org.infinispan.Cache<String, V> getCache(V value) {
        return getCache(value.getClass());
    }

    private <V extends IdImpl> org.infinispan.Cache<String, V> getCache(Class aClass) {
        return cacheManager.getCache(aClass.getSimpleName(), true);
    }

    public <V extends IdImpl> Optional<V> first(String field, String value, Class<V> aClass) {
        return getCache(aClass).values().stream()
                .filter(o -> ((JsonImpl)o).toJson().getString(field, "").equalsIgnoreCase(value))
                .map(o -> (V) o)
                .findFirst();
    }

    public <V extends IdImpl> void remove(V value) {
        getCache(value).remove(value.id(), value);
    }

    public <V extends IdImpl> Optional<V> random(Class<V> tClass) {
        final org.infinispan.Cache<String, V> cache = getCache(tClass);
        if (cache.isEmpty()) {
            return Optional.empty();
        } else {
            final long count = ThreadLocalRandom.current().nextLong(0, size(tClass));
            return cache.values().stream().skip(count).limit(1).findFirst();
        }
    }

    public Optional<Proxy> randomProxy(){
        return randomProxy(Comparator.comparingLong(Proxy::lastUpdateTime).reversed());
    }

    public Optional<Proxy> randomMinLatencyProxy() {
        return randomProxy(Comparator.comparingLong(Proxy::latency));
    }

    private Optional<Proxy> randomProxy(Comparator<Proxy> comparator){
        final Class<Proxy> aClass = Proxy.class;
        final org.infinispan.Cache<String, Proxy> cache = getCache(aClass);
        if (cache.isEmpty()) {
            return Optional.empty();
        } else {
            final long size = Math.min(20, size(aClass));
            final long count = ThreadLocalRandom.current().nextLong(0, size);
            return cache.values().stream().sorted(comparator).skip(count).limit(1).findFirst();
        }
    }

    public long checkedProxyAmount() {
        final org.infinispan.Cache<String, Proxy> cache = getCache(Proxy.class);
        return cache.values().stream().filter(p -> p.latency < Integer.MAX_VALUE).count();
    }
}