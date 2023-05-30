/** Folder Class 
* Description: Subclass of SaarujanItem, which represents a folder in the file system; 
  folders can only be created or deleted from the server; they cannot be downloaded or uploaded
* constructor() - Sets default initial values, and initializes items
* constructor(String) - Sets default values and name to the given name, and initializes items
* constructor(String, String, Date) - Sets the given path, uploader, upload date, and initializes items
* compare(String, String) - Helper method that compares two Strings alphabetically (including symbols)
* add(Item) - Adds the given item before the Item that has the next alphabetical precedence
* remove(Int) - Removes the item at the given index
* get(Int) - Returns the item at the given index
* search(String, Int, Int) - Helper recursive method for a binary search through the Folder, given a name, a min, and a max
* indexOf(String) - Accessible method that uses search() to search for the given name, in the Folder
* itemCount() - Calculates and returns the count of children in the Folder
* fileCount() - Calculates and returns the count of files in the folder
* folderCount() - Calculates and returns the count of folders in the folder
* backup(Server) - Saves the folder to the server's backup
* load(Server) - Loads the next folder from the server's backup
* sendFormat() - Returns a String containing important metadata of the folder
* size() - Returns the recursive size of all children in the folder
* resetLine() - Resets the current line position to 1, the start of the file
* buildString(Folder, String) - Builds the directory as a String
* toString() - Returns a directory-styled String containing all child items
**/
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.util.ArrayList;

public class SaarujanFolder extends SaarujanItem {
	private static short currentLine = 0; //Static variable that holds the current line that has been read
    private ArrayList<SaarujanItem> items; //ArrayList of children of the Folder
    
    public SaarujanFolder() { 
        super(); //Calling the default parent constructor
        items = new ArrayList<SaarujanItem>(); //Initializing the items ArrayList
    }

    public SaarujanFolder(String name) {
        super(name); //Calling the parent constructor with the given name
        items = new ArrayList<SaarujanItem>(); //Initializing the items ArrayList
    }

    public SaarujanFolder(String path, String uploadedBy, SaarujanDate dateUploaded) {
        super(path, uploadedBy, dateUploaded); //Calling the parent constructor with the given name
        items = new ArrayList<SaarujanItem>(); //Initializing the items ArrayList
    }

    private static boolean compare(String a, String b) {
        byte smallest = (byte) (a.length() > b.length() ? b.length() : a.length()); //Storing the smallest length between a and b
        for (byte i = 0; i < smallest; ++i) { //Looping from 0 to the smallest String length
            if (a.charAt(i) < b.charAt(i))  //If a preceeds b alphabetically
                return true; //True is returned
            else if (a.charAt(i) > b.charAt(i)) //If b preceeds a alphabetically
                return false; //False is returned
        }

        if (a.length() < b.length())  //If both strings are equal upto the given length, and a is shorter than b
            return true; //True is returned
        else 
            return false; //Otherwise, false is returned
    }

    public void add(SaarujanItem toAdd) {
        for (byte i = 0; i < items.size(); ++i) { //Looping through all children of the Folder
            if (compare(toAdd.getName(), items.get(i).getName())) { //If the value to add preceeds the current value alphabetically
                items.add(i, toAdd); //Adding the given value at the i'th index; shifts values to the right if needed
                return; //Exiting the method
            }
        }

        items.add(toAdd); //If the value to add doesn't preceed any of the items, the given value is appended to the end
    }

    public void remove(int index) {
        items.remove(index); //Removing the item at the given index
    }

    public SaarujanItem get(int index) {
        return items.get(index); //Returning the item at the given index
    }

    private int search(String name, int min, int max) { //Helper method for binary sort
        if (min > max) //If the minimum is over the maximum, the name wasn't found in items
            return -1; //Returning -1

        int half = (min + max) / 2; //Calculating and storing the midpoint of items
        if (items.get(half).getName().equals(name)) { //If the given value is equal to the current value
            return half; //The index is returned
        } else if (compare(name, items.get(half).getName())) { //If the given value preceeds the current value
            return search(name, min, half - 1); //Recursively searching the bottom half of the sub-array of items
        } else { //If the current value preceeds the given value
            return search(name, half + 1, max); //Recursively searching the top half of the sub-array of items
        }
    }

