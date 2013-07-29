package si.um.ii.swrools.generator;

import java.util.*;

import org.semanticweb.owlapi.model.*;

import si.um.ii.swrools.util.NameHelper;

class SimpleEntityGenerator implements IEntityGenerator {

	private OWLOntology owlOntology;

	public SimpleEntityGenerator(OWLOntology ontology) {
		owlOntology = ontology;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see si.um.ii.swrools.generator.IEntityGenerator#createModel()
	 */
	@Override
	public List<Entity> createModel() {
		ArrayList<Entity> javaClasses = new ArrayList<>();

		Set<OWLClass> owlClasses = owlOntology.getClassesInSignature();
		for (OWLClass c : owlClasses) {
			if (c.isTopEntity() || c.isBottomEntity()) {
				continue;
			}

			Entity entity = new Entity();
			entity.setClassName(c.getIRI().getFragment());
			entity.setNamespace(c.getIRI().getStart());
			entity.setAttributes(getAttributes(c));
			entity.setProperties(getProperties(c));

			javaClasses.add(entity);
		}

		Iterator<OWLClass> iOwl = owlClasses.iterator();
		Iterator<Entity> iJava = javaClasses.iterator();

		while (iOwl.hasNext() && iJava.hasNext()) {
			OWLClass c = iOwl.next();
			Entity entity = iJava.next();

			entity.setSubClasses(getSubClasses(c, javaClasses));
			entity.setSuperClasses(getSuperClasses(c, javaClasses));
			entity.setEquivalentWith(getEquivalentClasses(c, javaClasses));
			entity.setDisjointWith(getDisjointClasses(c, javaClasses));
		}

		return javaClasses;
	}

	private Set<Attribute> getAttributes(OWLClass c) {
		Set<Attribute> attributes = new HashSet<>();

		for (OWLClassExpression oce : c.getSuperClasses(owlOntology)) {
			if (!oce.isAnonymous()) {
				attributes.addAll(getAttributes(oce.asOWLClass()));
			}
		}

		Set<OWLDataProperty> dataProperties = owlOntology.getDataPropertiesInSignature();
		for (OWLDataProperty type : dataProperties) {
			Set<OWLClassExpression> domains = type.getDomains(owlOntology);

			for (OWLClassExpression domain : domains) {
				if (!domain.isAnonymous() && domain.asOWLClass().getIRI().equals(c.getIRI())) {
					Attribute att = new Attribute();
					att.setName(type.getIRI().getFragment());
					att.setUri(type.getIRI().toString());

					att.setType("String"); // default value for range
					for (OWLDataRange range : type.getRanges(owlOntology)) {
						if (range.isDatatype() && range.asOWLDatatype().isBuiltIn()) {
							att.setType(NameHelper.convertToType(range.asOWLDatatype().getBuiltInDatatype()));
							break;
						}
					}

					attributes.add(att);
					break;
				}
			}
		}
		return attributes;
	}

	private Set<Property> getProperties(OWLClass c) {
		Set<Property> properties = new HashSet<Property>();

		Set<OWLObjectProperty> owlProperties = owlOntology.getObjectPropertiesInSignature();
		for (OWLObjectProperty p : owlProperties) {
			Set<OWLClassExpression> domains = p.getDomains(owlOntology);
			Set<OWLClassExpression> ranges = p.getRanges(owlOntology);

			Property property = newProperty(c, p.getIRI(), p, domains, ranges, null);
			if (property != null) {
				properties.add(property);
			}

			for (OWLObjectPropertyExpression invpe : p.getInverses(owlOntology)) {
				if (!invpe.isAnonymous()) {
					OWLObjectProperty invp = invpe.asOWLObjectProperty();

					// Changed domain and range
					property = newProperty(c, invp.getIRI(), p, ranges, domains, p.getIRI().toString());
					if (property != null) {
						properties.add(property);
					}
				}
			}
		}
		return properties;
	}

	private Property newProperty(OWLClass c, IRI iri, OWLObjectProperty p, Set<OWLClassExpression> domains, Set<OWLClassExpression> ranges,
			String inverseIri) {
		Property property = new Property();
		property.setName(p.getIRI().getFragment());
		property.setUri(p.getIRI().toString());
		property.setSymetric(p.isSymmetric(owlOntology));
		property.setTransitive(p.isTransitive(owlOntology));
		property.setFunctional(p.isFunctional(owlOntology));
		property.setReflexive(p.isReflexive(owlOntology));
		property.setIrreflexive(p.isIrreflexive(owlOntology));
		property.setAsymmetric(p.isAsymmetric(owlOntology));
		property.setInverseFunctional(p.isInverseFunctional(owlOntology));

		property.setInverse(inverseIri);

		for (OWLObjectPropertyExpression exp : p.getSuperProperties(owlOntology)) {
			if (!exp.isAnonymous()) {
				property.setSuperProperty(exp.asOWLObjectProperty().getIRI().toString());
				break;
			}
		}

		if (newPropertyRecursive(c, p, domains, ranges, property, false, false)) {
			return property;
		}
		return null;
	}

	private boolean newPropertyRecursive(OWLClass c, OWLObjectProperty p, Set<OWLClassExpression> domains, Set<OWLClassExpression> ranges,
			Property property, boolean domainSet, boolean rangeSet) {
		if (!domainSet) {
			for (OWLClassExpression domain : domains) {
				if (!domain.isAnonymous() && domain.asOWLClass().getIRI().equals(c.getIRI())) {
					domainSet = true;
					break;
				}
			}
		}
		if (!domainSet) {
			for (OWLObjectPropertyExpression ope : p.getSuperProperties(owlOntology)) {
				if (!ope.isAnonymous()) {
					OWLObjectProperty superProp = ope.asOWLObjectProperty();
					domainSet = newPropertyRecursive(c, superProp, superProp.getDomains(owlOntology), superProp.getRanges(owlOntology),
							property, domainSet, rangeSet);
				}
			}
		}

		if (!rangeSet) {
			for (OWLClassExpression range : ranges) {
				if (!range.isAnonymous()) {
					property.setType(range.asOWLClass().getIRI().getFragment());
					rangeSet = true;
					break;
				}
			}
		}

		if (!rangeSet) {
			for (OWLObjectPropertyExpression ope : p.getSuperProperties(owlOntology)) {
				if (!ope.isAnonymous()) {
					OWLObjectProperty superProp = ope.asOWLObjectProperty();
					rangeSet = newPropertyRecursive(c, superProp, superProp.getDomains(owlOntology), superProp.getRanges(owlOntology),
							property, domainSet, rangeSet);
				}
			}
		}

		return domainSet && rangeSet;
	}

	private Set<Entity> getSubClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(c.getSubClasses(owlOntology), references);
	}

