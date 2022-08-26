package de.MarkusTieger.backup;

import java.io.File;
import java.io.IOException;
import java.util.List;

import eu.cloudnetservice.node.command.source.CommandSource;

public interface IBackupService<T> {

	public String getName();

	public void initialize(T config) throws IOException;

	public void start(CommandSource source, Backup backup, BackupInfo info) throws IOException;

	public Backup restore(CommandSource source, File tmp, String updateId) throws IOException;

	public boolean exists(String updateId) throws IOException;

	public List<BackupInfo> listBackups() throws IOException;

	public long calculateStartMemory(String updateId, Backup backup) throws IOException;

	public long calculateRestoreMemory(String updateId) throws IOException;

	public BackupInfo get(String name) throws IOException;

}