    public int indexOf(String name) {
		//Returning the index where the name was found; if the name wasn't found, or items is empty, -1 is returned
        return items.size() == 0 ? -1 : search(name, 0, items.size() - 1); 
    }

    public int itemCount() {
        return items.size(); //Returning the count of children in the Folder
    }

    public int folderCount() {
        short total = 0; //Storing the total count in total
        for (int i = 0; i < items.size(); ++i) { //Looping through all items
            if (items.get(i) instanceof SaarujanFolder) //If the current value is a Folder
                ++total; //Incrementing the total
        }
        
        return total; //Returning the calculated total
    }

    public int fileCount() {
        short total = 0; //Storing the total count in total
        for (int i = 0; i < items.size(); ++i) { //Looping through all items
            if (items.get(i) instanceof SaarujanFile) //If the current value is a File
                ++total; //Incrementing the total
        }

        return total; //Returning the calculated total
    }

    public boolean backup(SaarujanServer server) {
        for (int i = 0; i < items.size(); ++i) { //Looping through all children of the Folder
            items.get(i).backup(server); //Saving the children in the server's backup
        }

        try {
            FileWriter output = new FileWriter(server.getFoldersPath(), true); //Opening the folder backup file
            output.write(String.format("%s|%s|%s\n", server.encrypt(getPath()), server.encrypt(getUploader()), 
									   getDate().toString())); //Writing encrypted data to the backup file
            output.close(); //Closing the file
            return true; //Returning true, as the operation succeeded
        } catch (Exception e) { //If any exception occurs
            server.log("Cannot write to backup file!", true); 
            return false; //Returning false, as the operation failed
        }
    }

    public boolean load(SaarujanServer server) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(server.getFoldersPath())); //Opening the folder backup file
            for (int i = 1; i < currentLine; ++i) { //Looping to the line before the current line
                input.readLine(); //Reading the lines that already have been read
            }

			++currentLine; //Incrementing the current line
            String temp = readToken(input, server); //Reading a token and saving it in temp
            if (temp == null || temp.equals("")) { //If temp is invalid
                input.close(); //Closing the file
                server.log("Backup has no more files!", false); //Logging error message
                return false; //Returning false
            } else {
                if (setPath(temp)) //Setting the path
					setName(temp.substring(temp.lastIndexOf("/") + 1)); //Setting the name, only if the path is valid
			}

            temp = readToken(input, server); //Reading the next token
            if (temp != null) //If the token was read
                setUploader(temp); //Setting the uploader
            else { //If an error occured
                input.close(); //Closing the file
                return false; //Returning false
            }

            temp = readToken(input, server); //Reading the next token
            if (temp != null) //If the token was read 
                setDate(new SaarujanDate(server.encrypt(temp))); //Setting the upload date to the read token
            else {
                input.close(); //Closing the file
                return false; //Returning false
            }

            input.close(); //Closing the file
            return true; //Returning true, as the operation was successful
        } catch (Exception e) { //Catching any errors
            server.log("Cannot read backup file!", true); //Outputting error
            return false; //Returning false
        }
    }

    public int size() {
        int total = 0; //Saving the total size
        for (int i = 0; i < items.size(); ++i) { //Looping through all children of the Folder
            total += items.get(i).size(); //Adding the size of the child to the total
        }
        
        return total; //Returning the calculated total
    }

    public String sendFormat() { //Overriding the abstract sendFormat() method; returns important metadata about the folder
        return String.format("%s|%s|%s|%d|%s|%s", getName(), getUploader(), getDate().toString(), size(), fileCount(), folderCount());
    }
	
    private static String buildString(SaarujanFolder main, String indent) {
        String res = ""; //Storing the resulting string
        for (byte i = 0; i < main.itemCount(); ++i) { //Looping through all children of the Folder
            res += indent + "| " + main.get(i).getName() + "\n"; //Adding the formatted name of the file
            if (main.get(i) instanceof SaarujanFolder) { //If the current item is a Folder
                res += buildString((SaarujanFolder) main.get(i), indent + "   "); //The buildString() of the sub-folder is added
            }
        }

        return res; //The result is returned
    }

	public static void resetLine() {
		currentLine = 1; //Resets the line number back to the starting position
	}
	
    public String toString() { //Returns a directory-styled String containing the files and folders within the Folder
        return getName() + "\n" + buildString(this, "   ");
    }
}
