package de.MarkusTieger.compress;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import lombok.Setter;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class Compressor {

	@Setter
	private static String password;

	public static File compress(File tmp, File... content) throws IOException {

		final ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
		zipParameters.setCompressionLevel(CompressionLevel.MAXIMUM);
		zipParameters.setEncryptFiles(true);
		zipParameters.setEncryptionMethod(EncryptionMethod.AES);
		zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);

		File f = new File(tmp, UUID.randomUUID().toString());

		try (ZipFile zf = new ZipFile(f, password.toCharArray())) {

			for (File c : content) {
				if (c.isFile()) {
					zf.addFile(c, zipParameters);
				}
				if (c.isDirectory()) {
					zf.addFolder(c, zipParameters);
				}
			}

		}

		return f;
	}

	public static File compress_include(File tmp, File[] files, String... include) throws IOException {
		return compress(tmp, Arrays.stream(files).filter(
				(f) -> Arrays.stream(include).map(String::toLowerCase).toList().contains(f.getName().toLowerCase()))
				.toList().toArray(new File[0]));
	}

	public static File compress_exclude(File tmp, File[] files, String... exclude) throws IOException {
		return compress(tmp, Arrays.stream(files).filter(
				(f) -> !Arrays.stream(exclude).map(String::toLowerCase).toList().contains(f.getName().toLowerCase()))
				.toList().toArray(new File[0]));
	}

	public static void decompresseTo(File zip, File target) throws IOException {

		try (ZipFile zf = new ZipFile(zip, password.toCharArray())) {
			zf.extractAll(target.getAbsolutePath());
		}

	}
}
