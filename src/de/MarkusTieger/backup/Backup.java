package de.MarkusTieger.backup;

import java.io.File;
import java.util.List;
import java.util.Map;

public record Backup(Map<String, File> templates, Map<String, List<File>> regions, Map<String, File> worlds,
		Map<String, List<File>> playerdata, Map<String, File> extra_files) {

}
