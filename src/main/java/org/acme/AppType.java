package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppType {
    private static final String CHECKER = "proxy-checker";
    private static final String PARSER = "proxy-parser";

    @ConfigProperty(name = "application.type", defaultValue = "proxy-checker")
    String appType;

    public boolean isChecker(){
        return eq(CHECKER);
    }

    public boolean isParser(){
        return eq(PARSER);
    }

    public String type(){
        return appType;
    }

    private boolean eq(String parser) {
        return parser.equalsIgnoreCase(appType);
    }

}
