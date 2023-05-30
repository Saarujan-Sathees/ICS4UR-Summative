/** Item Class
* Description: An abstract class that holds general values and methods that both the Folder and File class use
* constructor() - Sets values to empty and null values; sets upload date to the current date
* constructor(String) - Sets the name to the given name, and every other variable is set to default values
* constructor(String, String, Date) - Sets the given values; takes the end of the given path, and assigns it to the name
* getName() - Returns the name
* getPath() - Returns the path
* getUploader() - Returns the uploader
* getDate() - Returns the upload date
* setName(String) - Sets the name to the given value if it only contains valid characters
* setPath(String) - Sets the path to the given value, after modifying it if it’s invalid
* setUploader(String) - Sets the uploader to the given value
* getDate() - Returns the upload date
* setDate(Date) - Sets the upload date
* readToken(BufferedReader, Server) - Helper method that reads a token from a backup file
* abstract backup(Server) - Abstract method that saves the Item to the Server's backup
* abstract load(Server) - Abstract method that loads the next Item from the Server's backup
* abstract sendFormat() - Abstract method that returns a String containing the metadata of the Item
* abstract size() - Abstract method that returns the size of the Item
**/
import java.io.BufferedReader;

public abstract class SaarujanItem {
	private String name, path, uploader; //Stores the name, the path, and the uploader (username) of the Item
	private SaarujanDate uploadDate; //Stores the upload date of the Item

	public SaarujanItem() {
		name = null; //Sets name to null
		path = null; //Sets path to null
		uploader = null; //Sets uploader to null
		uploadDate = SaarujanDate.currentDate(); //Sets the upload date to the current date
	}

	public SaarujanItem(String name) {
		setName(name); //Calls the setter of name to set the given name, as it must be verified
		path = null; //Sets the path to null
		uploader = null; //Sets uploader to null
		uploadDate = SaarujanDate.currentDate(); //Sets the upload date to the current date
	}

	public SaarujanItem(String path, String uploader, SaarujanDate uploadDate) {
		if (setPath(path)) //Calls the setter of path to validate the path
			setName(path.substring(path.lastIndexOf("/") + 1)); //Calls the setter of name to validate the name, if the path is valid
		
		this.uploader = uploader; //Sets uploader to the given username
		this.uploadDate = uploadDate; //Sets the upload date to the given Date
	}

	public String getName() {
		return name; //Returning the name
	}

	public boolean setName(String n) {
		if (n.length() > 20) //If the name is over 20 characters long, the method exits
			return false;

		byte periodCount = 0; //Stores the count of periods in the filename; it should only contain 1, for the extension (image.png)
		for (byte i = 0; i < n.length(); ++i) { //Iterating through the given name
			switch (n.charAt(i)) { 
				case '.': ++periodCount; break; //If the character is a period, the period count is incremented
				case '/': //Invalid character cascades to return false
				case '\\': //Invalid character cascades to return false
				case '|': //Invalid character cascades to return false
				case ':': return false; //All invalid characters make the method exit without setting the name
			}
		}

		if (periodCount < 2) { //If there is only 1 or no period (which is valid)
			name = n; //Setting the name to the given name
			return true; //Returning true
		} 

		return false; //Returning false, as there were too many periods (or none)
	}

	public String getPath() {
		return path; //Returns the path
	}

	public boolean setPath(String p) {
		if (p.length() > 128 || p.indexOf("://") == -1 || p.indexOf("://") != p.lastIndexOf("://"))
			return false; //If the path doesn't contain, or contains more than 1 root directory specified, the method exits
		
		path = p; //Sets the path to the given path
		return true; //Returns true, as the operation succeeded
	}

	public String getUploader() {
		return uploader; //Returns the uploader of the Item
	}

	public void setUploader(String user) {
		uploader = user; //Sets the uploader to the given username; no verification is needed, as the client must have an account
	}

	public SaarujanDate getDate() {
		return uploadDate; //Returns the upload date
	}

	public void setDate(SaarujanDate d) {
		uploadDate = d; //Sets the upload date; no verification is needed
	}

	protected static int strToInt(String s) {
		int result = 0, multiplier = 1; //Storing the result in result, and using multiplier to place digits in the right position
		for (byte i = (byte) (s.length() - 1); i >= 0; --i) { //Looping from the first position (ones) to the last position
			result += (s.charAt(i) - '0') * multiplier; //Adding the current digit to the result
			multiplier *= 10; //Moving to the next position
		}

		return result;
	}
	
	protected static String readToken(BufferedReader input, SaarujanServer server) {
		try {
			String result = ""; //Initializing a String to store the token
			int curr = input.read(); //Storing the read value in curr
			while (curr != -1 && (char) curr != '|') { //Loops until the end of the stream, or the delimeter '|' is reached
				result += (char) curr; //Adding the char value of the read value to the result
				curr = input.read(); //Reading another value from the stream
			}

			return server.decrypt(result); //Returning the decrypted token
		} catch (Exception e) { //If any error occurs (specifically IOException since FileNotFound is already taken care of)
			server.log("Error while reading token from backup file!", true); 
			return null; //Returning null
		}
	}
	
	public abstract boolean backup(SaarujanServer server); //Abstract backup method
	public abstract boolean load(SaarujanServer server); //Abstract load method
	public abstract String sendFormat(); //Abstract format method to send to client
	public abstract int size(); //Abstract size method
}