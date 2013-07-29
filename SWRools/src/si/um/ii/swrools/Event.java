package si.um.ii.swrools;

/**
 * @author Rok
 * 
 */
class Event {

	public Event(OWLThing object, EventType eventType, ObjectPropertyType propertyType) {
		this.object = object;
		this.eventType = eventType;
		this.propertyType = propertyType;
	}

	private EventType eventType;
	private ObjectPropertyType propertyType;
	private OWLThing object;
	private OWLThing sender;

	/**
	 * @return the eventType
	 */
	public EventType getEventType() {
		return eventType;
	}

	/**
	 * @param eventType
	 *            the eventType to set
	 */
	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	/**
	 * @return the propertyType
	 */
	public ObjectPropertyType getPropertyType() {
		return propertyType;
	}

	/**
	 * @param propertyType
	 *            the propertyType to set
	 */
	public void setPropertyType(ObjectPropertyType propertyType) {
		this.propertyType = propertyType;
	}

	/**
	 * @return the object
	 */
	public OWLThing getObject() {
		return object;
	}

	/**
	 * @param object
	 *            the object to set
	 */
	public void setObject(OWLThing object) {
		this.object = object;
	}

	/**
	 * @return the sender
	 */
	public OWLThing getSender() {
		return sender;
	}

	/**
	 * @param sender
	 *            the sender to set
	 */
	public void setSender(OWLThing sender) {
		this.sender = sender;
	}

	public enum EventType {
		ADD, REMOVE
	}
}
