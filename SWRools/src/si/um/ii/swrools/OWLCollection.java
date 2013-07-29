package si.um.ii.swrools;

import java.util.*;

/**
 * @author Rok
 * @param <T>
 * 
 */
@SuppressWarnings("unchecked")
class OWLCollection<T extends OWLThing> extends HashSet<T> implements Observer {

	/**
	 * Generated serial
	 */
	private static final long serialVersionUID = 4741396055375710665L;

	/**
	 * Fields
	 */
	private String name;
	private boolean symmetric;
	private boolean transitive;
	private boolean inverse;
	private boolean functional;
	private boolean reflexive;
	private boolean irreflexive;
	private boolean subProperty;

	private OWLThing parent;
	private OWLCollectionObservable observable = new OWLCollectionObservable();

	/**
	 * 
	 */
	public OWLCollection(String name, OWLThing parent) {
		this.name = name;
		this.parent = parent;

		if (reflexive) {
			add((T) parent);
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the symmetric
	 */
	public boolean isSymmetric() {
		return symmetric;
	}

	/**
	 * @param symmetric
	 *            the symmetric to set
	 */
	public void setSymmetric(boolean symmetric) {
		this.symmetric = symmetric;
	}

	/**
	 * @return the transitive
	 */
	public boolean isTransitive() {
		return transitive;
	}

	/**
	 * @param transitive
	 *            the transitive to set
	 */
	public void setTransitive(boolean transitive) {
		this.transitive = transitive;
	}

	/**
	 * @return the inverse
	 */
	public boolean isInverse() {
		return inverse;
	}

	/**
	 * @param inverse
	 *            the inverse to set
	 */
	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	/**
	 * @return the functional
	 */
	public boolean isFunctional() {
		return functional;
	}

	/**
	 * @param functional
	 *            the functional to set
	 */
	public void setFunctional(boolean functional) {
		this.functional = functional;
	}

	/**
	 * @return the reflexive
	 */
	public boolean isReflexive() {
		return reflexive;
	}

	/**
	 * @param reflexive
	 *            the reflexive to set
	 */
	public void setReflexive(boolean reflexive) {
		this.reflexive = reflexive;
	}

	/**
	 * @return the irreflexive
	 */
	public boolean isIrreflexive() {
		return irreflexive;
	}

	/**
	 * @param irreflexive
	 *            the irreflexive to set
	 */
	public void setIrreflexive(boolean irreflexive) {
		this.irreflexive = irreflexive;
	}

	/**
	 * @return the subProperty
	 */
	public boolean isSubProperty() {
		return subProperty;
	}

	/**
	 * @param subProperty
	 *            the subProperty to set
	 */
	public void setSubProperty(boolean subProperty) {
		this.subProperty = subProperty;
	}

	@Override
	public boolean add(T e) {
		if (contains(e)) {
			return false;
		}

		if ((functional && size() >= 1) || (isIrreflexive() && parent == e)) {
			return false;
		}

		boolean b = super.add(e);

		if (subProperty) {
			observable.setChanged();
			observable.notifyObservers(new Event(e, Event.EventType.ADD, ObjectPropertyType.SUBPROPERTY));
		}
		if (symmetric) {
			Materializer.addObserver(this, e, ObjectPropertyType.SYMMETRIC);

			observable.setChanged();
			observable.notifyObservers(new Event(parent, Event.EventType.ADD, ObjectPropertyType.SYMMETRIC));
		}
		if (transitive) {
			Materializer.addObserver(this, e, ObjectPropertyType.TRANSITIVE);

			observable.setChanged();
			observable.notifyObservers(new Event(parent, Event.EventType.ADD, ObjectPropertyType.TRANSITIVE));
		}
		if (inverse) {
			Materializer.addObserver(this, e, ObjectPropertyType.INVERSE);

			observable.setChanged();
			observable.notifyObservers(new Event(parent, Event.EventType.ADD, ObjectPropertyType.INVERSE));
		}

		return b;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean b = false;

		Iterator<? extends T> iterator = c.iterator();
		while (iterator.hasNext()) {
			b |= add(iterator.next());
		}

		return b;
	}

	@Override
	public boolean remove(Object o) {
		if (!contains(o) || (reflexive && parent == o)) {
			return false;
		}

		boolean b = super.remove(o);

		if (subProperty) {
			observable.setChanged();
			observable.notifyObservers(new Event((OWLThing) o, Event.EventType.REMOVE, ObjectPropertyType.SUBPROPERTY));
		}
		if (symmetric) {
			observable.setChanged();
			observable.notifyObservers(new Event(parent, Event.EventType.REMOVE, ObjectPropertyType.SYMMETRIC));
		}
		if (transitive) {
			observable.setChanged();
			observable.notifyObservers(new Event(parent, Event.EventType.REMOVE, ObjectPropertyType.TRANSITIVE));
		}
		if (inverse) {
			observable.setChanged();
			observable.notifyObservers(new Event(parent, Event.EventType.REMOVE, ObjectPropertyType.INVERSE));
		}

		return b;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean b = false;

		Iterator<?> iterator = c.iterator();
		while (iterator.hasNext()) {
			b |= remove(iterator.next());
		}

		return b;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(Observable paramObservable, Object paramObject) {
		if (!(paramObject instanceof Event)) {
			throw new IllegalArgumentException("paramObject");
		}

		Event e = (Event) paramObject;

		switch (e.getPropertyType()) {
			case SUBPROPERTY:
				switch (e.getEventType()) {
					case ADD:
						this.add((T) e.getObject());
						break;
					case REMOVE:
						this.remove(e.getObject());
						break;
				}
				break;
			case SYMMETRIC:
			case INVERSE:
			case TRANSITIVE:
				switch (e.getEventType()) {
					case ADD:
						super.add((T) e.getObject());
						break;
					case REMOVE:
						super.remove(e.getObject());
						break;
				}
				break;
		}
	}

	public void addObserver(Observer observer) {
		observable.addObserver(observer);
	}
}
