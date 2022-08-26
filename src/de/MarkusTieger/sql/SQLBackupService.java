package de.MarkusTieger.sql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.io.Files;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.MarkusTieger.backup.Backup;
import de.MarkusTieger.backup.BackupInfo;
import de.MarkusTieger.backup.IBackupService;
import de.MarkusTieger.config.SQLConnectionConfig;
import de.MarkusTieger.data.SizedStreamData;
import eu.cloudnetservice.node.command.source.CommandSource;

public class SQLBackupService implements IBackupService<SQLConnectionConfig> {

	private HikariDataSource hikariDataSource;

	private static final int SPLIT_SIZE = 8 * 1048576;

	@Override
	public void initialize(SQLConnectionConfig endpoint) throws IOException {

		SQLReader.setServerType(endpoint.serverType());
		
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(String.format(endpoint.uri(), new Object[] { endpoint

				.address().host(), Integer.valueOf(endpoint.address().port()), endpoint.database(),
				Boolean.valueOf(endpoint.useSsl()), Boolean.valueOf(endpoint.useSsl()) }));
		hikariConfig.setDriverClassName(endpoint.driver());
		hikariConfig.setUsername(endpoint.username());
		hikariConfig.setPassword(endpoint.password());
		for (Map.Entry<String, String> property : endpoint.options().entrySet()) {
			hikariDataSource.addDataSourceProperty(property.getKey(), property.getValue());
		}
		hikariConfig.setMinimumIdle(endpoint.minimumIdle());
		hikariConfig.setMaximumPoolSize(endpoint.maximumPoolSize());
		hikariConfig.setConnectionTimeout(endpoint.connectionTimeout());
		hikariConfig.setLeakDetectionThreshold(endpoint.leakDetectionThreshold());
		hikariConfig.setValidationTimeout(endpoint.validationTimeout());

		this.hikariDataSource = new HikariDataSource(hikariConfig);

		try (Connection con = hikariDataSource.getConnection()) {
			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_backups_table"))) {
				statement.executeUpdate();
			}

			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_templates_table"))) {
				statement.executeUpdate();
			}

			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_regions_table"))) {
				statement.executeUpdate();
			}

			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_worlds_table"))) {
				statement.executeUpdate();
			}

			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_playerdata_table"))) {
				statement.executeUpdate();
			}

			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_extrafiles_table"))) {
				statement.executeUpdate();
			}

			try (PreparedStatement statement = con.prepareStatement(
					SQLReader.read("create_files_table"))) {
				statement.executeUpdate();
			}
		} catch (SQLException e) {
			throw new IOException("Can't Create Table (if not exists)", e);
		}
	}

	@Override
	public void start(CommandSource source, Backup backup, BackupInfo info) throws IOException {

		String updateId = info.id();

		Map<String, File> templates = backup.templates();
		Map<String, List<File>> regions = backup.regions();
		Map<String, File> worlds = backup.worlds();
		Map<String, List<File>> playerdata = backup.playerdata();
		Map<String, File> extra_files = backup.extra_files();

		Map<String, File> filemapping = new HashMap<>();

		String prefix = "[SQL-Backup-Service] ";
		source.sendMessage(prefix + "Starting Backup with ID: " + info.id() + "...");

		try (Connection con = hikariDataSource.getConnection()) {

			try (PreparedStatement statement = con
					.prepareStatement(SQLReader.read("insert_backup"))) {
				statement.setString(1, info.id());
				statement.setString(2, info.cloudNetVersion());
				statement.executeUpdate();
			}

			source.sendMessage(prefix + "Uploading Templates...");

			int pos = 0;
			for (Map.Entry<String, File> entry : templates.entrySet()) {
				source.sendMessage(prefix + "Uploading Template \"" + entry.getKey() + "\" ...");

				try (PreparedStatement statement = con
						.prepareStatement(SQLReader.read("insert_template"))) {
					statement.setString(1, updateId);
					statement.setString(2, entry.getKey());
					statement.setString(3, file(filemapping, entry.getValue()));
					statement.executeUpdate();
				}

				pos++;
				System.out.println("Template \"" + entry.getKey() + "\" uploaded. " + pos + " / " + templates.size());
			}

			source.sendMessage(prefix + "Uploading Regions...");

			pos = 0;
			for (Map.Entry<String, List<File>> entry : regions.entrySet()) {

				for (File value : entry.getValue()) {

					source.sendMessage(prefix + "Uploading World: \"" + entry.getKey() + "\" Region: \""
							+ value.getName() + "\" ...");

					try (PreparedStatement statement = con.prepareStatement(
							SQLReader.read("insert_region"))) {
						statement.setString(1, updateId);
						statement.setString(2, entry.getKey());
						statement.setString(3, value.getName());
						statement.setString(4, file(filemapping, value));
						statement.executeUpdate();
					}

					pos++;
					source.sendMessage(
							prefix + "World: \"" + entry.getKey() + "\" Region: " + value.getName() + " uploaded.");

				}

			}

			source.sendMessage(prefix + "Uploading Worlds...");

			pos = 0;
			for (Map.Entry<String, File> entry : worlds.entrySet()) {
				source.sendMessage(prefix + "Uploading World \"" + entry.getKey() + "\" ...");

				try (PreparedStatement statement = con
						.prepareStatement(SQLReader.read("insert_world"))) {
					statement.setString(1, updateId);
					statement.setString(2, entry.getKey());
					statement.setString(3, file(filemapping, entry.getValue()));
					statement.executeUpdate();
				}

				pos++;
				source.sendMessage(
						prefix + "World \"" + entry.getKey() + "\" uploaded. " + pos + " / " + worlds.size());
			}

			source.sendMessage(prefix + "Uploading Playerdata...");

			pos = 0;
			for (Map.Entry<String, List<File>> entry : playerdata.entrySet()) {

				for (File value : entry.getValue()) {

					source.sendMessage(prefix + "Uploading World: \"" + entry.getKey() + "\" Player: \""
							+ value.getName() + "\" ...");

					try (PreparedStatement statement = con.prepareStatement(
							SQLReader.read("insert_playerdata"))) {
						statement.setString(1, updateId);
						statement.setString(2, entry.getKey());
						statement.setString(3, value.getName());
						statement.setString(4, file(filemapping, value));
						statement.executeUpdate();
					}

					pos++;
					source.sendMessage(
							prefix + "World: \"" + entry.getKey() + "\" Player: " + value.getName() + " uploaded.");

				}

			}

			source.sendMessage(prefix + "Uploading Extra-Files...");

			pos = 0;
			for (Map.Entry<String, File> entry : extra_files.entrySet()) {
				source.sendMessage(prefix + "Uploading File \"" + entry.getKey() + "\" ...");

				try (PreparedStatement statement = con
						.prepareStatement(SQLReader.read("insert_extrafiles"))) {
					statement.setString(1, updateId);
					statement.setString(2, entry.getKey());
					statement.setString(3, file(filemapping, entry.getValue()));
					statement.executeUpdate();
				}

				pos++;
				source.sendMessage(prefix + "File \"" + entry.getKey() + "\" uploaded. " + pos + " / " + worlds.size());
			}

			long size = 0L;
			long uploaded = 0L;

			for (File f : filemapping.values()) {
				size += f.length();
			}

			source.sendMessage(prefix + "Uploading Files...");
			source.sendMessage(prefix + "Size: " + size);

			for (Map.Entry<String, File> entry : filemapping.entrySet()) {

				List<SizedStreamData> toUpload = split(entry.getValue());

				pos = 0;

				for (SizedStreamData data : toUpload) {

					try (PreparedStatement statement = con
							.prepareStatement(SQLReader.read("insert_files"))) {
						statement.setString(1, updateId);
						statement.setString(2, entry.getKey());
						statement.setInt(3, pos);
						statement.setBlob(4, data.in());
						statement.executeUpdate();
					}

					uploaded += data.size();

					System.out.println(prefix + "Uploaded " + humanReadableByteCountBin(uploaded) + " / "
							+ humanReadableByteCountBin(size));

					pos++;
				}

			}

		} catch (SQLException e1) {
			throw new IOException("Backup create failed. ", e1);
		}

	}

	public static String humanReadableByteCountBin(long bytes) {
		long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
		if (absB < 1024) {
			return bytes + " B";
		}
		long value = absB;
		CharacterIterator ci = new StringCharacterIterator("KMGTPE");
		for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
			value >>= 10;
			ci.next();
		}
		value *= Long.signum(bytes);
		return String.format("%.1f %ciB", value / 1024.0, ci.current());
	}

	private List<SizedStreamData> split(File value) throws IOException {

		List<SizedStreamData> in = new ArrayList<>();

		int len;
		byte[] buffer = new byte[1024];

		try (FileInputStream fis = new FileInputStream(value)) {

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			while ((len = fis.read(buffer)) > 0) {
				out.write(buffer, 0, len);
				if (out.size() >= SPLIT_SIZE) {
					in.add(new SizedStreamData(new ByteArrayInputStream(out.toByteArray()), (long) out.size()));
					out = new ByteArrayOutputStream();
				}
			}

			if (out.size() > 0) {
				in.add(new SizedStreamData(new ByteArrayInputStream(out.toByteArray()), (long) out.size()));
			}

		}

		return in;
	}

	private String file(Map<String, File> filemapping, File value) {
		String str = UUID.randomUUID() + "-" + value.getName();
		filemapping.put(str, value);
		return str;
	}

	@Override
	public Backup restore(CommandSource source, File tmp, String updateId) throws IOException {

		if (!exists(updateId))
			throw new IOException("Update with Id \"" + updateId + "\" does not exists.");

		try (Connection con = hikariDataSource.getConnection()) {

			String prefix = "[MySQL-Backup-Service] ";
			source.sendMessage(prefix + "Restoring Backup with ID: " + updateId + "...");

			Map<String, File> templates = new HashMap<>();
			Map<String, List<File>> regions = new HashMap<>();
			Map<String, File> worlds = new HashMap<>();
			Map<String, List<File>> playerdata = new HashMap<>();
			Map<String, File> extra_files = new HashMap<>();

			Map<String, File> filemapping = new HashMap<>();

			source.sendMessage(prefix + "Finding Files to download...");

			List<String> toDownload = new ArrayList<>();
			try (PreparedStatement statement = con
					.prepareStatement(SQLReader.read("select_files_exclusive_data"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {

					while (result.next()) {
						toDownload.add(result.getString("file"));
					}

				}
			}

			source.sendMessage(prefix + "Downloading " + toDownload.size() + " files...");

			int len;
			byte[] buffer = new byte[1024];

			for (String file : toDownload) {

				try (PreparedStatement statement = con
						.prepareStatement(SQLReader.read("select_files_ordered_asc"))) {
					statement.setString(1, updateId);
					statement.setString(2, file);
					try (ResultSet result = statement.executeQuery()) {
						File f = new File(tmp, UUID.randomUUID().toString());
						if (!f.exists())
							f.createNewFile();

						try (FileOutputStream fos = new FileOutputStream(f)) {

							int pos = 0;
							while (result.next()) {
								if (result.getInt("id") != pos)
									throw new IOException("File Id \"" + pos + "\" was skipped. Pos given: "
											+ result.getInt("id") + ", File id: " + file);

								Blob b = result.getBlob("data");

								InputStream in = b.getBinaryStream();
								while ((len = in.read(buffer)) > 0) {
									fos.write(buffer, 0, len);
									fos.flush();
								}
								in.close();
								b.free();

								pos++;
							}
						}

						filemapping.put(file, f);
					}
				}

			}

			source.sendMessage(prefix + "Arrange files into templates...");

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_templates"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {

					while (result.next()) {
						File f = filemapping.get(result.getString("file"));
						if (f == null)
							throw new IOException(
									"File \"" + result.getString("file") + "\" does not exists in the database.");

						templates.put(result.getString("name"), f);

					}

				}
			}

			source.sendMessage(prefix + "Arrange files into regions...");

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_regions"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {

					while (result.next()) {
						File f = filemapping.get(result.getString("file"));
						if (f == null)
							throw new IOException(
									"File \"" + result.getString("file") + "\" does not exists in the database.");

						List<File> files = regions.get(result.getString("name"));
						if (files == null) {
							files = new ArrayList<>();
							regions.put(result.getString("name"), files);
						}

						File tmp_ = new File(tmp, UUID.randomUUID().toString());
						if (!tmp_.exists())
							tmp_.mkdirs();

						Files.copy(f, new File(tmp, result.getString("filename")));
						files.add(f);

					}

				}
			}

			source.sendMessage(prefix + "Arrange files into worlds...");

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_worlds"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {

					while (result.next()) {
						File f = filemapping.get(result.getString("file"));
						if (f == null)
							throw new IOException(
									"File \"" + result.getString("file") + "\" does not exists in the database.");

						worlds.put(result.getString("name"), f);

					}

				}
			}

			source.sendMessage(prefix + "Arrange files into playerdata...");

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_playerdata"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {

					while (result.next()) {
						File f = filemapping.get(result.getString("file"));
						if (f == null)
							throw new IOException(
									"File \"" + result.getString("file") + "\" does not exists in the database.");

						List<File> files = playerdata.get(result.getString("name"));
						if (files == null) {
							files = new ArrayList<>();
							playerdata.put(result.getString("name"), files);
						}

						File tmp_ = new File(tmp, UUID.randomUUID().toString());
						if (!tmp_.exists())
							tmp_.mkdirs();

						Files.copy(f, new File(tmp, result.getString("filename")));
						files.add(f);

					}

				}
			}

			source.sendMessage(prefix + "Arrange files into extra_files...");

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_extrafiles"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {

					while (result.next()) {
						File f = filemapping.get(result.getString("file"));
						if (f == null)
							throw new IOException(
									"File \"" + result.getString("file") + "\" does not exists in the database.");

						extra_files.put(result.getString("name"), f);

					}

				}
			}

			return new Backup(templates, regions, worlds, playerdata, extra_files);
		} catch (SQLException e1) {
			throw new IOException("Backup restore failed. ", e1);
		}
	}

	@Override
	public List<BackupInfo> listBackups() throws IOException {
		List<BackupInfo> list = new ArrayList<>();

		try (Connection con = hikariDataSource.getConnection()) {

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_backups"))) {
				try (ResultSet result = statement.executeQuery()) {

					while (result.next())
						list.add(new BackupInfo(result.getString("updateId"), result.getString("version"),
								result.getTimestamp("time").getTime()));

				}
			}

		} catch (SQLException e) {
			throw new IOException("List Backups failed.", e);
		}

		return list;
	}

	@Override
	public long calculateStartMemory(String updateId, Backup backup) throws IOException {

		try (Connection con = hikariDataSource.getConnection()) {

			long size = 0L;

			List<File> files = new ArrayList<>();

			files.addAll(backup.templates().values());
			backup.regions().values().forEach(files::addAll);
			files.addAll(backup.worlds().values());
			files.addAll(backup.extra_files().values());
			backup.playerdata().values().forEach(files::addAll);

			for (File f : files) {
				long l = f.length();
				if (size < l)
					size = l;
			}

			return calculateRestoreMemory(updateId) + size;

		} catch (SQLException e) {
			throw new IOException("Calculate Start Memory failed.", e);
		}
	}

	@Override
	public long calculateRestoreMemory(String updateId) {
		return 536870912L;
	}

	@Override
	public boolean exists(String updateId) throws IOException {
		try (Connection con = hikariDataSource.getConnection()) {

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_backup"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {
					return result.next();
				}
			}

		} catch (SQLException e) {
			throw new IOException("Backups check failed.", e);
		}
	}

	@Override
	public String getName() {
		return "sql";
	}

	@Override
	public BackupInfo get(String updateId) throws IOException {
		try (Connection con = hikariDataSource.getConnection()) {

			try (PreparedStatement statement = con.prepareStatement(SQLReader.read("select_backup"))) {
				statement.setString(1, updateId);
				try (ResultSet result = statement.executeQuery()) {
					if (result.next())
						return new BackupInfo(result.getString("updateId"), result.getString("version"),
								result.getTimestamp("time").getTime());
				}
			}

		} catch (SQLException e) {
			throw new IOException("Backup get failed.", e);
		}
		throw new IOException("Backup get failed.");
	}

}
