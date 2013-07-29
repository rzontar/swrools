/**
 * 
 */
package si.um.ii.swrools.generator;

import si.um.ii.swrools.exceptions.GenerationException;

/**
 * @author Rok
 * 
 */
public interface ICodeGenerator {

	void generateClasses(Iterable<Entity> entities, String path, String namespace) throws GenerationException;

}
