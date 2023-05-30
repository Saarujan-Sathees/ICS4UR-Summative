/** Server Class
* Description: The server class that handles connections of clients and stores the file system. 
  Can only handle one connection at a time, since asynchronous code is difficult to do in a short period of time
* constructor(String, String) - If a server folder already exists with the given name, then the server is initialized with that folder;
								if not, new files and default values are used
* log(String, Boolean) - Logs the given message onto the server's log file
* generateKey(Int) - Helper method that generates a 16-digit encryption key, as a string, using the given seed
* encrypt(String) - Encrypts the given string using the server’s key
* decrypt(String) - Decrypts the given string using the server’s key
* strToInt(String) - Helper method that converts a string to an integer
* navigateFolder(Folder, String, Int) - Recursive method that returns the parent folder of the given path
* loadAccounts() - Helper methods that loads and decrypts the accounts from a user file, into the ArrayList
* loadFS() - Helper method that loads all folders and files from a backup file, into the root folder on the server
* getBackupPath() - Returns the current backup path of the server
* getNextBackupPath() - Returns the next backup path of the server
* getFoldersPath() - Returns the folder backup path of the server
* getMaxSize() - Returns the maximum size of files that can be stored in memory
* backup() - Saves all of the items in the root folder to backup files
* send(String) - Sends a message to the client
* recv() - Receives and returns a message from the client
* handleConnection() - Handles the login / logout to the server, and every action that the client can take
* loginAccount() - Handles the login of a client to the server
* createAccount() - Handles the creation of an account by the client
* permitAccount(String) - Handles the modification of an account’s permission by the owner
* sendRecentLogs() - Handles the sending of recent logs
* createFolder(String) - Handles the creation of a folder by the client
* uploadFile(String) - Handles the upload of a file by the client
* downloadFile(String) - Handles the download of a file by the client
* deleteItem(String) - Handles the deletion of a folder or file by the client
* sendNavigation() - Handles the navigation of the file system by the client
* start() - Starts the server
**/
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;

public class SaarujanServer {
	private String name, key; //Variables to store the name of the server, and the encryption key
	private ArrayList<String[]> accounts; //ArrayList to store account information while the server is running
	private final int MAX_SIZE = 50000; //A constant to hold the maximum file size
	private SaarujanFolder root; //The root directory (folder) of the server's file system
	private SaarujanQueue inMemory; //A queue that contains the file-paths of files that should remain in memory
	private ServerSocket main; //The main socket that users will connect to
	private Socket connection; //The sub-socket that enables a two-way connection between server and client
	private InputStream sockIn; //The input stream from the client
	private OutputStream sockOut; //The output stream to the client
	private boolean backupNumber; //The current backup number (swaps between 1 and 0 to maximize effiency of backing up files)
	
	public void log(String message, boolean isWarning) {
		try {
			FileWriter logFile = new FileWriter(name + "/logs.txt", true); //Opens the log file, and writes a formatted message
			logFile.write(encrypt(SaarujanDate.currentDate() + " " + SaarujanDate.currentTime() + "\t" +
						  (isWarning ? "Warning: " : "Log: ") + message) + "\n");
			logFile.close(); //Closes the log file
		} catch (Exception e) { //If any exception occurs
			System.out.println("Cannot write to log file!"); //A message is outputted to the console, as it cannot write to logs
		}
	}

	private static String generateKey(int seed) {
		String result = ""; //String to contain the resulting key
		Random eng = new Random(seed); //Creates a new random engine using the given seed
		for (byte i = 0; i < 16; ++i) { //Loops 16 times for 16 digits; the key is a string, as it has a 100% chance of having 16 digits
			result += eng.nextInt(10); //Adding the next digit (0-9) to the result
		}

		return result; //Returning the result
	}
	
