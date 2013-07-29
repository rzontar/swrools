package si.um.ii.swrools.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import si.um.ii.swrools.exceptions.GenerationException;
import si.um.ii.swrools.util.NameHelper;

class JavaCodeGenerator implements ICodeGenerator {
	@Override
	public void generateClasses(Iterable<Entity> entities, String path, String namespace) throws GenerationException {
		namespace = namespace + ".gen";
		File basePath = new File(new File(path), namespace.replace('.', '/'));
		File implBasePath = new File(basePath, "impl");
		if (!implBasePath.exists()) {
			implBasePath.mkdirs();
		}

		for (Entity entity : entities) {
			// Interface
			writeInterface(entity, namespace, new File(basePath, entity.getClassName() + ".java"));

			// Implenting class
			writeImplementingClass(entity, namespace, new File(implBasePath, entity.getFullClassName() + "Impl.java"));
		}
	}

	private void writeInterface(Entity entity, String namespace, File file) throws GenerationException {
		try (BufferedLineWriter writer = new BufferedLineWriter(new FileWriter(file))) {
			writePackage(namespace, writer);
			writeImports(namespace, writer);

			writeInterfaceDescription(entity, writer);
			writer.writeLine("{");

			writeInterfaceFieldAccessDeclarations(entity, writer);

			writeInterfacePropertyAccessDeclarations(entity, writer);

			writerInterfaceDisjointDeclarations(entity, writer);

			writer.writeLine("}");

			writer.flush();
		} catch (IOException e) {
			throw new GenerationException(e);
		}
	}

	private void writerInterfaceDisjointDeclarations(Entity entity, BufferedLineWriter writer) throws IOException {
		for (Entity disjointEntity : entity.getDisjointWith()) {
			writer.writeLine(String.format("Class<%s> disjointWith%s();", disjointEntity.getClassName(),
					NameHelper.concatAlphabetically(entity.getClassName(), disjointEntity.getClassName())));
			writer.newLine();
		}
	}

	private void writeInterfacePropertyAccessDeclarations(Entity entity, BufferedLineWriter writer) throws IOException {
		for (Property p : entity.getProperties()) {
			if (p.isSymetric()) {
				writer.writeLine("@OWLSymmetricProperty()");
			}
			if (p.isTransitive()) {
				writer.writeLine("@OWLTransitiveProperty()");
			}
			if (p.isFunctional()) {
				writer.writeLine("@OWLFunctionalProperty()");
			}
			if (p.isInverseFunctional()) {
				writer.writeLine("@OWLInverseFunctionalProperty()");
			}
			if (p.isReflexive()) {
				writer.writeLine("@OWLReflexivelProperty()");
			}
			if (p.isIrreflexive()) {
				writer.writeLine("@OWLIrreflexiveProperty()");
			}
			if (p.isAsymmetric()) {
				writer.writeLine("@OWLAsymmetricProperty()");
			}
			if (p.isInverse()) {
				writer.writeLine(String.format("@OWLInverseOf(id=\"%s\")", p.getInverse()));
			}
			if (p.isSubProperty()) {
				writer.writeLine(String.format("@OWLSubPropertyOf(id=\"%s\")", p.getSuperProperty()));
			}

			writer.writeLine(String.format("@OWLObjectProperty(id=\"%s\")", p.getUri()));
			writer.writeLine(String.format("Collection<%s> get%s();", p.getType(), p.getNameForMethod()));
			writer.newLine();
		}
	}

	private void writeInterfaceFieldAccessDeclarations(Entity entity, BufferedLineWriter writer) throws IOException {
		for (Attribute f : entity.getAttributes()) {
			writer.writeLine(String.format("@OWLDatatypeProperty(id=\"%s\")", f.getUri()));
			writer.writeLine(String.format("%s get%s();", f.getType(), f.getNameForMethod()));
			writer.writeLine(String.format("void set%s(%s value);", f.getNameForMethod(), f.getType()));
			writer.newLine();
		}
	}

	private void writeImplementingClass(Entity entity, String namespace, File file) throws GenerationException {
		try (BufferedLineWriter writer = new BufferedLineWriter(new FileWriter(file))) {
			writePackage(namespace + ".impl", writer);
			writeImports(namespace, writer);

			writeImplClassDescription(entity, writer);
			writer.writeLine("{");

			// write constructor
			writer.writeLine(String.format("public %sImpl(String uri) {", entity.getFullClassName()));
			writer.writeLine("\tsuper(uri);");
			writer.writeLine("}");
			writer.newLine();

			writeFields(entity, writer);
			writeFieldAccessMethods(entity, writer);

			writeProperties(entity, writer);
			writePropertyAccessMethods(entity, writer);

			writeDisjointMethods(entity, writer);

			writer.writeLine("}");

			writer.flush();
		} catch (IOException e) {
			throw new GenerationException(e);
		}
	}

