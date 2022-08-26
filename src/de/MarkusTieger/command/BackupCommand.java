package de.MarkusTieger.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.parsers.Parser;
import cloud.commandframework.annotations.suggestions.Suggestions;
import cloud.commandframework.context.CommandContext;
import de.MarkusTieger.backup.AdvancedBackupInfo;
import de.MarkusTieger.backup.BackupInfo;
import de.MarkusTieger.backup.BackupSystem;
import de.MarkusTieger.backup.IBackupService;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowBasedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;

@CommandAlias({ "backups" })
@CommandPermission("backups.command")
@Description("module-backup-command-description")
public class BackupCommand {

	private static final RowBasedFormatter<BackupInfo> ENTRY_LIST_FORMATTER = RowBasedFormatter.<BackupInfo>builder()
			.defaultFormatter(
					ColumnFormatter.builder().columnTitles(new String[] { "id", "cloudnet-version", "date" }).build())
			.column(BackupInfo::id).column(BackupInfo::cloudNetVersion)
			.column((info) -> new Date(info.date()).toString()).build();

	private static final RowBasedFormatter<AdvancedBackupInfo> ENTRY_INFO_FORMATTER = RowBasedFormatter
			.<AdvancedBackupInfo>builder()
			.defaultFormatter(ColumnFormatter.builder()
					.columnTitles(new String[] { "id", "cloudnet-version", "services", "date" }).build())
			.column(AdvancedBackupInfo::id).column(AdvancedBackupInfo::cloudNetVersion)
			.column((info) -> Arrays.toString(info.services().stream().map(IBackupService::getName).toArray()))
			.column((info) -> new Date(info.date()).toString()).build();

	@Parser(name = "serviceParser", suggestions = "serviceSuggestions")
	public IBackupService<?> serviceParser(CommandContext<CommandSource> $, Queue<String> input) {
		if ($ == null)
			throw new NullPointerException("$ is marked non-null but is null");
		if (input == null)
			throw new NullPointerException("input is marked non-null but is null");
		String name = input.remove();

		for (IBackupService<?> srv : BackupSystem.getINSTANCE().getServices()) {

			if (srv.getName().equalsIgnoreCase(name))
				return srv;

		}

		throw new ArgumentNotAvailableException("The Backup-Service \"" + name + "\" was not found.");
	}

	@Suggestions("serviceSuggestions")
	public List<String> serviceSuggestions(CommandContext<CommandSource> $, String input) {
		return BackupSystem.getINSTANCE().getServices().stream().map(IBackupService::getName).toList();
	}

	@CommandMethod("backup|backups create|new [service]")
	public void createBackup(CommandSource source,
			@Argument(value = "service", parserName = "serviceParser") IBackupService<?> service) {
		if (source == null)
			throw new NullPointerException("source is marked non-null but is null");

		if (source.checkPermission("backups.create")) {
			if (service == null) {
				try {
					BackupSystem.getINSTANCE().startBackup(source);
				} catch (IOException e) {
					e.printStackTrace();
					source.sendMessage("Backup failed. " + e);
				}
			} else {
				try {
					BackupSystem.getINSTANCE().startBackup(source, service);
				} catch (IOException e) {
					e.printStackTrace();
					source.sendMessage("Backup failed. " + e);
				}
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@CommandMethod("backup|backups list|l [service]")
	public void listBackups(CommandSource source,
			@Argument(value = "service", parserName = "serviceParser") IBackupService<?> service) {
		if (source == null)
			throw new NullPointerException("source is marked non-null but is null");

		if (source.checkPermission("backups.list")) {
			if (service == null) {
				Map<String, BackupInfo> infos = new HashMap<>();
				for (IBackupService<?> srv : BackupSystem.getINSTANCE().getServices()) {
					try {
						for (BackupInfo info : srv.listBackups()) {
							if (infos.containsKey(info.id()))
								continue;
							infos.put(info.id(), info);
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

				source.sendMessage(ENTRY_LIST_FORMATTER.format(infos.values()));
			} else {
				try {
					source.sendMessage(ENTRY_LIST_FORMATTER.format(service.listBackups()));
				} catch (IOException e) {
					e.printStackTrace();
					source.sendMessage("Backups list failed. " + e);
				}
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@CommandMethod("backup|backups info|i <id>")
	public void backupInfo(CommandSource source,
			@Argument(value = "id", parserName = "backupParser") AdvancedBackupInfo info) {
		if (source == null)
			throw new NullPointerException("source is marked non-null but is null");

		if (info == null)
			throw new NullPointerException("info is marked non-null but is null");

		if (source.checkPermission("backups.info")) {
			source.sendMessage(ENTRY_INFO_FORMATTER.format(Collections.singleton(info)));
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@Suggestions("backupSuggestions")
	public List<String> backupSuggestions(CommandContext<CommandSource> $, String input) {
		Map<String, BackupInfo> infos = new HashMap<>();
		for (IBackupService<?> srv : BackupSystem.getINSTANCE().getServices()) {
			try {
				for (BackupInfo info : srv.listBackups()) {
					if (infos.containsKey(info.id()))
						continue;
					infos.put(info.id(), info);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return infos.keySet().stream().toList();
	}

	@Parser(name = "backupParser", suggestions = "backupSuggestions")
	public AdvancedBackupInfo backupParser(CommandContext<CommandSource> $, Queue<String> input) {
		if ($ == null)
			throw new NullPointerException("$ is marked non-null but is null");
		if (input == null)
			throw new NullPointerException("input is marked non-null but is null");
		String name = input.remove();

		List<IBackupService<?>> services = new ArrayList<>();
		BackupInfo info = null;

		for (IBackupService<?> srv : BackupSystem.getINSTANCE().getServices()) {

			try {
				if (!srv.exists(name))
					continue;

				info = srv.get(name);
				services.add(srv);
			} catch (Throwable e) {
				e.printStackTrace();
			}

		}

		if (info == null)
			throw new ArgumentNotAvailableException("The Backup-Service \"" + name + "\" was not found.");

		return new AdvancedBackupInfo(info.id(), info.cloudNetVersion(), info.date(), services);
	}

	@CommandMethod("backup|backups restore|load <id> <service>")
	public void restoreBackup(CommandSource source,
			@Argument(value = "id", parserName = "backupParser") AdvancedBackupInfo info,
			@Argument(value = "service", parserName = "serviceParser") IBackupService<?> service) {
		if (source == null)
			throw new NullPointerException("source is marked non-null but is null");

		if (service == null)
			throw new NullPointerException("service is marked non-null but is null");

		if (info == null)
			throw new NullPointerException("info is marked non-null but is null");

		if (source.checkPermission("backups.restore")) {
			try {
				BackupSystem.getINSTANCE().restoreBackup(source, service, info.id());
			} catch (IOException e) {
				e.printStackTrace();
				source.sendMessage("Backup failed. " + e);
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

}
