package si.um.ii.swrools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import si.um.ii.swrools.exceptions.MaterializationException;
import si.um.ii.swrools.generator.BufferedLineWriter;
import si.um.ii.swrools.util.NameHelper;

/**
 * @author Rok
 * 
 */
@SuppressWarnings("unchecked")
public class Factory {

	/**
	 * Fields
	 */
	private OWLOntology owlOntology;
	private OWLDataFactory owlDataFactory;
	private ReferenceMap reference;

	/**
	 * Private constructor
	 * 
	 * @param ontology
	 */
	private Factory(OWLOntology ontology) {
		owlOntology = ontology;
		OWLOntologyManager owlOntologyManager = ontology.getOWLOntologyManager();
		owlDataFactory = owlOntologyManager.getOWLDataFactory();
		reference = new ReferenceMap();
	}

	public static Factory getInstance(OWLOntology ontology) {
		return new Factory(ontology);
	}
	
	/***
	 * Create a new instance fo type T
	 * 
	 * @param cls
	 * @param uri
	 * @return
	 * @throws MaterializationException
	 */
	public <T extends OWLThing> T newInstance(Class<T> cls, String uri) throws MaterializationException {
		return new Materializer<T>(cls).createInstance(uri);
	}

	/**
	 * Gets all instances associated to any of the given classes
	 * 
	 * @param <T>
	 * @param classes
	 *            Polje razredov
	 * @return
	 * @throws MaterializationException
	 */
	public <T extends OWLThing> Collection<? extends OWLThing> getAll(Class<T>[] classes) throws MaterializationException {
		Collection<OWLThing> all = new ArrayList<>();

		for (Class<T> c : classes) {
			all.addAll(getAll(c));
		}

		return all;
	}

	/**
	 * Gets all instances associated to class c
	 * 
	 * @param c
	 * @return
	 * @throws MaterializationException
	 */
	public <T extends OWLThing> Collection<T> getAll(Class<T> c) throws MaterializationException {
		return getAll(c, false);
	}

	/**
	 * Gets all instances associated to class c
	 * 
	 * @param <T>
	 * @param c
	 * @param deep
	 * @return
	 * @throws MaterializationException
	 */
	public <T extends OWLThing> Collection<T> getAll(Class<T> c, boolean deep) throws MaterializationException {
		OWLClass attribute = c.getAnnotation(OWLClass.class);

		// Get OWLClass
		org.semanticweb.owlapi.model.OWLClass owlClass = owlDataFactory.getOWLClass(IRI.create(attribute.id()));

		List<T> col = new ArrayList<>();
		for (OWLIndividual i : owlClass.getIndividuals(owlOntology)) {
			if (i.isNamed()) {
				col.add(get(c, i.asOWLNamedIndividual(), deep));
			}
		}
		return col;
	}

	/**
	 * Gets an instances with given uri assosiated to class c
	 * 
	 * @param <T>
	 * @param c
	 * @param uri
	 * @return
	 */
	public <T extends OWLThing> T get(Class<T> c, String uri) throws MaterializationException {
		return get(c, uri, false);
	}

	public <T extends OWLThing> T get(Class<T> c, String uri, boolean deep) throws MaterializationException {
		return get(c, owlDataFactory.getOWLNamedIndividual(IRI.create(uri)), deep);
	}

	private <T extends OWLThing> T get(Class<T> c, OWLNamedIndividual individual, boolean deep) throws MaterializationException {
		Materializer<T> materializer = new Materializer<>(c);

		T instance = null;

		if (reference.containsKey(individual.getIRI())) {
			instance = (T) reference.get(individual.getIRI());
		} else {
			instance = materializer.createInstance(individual.getIRI().toString());
			loadDatatypeProperties(materializer, individual, instance);
			reference.put(instance.getId(), instance);
		}

		if (deep) {
			loadObjectProperties(materializer, individual, instance);
		}

		return instance;
	}

