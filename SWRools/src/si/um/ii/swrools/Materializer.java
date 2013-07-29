package si.um.ii.swrools;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import si.um.ii.swrools.exceptions.MaterializationException;
import si.um.ii.swrools.util.NameHelper;

/**
 * @author Rok
 * @param <T>
 * 
 */
@SuppressWarnings("unchecked")
class Materializer<T extends OWLThing> {

	private Class<T> cls;

	public Materializer(Class<T> c) throws MaterializationException {
		if (c == null) {
			throw new IllegalArgumentException();
		}

		cls = c;
		if (!cls.isInterface()) {
			throw new MaterializationException(c.getName() + " is not an Interface!");
		}
	}

	public Materializer(String className) throws MaterializationException {
		try {
			cls = (Class<T>) Class.forName(className);
			if (!cls.isInterface()) {
				throw new MaterializationException(className + " is not an Interface!");
			}
		} catch (ClassNotFoundException e) {
			throw new MaterializationException(e);
		}
	}

	private Class<? extends OWLThingImpl> getImplementationClass(Class<? extends OWLThing> cls) throws MaterializationException {
		try {
			String className = null;

			if (cls.isAnnotationPresent(OWLEquivalentClass.class)) {
				List<String> ids = new ArrayList<>();
				for (String uri : cls.getAnnotation(OWLEquivalentClass.class).id()) {
					ids.add(NameHelper.getNameFromUri(uri));
				}
				className = NameHelper.concatName(cls.getSimpleName(), ids);
			} else {
				className = cls.getSimpleName();
			}

			String fcName = NameHelper.getImplementationClassName(cls.getPackage().getName(), className);

			return (Class<? extends OWLThingImpl>) Class.forName(fcName);
		} catch (ClassNotFoundException e) {
			throw new MaterializationException(e);
		}
	}

	public <U extends OWLThing> void setProperty(String objPropId, OWLThing obj, Collection<U> individuals, Class<?> targetCls)
			throws MaterializationException {
		for (Method m : cls.getMethods()) {
			if (m.isAnnotationPresent(OWLObjectProperty.class) && m.getAnnotation(OWLObjectProperty.class).id().equals(objPropId)) {
				try {
					OWLCollection<U> col = (OWLCollection<U>) m.invoke(obj);
					col.addAll(individuals);
					break;
				} catch (ReflectiveOperationException e) {
					throw new MaterializationException(e);
				}
			}
		}
	}

	private <U extends OWLThing> void createSubPropertyObservers(OWLThing obj) throws MaterializationException {
		for (Method m : cls.getMethods()) {
			if (m.isAnnotationPresent(OWLSubPropertyOf.class)) {
				try {
					OWLCollection<U> col = (OWLCollection<U>) m.invoke(obj);
					if (col.isSubProperty()) {
						String superUri = m.getAnnotation(OWLSubPropertyOf.class).id();

						String methodName = "get" + NameHelper.getUpperCamelCase(NameHelper.getNameFromUri(superUri));
						Method targetMethod = cls.getMethod(methodName);

						OWLCollection<?> superCol = (OWLCollection<?>) targetMethod.invoke(obj);
						col.addObserver(superCol);
					}
				} catch (ReflectiveOperationException e) {
					throw new MaterializationException(e);
				}
			}
		}
	}

	public <U extends OWLThing> U createInstance(String id) throws MaterializationException {
		try {
			U instance = null;
			if (cls.isAssignableFrom(OWLThingImpl.class)) {
				instance = (U) cls.getConstructor(String.class).newInstance(id);
			} else {
				instance = (U) getImplementationClass(cls).getConstructor(String.class).newInstance(id);
			}

			initializeProperties(instance);
			createSubPropertyObservers(instance);

			return instance;
		} catch (ReflectiveOperationException e) {
			throw new MaterializationException(e);
		}
	}