	private Set<Entity> getDisjointClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(c.getDisjointClasses(owlOntology), references);
	}

	private Set<Entity> getEquivalentClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(c.getEquivalentClasses(owlOntology), references);
	}

	private Set<Entity> getSuperClasses(OWLClass c, Collection<Entity> references) {
		Set<Entity> entitySet = findReferences(c.getSuperClasses(owlOntology), references);

		// Intersections expressed as equivalents with type
		// OWLObjectIntersectionOf
		for (OWLClassExpression equivalents : c.getEquivalentClasses(owlOntology)) {
			if (equivalents instanceof OWLObjectIntersectionOf) {
				entitySet.addAll(findReferences(((OWLObjectIntersectionOf) equivalents).getOperands(), references));
			}
		}
		return entitySet;
	}

	private Set<Entity> findReferences(Set<? extends OWLClassExpression> selectedClasses, Collection<Entity> references) {
		HashSet<Entity> set = new HashSet<>();
		for (OWLClassExpression sc : selectedClasses) {
			if (!sc.isAnonymous()) {
				OWLClass oc = sc.asOWLClass();
				for (Entity e : references) {
					if (e.getUri().equals(oc.getIRI().toString())) {
						set.add(e);
					}
				}
			}
		}
		return set;
	}
}
