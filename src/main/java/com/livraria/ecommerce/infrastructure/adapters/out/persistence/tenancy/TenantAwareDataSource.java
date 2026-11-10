package com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class TenantAwareDataSource extends DelegatingDataSource {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareDataSource.class);

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        return wrapConnection(connection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        return wrapConnection(connection);
    }

    private Connection wrapConnection(Connection connection) {
        applyTenantContext(connection);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new ConnectionInvocationHandler(connection)
        );
    }

    private static void applyTenantContext(Connection connection) {
        String tenantId = TenantContext.getTenantIdAsString();
        if (tenantId != null && !tenantId.isBlank()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT set_config('app.current_tenant', ?, false)")) {
                ps.setString(1, tenantId);
                ps.execute();
                log.trace("Tenant context definido no PostgreSQL para a conexão: {}", tenantId);
            } catch (SQLException e) {
                log.error("Erro ao definir app.current_tenant no PostgreSQL: {}", e.getMessage());
            }
        }
    }

    private static void resetTenantContext(Connection connection) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("RESET app.current_tenant");
        } catch (SQLException e) {
            log.trace("Erro ou conexão já fechada ao resetar app.current_tenant: {}", e.getMessage());
        }
    }

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection delegate;

        public ConnectionInvocationHandler(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                resetTenantContext(delegate);
            }
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}

