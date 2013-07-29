/**
 * 
 */
package si.um.ii.swrools.exceptions;

/**
 * @author Rok
 * 
 */
public class ClassificationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8333636479721643184L;

	public ClassificationException() {
	}

	public ClassificationException(String s) {
		super(s);
	}

	public ClassificationException(Throwable e) {
		super(e);
	}

	public ClassificationException(String s, Throwable e) {
		super(s, e);
	}

	public ClassificationException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}
}
