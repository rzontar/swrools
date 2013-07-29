/**
 * 
 */
package si.um.ii.swrools.generator;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import si.um.ii.swrools.exceptions.OntologyIncoherencyException;

/**
 * @author Rok
 * 
 */
public class GeneratorFactory {

	public static IEntityGenerator createEntityGenerator(OWLOntology ontology) {
		if (ontology == null) {
			throw new IllegalArgumentException("ontology");
		}
		return new SimpleEntityGenerator(ontology);
	}

	public static IEntityGenerator createEntityGenerator(OWLOntology ontology, OWLReasoner reasoner) throws OntologyIncoherencyException {
		if (ontology == null) {
			throw new IllegalArgumentException("ontology");
		}
		if (reasoner == null) {
			throw new IllegalArgumentException("reasoner");
		}

		return new DeductiveEntityGenerator(ontology, reasoner);
	}

	public static ICodeGenerator createCodeGenerator() {
		return new JavaCodeGenerator();
	}
}
