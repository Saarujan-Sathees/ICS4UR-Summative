/** Node Class
* Description: A simple class that will be used to represent a Node in a Queue
* constructor() - Initializes all values to null
* constructor(String) - Initializes the cargo of the Node to the given value, and sets next to null
* constructor(String, Node) - Initializes the cargo and the next pointer of the Node to the given values
* toString() - Returns the value of the Node, which can also be null
**/

public class SaarujanNode {
	public String value; //Stores the cargo of the Node
	public SaarujanNode next; //Stores the pointer towards the next Node

	public SaarujanNode() {
		value = null; //Setting the cargo to null
		next = null; //Pointing this Node towards null
	}

	public SaarujanNode(String value) {
		this.value = value; //Setting the cargo to the given value
		this.next = null; //Pointing this Node towards null
	}

	public SaarujanNode(String value, SaarujanNode next) {
		this.value = value; //Setting the cargo to the given value
		this.next = next; //Pointing this Node towards the given Node
	}

	public String toString() {
		return value; //Returning the value; we do not need to call a toString() of the cargo, as it's predetermined to be a String
	}
}