	private <T> List<OWLThing> loadPropertyIndividuals(Class<T> c, Set<OWLIndividual> individuals) throws MaterializationException {
		List<OWLThing> propertyIndividuals = new ArrayList<>();

		for (OWLIndividual pi : individuals) {
			if (pi.isNamed()) {
				OWLNamedIndividual namedPI = pi.asOWLNamedIndividual();
				if (reference.containsKey(namedPI.getIRI())) {
					propertyIndividuals.add(reference.get(namedPI.getIRI()));
					continue;
				} else {
					for (OWLClassExpression piType : pi.getTypes(owlOntology)) {
						if (!piType.isAnonymous()) {
							org.semanticweb.owlapi.model.OWLClass piClass = piType.asOWLClass();

							String className = NameHelper.getInterfaceClassName(c.getPackage().getName(), piClass.getIRI().getFragment());
							Materializer<?> materializerForPropertyType = new Materializer<>(className);
							OWLThing propertyInstance = materializerForPropertyType.createInstance(namedPI.getIRI().toString());
							loadDatatypeProperties(materializerForPropertyType, namedPI, propertyInstance);
							loadObjectProperties(materializerForPropertyType, namedPI, propertyInstance);

							propertyIndividuals.add(propertyInstance);
							reference.put(propertyInstance.getId(), propertyInstance);
						}
					}
				}
			}
		}
		return propertyIndividuals;
	}

	private void loadObjectProperties(Materializer<?> materializer, OWLNamedIndividual namedIndividual, OWLThing instance)
			throws MaterializationException {
		Collection<String> properties = materializer.getObjectProperties();
		for (String objPropId : properties) {
			OWLObjectProperty owlObjectProperty = owlDataFactory.getOWLObjectProperty(IRI.create(objPropId));
			Set<OWLIndividual> propIndividuals = namedIndividual.getObjectPropertyValues(owlObjectProperty, owlOntology);
			Class<?> targetCls = materializer.getReturnType(objPropId);
			List<OWLThing> individuals = loadPropertyIndividuals(targetCls, propIndividuals);
			materializer.setProperty(objPropId, instance, individuals, targetCls);
		}
	}

	private void loadDatatypeProperties(Materializer<?> materializer, OWLNamedIndividual individual, OWLThing instance)
			throws MaterializationException {
		Set<OWLDataPropertyAssertionAxiom> dataProperties = owlOntology.getDataPropertyAssertionAxioms(individual);
		for (OWLDataPropertyAssertionAxiom a : dataProperties) {
			materializer.setValue(instance, a.getProperty().asOWLDataProperty().getIRI().toString(), a.getObject().getLiteral());
		}
	}

	/***
	 * Saves instances to RDF
	 * 
	 * @param instances
	 * @param fileName
	 * @throws MaterializationException
	 */
	@Deprecated
	public void save(Collection<? extends OWLThing> instances, String fileName) throws MaterializationException, IOException {
		HashMap<Class<?>, Collection<OWLThing>> map = new HashMap<>();

		for (OWLThing i : instances) {
			Class<?> cls = i.getClass();
			if (map.containsKey(cls)) {
				map.get(cls).add(i);
			} else {
				List<OWLThing> list = new ArrayList<>();
				list.add(i);
				map.put(cls, list);
			}
		}

		try (BufferedLineWriter writer = new BufferedLineWriter(new FileWriter(fileName))) {
			writer.writeLine("<rdf:RDF");
			writer.writeLine("\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
			writer.writeLine("\txmlns:owl=\"http://www.w3.org/2002/07/owl#\"");
			writer.writeLine("\txmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
			writer.writeLine("\txmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
			writer.writeLine(" > ");

			for (Entry<Class<?>, Collection<OWLThing>> entry : map.entrySet()) {
				Class<?> interfaceClass = entry.getKey().getInterfaces()[0];

				Materializer m = new Materializer(interfaceClass); // Use raw type

				for (OWLThing instance : entry.getValue()) {
					OWLClass owlClass = interfaceClass.getAnnotation(OWLClass.class);

					writer.writeLine(String.format("<rdf:Description rdf:about=\"%s\">", instance.getId()));
					writer.writeLine(String.format("<rdf:type rdf:resource=\"%s\"/>", owlClass.id()));

					Collection<Entry<String, Object>> dataTypeValues = m.getDatatypeValues(instance);
					for (Entry<String, Object> dataEntry : dataTypeValues) { // Write
						String dataName = NameHelper.getNameFromUri(dataEntry.getKey());
						writer.writeLine(String.format("<n0:%s>%s</n0:%s>", dataName, dataEntry.getValue(), dataName));
					}

					writer.writeLine("</rdf:Description>");
					writer.newLine();
				}
			}

			writer.writeLine("</rdf:RDF>");
			writer.flush();
		}
	}

	public void clearReference() {
		reference.clear();
	}
}
