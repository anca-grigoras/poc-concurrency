package ndw.dashboard.service;


import java.lang.ScopedValue;

public class RequestContext {
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
}
