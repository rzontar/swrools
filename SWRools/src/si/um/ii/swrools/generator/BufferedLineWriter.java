package si.um.ii.swrools.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Rok
 * 
 */
public class BufferedLineWriter extends BufferedWriter {

	/**
	 * @param out
	 */
	public BufferedLineWriter(Writer out) {
		super(out);
	}

	/**
	 * @param out
	 * @param sz
	 */
	public BufferedLineWriter(Writer out, int sz) {
		super(out, sz);
	}

	public void writeLine(String str) throws IOException {
		super.write(str);
		super.newLine();
	}
}
