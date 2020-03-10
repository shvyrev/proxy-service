package org.acme.model;

import com.google.common.net.HostAndPort;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Country;
import org.acme.utils.IdImpl;
import org.acme.utils.JsonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.acme.model.ProxyType.Https;
import static org.acme.utils.Utils.hashString;

public class Proxy implements IdImpl, JsonImpl {
    public String host;
    public Integer port;
    public Long latency;
    public ProxyType type;
    public String country = "";
    public String countryCode = "";
    public String city;

    public static Proxy of(String host, int port, ProxyType type) {
        final Proxy proxy = new Proxy();
        proxy.host = host;
        proxy.port = port;
        proxy.type = type;
        return proxy;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Proxy of(String s, ProxyType type) {
        final HostAndPort hostAndPort = HostAndPort.fromString(s);
        return Proxy.of(hostAndPort.getHost(), hostAndPort.getPort(), type);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static Proxy of(String s) {
        final HostAndPort hostAndPort = HostAndPort.fromString(s);
        return Proxy.of(hostAndPort.getHost(), hostAndPort.getPort(), Https);
    }

    @Override
    public String id() {
        return hashString(host, port);
    }

    public Long latency(){
        return latency;
    }

    private static final Logger log = LoggerFactory.getLogger( Proxy.class );

    public Proxy geo(String countryCode, String country, String city) {
        this.countryCode = countryCode;
        this.country = country;
        this.city = city;
        return this;
    }

    @Override
    public String toString() {
        return "Proxy{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", latency=" + latency +
                ", type='" + type + '\'' +
                ", country='" + country + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", city='" + (city == null ? "" : city) + '\'' +
                '}';
    }

    public InetAddress ipAddress() throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    public Proxy geo(CityResponse city) {
        final Country country = city.getCountry();
        return geo(country.getIsoCode(), country.getName(), city.getCity().getName());
    }
}
