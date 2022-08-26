package de.MarkusTieger.backup;

import java.util.List;

public record AdvancedBackupInfo(String id, String cloudNetVersion, long date, List<IBackupService<?>> services) {

}
