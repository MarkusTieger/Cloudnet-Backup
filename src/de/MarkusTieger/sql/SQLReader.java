package de.MarkusTieger.sql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import lombok.Getter;
import lombok.Setter;

public class SQLReader {

	private static final SQLServerType defaultType = SQLServerType.MYSQL;
	
	@Getter
	@Setter
	private static SQLServerType serverType = defaultType;
	
	public static String read(String name) throws IOException {
		String path = "statements/" + serverType.name().toLowerCase() + "/" + name.toLowerCase() + ".sql";
		
		InputStream in = SQLReader.class.getResourceAsStream(path);
		if(in == null) {
			path = "statements/" + defaultType.name().toLowerCase() + "/" + name.toLowerCase() + ".sql";
			in = SQLReader.class.getResourceAsStream(path);
			if(in == null) throw new IOException("SQL-File: \"" + name + "\" not found.");
		}
		byte[] data = in.readAllBytes();
		
		in.close();
		
		return new String(data, StandardCharsets.UTF_8);
	}

	
	
}
