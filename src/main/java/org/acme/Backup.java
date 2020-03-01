package org.acme;

import io.quarkus.scheduler.Scheduled;
import org.acme.model.ProxyEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class Backup {

    @Inject
    Cache cache;

    private static final Logger log = LoggerFactory.getLogger( Backup.class );

    @Transactional
    @Scheduled(every = "20m")
    void save(){
        cache.availableProxies().forEach(proxy -> {
            if (ProxyEntity.findById(proxy.id()) == null) {
                ProxyEntity.of(proxy).persist();
            }
        });
    }
}