	private static SaarujanFolder navigateFolder(SaarujanFolder folder, String[] path, int index) {
        if (index == path.length - 1) { //If the parent folder of the item that is specified in the path is reached
            return folder; //The parent folder is returned
        }
        
        int i = folder.indexOf(path[index]); //Stores the index of the current part of the path in the parent folder
        if (i == -1) { //If the current part of the path doesn't exist in the folder
			//A new folder under that path and name is created; some values are omitted now as they're unknown; they will be set later
            SaarujanFolder a = new SaarujanFolder(folder.getPath() + path[index], null, null); 
            folder.add(a); //The folder is added to the parent folder
            return navigateFolder(a, path, ++index); //Recursing through the new folder, and moving onto the next part of the path
        } else { //If it exists already, it recurses through the found folder, and moves onto the next part of the path
            return navigateFolder((SaarujanFolder) folder.get(i), path, ++index);
        }
    }

	public String encrypt(String value) { 
		if (value == null || value.length() == 0) //If the value is null, or is empty, the method returns an empty string
			return "";

		String encrypted = ""; //Variable that holds the end result, encrypted
		for (int i = 0; i < value.length(); ++i) { //Loops through the entire given String
            encrypted += (char) (value.charAt(i) - (key.charAt(i % key.length()) - '0')); //Encrypts the current char with the key 
		}

        return encrypted; //Returns the encrypted string
	}

	public String decrypt(String value) {
		if (value == null || value.length() == 0) //If the value is null, or is empty, the method returns an empty string
			return "";

		String decrypted = ""; //Variable that holds the end result, decrypted
		for (int i = 0; i < value.length(); ++i) { //Loops through the entire given String
			decrypted += (char) (value.charAt(i) + (key.charAt(i % key.length()) - '0')); //Decrypts the current char with the key
		}

        return decrypted; //Returns the decrypted string
	}
	
    public SaarujanServer(String name, String ownerUsername) {
        this.name = name.replaceFirst(name.charAt(0) + "", (name.charAt(0) + "").toUpperCase()); //Stores the given name 
        inMemory = new SaarujanQueue(); //Initalizes a queue for recently accessed files
        accounts = new ArrayList<String[]>(); //Initializes accounts
        try {
			//Creates an instance of File, with the server name
            File temp = new File(this.name);
            if (temp.isDirectory()) { //If a directory under such name exists
				backupNumber = new File(name + "/backup_1.txt").exists(); //If 'backup_1' exists, then the backup number is 1 (true)
                BufferedReader input = new BufferedReader(new FileReader(name + "/metadata.txt")); //Opens the server's metadata file
                key = ""; //Empties the key
                String encrypted = input.readLine(); //Reads the encrypted key
				String undoEncryption = "TheEncryptionKey";	//undoEncryption - the value that was used to encrypt the key
				key = ""; //Empties the key to add the decrypted characters
				for (byte i = 0; i < encrypted.length(); ++i) { //Loops through the encrypted key
					key += (int) (undoEncryption.charAt(i) - encrypted.charAt(i)); //Decrypts the current character
				}

				//Initializes the root folder with the remaining data in the metadata file
				root = new SaarujanFolder(name + "://", decrypt(input.readLine()), new SaarujanDate(input.readLine()));
                input.close(); //Closes the input stream
                loadFS(); //Loads the file system from backup files
                loadAccounts(); //Loads the accounts from the users file
            } else {
        		root = new SaarujanFolder(name + "://", ownerUsername, SaarujanDate.currentDate()); //Initializes the root folder
				backupNumber = false; //Sets the backup number to 0 (false); switches between 0 and 1
                temp.mkdir(); //Creates a new directory for this server using the given name as the folder name
                temp = new File(name + "/metadata.txt"); //Creates an instance of a File
				temp.createNewFile(); //Creates a file for the server's metadata
                FileWriter output = new FileWriter(temp); //Opens the metadata file
                key = generateKey(new Random().nextInt()); //Generates a new key using a random number as a seed
                output.write(encrypt("TheEncryptionKey") + "\n" + //Writes the server's metadata into the file
							 encrypt(ownerUsername) + "\n" + root.getDate().toString());
                output.close(); //Closes the output stream
                temp = new File(name + "/logs.txt"); //Creates an instance of a File
                temp.createNewFile(); //Creates a file for server logging
                temp = new File(getBackupPath()); //Creates an instance of a File
                temp.createNewFile(); //Creates a file for file backups
                temp = new File(getFoldersPath()); //Creates an instance of a File
                temp.createNewFile(); //Creates a file for folder backups
                temp = new File(name + "/users.txt"); //Creates an instance of a File
                temp.createNewFile(); //Creates a file for account information
            }
        } catch (Exception e) { //If any exception occurs
            System.out.println("Server cannot read or write files!"); //Error message is outputted
        } 
    }

