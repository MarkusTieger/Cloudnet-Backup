package de.MarkusTieger.config;

import java.util.HashMap;
import java.util.Map;

import de.MarkusTieger.sql.SQLServerType;
import eu.cloudnetservice.driver.network.HostAndPort;

public record SQLConnectionConfig(boolean enable, String uri, String username, String password, boolean useSsl,
		String database, SQLServerType serverType, HostAndPort address, String driver, Map<String, String> options, int minimumIdle, // 2
		int maximumPoolSize, // 100
		long connectionTimeout, // 10000
		long leakDetectionThreshold, // 4000
		long validationTimeout // 10000
) {

	public SQLConnectionConfig(boolean b, String defaultUri, String string, String string2, boolean c, String string3, SQLServerType serverType,
			HostAndPort hostAndPort, String defaultDriver, Map<String, String> defaultOptions) {
		this(b, defaultUri, string, string2, c, string3, serverType, hostAndPort, defaultDriver, defaultOptions, 2, 100, 10000,
				4000, 10000);
	}

	public static final String DEFAULT_URI = "jdbc:mysql://%s:%d/%s?serverTimezone=UTC&useSSL=%b&trustServerCertificate=%b";
	public static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
	public static final Map<String, String> DEFAULT_OPTIONS;

	static {

		DEFAULT_OPTIONS = new HashMap<>();

		DEFAULT_OPTIONS.put("cachePrepStmts", "true");
		DEFAULT_OPTIONS.put("prepStmtCacheSize", "250");
		DEFAULT_OPTIONS.put("prepStmtCacheSqlLimit", "2048");
		DEFAULT_OPTIONS.put("useServerPrepStmts", "true");
		DEFAULT_OPTIONS.put("useLocalSessionState", "true");
		DEFAULT_OPTIONS.put("rewriteBatchedStatements", "true");
		DEFAULT_OPTIONS.put("cacheResultSetMetadata", "true");
		DEFAULT_OPTIONS.put("cacheServerConfiguration", "true");
		DEFAULT_OPTIONS.put("elideSetAutoCommits", "true");
		DEFAULT_OPTIONS.put("maintainTimeStats", "false");

	}

}
