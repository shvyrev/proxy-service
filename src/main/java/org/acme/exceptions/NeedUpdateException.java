package org.acme.exceptions;

public class NeedUpdateException extends IllegalStateException {
    public NeedUpdateException(String message) {
        super(message);
    }

    public static final NeedUpdateException of(String message){
        return new NeedUpdateException(message);
    }
}
