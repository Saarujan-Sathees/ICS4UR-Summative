import java.net.Socket;
import java.net.SocketException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;

public class SaarujanClient {
    private OutputStream sockOut; //The output stream to the server
    private InputStream sockIn; //The input stream from the server
    private Scanner input; //The scanner for user input
    private boolean isOwner; //A boolean stating whether the current user is the server owner or not
	//Stores the client's username, the server name, the current path for navigation, and the download path where files should be saved
    private String username, serverName, currentPath, downloadPath; 
    private final static String SPACE_FORMAT = "                              "; //Static String for formatting purposes
    private ArrayList<String[]> currentItems; //ArrayList of the current items in the directory that the user is navigating

    private static void clearConsole() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); //Clears the console using platform's command
        } catch (Exception e) {
            System.out.print("\033[H\033[2J"); //If that doesn't work, prints an ANSI character
        }
    }
	
    private static int strToInt(String s) {
        int result = 0, mult = 1; //Result stores the converted integer; mult stores the current digit placement
        for (byte i = (byte) (s.length() - 1); i >= 0; --i) { //Loops from the lowest placed digit (ones) to the highest
            result += mult * (s.charAt(i) - '0'); //Adds the digit at the current position
            mult *= 10; //Moves to the next position (Ex: ones to tens)
        }
        
        return result; //Returns the converted integer
    }

	private static void printColour(String message, int colour) {
		switch (colour) {
			case 0: break; //White
			case 1: System.out.print("\u001B[31m"); break; //Red
			case 2: System.out.print("\u001B[35m"); break; //Purple
			case 3: System.out.print("\u001B[36m"); break; //Cyan
			case 4: System.out.print("\033[38;5;214m"); break; //Yellow
		}
		System.out.print(message + "\u001B[0m"); //Prints the message and resets the colour to white
	}
	
    private void send(String s) {
        try {
            sockOut.write(String.format("%016d", s.getBytes().length).getBytes()); //Sends the size of the message as 16 digits
            sockOut.write(s.getBytes()); //Writes the bytes of the message
            sockOut.flush(); //Flushes the output stream
        } catch (SocketException e) { //If a socket exception occurs, that means that the server stopped
            clearConsole(); //The console is cleared
            printColour("Server has been shut down!\n", 4); //Outputs a message to let the client know that the server stopped
            System.exit(0); //Exits the program
        } catch (Exception e) { //If any other normal exceptions happen
            clearConsole(); //The console is cleared
            printColour("Error while sending data to server!\n", 1); //Error message is outputted
        }
    }
	
	private String recv() {
    	String result = "", size = ""; //result - the message from the server; size - the size of the message
		try {
            for (int i = 0; i < 16; ++i) { //Loops 16 times; the size will always be sent as a 16 digit string
                size += (char) sockIn.read(); //Adds the received character to size
            }

            for (int i = 0; i < SaarujanItem.strToInt(size); ++i) { //Loops through the message using the received size
                result += (char) sockIn.read(); //Adds the received character to result
            }


            return result; //Returns the resulting message
        } catch (SocketException e) { //If a socket exception occurs, then the server has stopped running
            clearConsole(); //Clears the console
            printColour("Server has been shut down!\n", 4); //Outputs a message to let the client know that the server stopped
            System.exit(0); //Exits the program
        } catch (Exception e) { //If any other normal exception occurs
            clearConsole(); //Clears the console
        	printColour("Error while receiving data from server!\n", 1); //Outputs error message
        }
        return "";
    }
	  
	private String getInput() {
        String result = input.nextLine(); //Reads the next line from user input
        if (result.equals("")) { //If the next line is invalid
            printColour("Please enter a proper value!\n", 1); //The user is asked to input a valid value
            return getInput(); //Recursively gets another value
        } 
        
        return result; //Returns the valid result
    }

    private byte getChoice(String prompt, byte limit) {
        printColour(prompt + "\n", 0); //Outputs the given prompt
        try {
            byte result = input.nextByte(); //Reads the next bug
            input.nextLine(); //Reads the newline character to avoid scanner bug
            if (result < 1 || result > limit) { //If the value was invalid otherwise
                printColour("Please enter a value between 1 and " + limit + "!\n", 1); //Error message is outputted
                return getChoice(prompt, limit); //Recursively returns valid input
            }
            return result; //Returns the validated result
        } catch (Exception e) { //If any exception occurs
            input.nextLine();
            printColour("Please enter a valid number!\n", 1); //User is prompted for a proper value
            return getChoice(prompt, limit); //Recursively returns valid input
        }
    }

    public SaarujanClient(String downloadPath) {
        input = new Scanner(System.in); //Initializing a permanent scanner, as the scanner will be used often
        isOwner = false; //Setting it to false, as there is only one owner
        username = ""; //Initializing username to an empty string
        currentItems = new ArrayList<String[]>(); //Initializing the currentItems arraylist
        try {
            File test = new File(downloadPath); //Opening the download path directory
            if (test.isDirectory()) //If the given path is a directory, then the download path is set to the given path
                this.downloadPath = downloadPath;
            else { //If the given path is not a directory, or doesn't even exist
                printColour("Invalid download path! Downloading files to the app folder!\n", 1); //Outputs error message
                this.downloadPath = System.getProperty("user.dir"); //Sets the default download path to the program folder
            }
        } catch (Exception e) { //If any exception occurs
            printColour("Error occured while setting download path! Setting download path to the app folder!\n", 1);
            this.downloadPath = System.getProperty("user.dir"); //Sets default download path to the program folder, and outputs message
        }
    }

    private boolean isItem(String name) {
        String[] curr; //Stores the current item
        for (short i = 0; i < currentItems.size(); ++i) { //Loops through all items
            curr = currentItems.get(i); //Stores the current item
            if (curr[0].equals(name)) //If the current item matches the given name
                return true; //True is returned
        }

        return false; //False is returned; the given name is not an item
    }

    private boolean inRoot() {
        return currentPath.equals(serverName + "://"); //Returns a boolean whether the current directory is the root directory
    }

	private void navigate(String path) {
        send("NAVIGATE"); //Sends the NAVIGATE keyword to the server
        send(path); //Sends the new path to be navigated to
        String count = recv(); //Receives the supposed children count
        if (count.equals("NOTAFOLDER")) { //If the given path is actually a file, and not a folder
            printColour(path.substring(path.lastIndexOf("/") + 1) + " is not a folder!\n", 1); //Outputs an error message
            return; //Exits the method
        }

		currentPath = path; //Sets the current path to the given path
        currentItems.clear(); //Clears the current items
        for (short i = (short) strToInt(count); i > 0; --i) { //Loops through the new items using the received count
            currentItems.add(recv().split("\\|")); //Adds the received item to current items
        }
    }

    private boolean loginAccount() {
        send("LOGINACC"); //Sends the LOGINACC keyword to the server
        printColour("Username: ", 4); //Asks the user for their username
        username = getInput(); //Stores the username
        send(username); //Sends the username to the server
        printColour("Password: ", 4); //Asks the user for their password
        send(getInput()); //Sends the password directly to the server

        switch (recv()) {
            case "OWNER": isOwner = true; //If they are the owner, isOwner is set to true
            case "SUCCESSFUL": clearConsole(); printColour("Logged in as " + username + "!\n", 4); return true; //Successful login
            case "INCPASS": printColour("Incorrect Password!\n", 1); return loginAccount(); //Incorrect password; given another chance
            case "PENDING": clearConsole(); printColour("Your access permission is still pending!\n", 4); 
                            send("OK"); System.exit(0); //Pending
            case "INEXISTANT": clearConsole(); printColour("This account doesn't exist!\n", 1); System.exit(0); //Account doesn't exist
            case "DENIEDACC": clearConsole(); printColour("Your account is banned from this server!\n", 4); 
                              send("OK"); System.exit(0);//Account is banned
            default: return false; //Otherwise, false is returned
        }
    }

    private byte createAccount() {
        send("CREATEACC"); //Sends the CREATEACC keyword to the server
        printColour("Username: ", 4); //Asks user for their username
		username = getInput(); //Stores the given username
        send(username); //Sends the username to the server
    	printColour("Password: ", 4); //Asks user for their password
        send(getInput()); //Sends the password directly to the server

        clearConsole(); //Clears the console
        switch (recv()) {
            case "SUCCESSFUL": printColour("Owner account has been created!\n", 4); isOwner = true; return 1; //Owner account
            case "ALREXISTS": printColour("This account already exists!\n", 1); return 0; //Account exists already
            case "PENDING": printColour("Your account has been successfully created, and is waiting for permission!\n", 4); //Pending
            default: return -1; //Default message exits the program
        }
    }

    private void permitAccount() { 
        send("PERMITACC"); //Sends the PERMITACC keyword to the server
        String size = recv(); //Stores the received amount of accounts
        if (size.equals("NOPERMISSION")) { //If the received message was actually a NOPERMISSION warning
			clearConsole(); //Clears the console
            printColour("You do not have owner priviledges!\n", 4); //Error message is outputted
            return; //Method is exited
        }
        
        printColour("Current Account Permissions\n", 4); //Prints title for the account list
        for (int i = strToInt(size); i > 0; --i) { //Loops through the accounts to be received
            printColour("  -\t" + recv() + "\n", 3); //Outputs the username and their permission, with formatting
        }
		
        printColour("Enter username of account you want to change permissions of: ", 4); //Asks for the user to modify access for
        send(getInput()); //Sends the received input from the user; then, asks user for the permission that they want to give
        printColour("Enter 'grant' if you want to grant them permission to your server; type anything to deny access: ", 4);
        send(getInput().equalsIgnoreCase("grant") ? "PERMIT" : "DEN"); //Sends the permission that the user wants to give
        clearConsole(); //Clears the console

		switch (recv()) {
			case "NOSELFMOD": printColour("You cannot modify your own permissions!\n", 1); break; //Owner can't modify themselves
			case "SUCCESSFUL": printColour("You have successfully modified their permissions!\n", 4); break; //Successful
			case "INEXISTANT": printColour("This account doesn't exist!\n", 1); break; //The given username doesn't exist
		}
    }

	private void viewLogs() {
        send("ACCESSLOG"); //Sends the ACCESSLOG keyword to the server
		String count = recv(); //Stores the log count
		clearConsole(); //Clears the console
		if (count.equals("ERRORLOG")) { //If an error occured
			printColour("Error while viewing logs!\n", 1); //Outputs error message
			return; //The method is exited
		}

		for (byte i = 0; i < strToInt(count); ++i) {
            String temp = recv();
            printColour(temp + "\n", temp.indexOf("Warning") != -1 ? 1 : 3);
        }
	}
	
    private String readFile(String path) {
        try {
            File test = new File(path); //Opens a File instance using the given path
            if (!test.isFile()) { //If the file doesn't exist
				clearConsole(); //Clears the console
				printColour("File doesn't exist!\n", 1); //Outputs error message
				return "EXCEPTION"; //Returns an exception message
			} else if (test.length() > 100000000) { //If the file is over 100 MB (not targeted file size)
				clearConsole(); //Clears the console
                printColour("File is too big!\n", 1); //Outputs error message
                return "EXCEPTION"; //Returns an exception message
            }

            FileReader a = new FileReader(path); //Opens the given file
            String data = ""; //String to store the file's data
            int curr = a.read(); //Reads the first character of the file
            while (curr != -1) { //While the end of the file hasn't been reached
                data += (char) curr; //Adds the read character to data
                curr = a.read(); //Reads the next character
            }

            a.close(); //Closes the file stream
            return data; //Returns the resulting data
        } catch (Exception e) { //If an IOException occurs (FileNotFound has been taken care of)
            clearConsole(); //Console is cleared
            printColour("Error while reading file: " + path + "\n", 1); //Outputs error message
        }
        return "EXCEPTION";
    }

    private void uploadFile() {
       	printColour("Enter path of file that you want to upload: ", 4); //Asks user for the path of the file
        String path = getInput(); //Stores the path of the file they want to upload
        switch (path.substring(path.lastIndexOf('.') + 1)) {
            case "cpp":
            case "c++":
            case "js":
            case "py":
            case "html":
            case "css":
            case "java":
            case "cs":
            case "c":
            case "txt": break; //If the extension passed the filter, then the method continues
            default: clearConsole(); printColour("File Extension not Supported!\n", 1); return; //Outputs error message and exits
        }
        String data = readFile(path); //The data is read from the file
        if (!data.equals("EXCEPTION")) { //If an exception didn't occur during the reading of the file
            if (isItem(path.substring(path.lastIndexOf("/") + 1))) { //If another file with the same name and extension exists
				clearConsole(); //Clears the console
                printColour("Item under this name already exists!\n", 1); //Outputs error message
                return; //Exits the method
            }

            send("ULOADFILE"); //Sends the ULOADFILE keyword to the server
            if (path.indexOf("/") == -1) { //If the path is a relative path
                path = "/" + path; //Adds a '/'
            }
            if (inRoot()) //If the current directory is the root directory
                send(currentPath + path.substring(path.lastIndexOf("/") + 1)); //Sends the path without an extra '/'
            else 
                send(currentPath + path.substring(path.lastIndexOf("/"))); //Sends the path with an extra '/'

            send(data); //Sends the data from the file
            recv(); //Waits until the server finishes uploading the data
            clearConsole(); //Clears the console
            printColour("File successfully uploaded!\n", 4); //Outputs success message
            navigate(currentPath); //Navigates again to the directory, to include the sorted directory, including the uploaded file
        }
    }

    private void createFolder() {
        printColour("Enter the name of the folder you want to create: ", 4); //Asks user for the folder name
        String name = getInput(); //Stores the user input; the folder name
        for (byte i = 0; i < name.length(); ++i) { //Loops through all the characters in name
            switch (name.charAt(i)) { 
                case '/':
                case '\\':
                case ':':
                case '*':
                case '?':
                case '"':
                case '<':
                case '>':
                case '.': //If any of these invalid characters were used, then the method is called again
                case '|': clearConsole(); printColour("Cannot use the following characters: / \\ : * ? \" < > | .\n", 1); 
					      createFolder(); return;
            }
        }

        if (isItem(name)) { //If a folder with the same name already exists
			clearConsole(); //The console is cleared
            printColour("Folder under this name already exists!\n", 1); //Outputs error message
            return; //Exits the method
        }

        send("CREATEFOL"); //Sends the CREATEFOL keyword to the server
        if (inRoot()) //If the current directory is the root directory
            send(currentPath + name); //Sends the path without an extra '/'
        else
            send(currentPath + "/" + name); //Sends the path with an extra '/'
        
        clearConsole(); //Clears the console
        printColour("Folder created successfully!\n", 4); //Outputs a success message
        navigate(currentPath); //Navigates to the directory to include the newly created folder
    }

    private void downloadFile() {
        printColour("Enter name, and file extension of file you want to download:\n", 4); //Asks user for file name and extension
        printColour("Ex: image.png\n", 4); //Gives an example

        String file = getInput(); //Stores the inputted file name and extension
        if (!isItem(file) || file.indexOf(".") == -1) { //If the filename doesn't contain the extension, or the file doesn't exist
			clearConsole(); //Clears the console
            printColour("This file doesn't exist!\n", 1); //Outputs error message
            return; //Exits the method
        }

        send("DLOADFILE"); //Sends the DLOADFILE keyword to the server
        if (inRoot()) //If the current directory is the root directory
            send(currentPath + file); //Sends the path without an extra '/'
        else 
            send(currentPath + "/" + file); //Sends the path with an extra '/'

        String data = recv(); //Receives the file data
        clearConsole(); //Clears the console
        try {
            File open = new File(downloadPath + file); //Opens a File instance with the given path
            open.createNewFile(); //Creates a new file
            FileWriter a = new FileWriter(downloadPath + file); //Opens the created file
            a.write(data.replaceAll("/newline", System.getProperty("line.separator"))); //Saves the data
            a.close(); //Closes the file stream
            printColour("File successfully saved as: " + downloadPath + file + "\n", 4); //Outputs success message
        } catch (Exception e) { //If any exception occurs
            printColour("Error occured while saving file!\n", 1); //Error message is outputted
        }
    }

    private void deleteItem() {
        printColour("Enter name of item you want to delete; if it is a file, enter the extension as well: ", 4); //Asks user for item
        String name = getInput(); //Stores the inputted item name
        clearConsole(); //Clears the console

        if (!isItem(name)) { //If the item doesn't exist
            printColour("This item doesn't exist!\n", 1); //Outputs error message
            return; //Exits the method
        }

        send("DELETEITEM"); //Sends the DELETEITEM keyword to the server
        if (inRoot()) //If the current directory is the root directory
            send(currentPath + name); //Sends the total path without an extra '/'
        else 
            send(currentPath + "/" + name); //Sends the total path with an extra '/'
        
        recv(); //Waits for the server to complete a backup of the file system
        printColour("Item successfully removed!\n", 4); //Outputs a success message
        navigate(currentPath); //Navigates again to the current directory, to acknowledge the removed item
    } 

    private void navigate() {
        printColour("Which folder do you want to enter? Type < to go to the previous folder: ", 4); //Asks user for the directory
        String folder = getInput(); //Stores the name of the directory
        String path = currentPath; //Stores the current path; this allows the current path to be reverted back to the original path
		clearConsole(); //Clears the console
        if (folder.equals("<")) { //If the user wants to go to the current directory's parent directory
            if (inRoot()) { //If the current directory is the root directory
                printColour("Cannot go back anymore!\n", 1); //Outputs an error message
                return; //Exits the method
            }

            path = path.substring(0, path.lastIndexOf("/")); //Removes the current folder name from the path
            if (path.equals(serverName + ":/")) //If the new directory should be the root directory
                path += "/"; //Adds an extra '/', as the root directory should always be <server name>://
        } else { //If the user wants to go to a subfolder
            if (currentItems.size() == 0) { //If the current directory is empty
                printColour("Folder is empty!\n", 1); //An error message is outputted
                return; //The method is exited
            } else if (!isItem(folder)) { //If the given name doesn't exist
    			printColour("This folder doesn't exist!\n", 1); //Outputs an error message
                return; //Exits the  method
            }

            if (inRoot()) //If the current directory is the root
                path += folder; //Adds the new folder name to the current path
            else
                path += "/" + folder; //Adds the new folder name, with an extra '/'
        }
		navigate(path); //Calls the navigate() method with the newly created path
	}

    private void displayFolder() {
        String sizeFormat = "    ";
        printColour(currentPath + "\n", 4); //Outputs the current path
        printColour("    Name   " + SPACE_FORMAT.substring(4) + "Uploader" + SPACE_FORMAT.substring(18) + "       Upload Date   " +
                    "   Size       Files / Ext.    Folders\n", 4); //Outputs guide 
        for (short i = 0; i < currentItems.size(); ++i) { //Loops through current items of directory
            String[] line = currentItems.get(i); //Stores the current item
            if (line.length > 5) { //If the item is a folder, it outputs the folder item differently
                printColour("   |  " + line[0] + SPACE_FORMAT.substring(line[0].length()) + "|      " + 
                            line[1] + SPACE_FORMAT.substring(line[1].length() + 10) + "|   " + line[2] + "   |   " + 
                            line[3] + " KB" + sizeFormat.substring(line[3].length()) + 
                            "|   " + line[4] + "           |   " + line[5] + "\n", 3);
            } else {
				String fileName = line[0].substring(0, line[0].indexOf('.')); //Stores the file name, and outputs the file
                printColour("   |  " + fileName + SPACE_FORMAT.substring(fileName.length()) + "|      " + line[1] + 
                            SPACE_FORMAT.substring(line[1].length() + 10) + "|   " + line[2] + "   |   " + line[3] + " KB" + 
                            sizeFormat.substring(line[3].length()) + "|   " + 
                            line[0].substring(line[0].indexOf('.') + 1).toUpperCase() + "\n", 2);
            }
        }

        System.out.println(); //Outputs new line 
    }

    private boolean connect() {
		//Asks user whether they want to login or create an account
        printColour("Enter 'create' to create a new account, or anything else to login to an existing account: ", 4);
        if (getInput().equalsIgnoreCase("create")) { //If they want to create an account
            switch (createAccount()) { 
                case 1: return true; //If the account was made, and it is the owner account, the program continues
                case 0: return connect(); //If another option wants to be made, the connect() method is called again
                case -1: System.exit(0); //If the account made is pending permission, etc. the program exits
            }
        }
            
        return loginAccount(); //If create wasn't chosen, the login() method is used
    }

    private void displayOwnerMenu() {
        byte choice; //A choice variable is declared
		
        do {
			displayFolder(); //Displays the current folder
        	choice = getChoice("Enter the number beside the following actions: \n1 - Navigate\n2 - Create Folder\n" + 
                                "3 - Upload File\n4 - Download File\n5 - Delete Item\n6 - Modify Permissions\n" + 
								"7 - View Recent Logs\n8 - Logout", (byte) 8); //Asks owner for a choice
			
			switch (choice) {
				case 1: navigate(); break; //Navigates to a folder
				case 2: createFolder(); break; //Creates a folder
				case 3: uploadFile(); break; //Uploads a file
				case 4: downloadFile(); break; //Downloads a file
				case 5: deleteItem(); break; //Deletes an item
				case 6: permitAccount(); break; //Modifies permissions
				case 7: viewLogs(); break; //Views recent logs
				default: break; //Default value exits the switch statement
			}
		} while (choice != 8);
    }

    private void handleConnection() {
        printColour("Click enter to continue...\n", 4); //Acts as buffer for the user
        input.nextLine(); //Waits until user clicks enter

        if (isOwner) { //If the client is the owner of the server
            displayOwnerMenu(); //Displays the owner menu
        } else {
            byte choice = 0; //Stores the current choice
            do {
                displayFolder(); //Displays the current folder, and asks the user to make a choice from the menu
                choice = getChoice("Enter the number beside the following actions: \n1 - Navigate\n2 - Create Folder\n" + 
                                   "3 - Upload File\n4 - Download File\n5 - Delete Item\n6 - Logout", (byte) 6);
                
                switch (choice) {
                    case 1: navigate(); break; //Navigates to a folder
                    case 2: createFolder(); break; //Creates a folder
                    case 3: uploadFile(); break; //Uploads a file
                    case 4: downloadFile(); break; //Downloads a file
                    case 5: deleteItem(); break; //Deletes an item
                    case 6: return; //Logs out of the server, and exits the loop
                }
            } while (choice != 6); //While the user doesn't want to log out
        }
    }

    public void start(String serverAddress, short serverPort) {
        try {
            Socket main = new Socket(serverAddress, serverPort); //Connects to the server socket
            sockOut = main.getOutputStream(); //Stores the output stream to the server
            sockIn = main.getInputStream(); //Stores the input stream from the server

            if (connect()) { //If the user is autheniicated by the server
                serverName = recv(); //Stores the server name
                currentPath = serverName + "://"; //Stores the current path
                navigate(currentPath); //Gets the current items in the root folder
                handleConnection(); //Handles the connection between the server and this client
            }
            
            send("LOGOUTACC"); //Logs out of the server
            printColour("Logged Out!", 4); //Outputs a success message
            main.close(); //Closes the connection once the user is finished
        } catch (IOException e) { //If an io exception occurs
            printColour("Error while connecting to server!\n", 1); //Outputs error message
        }
    }

    public static void main(String[] args) {
        SaarujanClient me = new SaarujanClient("C:\\Users\\saaru\\Downloads\\");
        me.start("192.168.2.36", (short) 2023);
    }
}