	private <U extends OWLThing> void initializeProperties(U instance) throws NoSuchFieldException, IllegalAccessException {
		for (Method m : cls.getMethods()) {
			if (m.isAnnotationPresent(OWLObjectProperty.class)) {
				Field colField = instance.getClass().getDeclaredField(NameHelper.getLowerCamelCase(m.getName().substring(3)));
				colField.setAccessible(true);

				OWLCollection<U> col = new OWLCollection<U>(m.getAnnotation(OWLObjectProperty.class).id(), instance);
				col.setSymmetric(m.isAnnotationPresent(OWLSymmetricProperty.class));
				col.setTransitive(m.isAnnotationPresent(OWLTransitiveProperty.class));
				col.setInverse(m.isAnnotationPresent(OWLInverseOf.class));
				col.setFunctional(m.isAnnotationPresent(OWLFunctionalProperty.class));
				col.setReflexive(m.isAnnotationPresent(OWLReflexiveProperty.class));
				col.setIrreflexive(m.isAnnotationPresent(OWLIrreflexiveProperty.class));

				col.setSubProperty(m.isAnnotationPresent(OWLSubPropertyOf.class));

				colField.set(instance, col);
			}
		}
	}

	public Collection<String> getDatatypeProperties() throws MaterializationException {
		return getProperties(OWLDatatypeProperty.class);
	}

	public Collection<String> getObjectProperties() throws MaterializationException {
		return getProperties(OWLObjectProperty.class);
	}

	private Collection<String> getProperties(Class<? extends Annotation> prop) throws MaterializationException {
		ArrayList<String> ids = new ArrayList<String>();

		for (Method m : cls.getMethods()) {
			if (m.isAnnotationPresent(prop)) {
				Annotation objectProperty = m.getAnnotation(prop);

				try {
					Method am = objectProperty.getClass().getMethod("id");
					ids.add((String) am.invoke(objectProperty));

				} catch (ReflectiveOperationException e) {
					throw new MaterializationException(e);
				}
			}
		}

		return ids;
	}

	public Class<?> getReturnType(String id) throws MaterializationException {
		for (Method m : cls.getMethods()) {
			if (m.isAnnotationPresent(OWLObjectProperty.class)) {
				OWLObjectProperty objectProperty = m.getAnnotation(OWLObjectProperty.class);
				if (objectProperty.id().equals(id)) {
					Type returnType = m.getGenericReturnType();

					if (returnType instanceof ParameterizedType) {
						Type colType = ((ParameterizedType) returnType).getActualTypeArguments()[0];

						if (colType instanceof WildcardType) {
							return (Class<?>) ((WildcardType) colType).getUpperBounds()[0];
						}
						return (Class<?>) colType;
					}
					return (Class<?>) returnType;
				}
			}
		}
		throw new MaterializationException("Method with OWLObjectProperty(id=\"" + id + "\") not found.");
	}

	public void setValue(OWLThing instance, String datatypeURI, String value) throws MaterializationException {
		try {
			String getMethod = "get" + NameHelper.getUpperCamelCase(NameHelper.getNameFromUri(datatypeURI));
			Class<?> returnType = cls.getMethod(getMethod).getReturnType();

			String setMethod = "s" + getMethod.substring(1);

			Class<?> paramType = returnType;

			if (paramType.isPrimitive()) {
				paramType = primitiveMap.get(returnType);
			}

			Object realValue = paramType.getConstructor(new Class[] { String.class }).newInstance(value);
			cls.getMethod(setMethod, returnType).invoke(instance, realValue);
		} catch (ReflectiveOperationException e) {
			throw new MaterializationException(e);
		}
	}

	public Collection<Entry<String, Object>> getDatatypeValues(OWLThing instance) throws MaterializationException {
		ArrayList<Entry<String, Object>> list = new ArrayList<Entry<String, Object>>();

		Collection<String> datatypes = getDatatypeProperties();

		for (String dt : datatypes) {
			// get datatype value from field
			list.add(new AbstractMap.SimpleEntry<String, Object>(dt, getValue(dt, instance)));
		}

		return list;
	}

	private Object getValue(String datatypeURI, Object instance) throws MaterializationException {
		String methodName = "get" + NameHelper.getUpperCamelCase(NameHelper.getNameFromUri(datatypeURI));

		try {
			Method m = cls.getMethod(methodName);
			return m.invoke(instance);
		} catch (ReflectiveOperationException e) {
			throw new MaterializationException(e);
		}
	}