	private void loadAccounts() {
        try {
            BufferedReader input = new BufferedReader(new FileReader(name + "/users.txt")); //Opens the users file
            String line = input.readLine(), tokens[]; //line - stores the current line in the file; tokens - the line, split by '|'
            while (line != null) { //While another line exists
				tokens = line.split("\\|"); //Splits the line with the delimeter character, and stores it in tokens
				for (byte i = 0; i < tokens.length; ++i) { //Loops through the tokens
					tokens[i] = decrypt(tokens[i]); //Decrypts each individual token
				}
				
                accounts.add(tokens); //Adds the account information to the arraylist
                line = input.readLine(); //Reads the next line
            }

            input.close(); //Closes the input stream
        } catch (Exception e) { //If any exception occurs
            log("Error while reading user file!", true); //An error message is logged
        }
    }
	
	private void loadFS() {
        SaarujanFolder folder = new SaarujanFolder(); //Stores the current folder
		SaarujanFolder.resetLine(); //Resets the current line to 1
        while (folder.load(this)) { //While a new folder can be loaded into 'folder'
            SaarujanFolder parent = navigateFolder(root, folder.getPath().split("/"), 2); //Navigates to the parent directory
            int index = parent.indexOf(folder.getName()); //Stores the index of the current folder in the parent folder
            if (index != -1) { //If the folder already exists in the folder
                parent = (SaarujanFolder) parent.get(index); //Stores the existing folder in parent
                parent.setUploader(folder.getUploader()); //Sets the uploader; if it already existed, this info wasn't initialized
                parent.setDate(folder.getDate()); //Sets the upload date; if the folder already existed, this info wasn't initialized
            } else { //If the folder doesn't exist in the folder
                parent.add(folder); //Adds the new folder to the parent directory
            }
            folder = new SaarujanFolder(); //Creates a new folder; the next folder in the backup will be loaded onto this
        }

        SaarujanFile file = new SaarujanFile(); //Stores the current file
		SaarujanFile.resetLine(); //Resets the current line to 1
        while (file.load(this)) { //While a new file can be loaded into 'file'
			file.setData(null); //Clears the data from memory; only recently accessed files should be stored in memory
            navigateFolder(root, file.getPath().split("/"), 2).add(file); //Adds the file to the parent of the file
            file = new SaarujanFile(); //Creates a new file; the next file in the backup will be loaded onto this
        }
    }

	public String getBackupPath() {
		return name + "/backup_" + (backupNumber ? 1 : 0) + ".txt"; //Returns the current backup path
	}

	public String getNextBackupPath() {
		//The getBackupPath() will be used to load data from the previous backup since most data isn't stored in memory
		return name + "/backup_" + (backupNumber ? 0 : 1) + ".txt"; //Returns the next backup path
	}

	public String getFoldersPath() {
		return name + "/folders.txt"; //Returns the folders backup path
	}

	public int getMaxSize() {
		return MAX_SIZE; //Returns the maximum file size
	}

	private void backup() {
        try {
            File backup = new File(getNextBackupPath()); //Opens a file using the next backup path
            backup.createNewFile(); //Creates the new backup file
            backup = new File(getFoldersPath()); //Creates an instance of File
			backup.delete(); //Deletes the previous folder backup (isn't required during the backup of folders, unlike files)
            backup.createNewFile(); //Creates the folder backup file, if it doesn't exist
            for (short i = 0; i < root.itemCount(); ++i) { //Loops through all direct children of the root directory
				//For files, if their data isn't stored in memory, they can be loaded from the backup, which is why two backups are used
                root.get(i).backup(this); //Saves the current item to the backup file (folders are recursive, files are normal)
            }

			backup = new File(getBackupPath()); //Creates an instance of the previous backup
			backup.delete(); //Deletes the previous backup
			backupNumber = !backupNumber; //Swaps to the new backup number (0 to 1, 1 to 0);
            log("Server storage saved in" + getBackupPath(), false); //Logs a success message
        } catch (Exception e) { //If any exception occurs
            log("Cannot write to backup files!", true); //An error message is logged
        }
    }

