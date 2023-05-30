/** File Class
* Description: Subclass of Item, which represents a file in the file system;
  files can be uploaded or deleted from the server, but cannot be edited.
* constructor() - Sets default initial values, and backupLine to -1 (inexistant)
* constructor(String) - Sets name to given name, and sets the other values to default values
* constructor(String, String, Date, String) - Sets the values of the path, name, uploader, upload date, data, and the size
* getData(Server) - Returns the data of the file, even if it only exists in the backup file
* setData(String) - Sets the data and size to the given data, only if the file doesn't contain data already
* backup(Server) - Saves the file to the server's backup
* load(Server) - Loads data from the current file in the server's backup
* sendFormat() - Returns a String to send to the client, when they navigate the file system
* size() - Returns the size of the file, in kilobytes (KB)
* resetLine() - Resets the current line to 1, the start of the file
* toString() - Returns the name of the file, with formatting
**/
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SaarujanFile extends SaarujanItem {
	private int backupLine, size; //backupLine stores the line number where the file is, in the backup; size stores the size in KB
	private static int currentLine = 1; //
	private String data; //Stores the data, if it is small enough, and if it was recently accessed

	public SaarujanFile() {
		super(); //Calls the parent constructor
		size = 0; //Sets the size to 0
		backupLine = -1; //Sets the backup line to -1
		data = null; //Sets the data to null
	}

	public SaarujanFile(String name) {
		super(name); //Calls the parent constructor with the given name
		size = 0; //Sets the size to 0
		backupLine = -1; //Sets the backup line to -1
		data = null; //Sets the data to null
	}

	public SaarujanFile(String path, String uploader, SaarujanDate uploadDate, String data) {
		super(path, uploader, uploadDate); //Calls the parent constructor with the given vallues
		this.data = data; //Sets the data to the given data
		size = data.getBytes().length / 1000; //Calculates the size of the file
	}
	
	public String getData(SaarujanServer server) {
		if (data != null) { //If the data is already stored in memory
			return data; //The data is returned
		} else if (backupLine != -1) { //If there is a backup line (meaning the file is stored in the backup file)
			try {
				BufferedReader input = new BufferedReader(new FileReader(server.getBackupPath())); //Opening the backup file
				for (int i = 1; i < backupLine; ++i) { //Looping until the line before the backup line
					input.readLine(); //Reading unneccessary lines
				}

				for (byte i = 0; i < 4; ++i) { //Looping 4 times to read the unneccessary path, uploader, upload date, and size
					if (readToken(input, server) == null) { //If an error occured while reading the information
						input.close(); //The input is closed
						return null; //Null is returned
					}
				}
				
				return server.decrypt(input.readLine()); //Returns the decrypted version of the found data
			} catch (FileNotFoundException e) { //If the backup file doesn't exist
				server.log(server.getBackupPath() + " cannot be found!", true);
			} catch (IOException e) { //If an error occurs while writing to the backup file
				server.log("Error while writing to file backup!", true);
			}
		}
		return null; //Returns null if an error occured, or if no data exists in the first place
	}

	public void setData(String data) { 
		this.data = data; //The data is set to the given data
		if (data != null) { //If the data isn't cleared from memory
			size = data.getBytes().length / 1000; //The size is calculated
		}
	}

	public boolean backup(SaarujanServer server) {
		if (backupLine == -1 && data == null) //If no data is stored in this file
			return false; //False is returned
		
		try {
			FileWriter output = new FileWriter(server.getNextBackupPath(), true); //Opening the new backup path and saving the file
			output.write(String.format("%s|%s|%s|%s|%s\n", server.encrypt(size + ""), server.encrypt(getPath()),
									   server.encrypt(getUploader()), getDate().toString(), server.encrypt(getData(server))));
			backupLine = currentLine++; //Saving the backup line number
			output.close(); //Closing the output stream
			return true;
		} catch (FileNotFoundException e) { //If the file wasn't found
			server.log(server.getNextBackupPath() + " cannot be found!", true);
			return false; //False is returned
		} catch (IOException e) { //If an error occured while writing to the file
			server.log(server.getNextBackupPath() + " cannot be found!", true); 
			return false; //False is returned
		}
	}

	public boolean load(SaarujanServer server) {
		try {
			BufferedReader input = new BufferedReader(new FileReader(server.getBackupPath()));
			if (backupLine == -1) //If there is no backup line
                backupLine = currentLine++; //The backup line is set to the current line, and the current line number is incremented
            
            for (int i = 1; i < backupLine; ++i) { //Looping to the line before the backup line
                input.readLine(); //Reading unneccessary lines
            } 
            
            String temp = readToken(input, server); //Storing the read token in temp
            if (temp == null || temp.equals("")) { //If temp is null (error has occured) or if temp is empty
                input.close(); //Closing the input
                return false; //Returning false
            } else { 
                size = strToInt(temp); //Converting and storing the token as size
            } 

            temp = readToken(input, server);
            if (temp == null || temp.equals("")) { //If temp is null (error has occured) or if temp is empty
                input.close(); //Closing the input
                return false; //Returning false
            } else { 
                if (setPath(temp)) //Storing the path
					setName(temp.substring(temp.lastIndexOf("/") + 1)); //Storing the name, only if the path is valid
            } 

			temp = readToken(input, server);
            if (temp == null || temp.equals("")) { //If temp is null (error has occured) or if temp is empty
                input.close(); //Closing the input
                return false; //Returning false
            } else { 
                setUploader(temp); //Storing the uploader
            } 

			temp = readToken(input, server);
            if (temp == null || temp.equals("")) { //If temp is null (error has occured) or if temp is empty
                input.close(); //Closing the input
                return false; //Returning false
            } else { 
                setDate(new SaarujanDate(server.encrypt(temp))); //Storing the upload date
            } 

			data = server.decrypt(input.readLine()); //Stores the remaining data
			input.close(); //Closing the input
			return true; //Returning true, as the operation succeeded
		} catch (FileNotFoundException e) { //If the backup file wasn't found
			server.log(server.getBackupPath() + " not found!", true);
		} catch (IOException e) { //If an error occured while reading from the backup file
			server.log("Error while reading to backup file!", true);
		}
		
		return false; //Returning false if the operation failed
	}
	
	public String sendFormat() { 
		//Returns a String in the format name|uploader|date|size
		return getName() + "|" + getUploader() + "|" + getDate() + "|" + size;
	}
	
	public int size() {
		return size; //Returns the file size
	}

	public static void resetLine() {
		currentLine = 1; //Resets the current line to the starting line position
	}
	
	public String toString() {
		return getName() + "\n"; //Returns the name, and a newline character
	}
}