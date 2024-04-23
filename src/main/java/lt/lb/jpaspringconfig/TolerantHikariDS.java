package lt.lb.jpaspringconfig;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import lt.lb.commons.jpa.DelegatedDataSource;
import lt.lb.simplespring.ContextHolder;
import lt.lb.uncheckedutils.SafeOpt;
import lt.lb.uncheckedutils.func.UncheckedSupplier;

/**
 *
 * @author laim0nas100
 */
public class TolerantHikariDS extends DelegatedDataSource<HikariDataSource> {

    protected HikariDataSource testPool;
    protected HikariConfig config;

    private static final ConcurrentLinkedDeque<HikariDataSource> pools = new ConcurrentLinkedDeque<>();

    static {
        ContextHolder.addCloseTask(cc -> {
            Iterator<HikariDataSource> iterator = pools.iterator();
            while (iterator.hasNext()) {
                HikariDataSource ds = iterator.next();
                if (ds != null) {
                    ds.close();
                    iterator.remove();
                }
            }
        });
    }

    public TolerantHikariDS(HikariConfig config) {
        super(supplier(config), ContextHolder::isClosed);
        this.config = config;
    }

    private static UncheckedSupplier<HikariDataSource> supplier(HikariConfig config) {
        Objects.requireNonNull(config);
        config.setInitializationFailTimeout(-1);
        return () -> {
            HikariDataSource ds = new HikariDataSource(config);
            pools.add(ds);
            return ds;
        };

    }

    private void initTestPool() {
        if (testPool != null) {
            return;
        }
        HikariConfig testConfig = new HikariConfig();
        config.copyStateTo(testConfig);
        testConfig.setAutoCommit(false);
        testConfig.setMaximumPoolSize(4);
        testConfig.setMaxLifetime(5_000);
        testConfig.setReadOnly(true);
        testConfig.setConnectionTimeout(3_000);
        testConfig.setValidationTimeout(3_000);
        testConfig.setInitializationFailTimeout(-1);
        testPool = new HikariDataSource(testConfig);
        pools.add(testPool);
    }

    @Override
    public SafeOpt<Boolean> isConnected() {
        initTestPool();
        return SafeOpt.of(testPool).map(m -> m.getConnection()).map(con -> {
            con.close();
            return true;
        });
    }

}
