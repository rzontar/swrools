/**
 * 
 */
package si.um.ii.swrools;

import java.util.HashMap;

import org.semanticweb.owlapi.model.IRI;

/**
 * @author Rok
 * 
 */
class ReferenceMap extends HashMap<String, OWLThing> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3857014876728072228L;

	public OWLThing get(String key) {
		return super.get(key);
	}

	public OWLThing get(IRI key) {
		return get(key.toString());
	}

	public boolean containsKey(String key) {
		return super.containsKey(key);
	}

	public boolean containsKey(IRI key) {
		return containsKey(key.toString());
	}
}
