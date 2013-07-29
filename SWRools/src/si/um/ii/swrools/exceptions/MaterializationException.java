package si.um.ii.swrools.exceptions;

public class MaterializationException extends Exception {

	/**
	 * Generirana verzija
	 */
	private static final long serialVersionUID = 7512671236644257096L;

	public MaterializationException() {

	}

	public MaterializationException(String s) {
		super(s);
	}

	public MaterializationException(Throwable e) {
		super(e);
	}

	public MaterializationException(String s, Throwable e) {
		super(s, e);
	}

	public MaterializationException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}
}
