package org.acme.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "proxy")
public class ProxyEntity extends PanacheEntityBase {

    @Id
    public String id;
    public String host;
    public Integer port;
    public Long latency;

    public static ProxyEntity of(Proxy proxy){
        final ProxyEntity result = new ProxyEntity();
        result.id = proxy.id();
        result.host = proxy.host;
        result.port = proxy.port;
        result.latency = proxy.latency;
        return result;
    }

    @Override
    public String toString() {
        return "ProxyEntity{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", latency=" + latency +
                '}';
    }
}