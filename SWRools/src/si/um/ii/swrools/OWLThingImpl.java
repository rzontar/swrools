package si.um.ii.swrools;

public abstract class OWLThingImpl implements OWLThing {

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	protected OWLThingImpl(String id) {
		this.id = id;
	}

	/*
	 * Instance identifier
	 */
	protected String id;

	@Override
	public String getId() {
		return id;
	}
}
