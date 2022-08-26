package de.MarkusTieger.sql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import lombok.Getter;
import lombok.Setter;

public class SQLReader {

	@Getter
	@Setter
	private static SQLServerType serverType = SQLServerType.MYSQL;
	
	public static String read(String name) throws IOException {
		String path = "statements/" + serverType.name().toLowerCase() + "/" + name.toLowerCase() + ".sql";
		
		InputStream in = SQLReader.class.getResourceAsStream(path);
		if(in == null) return null;
		byte[] data = in.readAllBytes();
		
		in.close();
		
		return new String(data, StandardCharsets.UTF_8);
	}

	
	
}