    private void send(String s) {
        try {
            sockOut.write(String.format("%016d", s.getBytes().length).getBytes()); //Sends the size of the message as 16 characters
            sockOut.write(s.getBytes()); //Sends the bytes of the given message
            sockOut.flush(); //Flushes the stream
        } catch (Exception e) { //If an exception occurs
            log("Error while sending data to client!", true); //Logs a warning message
        }
    }
	
	private String recv() {
        String result = "", size = ""; //result - the message from the client; size - the size of the message
		try {
            for (int i = 0; i < 16; ++i) { //Loops 16 times; the size will always be sent as a 16 digit string
                size += (char) sockIn.read(); //Adds the received character to size
            }

            for (int i = 0; i < SaarujanItem.strToInt(size); ++i) { //Loops through the message using the received size
                result += (char) sockIn.read(); //Adds the received character to result
            }

            return result; //Returns the resulting message
        } catch (Exception e) { //If an exception occurs
            log("Error while receiving data from client!", true); //Logs a warning message
            return ""; //Returns an empty string
        }
    }

	private String loginAccount() {
        String username = recv(), password = recv(); //Stores the received username and password from the client

        String[] line; //Temporary variable that stores the current account
        for (byte i = 0; i < accounts.size(); ++i) { //Loops through all accounts
            line = accounts.get(i); //Stores the current account in accounts
            if (line[0].equals(username)) { //If the current username matches the given username
                if (line[2].equals("PENDING")) { //If their account access to this server is pending
                    send("PENDING"); //A message is sent to let the client know that they need to wait for permission to be granted
                    recv(); //Waits until the client exits
                    return null; //Returns null to let handleConnection() know that the connection should be closed 
                } else if (line[2].equals("DENIED")) { //If their account access to this server is denied
                    send("DENIEDACC"); //A message is sent to let the client know that they cannot access this server
                    recv(); //Waits until the client exits
                    return null; //Returns null to let handleConnection() know that the connection should be closed
                } else if (line[1].equals(password)) { //If they have access to the server, and the given password is correct
                    if (line[0].equals(root.getUploader())) //If the user is the owner of the server
                        send("OWNER"); //A message is sent to let the client know that they have owner priviledge
                    else //Otherwise
                        send("SUCCESSFUL"); //A message is sent to let the client know that they successfully logged in
                    
                    log(username + " logged onto this server", false); //Logs a message 
                    return username; //Returns the username of the client
                } else { //If the password was incorrect
                    send("INCPASS"); //A message is sent to let the client know that they inputted the wrong password
                    return "DIFF_ACTION"; //Returns "DIFF_ACTION" to let handleConnection() know that the user wants to act another way
                }
            } 
        }

        send("INEXISTANT"); //If the username wasn't found
        return "DIFF_ACTION"; //Returns "DIFF_ACTION" to let handleConnection() know that the user might want to perform another action
    }

	private String createAccount() {
        String username = recv(), password = recv(); //Stores the given username and password from the client

        for (byte i = 0; i < accounts.size(); ++i) { //Loops through all the accounts
            if (accounts.get(i)[0].equals(username)) { //If the current account username equals the given username
                send("ALREXISTS"); //Lets the client know that the account already exists
                return "DIFF_ACTION"; //Lets handleConnection() know that the user might want to perform a different action
            }
        }

        log("A new account was created: " + username, false); //Logs a message
		//Adds the new account to the arraylist of accounts; if the username is the owner's username, they are granted permission
        if (username.equals(root.getUploader())) { //If the username is the owner's username; they are granted permission immediately
			accounts.add(new String[]{ username, password, "PERMIT_" }); //The account is added to the ArrayList of accounts
            send("SUCCESSFUL"); //Lets the client know that the account was created and they know have permission to access the server
            return username; //Returns the username
        } else { //If the user is not the owner
			accounts.add(new String[]{ username, password, "PENDING" }); //The account is added to the ArrayList of accounts
            send("PENDING"); //Lets the client know that the account was created, and they have to wait for permission
            return null; //Returns null to let handleConnection() know that the connection should be closed
        }
    }

