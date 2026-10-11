package com.geopslabs.geops.catalog.infrastructure.persistence;

import com.geopslabs.geops.catalog.domain.ports.JobLockPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * PostgreSQL advisory locks. tryLock takes the transaction-level lock (pg_try_advisory_xact_lock), released at
 * commit or rollback. runExclusively takes the session-level lock on a connection of its own, keeps it while the job
 * runs and releases it at the end; if the replica dies the connection closes and PostgreSQL drops the lock.
 */
@Component
public class PostgresJobLockAdapter implements JobLockPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresJobLockAdapter.class);
    private static final String TRY_XACT_LOCK = "SELECT pg_try_advisory_xact_lock(hashtext(:name))";
    private static final String TRY_SESSION_LOCK = "SELECT pg_try_advisory_lock(hashtext(?))";
    private static final String UNLOCK = "SELECT pg_advisory_unlock(hashtext(?))";

    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    public PostgresJobLockAdapter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean tryLock(String jobName) {
        var locked = entityManager.createNativeQuery(TRY_XACT_LOCK).setParameter("name", jobName).getSingleResult();
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public <T> Optional<T> runExclusively(String jobName, Supplier<T> job) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            if (!call(connection, TRY_SESSION_LOCK, jobName)) {
                return Optional.empty();
            }
            try {
                return Optional.ofNullable(job.get());
            } finally {
                release(connection, jobName);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("The lock " + jobName + " could not be taken", exception);
        }
    }

    /**
     * If the unlock fails, closing is not enough: the pool would keep the physical connection and PostgreSQL would
     * keep the session lock, so no replica could run the job again. Aborting makes the pool drop the connection and
     * PostgreSQL releases the lock when the session ends.
     */
    static void release(Connection connection, String jobName) {
        try {
            if (!call(connection, UNLOCK, jobName)) {
                throw new SQLException("The lock " + jobName + " was not held by this session");
            }
        } catch (SQLException exception) {
            LOGGER.warn("job.lock.release-failed job={} reason={} action=abort-connection", jobName,
                    exception.getClass().getSimpleName());
            abort(connection, jobName);
        }
    }

    private static void abort(Connection connection, String jobName) {
        try {
            connection.abort(Runnable::run);
        } catch (SQLException exception) {
            LOGGER.error("job.lock.abort-failed job={} reason={}", jobName, exception.getClass().getSimpleName());
        }
    }

    private static boolean call(Connection connection, String sql, String jobName) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, jobName);
            try (var result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }
}
