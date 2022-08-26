package de.MarkusTieger.config;

import eu.cloudnetservice.driver.network.HostAndPort;

public record MySQLConnectionConfig(boolean enable, String username, String password, boolean useSsl, String database,
		HostAndPort address) {

}