	private void permitAccount(String currentClient) {
        if (!currentClient.equals(root.getUploader())) { //If the user is not the owner
            send("NOPERMISSION"); //Notifying the client that they do not have permission
            return; //Exiting the method
        }
		
		send(accounts.size() + ""); //Sends the number of accounts
		for (byte i = 0; i < accounts.size(); ++i) { //Loops through all accounts, and sends them (except passwords)
			send(accounts.get(i)[0] + ": " + (accounts.get(i)[2].equals("PENDING") ? "pending" :
											 accounts.get(i)[2].equals("PERMIT_") ? "permitted" : "denied"));
		}
		
        String username = recv(), permission = recv(); //Receives and stores the username and password
		if (username.equals(root.getUploader())) { //
			send("NOSELFMOD"); //Notifies the owner that they cannot change their own permissions
			return; //Exiting the method
		}
		
        for (byte i = 0; i < accounts.size(); ++i) { //Loops through the accounts items
            String[] temp = accounts.get(i); //Stores the current account
            if (temp[0].equals(username)) { //If the user matches the given username
                temp[2] = permission.equals("PERMIT") ? "PERMIT_" : "DENIED"; //Sets the permission to the given permission
                accounts.set(i, temp); //Updates the arraylist of accounts
                log(username + " was " + (permission.equals("DEN") ? "denied" : "given") + //Logs a message
                    " permission to access this server", false);
                send("SUCCESSFUL"); //Notifies the user that the operation was successful
                return; //Exits the method
            }
        }

        send("INEXISTANT"); //Notifies the user that the user doesn't exist
    }

	private void sendRecentLogs() {
		try {
			SaarujanQueue recent = new SaarujanQueue((byte) 20); //Creates a queue to store recent logs; old logs are dequeued 
			BufferedReader input = new BufferedReader(new FileReader(name + "/logs.txt")); //Opens the log file

			String temp; //Temp variable is declared in order to store the input lines
			while ((temp = input.readLine()) != null) { //While there is another line in the file
				recent.enqueue(temp); //The current line is added
			}

			input.close(); //The input is closed
			send(recent.length() + ""); //Notifies the client about the amount of log messages they will receive
			FileWriter output = new FileWriter(name + "/logs.txt"); //Opens the log file, to reduce the size of the log file
			for (byte i = (byte) (recent.length() - 1); i >= 0; --i) { //Loops 'queue length' times
                temp = recent.dequeue(); //Stores the current log message
				send(decrypt(temp)); //Sends the decrypted log message to the client
				output.write(temp + "\n"); //Writes the log message to the log file
			}

			output.close(); //Closes the output stream
		} catch (Exception e) { //If any error occurs
			send("ERRORLOG"); //Notifies the client that an error occured
			log("Error while sending recent log messages!", true); //Logs a message
		}
	}
	
	private void createFolder(String currentClient) {
		//Creating a new folder with received path; the current client is the uploader, and the current date is the upload date
        SaarujanFolder folder = new SaarujanFolder(recv(), currentClient, SaarujanDate.currentDate());
        navigateFolder(root, folder.getPath().split("/"), 2).add(folder); //Navigates to its parent folder, and adds the created folder
        log(currentClient + " created a new folder: " + folder.getPath(), false); //Logs a message
    }

