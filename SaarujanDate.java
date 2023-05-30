/** Date Class
* Description: A class that stores the day, month, and year of a date; this can also be used to find the current date
*
* constructor() - Initializes the Date with 01/01/2023
* constructor(Byte, Byte, Short) - Initializes the Date with the given day, month, and year
* constructor(String) - Initializes the Date with the given format String: mm/dd/yyyy
* strToInt(String, Byte, Byte) - Helper method that parses a given portion of a format String, into an integer value
* getDay() - Returns the day
* getMonth() - Returns the month
* getYear() - Returns the year
* currentDate() - Returns the current date
* currentTime() - Returns the current time, as a String in the format hh:mm
* toString() - Returns the date in the format mm/dd/yyyy
**/
import java.time.LocalDateTime;

public class SaarujanDate {
	private byte day, month; //Storing the day and month of the Date, as a byte; the maximum for these is only 31 and 12
 	private short year; //Storing the year of the Date, as a short

	public SaarujanDate() {
		day = 1; //Setting the default day to 1
		month = 1; //Setting the default month to January
		year = 2023; //Setting the default month to 2023
	}

	public SaarujanDate(byte day, byte month, short year) {
		this.day = day; //Setting the day to the given day 
		this.month = month; //Setting the month to the given month
		this.year = year; //Setting the year to the given year
	}

	public SaarujanDate(String format) {
		if (format.length() != 10) { //If the format string isn't the right length, the Date will be initialized with default values
			day = 1;
			month = 1;
			year = 2023;
			return; //Exiting the method
		}
		
		month = (byte) strToInt(format, (byte) 0, (byte) 2); //Taking the mm portion of mm/dd/yyyy
		day = (byte) strToInt(format, (byte) 3, (byte) 5); //Taking the dd portion of mm/dd/yyyy
		year = (short) strToInt(format, (byte) 6, (byte) 10); //Taking the yyyy portion of mm/dd/yyyy
	}

	private static int strToInt(String value, byte start, byte end) {
		int result = 0, multiplier = 1; //Result will store the integer result; multiplier determines the position of the digit
		for (byte i = (byte) (end - 1); i >= start; --i) { //Looping from the given end, till the given starting point
			if (value.charAt(i) < '0' && value.charAt(i) > '9') //If the value is not a number, -1 is returned
				return -1;
			
			result += (value.charAt(i) - '0') * multiplier; //Adding the digit at a specific position using the multipllier
			multiplier *= 10; //Moving to the next place in the number (Ex: tens to hundreds)
		}

		return result; //Returning the result
	}

	public byte getDay() {
		return day; //Returning the day
	}

	public byte getMonth() {
		return month; //Returning the month
	}

	public short getYear() {
		return year; //Returning the year
	}
	
	public static SaarujanDate currentDate() {
		SaarujanDate now = new SaarujanDate(); //Initializing a new Date
		LocalDateTime curr = LocalDateTime.now(); //Getting the current time
		now.day = (byte) curr.getDayOfMonth(); //Storing the current day in day
		now.month = (byte) curr.getMonthValue(); //Storing the current month in month
		now.year = (short) curr.getYear(); //Storing the current year in year 
		return now; //Returning the current date
	}

	public static String currentTime() {
		LocalDateTime curr = LocalDateTime.now(); //Storing the current date
		return String.format("%02d:%02d", curr.getHour(), curr.getMinute()); //Returning the time in the format hh:mm
	}

	public String toString() {
		return String.format("%02d/%02d/%04d", month, day, year); //Returning the date in the format mm/dd/yyyy
	}
	
}