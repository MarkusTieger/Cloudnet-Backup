package de.MarkusTieger.backup;

import static de.MarkusTieger.archieve.StaticArchiever.compress;
import static de.MarkusTieger.archieve.StaticArchiever.compress_exclude;
import static de.MarkusTieger.archieve.StaticArchiever.compress_include;
import static de.MarkusTieger.archieve.StaticArchiever.decompresseTo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.cloudnetservice.driver.CloudNetDriver;
import eu.cloudnetservice.node.command.source.CommandSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class BackupSystem {

	private final File root;

	@Getter
	private final List<IBackupService<?>> services;

	@Getter
	@Setter
	private static BackupSystem INSTANCE;

	public List<IBackupService<?>> listWithBackup(String updateId) {
		return services.stream().filter((srv) -> {
			try {
				return srv.exists(updateId);
			} catch (Throwable th) {
				th.printStackTrace();
			}
			return false;
		}).toList();
	}

	public void restoreBackup(CommandSource source, IBackupService<?> service, String updateId) throws IOException {
		if (source == null)
			source = CommandSource.console();

		long start = System.currentTimeMillis();

		String prefix = "[Backup-System] ";

		source.sendMessage(prefix + "Restoring Backup... " + updateId);

		File tmp = new File(".tmp");
		if (!tmp.exists())
			tmp.mkdirs();

		Backup backup = service.restore(source, tmp, updateId);

		source.sendMessage(prefix + "Generating Directories...");

		File backups_restored = new File(root, "backups_restored");
		if (!backups_restored.exists())
			backups_restored.mkdirs();

		File backup_root = new File(backups_restored, updateId);
		if (!backup_root.exists())
			backup_root.mkdirs();

		File local = new File(backup_root, "local");
		if (!local.exists())
			local.mkdirs();

		File templates = new File(local, "templates");
		if (!templates.exists())
			templates.mkdirs();

		File services = new File(local, "services");
		if (!services.exists())
			services.mkdirs();

		source.sendMessage(prefix + "Restoring " + backup.templates().size() + " Templates...");

		for (Map.Entry<String, File> e : backup.templates().entrySet()) {

			try {
				URI uri = new URI(e.getKey());

				if (!uri.getScheme().equalsIgnoreCase("templates"))
					throw new IOException("Invalid Scheme for templates: " + uri.getScheme());

				String template_name = uri.getHost();
				String template_sub = uri.getPath();
				if (template_sub.startsWith("/"))
					template_sub = template_sub.substring(1);

				source.sendMessage(prefix + "Restoring Template " + template_name + "/" + template_sub + " ...");

				File f = new File(templates, template_name);
				if (!f.exists())
					f.mkdirs();
				f = new File(f, template_sub);
				if (!f.exists())
					f.mkdirs();

				decompresseTo(e.getValue(), f);

			} catch (URISyntaxException e1) {
				throw new IOException("Can't parse URI", e1);
			}

		}

		source.sendMessage(prefix + "Restoring " + backup.worlds().size() + " Worlds...");

		for (Map.Entry<String, File> e : backup.worlds().entrySet()) {

			try {
				URI uri = new URI(e.getKey());

				if (!uri.getScheme().equalsIgnoreCase("worlds"))
					throw new IOException("Invalid Scheme for worlds: " + uri.getScheme());

				String service_name = uri.getHost();
				String world_name = uri.getPath();
				if (world_name.startsWith("/"))
					world_name = world_name.substring(1);

				source.sendMessage(prefix + "Restoring Template " + service_name + "/" + world_name + " ...");

				File f = new File(services, service_name);
				if (!f.exists())
					f.mkdirs();
				f = new File(f, world_name);
				if (!f.exists())
					f.mkdirs();

				decompresseTo(e.getValue(), f);

			} catch (URISyntaxException e1) {
				throw new IOException("Can't parse URI", e1);
			}

		}

		source.sendMessage(prefix + "Restoring Region Files from " + backup.regions().size() + " Worlds ...");

		for (Map.Entry<String, List<File>> e : backup.regions().entrySet()) {

			try {
				URI uri = new URI(e.getKey());

				if (!uri.getScheme().equalsIgnoreCase("region"))
					throw new IOException("Invalid Scheme for region: " + uri.getScheme());

				String service_name = uri.getHost();
				String world_name = uri.getPath();
				if (world_name.startsWith("/"))
					world_name = world_name.substring(1);

				source.sendMessage(
						prefix + "Restoring Region Files for World " + service_name + "/" + world_name + " ...");

				File f = new File(services, service_name);
				if (!f.exists())
					f.mkdirs();
				f = new File(f, world_name);
				if (!f.exists())
					f.mkdirs();
				f = new File(f, "region");
				if (!f.exists())
					f.mkdirs();

				for (File file : e.getValue()) {
					file.renameTo(f);
				}

			} catch (URISyntaxException e1) {
				throw new IOException("Can't parse URI", e1);
			}

		}

		source.sendMessage(prefix + "Restoring Playerdata Files from " + backup.playerdata().size() + " Worlds ...");

		for (Map.Entry<String, List<File>> e : backup.playerdata().entrySet()) {

			try {
				URI uri = new URI(e.getKey());

				if (!uri.getScheme().equalsIgnoreCase("playerdata"))
					throw new IOException("Invalid Scheme for playerdata: " + uri.getScheme());

				String service_name = uri.getHost();
				String world_name = uri.getPath();
				if (world_name.startsWith("/"))
					world_name = world_name.substring(1);

				System.out.println(
						prefix + "Restoring Playerdata Files for World " + service_name + "/" + world_name + " ...");

				File f = new File(services, service_name);
				if (!f.exists())
					f.mkdirs();
				f = new File(f, world_name);
				if (!f.exists())
					f.mkdirs();
				f = new File(f, "playerdata");
				if (!f.exists())
					f.mkdirs();

				for (File file : e.getValue()) {
					file.renameTo(f);
				}

			} catch (URISyntaxException e1) {
				throw new IOException("Can't parse URI", e1);
			}

		}

		source.sendMessage(prefix + "Restoring Other files... ");

		for (Map.Entry<String, File> e : backup.extra_files().entrySet()) {

			try {
				URI uri = new URI(e.getKey());

				if (uri.getScheme().equalsIgnoreCase("services")) {

					String service_name = uri.getHost();
					String service_reason = uri.getPath();
					if (service_reason.startsWith("/"))
						service_reason = service_reason.substring(1);

					System.out
							.println(prefix + "Restoring Service File " + service_name + "/" + service_reason + " ...");

					File f = new File(services, service_name);
					if (!f.exists())
						f.mkdirs();
					decompresseTo(e.getValue(), f);

				} else if (uri.getScheme().equalsIgnoreCase("cloudnet")) {

					File rf = null;
					if (uri.getHost().equalsIgnoreCase("root")) {
						rf = backup_root;
					} else if (uri.getHost().equalsIgnoreCase("local")) {
						rf = local;
					} else
						throw new IOException("Invalid Location for extraction: " + uri.getHost());

					String file_reason = uri.getPath();
					if (file_reason.startsWith("/"))
						file_reason = file_reason.substring(1);

					System.out.println(prefix + "Restoring Service File " + uri.getHost() + "/" + file_reason + " ...");

					decompresseTo(e.getValue(), rf);

				} else
					throw new IOException("Invalid Scheme for extra files: " + uri.getScheme());

			} catch (URISyntaxException e1) {
				throw new IOException("Can't parse URI", e1);
			}

		}

		source.sendMessage(prefix + "Cleaning...");
		delete(tmp);

		source.sendMessage(
				prefix + "Backup Successfully restored and" + " Took: " + (System.currentTimeMillis() - start));
	}

	public BackupInfo startBackup(CommandSource source) throws IOException {
		try {
			return startBackup0(source, services);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public BackupInfo startBackup(CommandSource source, List<IBackupService<?>> services) throws IOException {
		try {
			return startBackup0(source, services);
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public BackupInfo startBackup(CommandSource source, IBackupService<?>... services) throws IOException {
		try {
			return startBackup0(source, Arrays.stream(services).toList());
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private BackupInfo startBackup0(CommandSource source, List<IBackupService<?>> services)
			throws IOException, URISyntaxException {
		if (source == null)
			source = CommandSource.console();

		long start = System.currentTimeMillis();
		String prefix = "[Backup-System] ";

		source.sendMessage(prefix + "Starting Backup...");

		File tmp = new File(".tmp");
		if (!tmp.exists())
			tmp.mkdirs();

		Map<String, File> templates = new HashMap<>();
		Map<String, List<File>> regions = new HashMap<>();
		Map<String, File> worlds = new HashMap<>();
		Map<String, List<File>> playerdata = new HashMap<>();
		Map<String, File> extra_files = new HashMap<>();

		File local = new File(root, "local");

		File templatesdir = new File(local, "templates");
		File servicesdir = new File(local, "services");

		source.sendMessage(prefix + "Preparing Templates...");

		if (templatesdir.exists() && templatesdir.isDirectory()) {

			for (File f : templatesdir.listFiles()) {

				if (!f.isDirectory()) {
					source.sendMessage(prefix + "Unknown File: " + f.getAbsolutePath());
					continue;
				}

				for (File sf : f.listFiles()) {

					if (!sf.isDirectory()) {
						source.sendMessage(prefix + "Unknown File: " + f.getAbsolutePath());
						continue;
					}

					source.sendMessage(
							prefix + "Compressing Template \"" + f.getName() + "/" + sf.getName() + "\" ...");
					templates.put(new URI("templates", f.getName(), "/" + sf.getName(), null).toString(),
							compress(tmp, sf.listFiles()));
				}

			}

		}

		source.sendMessage(prefix + "Preparing Services...");

		if (servicesdir.exists() && servicesdir.isDirectory()) {

			for (File f : servicesdir.listFiles()) {

				if (!f.isDirectory()) {
					source.sendMessage(prefix + "Unknown File: " + f.getAbsolutePath());
					continue;
				}

				extra_files.put(new URI("services", f.getName(), "/" + "config", null).toString(),
						compress_include(tmp, f.listFiles(), "config", "spigot.yml", "server.properties", "bukkit.yml",
								"whitelist.json", "ops.json", "commands.yml", "banned-players.json", "banned-ips.json",
								"permissions.json", "version_history.json", "usercache.json", "config.yml",
								"waterfall.yml"));

				extra_files.put(new URI("services", f.getName(), "/" + "plugins", null).toString(),
						compress_include(tmp, f.listFiles(), "plugins"));

				extra_files.put(new URI("services", f.getName(), "/" + "classpath", null).toString(), compress_include(
						tmp, f.listFiles(), "versions", "libraries", "paper.jar", "waterfall.jar", "cache"));

				extra_files.put(new URI("services", f.getName(), "/" + "logs", null).toString(),
						compress_include(tmp, f.listFiles(), "logs"));

				for (File wld : f.listFiles()) {

					if (!wld.isDirectory())
						continue;
					if (!new File(wld, "region").exists())
						continue;

					worlds.put(new URI("worlds", f.getName(), "/" + wld.getName(), null).toString(),
							compress_exclude(tmp, wld.listFiles(), "playerdata", "region"));

					File playerdatadir = new File(wld, "playerdata");
					if (playerdatadir.exists() && playerdatadir.isDirectory()) {
						List<File> playerdatatmp = new ArrayList<>();
						playerdata.put(new URI("playerdata", f.getName(), "/" + wld.getName(), null).toString(),
								playerdatatmp);

						for (File pd : playerdatadir.listFiles()) {
							if (!pd.isFile()) {
								System.out.println(prefix + "Unknown Directory: " + f.getAbsolutePath());
								continue;
							}

							playerdatatmp.add(pd);
						}
					}

					File regiondir = new File(wld, "region");
					if (regiondir.exists() && regiondir.isDirectory()) {
						List<File> regiontmp = new ArrayList<>();
						regions.put(new URI("region", f.getName(), "/" + wld.getName(), null).toString(), regiontmp);

						for (File pd : regiondir.listFiles()) {
							if (!pd.isFile()) {
								source.sendMessage(prefix + "Unknown Directory: " + f.getAbsolutePath());
								continue;
							}

							regiontmp.add(pd);
						}
					}
				}
			}

		}

		source.sendMessage(prefix + "Preparing Database...");

		extra_files.put(new URI("cloudnet", "local", "/" + "database", null).toString(),
				compress_include(tmp, local.listFiles(), "database"));

		if (local.exists() && local.isDirectory()) {

			source.sendMessage(prefix + "Preparing Local Configs...");
			extra_files.put(new URI("cloudnet", "local", "/" + "configs", null).toString(),
					compress_include(tmp, local.listFiles(), "tasks", "groups", "permissions.json"));

			source.sendMessage(prefix + "Preparing Local Caches...");
			extra_files.put(new URI("cloudnet", "local", "/" + "caches", null).toString(),
					compress_include(tmp, local.listFiles(), "versioncache"));

			source.sendMessage(prefix + "Preparing Local Logs...");
			extra_files.put(new URI("cloudnet", "local", "/" + "logs", null).toString(),
					compress_include(tmp, local.listFiles(), "logs"));

			source.sendMessage(prefix + "Preparing Modules...");
			extra_files.put(new URI("cloudnet", "root", "/" + "modules", null).toString(),
					compress_include(tmp, root.listFiles(), "modules", "plugins"));

			source.sendMessage(prefix + "Preparing Root Configs...");
			extra_files.put(new URI("cloudnet", "root", "/" + "configs", null).toString(), compress_include(tmp,
					root.listFiles(), "config.json", "launcher.cnl", "start.sh", "start.bat", "start.command"));

			source.sendMessage(prefix + "Preparing CloudNet Classpath...");
			extra_files.put(new URI("cloudnet", "root", "/" + "classpath", null).toString(),
					compress_include(tmp, root.listFiles(), "launcher.jar"));

		}

		Backup backup = new Backup(templates, regions, worlds, playerdata, extra_files);
		UUID uuid = UUID.randomUUID();
		BackupInfo info = new BackupInfo(uuid.toString(), CloudNetDriver.instance().version().major() + "-"
				+ CloudNetDriver.instance().version().minor() + "-" + CloudNetDriver.instance().version().patch(), -1);

		for (IBackupService<?> service : services) {

			try {
				service.start(source, backup, info);
			} catch (IOException e) {
				e.printStackTrace();
				source.sendMessage(prefix + "Service \"" + service.getClass().getName() + "\" failed.");
			}

		}

		source.sendMessage(prefix + "Cleaning...");
		delete(tmp);

		source.sendMessage(prefix + "Backup finish: " + info.id() + " Took: " + (System.currentTimeMillis() - start));

		return info;

	}

	private void delete(File tmp) throws IOException {
		if (tmp.isDirectory()) {
			for (File f : tmp.listFiles()) {
				delete(f);
			}
		}
		Files.delete(tmp.toPath());
	}

}
