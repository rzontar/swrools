package si.um.ii.swrools.generator;

import si.um.ii.swrools.util.NameHelper;

public class Attribute {

	private String name;
	private String type = "String"; // default value for range
	private String uri;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getNameForField() {
		return NameHelper.getLowerCamelCase(name);
	}

	public Object getNameForMethod() {
		return NameHelper.getUpperCamelCase(name);
	}

	@Override
	public int hashCode() {
		return (type.hashCode() + name.hashCode());
	}

	@Override
	public boolean equals(Object paramObject) {
		if (paramObject instanceof Attribute) {
			Attribute field = (Attribute) paramObject;
			return ((field.type.equals(this.type)) && (field.name.equals(this.name)));
		}
		return false;
	}
}
