package de.MarkusTieger.archieve;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractArchiever {

	@Setter
	protected static String password;

	@Getter
	@Setter
	private static AbstractArchiever instance;

	public abstract File compress(File tmp, File... content) throws IOException;

	public File compress_include(File tmp, File[] files, String... include) throws IOException {
		return compress(tmp, Arrays.stream(files).filter(
				(f) -> Arrays.stream(include).map(String::toLowerCase).toList().contains(f.getName().toLowerCase()))
				.toList().toArray(new File[0]));
	}

	public File compress_exclude(File tmp, File[] files, String... exclude) throws IOException {
		return compress(tmp, Arrays.stream(files).filter(
				(f) -> !Arrays.stream(exclude).map(String::toLowerCase).toList().contains(f.getName().toLowerCase()))
				.toList().toArray(new File[0]));
	}

	public abstract void decompresseTo(File zip, File target) throws IOException;
}
