package de.MarkusTieger.archieve;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class Zip4jArchiever extends AbstractArchiever {

	@Override
	public File compress(File tmp, File... content) throws IOException {

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

	@Override
	public void decompresseTo(File zip, File target) throws IOException {

		try (ZipFile zf = new ZipFile(zip, password.toCharArray())) {
			zf.extractAll(target.getAbsolutePath());
		}

	}

}
