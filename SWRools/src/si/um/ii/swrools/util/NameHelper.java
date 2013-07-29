package si.um.ii.swrools.util;

import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.vocab.OWL2Datatype;

/**
 * @author Rok
 * 
 */
public class NameHelper {

	public static String getLowerCamelCase(String str) {
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	public static String getUpperCamelCase(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	public static String getInterfaceClassName(String pckg, String className) {
		if (pckg.endsWith(".gen")) {
			return String.format("%s.%s", pckg, className);
		}

		return String.format("%s.gen.%s", pckg, className);
	}

	public static String getImplementationClassName(String pckg, String className) {
		if (pckg.endsWith(".gen")) {
			return String.format("%s.impl.%sImpl", pckg, className);
		}
		return String.format("%s.gen.impl.%sImpl", pckg, className);
	}

	public static String getNameFromClass(String className) {
		return getNameAfter(className, ".");
	}

	public static String getNameFromUri(String uri) {
		return getNameAfter(uri, "#");
	}

	private static String getNameAfter(String str, String after) {
		int li = str.lastIndexOf(after);

		if (li < 0) {
			return str;
		}

		return str.substring(li + after.length());
	}

	public static String concatName(String name, List<String> names) {
		names.add(name);
		Collections.sort(names);

		StringBuilder sb = new StringBuilder();
		for (String id : names) {
			sb.append(id);
		}

		return sb.toString();
	}

	public static String concatAlphabetically(String s1, String s2) {
		return s1.compareTo(s2) < 0 ? s1 + s2 : s2 + s1;
	}

	/**
	 * Static method for converting OWL2Datatypes to equivalent java strings
	 * 
	 * @param dt
	 * @return String
	 */
	public static String convertToType(OWL2Datatype dt) {
		switch (dt) {
			case XSD_SHORT:
				return "short";
			case XSD_INT:
			case XSD_INTEGER:
				return "int";
			case XSD_LONG:
				return "long";
			case XSD_FLOAT:
				return "float";
			case XSD_DOUBLE:
				return "double";
			case XSD_BOOLEAN:
				return "boolean";
			case XSD_STRING:
				return "String";
			case XSD_DATE_TIME:
				return "DateTime";
			default:
				return "/* Unsupported data type */";
		}
	}
}
