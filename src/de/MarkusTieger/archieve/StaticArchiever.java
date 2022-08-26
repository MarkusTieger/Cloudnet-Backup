package de.MarkusTieger.archieve;

import java.io.File;
import java.io.IOException;

public class StaticArchiever {

	public static File compress(File tmp, File... content) throws IOException {
		return AbstractArchiever.getInstance().compress(tmp, content);
	}

	public static File compress_include(File tmp, File[] files, String... include) throws IOException {
		return AbstractArchiever.getInstance().compress_include(tmp, files, include);
	}

	public static File compress_exclude(File tmp, File[] files, String... exclude) throws IOException {
		return AbstractArchiever.getInstance().compress_exclude(tmp, files, exclude);
	}

	public static void decompresseTo(File zip, File target) throws IOException {
		AbstractArchiever.getInstance().decompresseTo(zip, target);
	}

}
