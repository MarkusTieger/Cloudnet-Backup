package de.MarkusTieger.config;

public record BackupStorageConfig(boolean enabled, boolean backupOnStartup, SQLConnectionConfig mysql,
		String secure_temp_folder) {

}