	private void uploadFile(String currentClient) {
		//Creates a new file with received path; the uploader is the current client, and the upload date is the current date
        SaarujanFile file = new SaarujanFile(recv(), currentClient, SaarujanDate.currentDate(), "");
		//Sets the file data to the received data; replaces the line seperators with '/newline', as the backup stores 1 file per line
        file.setData(recv().replaceAll("[\\r\\n]+", "/newline"));

		backupNumber = !backupNumber; //Swaps the backup number temporarily, as it should be saved on the current backup
        file.backup(this); //Saves the file to the server's backup
		backupNumber = !backupNumber; //Undoes the swapped backup number

        navigateFolder(root, file.getPath().split("/"), 2).add(file); //Navigates to the parent folder, and adds the file
        if (file.size() <= MAX_SIZE) { //If the file meets the size requirements
            String path = inMemory.enqueue(file.getPath()); //Enqueues the file in the memory queue; saves the dequeued file path
            if (path != null) { //If a path was dequeued from the memory queue
                SaarujanFolder parent = navigateFolder(root, path.split("/"), 2); //Stores the parent directory of the dequeued file
                ((SaarujanFile) parent.get(parent.indexOf(file.getName()))).setData(null); //Clears the data of the file from memory
            }
        } else { //If the file is too large to remain in-memory
			file.setData(null); //Clears the data of the file from memory
		}

		send("SUCCESSFUL"); //Sends a message to the client; this allows the client program to wait, so they don't perform actions while the file is uploading
        log(currentClient + " uploaded a new file: " + file.getPath(), false); //Logs a message 
    }

    private void downloadFile(String currentClient) {
        String path = recv(); //Stores the received path
        SaarujanFolder parent = navigateFolder(root, path.split("/"), 2); //Stores the parent folder of the file to download
        SaarujanFile file = (SaarujanFile) parent.get(parent.indexOf(path.substring(path.lastIndexOf("/") + 1))); //Stores the file
		inMemory.remove(path); //Removes the file from the memory queue; if it doesn't exist in it currently, nothing happens

		//Replaces the '/newline's with actual line seperators, and sends it to the client
        send(file.getData(this)); 
        log(currentClient + " downloaded a file: " + path, false); //Logs a message
        path = inMemory.enqueue(path); //Adds the path (possibly back) to the queue; now it ranks higher than it may have before
        if (path != null) { //If a file was dequeued from the memory queue 
            parent = navigateFolder(root, path.split("/"), 2); //The parent directory of the dequeued file is stored
            ((SaarujanFile) parent.get(parent.indexOf(file.getName()))).setData(null); //The data of the file is cleared from memory
        }
    }

	private void deleteItem(String currentClient) {
        String path = recv(); //Stores the received path
        SaarujanFolder parent = navigateFolder(root, path.split("/"), 2); //Navigates to the parent directory of the item to delete
        parent.remove(parent.indexOf(path.substring(path.lastIndexOf("/") + 1))); //Deletes the item from the parent directory
        backup(); //The server conducts a backup to acknowledge the removed file
		send("SUCCESSFUL"); //Sends a success message; this allows the client to wait for the backup to be completed
        log(currentClient + " deleted an item: " + path, false); //A message is logged
        inMemory.remove(path); //The file is removed from the memory queue, if it existed within
    }

	private void sendNavigation() {
        String path = recv(); //Stores the received path
        SaarujanFolder parent; //Creates a variable to store the parent directory
        if (path.equals(name + "://")) { //If the path is the root directory
            parent = root; //The parent is set to the root directory
        } else { //Otherwise
            parent = navigateFolder(root, path.split("/"), 2); // The parent directory is set to the parent of the item
            SaarujanItem curr = parent.get(parent.indexOf(path.substring(path.lastIndexOf("/") + 1))); //Stores the item
            if (curr instanceof SaarujanFile) { //If the item is actually a file
                send("NOTAFOLDER"); //The client is notified that it isn't a folder
                return; //The method is exited
            }

            parent = (SaarujanFolder) curr; //The parent variable is set to the actual directory
        }
        send(parent.itemCount() + ""); //The item count of the directory is sent to the client
        for (int i = 0; i < parent.itemCount(); ++i) { //Loops through all items in the folder
            send(parent.get(i).sendFormat()); //Sends the current item to the client
        }
    }
	
