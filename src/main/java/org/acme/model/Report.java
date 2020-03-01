package org.acme.model;

import org.acme.utils.IdImpl;
import org.acme.utils.JsonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.acme.utils.Utils.hashString;
import static org.acme.utils.Utils.nowMillis;

public class Report implements IdImpl, JsonImpl {

    public Long startedAt = nowMillis();
    public Long completedAt = Long.MAX_VALUE;
    public Long checked = 0L;
    public Long available = 0L;
    public Long unavailable = 0L;
    public Long amount = 0L;
    public Long avgFreq = 0L;
    public int cpu = 0;

    public static Report create() {
        return new Report();
    }

    private static final Logger log = LoggerFactory.getLogger( Report.class );

    public Report finish(long checked, long available, long unavailable, long amount){
        this.checked = checked;
        this.available = available;
        this.unavailable = unavailable;
        this.amount = amount;
        this.cpu = Runtime.getRuntime().availableProcessors();
        completedAt = nowMillis();
        long time = completedAt - startedAt;
        avgFreq = this.checked == 0L ? 0L : TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS) / this.checked;
        return this;
    }

    @Override
    public String id() {
        return hashString(startedAt);
    }
}