	public static void addObserver(OWLCollection<? extends OWLThing> sender, OWLThing instance, ObjectPropertyType type) {
		Class<? extends OWLThing> implClass = instance.getClass();
		Class<?> owlThingInterface = implClass.getInterfaces()[0];

		switch (type) {
			case INVERSE:
				for (Method m : owlThingInterface.getMethods()) {
					if (m.isAnnotationPresent(OWLInverseOf.class) && m.getAnnotation(OWLInverseOf.class).id().equals(sender.getName())) {
						try {
							OWLCollection<OWLThing> col = (OWLCollection<OWLThing>) m.invoke(instance);
							sender.addObserver(col);
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}
				break;
			case TRANSITIVE:
				for (Method m : owlThingInterface.getMethods()) {
					if (m.isAnnotationPresent(OWLTransitiveProperty.class)
							&& m.getAnnotation(OWLObjectProperty.class).id().equals(sender.getName())) {
						try {
							OWLCollection<OWLThing> col = (OWLCollection<OWLThing>) m.invoke(instance);
							((OWLCollection<OWLThing>) sender).addAll(col); // Add all existing
							// instances
							col.addObserver(sender);
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}
				break;
			case SYMMETRIC:
				for (Method m : owlThingInterface.getMethods()) {
					if (m.isAnnotationPresent(OWLSymmetricProperty.class)
							&& m.getAnnotation(OWLObjectProperty.class).id().equals(sender.getName())) {
						try {
							OWLCollection<OWLThing> col = (OWLCollection<OWLThing>) m.invoke(instance);
							sender.addObserver(col);
						} catch (ReflectiveOperationException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}
				break;
		}
	}

	public void copyFields(OWLThing from, T to) throws MaterializationException {
		for (Class<?> i : from.getClass().getInterfaces()) {
			if (!OWLThing.class.isAssignableFrom(i)) {
				continue;
			}

			for (Method m : i.getMethods()) {
				if (m.isAnnotationPresent(OWLDatatypeProperty.class)) {
					try {
						Method setMethod = to.getClass().getMethod("set" + m.getName().substring(3), m.getReturnType());
						setMethod.invoke(to, m.invoke(from));
					} catch (ReflectiveOperationException e) {
						throw new MaterializationException(e);
					}
				}
			}
		}
	}

	public void copyCollections(OWLThing from, T to) throws MaterializationException {
		for (Class<?> i : from.getClass().getInterfaces()) {
			if (!OWLThing.class.isAssignableFrom(i)) {
				continue;
			}

			for (Method m : i.getMethods()) {
				if (m.isAnnotationPresent(OWLObjectProperty.class)) {
					try {
						String fieldName = NameHelper.getLowerCamelCase(m.getName().substring(3));

						Field fieldFrom = from.getClass().getDeclaredField(fieldName);
						fieldFrom.setAccessible(true);

						Field fieldTo = to.getClass().getDeclaredField(fieldName);
						fieldTo.setAccessible(true);

						Field parent = OWLCollection.class.getDeclaredField("parent");
						parent.setAccessible(true);
						
						OWLCollection<?> val = (OWLCollection<?>) fieldFrom.get(from);						
						parent.set(val, to);						
						fieldTo.set(to, val);

					} catch (ReflectiveOperationException e) {
						throw new MaterializationException(e);
					}
				}
			}
		}
	}

	/**
	 * Mapiranje primitivnih tipov v razrede http://www.xinotes.org/notes/note/1330/
	 */
	private static Map<Class<?>, Class<?>> primitiveMap = new HashMap<>();
	static {
		primitiveMap.put(boolean.class, Boolean.class);
		primitiveMap.put(byte.class, Byte.class);
		primitiveMap.put(char.class, Character.class);
		primitiveMap.put(short.class, Short.class);
		primitiveMap.put(int.class, Integer.class);
		primitiveMap.put(long.class, Long.class);
		primitiveMap.put(float.class, Float.class);
		primitiveMap.put(double.class, Double.class);
	}
}
