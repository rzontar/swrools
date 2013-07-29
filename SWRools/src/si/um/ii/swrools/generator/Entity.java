package si.um.ii.swrools.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import si.um.ii.swrools.util.NameHelper;

public class Entity {
	private String className;
	private Set<Property> properties = new HashSet<Property>();
	private Set<Attribute> attributes = new HashSet<Attribute>();
	private String namespace;
	private Set<Entity> superClasses = null;
	private Set<Entity> subClasses = null;
	private Set<Entity> disjointWith = null;
	private Set<Entity> equivalentWith = null;

	public String getClassName() {
		return this.className;
	}

	public void setClassName(String paramString) {
		this.className = paramString;
	}

	public Set<Property> getProperties() {
		return this.properties;
	}

	public void setProperties(Set<Property> paramSet) {
		this.properties = paramSet;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String paramString) {
		this.namespace = paramString;
	}

	public Set<Entity> getSuperClasses() {
		return this.superClasses;
	}

	public void setSuperClasses(Set<Entity> paramSet) {
		this.superClasses = paramSet;
	}

	public Set<Entity> getSubClasses() {
		return this.subClasses;
	}

	public void setSubClasses(Set<Entity> paramSet) {
		this.subClasses = paramSet;
	}

	public Set<Entity> getDisjointWith() {
		return this.disjointWith;
	}

	public void setDisjointWith(Set<Entity> paramSet) {
		this.disjointWith = paramSet;
	}

	public void setAttributes(Set<Attribute> fields) {
		this.attributes = fields;
	}

	public Set<Attribute> getAttributes() {
		return attributes;
	}

	public void setEquivalentWith(Set<Entity> equivalentWith) {
		this.equivalentWith = equivalentWith;
	}

	public Set<Entity> getEquivalentWith() {
		return equivalentWith;
	}

	@Override
	public boolean equals(Object paramObject) {
		if (paramObject instanceof Entity) {
			Entity localEntity = (Entity) paramObject;
			return ((localEntity.className.equals(this.className)) && (localEntity.namespace.equals(this.namespace)));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.className.hashCode() + this.namespace.hashCode());
	}

	@Override
	public String toString() {
		return getUri();
	}

	public String getUri() {
		return namespace + className;
	}

	public String getFullClassName() {
		if (equivalentWith.isEmpty()) {
			return className;
		}

		// sort names to assure correct order
		ArrayList<String> names = new ArrayList<>();
		for (Entity e : equivalentWith) {
			names.add(e.getClassName());
		}

		return NameHelper.concatName(className, names);
	}

}
