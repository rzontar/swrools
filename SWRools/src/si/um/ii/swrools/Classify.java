/**
 * 
 */
package si.um.ii.swrools;

import si.um.ii.swrools.exceptions.ClassificationException;
import si.um.ii.swrools.exceptions.MaterializationException;

/**
 * @author Rok
 * 
 */
public class Classify {

	/***
	 * Classifies class as subclass
	 * 
	 * @param individual
	 * @param cls
	 * @return
	 * @throws ClassificationException
	 */
	public static <T extends OWLThing> T asClass(OWLThing individual, Class<T> cls) throws ClassificationException {
		checkSubClass(individual.getClass(), cls);

		try {
			Materializer<T> materializer = new Materializer<T>(cls);

			// create a new instance of sub-class using same id
			T newIndividual = materializer.createInstance(individual.getId());

			// copy fields and collections
			materializer.copyFields(individual, newIndividual);
			materializer.copyCollections(individual, newIndividual);

			return newIndividual;
		} catch (MaterializationException e) {
			throw new ClassificationException(e);
		}
	}

	/***
	 * Checks if T is subclass of U
	 * 
	 * @param superClass
	 * @param subClass
	 * @throws ClassificationException
	 */
	private static <U extends OWLThing, T extends OWLThing> void checkSubClass(Class<U> superClass, Class<T> subClass)
			throws ClassificationException {
		boolean isSubClass = false;

		for (Class<?> i : superClass.getInterfaces()) {
			isSubClass |= i.isAssignableFrom(subClass);
		}
		if (!isSubClass) {
			throw new ClassificationException(String.format("%1s is not a subclass of %2s.", subClass.getName(), superClass.getName()));
		}
	}
}
