package de.MarkusTieger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.MarkusTieger.archieve.AbstractArchiever;
import de.MarkusTieger.archieve.Zip4jArchiever;
import de.MarkusTieger.backup.BackupSystem;
import de.MarkusTieger.backup.IBackupService;
import de.MarkusTieger.command.BackupCommand;
import de.MarkusTieger.config.BackupStorageConfig;
import de.MarkusTieger.config.SQLConnectionConfig;
import de.MarkusTieger.sql.SQLBackupService;
import de.MarkusTieger.sql.SQLServerType;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.node.Node;
import lombok.Getter;

public class CloudNetBackupModule extends DriverModule {

	@Getter
	private volatile BackupStorageConfig cfg;

	private final List<IBackupService<?>> services = new ArrayList<>();

	@ModuleTask(event = ModuleLifeCycle.LOADED)
	public void init() {

		if (!cfg.enabled())
			return;

		AbstractArchiever.setPassword(Node.instance().config().clusterConfig().clusterId().toString());
		AbstractArchiever.setInstance(new Zip4jArchiever());

		if (cfg.mysql().enable()) {
			SQLBackupService service = new SQLBackupService();
			try {
				service.initialize(cfg.mysql());
				services.add(service);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		BackupSystem system = new BackupSystem(new File("."), services);
		BackupSystem.setINSTANCE(system);
		if (cfg.backupOnStartup()) {
			try {
				system.startBackup(null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@ModuleTask
	public void finishStartup() {
		Node.instance().commandProvider().register(new BackupCommand());
	}

	@ModuleTask(order = 64, event = ModuleLifeCycle.LOADED)
	public void initConfig() {
		this.cfg = readConfig(BackupStorageConfig.class,
				() -> new BackupStorageConfig(false, true,
						new SQLConnectionConfig(true, SQLConnectionConfig.DEFAULT_URI, "insert_only", "secure",
								false, "backup", SQLServerType.MYSQL, new HostAndPort("127.0.0.1", 3306),
								SQLConnectionConfig.DEFAULT_DRIVER, SQLConnectionConfig.DEFAULT_OPTIONS),
						".tmp"));
	}

	@ModuleTask(event = ModuleLifeCycle.RELOADING)
	public void reload() {
		initConfig();
	}

}
