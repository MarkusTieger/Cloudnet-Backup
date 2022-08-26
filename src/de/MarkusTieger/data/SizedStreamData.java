package de.MarkusTieger.data;

import java.io.InputStream;

public record SizedStreamData(InputStream in, long size) {

}
