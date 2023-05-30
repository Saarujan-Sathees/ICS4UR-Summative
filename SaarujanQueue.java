/** Queue Class
* Description: A variation of a normal queue, with a capacity; if it exceeds the capacity, the first value will be dequeued
*
* constructor() - Initializes values to null, and length to 0; the default capacity is 10
* constructor(Byte) - Sets the capacity to the given capacity, and head to null, length to 0
* constructor(Node) - Sets the head to the given node, and calculates the length, using the given Node; sets capacity to 10, as default
* enqueue(String) - Adds the given string to the end of the queue; if the length exceeds the capacity after adding the new value, 
*				    the first value is dequeued and returned
* dequeue() - Removes the first value of the queue and returns it
* remove(String) - Removes the given value from the queue; if it doesn’t exist inside the queue, the method returns false
* length() - Returns the length of the queue
* toString() - Returns the queue as a string, containing every value in the queue
**/

public class SaarujanQueue {
	private SaarujanNode head; //Stores the head Node of the queue
	private byte length, capacity; //Stores the current length of the queue, and the maximum capacity the queue can store

	public SaarujanQueue() {
		head = null; //Sets the head to null
		length = 0; //Sets the length to 0, as the queue is empty
		capacity = 10; //Sets the capacity to a default value of 10
	}

	public SaarujanQueue(byte capacity) {
		head = null; //Sets the head to null
		length = 0; //Sets the length to 0, as the queue is empty
		this.capacity = capacity; //Sets the capacity to the given capacity
	}

	public SaarujanQueue(SaarujanNode node, byte capacity) {
		head = node; //Sets the head to the given Node
		this.capacity = capacity; //Sets the capacity to the given capacity
		length = 0; //Sets the starting length to 0
		while (node != null && length < capacity) { //Loops until the length reaches the capacity, or the current Node is null
			++length; //Incrementing the length
			node = node.next; //Iterating to the next Node in the queue
		}

		if (length == capacity && node != null) //If the length met the capacity and the current node isn't null
			node.next = null; //We end the queue at the given Node
	}

	public String enqueue(String value) {
		if (head == null) { //If the queue is empty
			head = new SaarujanNode(value); //The head is assigned a new Node, with the given value
		} else {
			SaarujanNode n = head; //A temporary Node is initialized with the queue's head
			while (n.next != null) { //Looping until the Node points towards null
				n = n.next; //Iterating to the next Node in the queue
			}
			
			n.next = new SaarujanNode(value); //Pointing the last Node to a new Node, with the given value
		}

		++length; //Incrementing the length by 1
		//If the queue exceeds its capacity, the first value is dequeued and returned; otherwise, null is returned
		return length > capacity ? dequeue() : null; 
	}

	public String dequeue() {
		if (head == null) //If the queue is empty
			return null; //Null is returned, as there is no value to dequeue

		String result = head.value; //Storing the value of the first Node
		head = head.next; //Shifting the queue to the next Node, in order to remove the first Node
		--length; //Decrementing the length
		return result; //Returning the saved result
	}

	public boolean remove(String value) {
		if (head == null) { //If the queue is empty
			return false; //Returning false, as the queue doesn't contain any values
		} else if (head.value == value) { //If the first Node contains the value
			head = head.next; //Shifting the queue to the next Node;
			--length; //Decrementing the length
			return true; //Returning true, as the value was found in the queue
		} else {
			SaarujanNode n = head; //A temporary Node is initialized with the queue's head
			while (n.next != null) { //Iterating through the queue
				if (n.next.value == value) { //If the next Node contains the given value
					n.next = n.next.next; //The Node before the given value is pointed towards the Node after the given value
					--length; //Decrementing the length
					return true; //Returning true, as the operation succeeded
				}
				n = n.next; //Iterating to the next Node in the queue
			}
	
			return false; //Returning false, as the given value wasn't found in the queue
		}
	}

	public byte length() {
		return length;
	}

	public String toString() {
		if (head == null) //If the queue is empty
			return "[  ]"; //A pair of empty brackets is returned
				
		String result = "[ "; //Result is created to store the output, starting with a open bracket
		SaarujanNode n = head; //A temporary Node is initialized with the queue's head
		while (n.next != null) { //Looping until the 2nd last value in the queue
			result += n.value + "\n  "; //Adding the Node's value, and some formatting
			n = n.next; //Iterating to the next Node in the queue
		}

		return result + n.value + " ]\n"; //Returning the result, the last value in the queue, along with a closing bracket
	}

}