package si.um.ii.swrools.generator;

import si.um.ii.swrools.util.NameHelper;

public class Property {
	private String name;
	private String type;
	private String uri;
	private boolean symetric;
	private boolean transitive;
	private boolean functional;
	private boolean inverseFunctional;
	private boolean reflexive;
	private boolean irreflexive;
	private boolean asymmetric;
	private String inverse;
	private String superProperty;

	public String getName() {
		return this.name;
	}

	public void setName(String paramString) {
		this.name = paramString;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String paramString) {
		this.uri = paramString;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String str) {
		this.type = str;
	}

	public boolean isSymetric() {
		return symetric;
	}

	public void setSymetric(boolean symetric) {
		this.symetric = symetric;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public void setTransitive(boolean transitive) {
		this.transitive = transitive;
	}

	public boolean isFunctional() {
		return this.functional;
	}

	public void setFunctional(boolean functional) {
		this.functional = functional;
	}

	/**
	 * @return the inverseFunctional
	 */
	public boolean isInverseFunctional() {
		return inverseFunctional;
	}

	/**
	 * @param inverseFunctional
	 *            the inverseFunctional to set
	 */
	public void setInverseFunctional(boolean inverseFunctional) {
		this.inverseFunctional = inverseFunctional;
	}

	/**
	 * @return the refleksive
	 */
	public boolean isReflexive() {
		return reflexive;
	}

	/**
	 * @param refleksive
	 *            the refleksive to set
	 */
	public void setReflexive(boolean reflexive) {
		this.reflexive = reflexive;
	}

	/**
	 * @return the irreflexive
	 */
	public boolean isIrreflexive() {
		return irreflexive;
	}

	/**
	 * @param irreflexive
	 *            the irreflexive to set
	 */
	public void setIrreflexive(boolean irreflexive) {
		this.irreflexive = irreflexive;
	}

	/**
	 * @return the asymmetric
	 */
	public boolean isAsymmetric() {
		return asymmetric;
	}

	/**
	 * @param asymmetric
	 *            the asymmetric to set
	 */
	public void setAsymmetric(boolean asymmetric) {
		this.asymmetric = asymmetric;
	}

	public void setInverse(String inverse) {
		this.inverse = inverse;
	}

	public String getInverse() {
		return this.inverse;
	}

	public boolean isInverse() {
		return inverse != null;
	}

	public void setSuperProperty(String id) {
		this.superProperty = id;
	}

	public String getSuperProperty() {
		return this.superProperty;
	}

	public boolean isSubProperty() {
		return this.superProperty != null;
	}

	@Override
	public boolean equals(Object paramObject) {
		if (paramObject instanceof Property) {
			Property property = (Property) paramObject;
			return ((property.name.equals(this.name)) && (property.uri.equalsIgnoreCase(this.uri)));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.uri.hashCode();
	}

	public String getNameForField() {
		return NameHelper.getLowerCamelCase(name);
	}

	public String getNameForMethod() {
		return NameHelper.getUpperCamelCase(name);
	}

}