	private void writeDisjointMethods(Entity entity, BufferedLineWriter writer) throws IOException {
		for (Entity superEntity : entity.getSuperClasses()) {
			writeDisjointMethods(superEntity, writer);
		}

		for (Entity disjointEntity : entity.getDisjointWith()) {
			writer.writeLine(String.format("public Class<%s> disjointWith%s() {", disjointEntity.getClassName(),
					NameHelper.concatAlphabetically(entity.getClassName(), disjointEntity.getClassName())));
			writer.writeLine(String.format("\treturn %s.class;", disjointEntity.getClassName()));
			writer.writeLine("}");
			writer.newLine();
		}
	}

	private void writePropertyAccessMethods(Entity entity, BufferedLineWriter writer) throws IOException {
		for (Property p : getProperties(entity)) {
			writer.writeLine(String.format("public Collection<%s> get%s() {", p.getType(), p.getNameForMethod()));
			writer.writeLine(String.format("\treturn this.%s;", p.getNameForField()));
			writer.writeLine("}");
			writer.newLine();
		}
	}

	private void writeProperties(Entity entity, BufferedLineWriter writer) throws IOException {
		// write properties
		for (Property p : getProperties(entity)) {
			writer.writeLine(String.format("private Collection<%s> %s;", p.getType(), p.getNameForField()));
			writer.newLine();
		}
	}

	private Set<Property> getProperties(Entity entity) {
		Set<Property> properties = new HashSet<Property>(entity.getProperties());

		for (Entity superEntity : entity.getSuperClasses()) {
			properties.addAll(getProperties(superEntity));
		}

		return properties;
	}

	private void writeFields(Entity entity, BufferedLineWriter writer) throws IOException {
		// write fields
		for (Attribute f : getFields(entity)) {
			writer.writeLine(String.format("private %s %s;", f.getType(), f.getNameForField()));
			writer.newLine();
		}
	}

	private Set<Attribute> getFields(Entity entity) {
		Set<Attribute> fields = new HashSet<Attribute>(entity.getAttributes());

		for (Entity superEntity : entity.getSuperClasses()) {
			fields.addAll(getFields(superEntity));
		}

		return fields;
	}

	private void writeFieldAccessMethods(Entity entity, BufferedLineWriter writer) throws IOException {
		Set<Attribute> fields = getFields(entity);

		for (Attribute f : fields) {
			writer.writeLine(String.format("public %s get%s() {", f.getType(), f.getNameForMethod()));
			writer.writeLine(String.format("\treturn this.%s;", f.getNameForField()));
			writer.writeLine("}");
			writer.newLine();

			writer.writeLine(String.format("public void set%s(%s value) {", f.getNameForMethod(), f.getType()));
			writer.writeLine(String.format("\tthis.%s = value;", f.getNameForField()));
			writer.writeLine("}");
			writer.newLine();
		}
	}

	private void writeInterfaceDescription(Entity entity, BufferedLineWriter writer) throws IOException {
		writer.writeLine(String.format("@OWLClass(id=\"%s\")", entity.getUri()));

		writeAnnotation("OWLDisjointWith", entity.getDisjointWith(), writer);
		writeAnnotation("OWLEquivalentClass", entity.getEquivalentWith(), writer);

		StringBuilder sb = new StringBuilder();
		for (Entity subClass : entity.getSuperClasses()) {
			sb.append(subClass.getClassName());
			sb.append(", ");
		}

		int l;
		if ((l = sb.length()) == 0) {
			sb.append("OWLThing");
		} else {
			sb.delete(l - 2, l);
		}

		writer.writeLine(String.format("public interface %s extends %s", entity.getClassName(), sb.toString()));
	}

	private void writeAnnotation(String annotationName, Set<Entity> entities, BufferedLineWriter writer) throws IOException {
		if (entities.size() == 0) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		for (Entity entity : entities) {
			sb.append("\"");
			sb.append(entity.getUri());
			sb.append("\", ");
		}

		int l = sb.length();
		sb.delete(l - 2, l);

		writer.writeLine(String.format("@%s(id={%s})", annotationName, sb));
	}

	private void writeImplClassDescription(Entity entity, BufferedLineWriter writer) throws IOException {
		StringBuilder interfaces = new StringBuilder();

		if (entity.getEquivalentWith().isEmpty()) {
			interfaces.append(entity.getClassName());
		} else {
			// sort names to assure correct order
			ArrayList<String> names = new ArrayList<>();
			names.add(entity.getClassName());

			for (Entity e : entity.getEquivalentWith()) {
				names.add(e.getClassName());
			}

			Collections.sort(names);

			for (String s : names) {
				interfaces.append(s);
				interfaces.append(", ");
			}

			int l = interfaces.length();
			interfaces.delete(l - 2, l);
		}

		writer.writeLine(String.format("public class %sImpl extends OWLThingImpl implements %s", entity.getFullClassName(), interfaces));
	}

	private void writeImports(String namespace, BufferedLineWriter writer) throws IOException {
		writer.writeLine("import java.util.*;");
		writer.writeLine("import si.um.ii.swrools.*;");
		writer.writeLine(String.format("import %s.*;", namespace));
		writer.newLine();
	}

	private void writePackage(String namespace, BufferedLineWriter writer) throws IOException {
		writer.writeLine(String.format("package %s;", namespace));
		writer.newLine();
	}
}