	private void handleConnection() {
        String currentClient; //Stores the current client's username, to use in logging operations
		switch (recv()) { //Receives the initial message from the client
			case "LOGINACC": currentClient = loginAccount(); break; //If user wants to login, currentClient is set to loginAccount()
			case "CREATEACC": currentClient = createAccount(); break; //If user wants to create, currentClient is set to createAccount()
			default: return; //If an invalid message was received, the method exits
		} 
        
        if (currentClient == null) //If currentClient is null, that means that the connection should be closed
            return; //Exits the method
        else if (currentClient.equals("DIFF_ACTION")) { //If currentClient equals "DIFF_ACTION", the user wants to perform another task
            handleConnection(); //Calls the method again
            return; //Exits this instance of the method
        }

        send(name); //Sends the server name 
        while (true) { //Loops until an invalid message is received, or the client wants to log out
            switch (recv()) { 
                case "PERMITACC": permitAccount(currentClient); break; //If the owner wants to modify an account's permission
				case "ACCESSLOG": sendRecentLogs(); break; //If the owner wants to check recent logs
                case "NAVIGATE": sendNavigation(); break; //If the client wants to navigate to a certain folder
                case "ULOADFILE": uploadFile(currentClient); break; //If the client wants to upload a file
                case "CREATEFOL": createFolder(currentClient); break; //If the client wants to create a folder
                case "DLOADFILE": downloadFile(currentClient); break; //If the client wants to downloa a file
                case "DELETEITEM": deleteItem(currentClient); break; //If the client wants to delete an item
                //If the client wants to log out, or an invalid message was sent, the method is exited
                default: log(currentClient + " logged out of the server!", false); return; 
            }
        }
    }

	public void start() {
        Thread exitCondition = new Thread("exit-condition") { //The exit thread is created
            public void run() { //The run() method of the exit condition is overrided
                try {
                    Scanner input = new Scanner(System.in); //A scanner is opened for user input
                    System.out.println("Type anything to stop the server: "); //Whenever someone enters something on the server
                    input.nextLine(); //The server waits for someone to stop the server
                    input.close(); //Closes the scanner
                    if (connection != null) //If there is a connection to a client
                        connection.close(); //The connection is closed, as sockets hang until they're closed; 

                    if (main != null) //If the main socket is active
                        main.close(); //The connection is closed in order to allow the thread interruption to function 

                    System.out.println("Server stopped successfully!"); //Outputs a message to the console
                } catch (Exception e) { //If any exception occurs
                    log("Error while closing server!", true); //An error message is logged
                } 
            }
        };

        exitCondition.start(); //The exit thread is started on another thread
        try {
            main = new ServerSocket(2023); //A new server socket is created on port 2023
            Socket testConnection = new Socket("0.0.0.0", 2023);
            Socket getAddress = main.accept(); //Connects to get the address and port of the server
            System.out.println("Connect to this address: " + getAddress.getLocalAddress() + ":" + getAddress.getLocalPort());
            getAddress.close(); //Closes the connection
            testConnection.close(); //Closes the connection
            while (true) {
                System.out.println("Waiting for connection...");
                connection = main.accept();
				sockIn = connection.getInputStream();
				sockOut = connection.getOutputStream();
                handleConnection();
                connection.close();
            } 
        } catch (Exception e) { //If the server is interrupted by the user through the exit thread
            log("Server stopped successfully", false); //Logging a message that the server was stopped
			backup(); //The file system is backed up onto the server's backup
	        try {
	            FileWriter meta = new FileWriter(name + "/metadata.txt"); //Opening the metadata file, and writing the encrypted info
	            meta.write(String.format("%s\n%s\n%s", encrypt("TheEncryptionKey"), 
                                         encrypt(root.getUploader()), root.getDate().toString()));
	            meta.close(); //Closing the metadata file
	            meta = new FileWriter(name + "/users.txt"); //Opening the accounts file
	            for (byte i = 0; i < accounts.size(); ++i) { //Looping through all of the accounts
	                String[] temp = accounts.get(i); //Storing the current account
	                meta.write(encrypt(temp[0]) + "|" + encrypt(temp[1]) + "|" + 
                               encrypt(temp[2]) + "\n"); //Writing the formatted account
	            }
	            meta.close(); //Closing the accounts file
	        } catch (Exception err) { //If any exception occurs
	            log("Cannot write to metadata and user file!", true); //Logging an error message
	        }
        }    
    }

    public static void main(String[] args) {
		SaarujanServer local = new SaarujanServer("Storage", "Saarujan");
		local.start();
    }
}