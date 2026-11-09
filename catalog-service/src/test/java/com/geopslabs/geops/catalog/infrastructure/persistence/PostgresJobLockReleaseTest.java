package com.geopslabs.geops.catalog.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The error path of the session lock: a connection whose unlock failed is aborted, never returned to the pool.
 */
class PostgresJobLockReleaseTest {
    private Connection connection;
    private PreparedStatement lock;
    private PreparedStatement unlock;
    private PostgresJobLockAdapter adapter;

    @BeforeEach
    void setUp() throws SQLException {
        var dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        lock = mock(PreparedStatement.class);
        unlock = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(contains("pg_try_advisory_lock"))).thenReturn(lock);
        when(connection.prepareStatement(contains("pg_advisory_unlock"))).thenReturn(unlock);
        var locked = resultSet(true);
        when(lock.executeQuery()).thenReturn(locked);
        adapter = new PostgresJobLockAdapter(dataSource);
    }

    @Test
    void aFailedUnlockAbortsTheConnection() throws SQLException {
        when(unlock.executeQuery()).thenThrow(new SQLException("connection reset"));

        assertThat(adapter.runExclusively("catalog.test-job", () -> "done")).contains("done");

        verify(connection).abort(any(Executor.class));
        verify(connection).close();
    }

    @Test
    void anUnlockThatFindsNoLockAlsoAbortsTheConnection() throws SQLException {
        var notHeld = resultSet(false);
        when(unlock.executeQuery()).thenReturn(notHeld);

        adapter.runExclusively("catalog.test-job", () -> "done");

        verify(connection).abort(any(Executor.class));
    }

    @Test
    void aReleasedLockKeepsTheConnectionForThePool() throws SQLException {
        var released = resultSet(true);
        when(unlock.executeQuery()).thenReturn(released);

        adapter.runExclusively("catalog.test-job", () -> "done");

        verify(connection, never()).abort(any(Executor.class));
        verify(connection).close();
    }

    @Test
    void aJobThatFailsStillReleasesTheLock() throws SQLException {
        var released = resultSet(true);
        when(unlock.executeQuery()).thenReturn(released);

        try {
            adapter.runExclusively("catalog.test-job", () -> {
                throw new IllegalStateException("job failed");
            });
        } catch (IllegalStateException expected) {
            // The job's error goes up; the lock must be released anyway.
        }

        verify(unlock).executeQuery();
    }

    private static ResultSet resultSet(boolean value) throws SQLException {
        var resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(1)).thenReturn(value);
        return resultSet;
    }
}
