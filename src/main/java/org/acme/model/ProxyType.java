package org.acme.model;

public enum ProxyType {
    Https("https"), Socks4("socks4"), Socks5("socks5");

    String value;

    ProxyType( String fun ) { this.value = fun; }

    public String getValue() { return value; }

}
