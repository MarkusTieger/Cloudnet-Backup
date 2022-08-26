package de.MarkusTieger.archieve;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

public class SevenZArchiever extends AbstractArchiever {

	@Override
	public File compress(File tmp, File... content) throws IOException {
		File f = new File(tmp, UUID.randomUUID().toString());
		if (!f.exists())
			f.createNewFile();

		try (SevenZOutputFile file = new SevenZOutputFile(f)) {
			file.setContentCompression(SevenZMethod.LZMA2);
			
			for(File c : content) fill("", file, c);
			
			file.finish();
		}

		return f;
	}

	private void fill(String prefix, SevenZOutputFile file, File c) throws IOException {
		if(c.isDirectory()) {
			file.putArchiveEntry(file.createArchiveEntry(c, prefix + c.getName() + "/"));
			file.closeArchiveEntry();
			for(File f : c.listFiles()) {
				fill(prefix + c.getName() + "/", file, f);
			}
		}
		if(c.isFile()) {
			SevenZArchiveEntry entry = file.createArchiveEntry(c, prefix + c.getName());
			file.putArchiveEntry(entry);
			try (FileInputStream fis = new FileInputStream(c)) {
				int len;
				byte[] buffer = new byte[1024];
				while((len = fis.read(buffer)) > 0) {
					file.write(buffer, 0, len);
				}
			}
			file.closeArchiveEntry();
		}
	}

	@Override
	public void decompresseTo(File zip, File target) throws IOException {
		try (SevenZFile file = new SevenZFile(zip, password.toCharArray())) {

			if (!target.exists())
				target.mkdirs();

			int len;
			byte[] buffer = new byte[1024];

			SevenZArchiveEntry ze = null;
			while ((ze = file.getNextEntry()) != null) {

				File t = new File(target, ze.getName());

				if (ze.isDirectory()) {
					if (!t.exists())
						t.mkdirs();
				} else {
					if (!t.exists())
						t.createNewFile();

					try (FileOutputStream fos = new FileOutputStream(t)) {
						while ((len = file.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
							fos.flush();
						}
					}
				}

			}
		}
	}

}
