package si.um.ii.swrools.generator;

import java.util.*;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import si.um.ii.swrools.exceptions.OntologyIncoherencyException;
import si.um.ii.swrools.util.NameHelper;

class DeductiveEntityGenerator implements IEntityGenerator {

	private OWLOntology owlOntology;
	private OWLReasoner reasoner;

	public DeductiveEntityGenerator(OWLOntology ontology, OWLReasoner reasoner) throws OntologyIncoherencyException {
		owlOntology = ontology;
		this.reasoner = reasoner;

		checkForIncoherence();

		removeIndividuals();
	}

	private void removeIndividuals() {
		Set<OWLNamedIndividual> individuals = owlOntology.getIndividualsInSignature();
		if (individuals.size() == 0) {
			return;
		}

		OWLOntologyManager manager = owlOntology.getOWLOntologyManager();
		OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(owlOntology));
		for (OWLNamedIndividual i : individuals) {
			i.accept(remover);
		}
		manager.applyChanges(remover.getChanges());
	}

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
		Set<Attribute> attributes = new HashSet<Attribute>();

		for (OWLClassExpression oce : c.getSuperClasses(owlOntology)) {
			if (!oce.isAnonymous()) {
				attributes.addAll(getAttributes(oce.asOWLClass()));
			}
		}

		Set<OWLDataProperty> dataProperties = owlOntology.getDataPropertiesInSignature();
		for (OWLDataProperty dataProp : dataProperties) {
			NodeSet<OWLClass> domains = reasoner.getDataPropertyDomains(dataProp, true);

			for (Node<OWLClass> domainNode : domains) {
				OWLClass domain = domainNode.getRepresentativeElement();
				if (domainNode.equals(reasoner.getTopClassNode()) || domain.getIRI().equals(c.getIRI())) {
					Attribute att = new Attribute();
					att.setName(dataProp.getIRI().getFragment());
					att.setUri(dataProp.getIRI().toString());

					for (OWLDataRange range : dataProp.getRanges(owlOntology)) {
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
		Set<Property> properties = new HashSet<>();

		Set<OWLObjectProperty> owlProperties = owlOntology.getObjectPropertiesInSignature();
		for (OWLObjectProperty p : owlProperties) {

			NodeSet<OWLClass> domains = reasoner.getObjectPropertyDomains(p, true);
			NodeSet<OWLClass> ranges = reasoner.getObjectPropertyRanges(p, true);

			Property property = newProperty(c, p, domains, ranges);
			if (property != null) {
				properties.add(property);
			}
		}
		return properties;
	}

	private Property newProperty(OWLClass c, OWLObjectProperty p, NodeSet<OWLClass> domains, NodeSet<OWLClass> ranges) {
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

		OWLObjectPropertyExpression element = reasoner.getInverseObjectProperties(p).getRepresentativeElement();
		if (element != null && !element.isAnonymous()) {
			property.setInverse(element.asOWLObjectProperty().getIRI().toString());
		}

		for (Node<OWLObjectPropertyExpression> node : reasoner.getSuperObjectProperties(p, true)) {
			if (node.equals(reasoner.getTopObjectPropertyNode())) {
				continue;
			}

			for (OWLObjectPropertyExpression ope : node.getEntitiesMinusBottom()) {
				if (!ope.isAnonymous()) {
					property.setSuperProperty(ope.asOWLObjectProperty().getIRI().toString());
					break;
				}
			}
		}

		for (Node<OWLClass> node : domains) {
			for (OWLClass domain : node.getEntitiesMinusTop()) {
				if (domain.getIRI().equals(c.getIRI())) {
					for (Node<OWLClass> node2 : ranges) {
						for (OWLClass range : node2.getEntitiesMinusTop()) {
							property.setType(range.getIRI().getFragment());
							return property;
						}
					}
				}
			}
		}

		return null;
	}

	private Set<Entity> getSubClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(reasoner.getSubClasses(c, true), references);
	}

	private Set<Entity> getDisjointClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(reasoner.getDisjointClasses(c), references);
	}

	private Set<Entity> getEquivalentClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(reasoner.getEquivalentClasses(c), c, references);
	}

	private Set<Entity> getSuperClasses(OWLClass c, Collection<Entity> references) {
		return findReferences(reasoner.getSuperClasses(c, true), references);
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

	private Set<Entity> findReferences(Node<OWLClass> node, OWLClass c, Collection<Entity> references) {
		return findReferences(node.getEntitiesMinus(c), references);
	}

	private Set<Entity> findReferences(NodeSet<OWLClass> nodes, Collection<Entity> references) {
		HashSet<Entity> set = new HashSet<>();
		for (Node<OWLClass> node : nodes) {
			set.addAll(findReferences(node.getEntitiesMinusBottom(), references));
		}
		return set;
	}

	private void checkForIncoherence() throws OntologyIncoherencyException {
		Node<OWLClass> unsatisfiableClasses = reasoner.getUnsatisfiableClasses();

		if (unsatisfiableClasses.getEntitiesMinusBottom().size() > 0) {
			throw new OntologyIncoherencyException();
		}
	}

}
