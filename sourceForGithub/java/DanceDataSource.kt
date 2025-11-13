package com.dances.danceschedule.com.example.dancesapi29

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dances.dances.Appearance
import com.dances.dances.Dance
import java.time.LocalDate
import java.time.LocalTime


// Return a list of strings containing a single string with an error description.
// The (one) string looks like a spreadsheet row with three cells, .csv format.
private fun makeErrorRow (
    filename:String,
    message:String) : List<String>
{
    // If viewed on a spreadsheet these would be cells A1 B1 and C1.
    return listOf("ERROR,$filename,$message")
}


/* Return the rows of the .csv file of dances.
   Upon return, the contents are in RAM as a list of strings.
   The file is closed.

   Several errors are possible including a missing or empty file.
   Upon error this function generates a List<String> with one string
   containing one .csv row containing 3 cells that describe the error.
 */

//@Composable    // Required to get to the context.
private fun readSpreadsheetRows (myLocalContext: Resources) : List<String> {
    // This file is in the phone in the assets directory.
    // I do not know whether this is where FireBase will put the .csv !
    val fileNameOfSpreadsheet =  "schedule/summer2025.csv"
// handles ok    val fileNameOfSpreadsheet = "schedule/empty.csv"

    // Provides access to Android services.  Must be a composable function.
//    val myLocalContext = LocalContext.current.resources

    // Open and read the .csv file.  Could throw but runCatching catches any
    // exception and puts it into a Result object along with a successful result.
    val openResult = runCatching() {
        myLocalContext.assets.open(fileNameOfSpreadsheet)
            .bufferedReader()
            .use { it.readLines() }
    }

    // Did open succeed?  Get List of Strings or a null.
    val csvRowsInList = openResult.getOrNull()
 //   csvRowsInList = null  // Test proper handling of null

    if (openResult.isSuccess) {
        // Yes, however the reference might be null.
        if (csvRowsInList == null)
        {
            return makeErrorRow(fileNameOfSpreadsheet, "null returned from readlines()")
        }

        // Complain if the spreadsheet is empty.
        if (csvRowsInList.isEmpty())
        {
            return makeErrorRow(fileNameOfSpreadsheet, ".csv empty")
        }
        return csvRowsInList  // Placate compiler using !!

    } else {
        val problemDescription = openResult.exceptionOrNull().toString()
        return makeErrorRow(fileNameOfSpreadsheet,
            problemDescription
        )
    }
}

// This increments as a row is parsed.
var rowIX = 0

/*
 Find the first set of numeric digits and convert to an integer.
 Return both the value and the subscript past the last numeric digit.
 That subscript value can be 1 greater than the last valid subscript of the string.
 Not found is indicated by -1.
 */
fun findPositiveInteger (str: String, start: Int) : Pair<Int,Int>
{
    var ix = start
    var accumulator = 0
    var found = false

    // Find the first digit.
    while (ix<str.length && (!str[ix].isDigit())) ++ix

    // Maybe found a digit but maybe end of string.
    while (ix<str.length && ( str[ix].isDigit()))
    {
        // Convert to appropriate power of 10 and accumulate.
        val thisDigit = str[ix].digitToInt()
        accumulator *=10
        accumulator += thisDigit
        found = true
        ++ix
    }

    return if (found)  Pair(accumulator, ix) else Pair(-1, ix)
}

val monthMap = mapOf(
    "JAN" to 1,
    "FEB" to 2,
    "MAR" to 3,
    "APR" to 4,
    "MAY" to 5,
    "JUN" to 6,
    "JUL" to 7,
    "AUG" to 8,
    "SEP" to 9,
    "OCT" to 10,
    "NOV" to 11,
    "DEC" to 12
)
var dayFromDateRow  = 1
var monthFromDateRow = 1
var yearFromYearRow = LocalDate.now().year  // Year of dances.  Can change during parse of .csv

/* Take section of a spreadsheet .csv row, examine (only) the first cell,
   and return it as a list of strings.

   Also return the number of characters to the , terminating the cell, or
   the subscript of the end of the string if no , found.

   The first spreadsheet cell might contain "commas, with double quotes".
   In that case...
   "here, is an example of something returned as two strings",

   But usually,
   this is only one string,this is the second cell and not examine here

   And frequently,
   ,next cell
   Return one empty string and one character position consumed.

   Possible
   last cell
   Return one string with 9 characters (so now pointing past end of row)

 */

fun firstCellToStrings (c: String) : Pair<List<String>, Int>
{
    // Handle empty string.  Translate to "" but 0 characters consumed.
    if (c.isEmpty()) return Pair(listOf(""), 0)

    // A cell STARTing with comma is valid but empty.
    if ( c[0]==',') return Pair(listOf(""), 1)

    // Split the cell into pieces.  Cases...
    // one piece to this cell,
    // "two, or more pieces",
    val pieces = c.split(',', limit = 32)  // Limit should not be an issue.
    // Note that pieces returns everything in the row after the starting point,
    // not just one spreadsheet cell.

    // Handle a cell without "
    if (c[0] != '"') return Pair(listOf( pieces.first()), pieces.first().length+1)  // Point past terminating ,

    // " starting a cell signals a "cell, with quotes within"
    // Expand the "quoted, cell contents" to multiple strings.

    var charactersConsumed = 0
    var piecesWithoutDoubleQuotes = mutableListOf<String> ()
    var done = false   // Use to end parsing when terminating " found.

    for (piece in pieces)
    {
        charactersConsumed+=piece.length   // Cumulative length to return.
        ++charactersConsumed   // TEST:  one more for each piece.
        var pieceReturnCandidate = piece
        var pieceStart = 0
        var pieceEnd = piece.length

        // Strip a leading "    truncate a trailing "  possibly in same string.
        // (Calling indexOf is safer than piece[0]
        if (piece.indexOf('"') == 0 )
        {
            // Starts with ".  Omit ".
            pieceStart = 1
        }

        val trailingDoubleQuote = pieceReturnCandidate.indexOf('"', pieceStart)
        if (trailingDoubleQuote> -1)
        {
            // Remove the trailing "
            pieceEnd = trailingDoubleQuote
            done = true
        }
        piecesWithoutDoubleQuotes.add(piece.substring(pieceStart, pieceEnd))  // Perhaps, return piece as is, but...
        if (done) break
    }

    return Pair(piecesWithoutDoubleQuotes, charactersConsumed)
}

// Each spreadsheet cell must be parsed into this number of strings.

val stringCountRequired =

    listOf(//  properties subscript          Spreadsheet column
        1, //  0: Federation abbreviation.   A
        1, //  1: Club name.                 B
        1, //  2: Levels code letters.       C
        4, //  3, 4, 5, 6: Address:  line 1, line2, line3, and city.  D
        1, //  7: Talent names: caller and cuer (one string using / to separate them).  E
        1, //  8: Dance time and optional comment (one string ideally hh:mm - hh:mm but often abbreviated)   F
        1, //  9: Club logo file name, (string without any path information).  G
        2, // 10 and 11: Club latitude and longitude (string as "latitude,longitude"). H
        1, // 12: Map file name in case online map service unavailable. I
        1, // 13: Caller photo file name, (string without any path information). J
        1, // 14: Cuer file name, (string without any path information). K
        1, // 15: Contact. L
        1, // 16: Cost (a string) M
    )

/* Separate the ,cells, of a .csv row into a list of strings.

   Quirk:
   Some spreadsheet cells (address and geo) contain embedded commas.
   These need to be broken up into individual strings so that they can be
   put onto the screen in a convenient manner.
   However, the single spreadsheet cell for an address might contain
   anywhere from one to four pieces, each going into a separate Dance "property"
   (C: "struct element"   Kotlin: "property").

   Solution:  for each spreadsheet cell, specify the number
   of strings that need to be returned for that spreadsheet cell.
   Pad with null strings if necessary to create the number required.
 */

fun oneRowToStrings (row: String) : List<String>
{
    var ix = 0  // Index through the .csv row string.
    var propertyStrings = mutableListOf<String> ()

    // Each spreadsheet cell must be expanded as necessary to this number of strings.
    // Iterate the minimum element counts to guarantee all are satisfied.
    for (sr in stringCountRequired) {

        // How many elements in this spreadsheet cell?
        // Maybe there is no cell at all.  In that case make a list with
        // one empty string.
        val oneCSVcell = if (ix<row.length)
            firstCellToStrings(row.substring(ix)) else
            Pair(listOf(""), 0)

        // The elements are returned as a Pair.
        // .first is a list of strings.
        // .second is a count of the characters of the row that were parsed.
        ix += oneCSVcell.second

        // Insert the number of strings required.
        // If not enough come from the spreadsheet, add empty ones
        // to meet the requirement.  Example:  IOOF Hall, Wever
        var element = 0
        repeat ( sr )  // Want this many strings in the list, even if empty.
        {
            if (element < oneCSVcell.first.size)
            {
                propertyStrings.add(oneCSVcell.first[element++])
            }
            else propertyStrings.add("")  // Add empty strings to fulfill the requirement.
        }
    }

    return propertyStrings
}

/*
    Parse the spreadsheet cells provided looking for a dance date.
    If found, set the globals above and return true.
    Return false if not found.
 */
private fun parseDateCells (cells: List<String>) : Boolean
{
    // Look for a day of week text such as MONDAY (but only check 3 characters).
    if (cells[1].isEmpty() || cells[1].isBlank() || cells[1].length<3) return false

    val dayText = cells[1].substring(0,3).uppercase()
    if (dayText!="FRI" && dayText!="SAT" && dayText!="SUN" &&
        dayText!="MON" && dayText!="TUE" && dayText!="WED" &&
        dayText!="THU") return false

    // Find day...
    var monthCell = cells[3].uppercase()
    var resultPair = findPositiveInteger(monthCell, 0)
    val dayNumber = resultPair.first

    if (dayNumber < 1 || dayNumber>31) return false // Error, why out of range?

    // Find the month...
    val indexFirstMonthLetter = monthCell.indexOfFirst { it in "JFMASOND" }
    if (indexFirstMonthLetter<0) return false

    val key = cells[3].substring(indexFirstMonthLetter, indexFirstMonthLetter+3).uppercase()
    if (! monthMap.containsKey(key)) return false

    val monthNumber = monthMap[key]
    if (monthNumber == null) return false
    if (monthNumber<1 || monthNumber>12) return false // reject bad month

    // Update the date of dances associated with this date...
    monthFromDateRow = monthNumber
    dayFromDateRow = dayNumber

    // There might also be a year
    resultPair = findPositiveInteger(monthCell, indexFirstMonthLetter)

    val yearIfPresent = resultPair.first // fourDigitInteger(monthCell)
    if (yearIfPresent>2000) yearFromYearRow = yearIfPresent
    return true
}


/*
    Convert a string of the form hh:mm to integers.
    Hours and minutes can be any number of digits (although > 2 would be dumb).
    A single number without a colon is treated as the start of an hour, (0 minutes).

    This function takes a starting subscript and returns the subscript of the
    character after the final numeric digit in pair.second.
    Note that this subscript is not necessarily the number of characters scanned
    because the subscript passed in ix can be any positive value.

    No checking here for values too large such as minute 61.
 */
private fun parseTime (tt: String, start: Int) : Triple<Int,Int,Int>
{
    var ix = start
    var conversionPair = findPositiveInteger(tt, ix)
    val hourCandidate = conversionPair.first
    ix = conversionPair.second
    // Was any number found?  -1 if not with index set 1 past end of string.
    if (hourCandidate<0) return Triple(-1, 0, ix)

    // Require a colon to be right after the hour.
    // If not present, the hour is found but not the minute.
    if (ix>=tt.length || tt[ix] != ':') return Triple(hourCandidate, 0, ix)

    conversionPair = findPositiveInteger(tt, ix)

    val minuteCandidate = conversionPair.first
    ix = conversionPair.second
    // Tolerate the absence of a minute even if a colon was found.
    if (minuteCandidate<0) return Triple(hourCandidate, 0, ix)
    return Triple(hourCandidate, minuteCandidate, ix)
}

/*
    Parse both the start and end times of a dance typically as hh:mm - hh:mm

    This function returns the number of characters scanned which will include
    anything skipped over before finding numeric digits, if any.
 */
private fun parseDanceStartEnd (tc: String, dance: Dance) : Int
{
    var ix = 0  // Subscript into string.  Increment as parse proceeds.

    var conversionTriple = parseTime(tc, ix)
    val maybeStartHour = conversionTriple.first
    val maybeStartMinute = conversionTriple.second
    ix = conversionTriple.third

    // No change to dance if the starting hour is not present.
    if (maybeStartHour<0) return ix

    // A dash is not required however if absent, the end time is assumed 0:0
    var maybeEndHour = 0
    var maybeEndMinute = 0

    // Assume the - is past the start time (not likely but not checked here).
    val dash = tc.indexOf('-')
    if (dash>=0) {
        // Expect, but do not require an end time.
        conversionTriple = parseTime(tc, ix)
        ix = conversionTriple.third

        // End time?
        if (conversionTriple.first != -1) {
            maybeEndHour = conversionTriple.first
            maybeEndMinute = conversionTriple.second
        }
    }

    // There is at least a starting hour.
    dance.danceStartHour = maybeStartHour
    dance.danceStartMinute = maybeStartMinute
    dance.danceEndHour = maybeEndHour
    dance.danceEndMinute = maybeEndMinute
    return ix
}




/* Put the details of the dance location into the Dance object.

  This program provides a dance location containing up to 4 details:
  1: Venue name, for example a church.
  2: A street address, usually something like 123 Main St.
  3: An optional second line of an address (not often provided), and
  4: A city (without comma state, so Morrison IL   not   Morrison, IL

  The (single!) cell of the spreadsheet .csv file containing the address is always
  split into 4 strings by function oneRowToStrings EVEN IF <4 of the
  above address details are not provided in the .csv row.  The first part of the
  address is always put into dance object at .address1  This is done by
  this function's caller.  Accordingly, this function only gets the remaining 3 details.

  Function oneRowToStrings makes a list of 4 strings in the order parsed.
  If only 3 address details are provided, the 4th string is empty "".
  If only 2 address details are provided, strings 3 and 4 are empty "". (IOOF HALL, Wever)
  If just 1 (June barn dance) strings 2, 3, and 4 are empty.

  This program identifies the city so that it can go onto the main screen.
  This function finds which detail string contains the city.
 */

private fun addAddressDetails (
    detail2: String,  // Often venue address but could be the city name, often Wever.
    detail3: String,  // The city is usually here but could be a second address line.
    detail4: String,  // If all three of the above are used the city is here.
    d:Dance)
{
    if (detail2.isBlank()) return   // Everything was in one spreadsheet cell.

    if (detail4.isNotBlank())
    {
        // All 4 details have info.
        d.city = detail4
        d.address3 = detail3
        d.address2 = detail2
    }

    else if (detail3.isNotBlank())
    {
        d.address2 = detail2
        d.city = detail3
    } else {
        // This happens when the address is just IOOF Hall, Wever
        d.city = detail2
    }
 }


/* Parse the spreadsheet containing the dance schedule.
 Here an overview of how a spreadsheet is converted into
a list of Dance objects...

  Then the role of this function...



 */

private fun parseSpreadsheetFile (file: List<String>) : List<Dance>
{
    val danceobjectlist = mutableListOf<Dance> ()

    // Iterate the rows of the .csv file.
    for (row in file)
    {
        // Make strings from every comma delimited part of a row.
        // Note that this does not necessarily correspond to spreadsheet
        // cells because "something, containing quotes" will be 2 strings even
        // though just one spreadsheet cell.
        val cells = row.split(',')

        // Ignore a row that is too short to even have a date line.
        if (cells.size<4) continue

        // Try to find a date.
        if (parseDateCells(cells)==true) continue  // If found, proceed to next row.

        // Maybe this row has a dance.
        // A good indicator is whether the first cell is empty.
        if (row[0] == ',') continue  // Do not parse if row begins with ,

        val danceProperties = oneRowToStrings(row)

        // Examine the strings.  One String can go into one property of the Dance object...
        assert(danceProperties.size == stringCountRequired.sum())
        {
            "List of dance properties != expected"
        }

        // Make a Dance beginning with some of the details.  Fill in more later.
        var d = Dance(
            danceYear = yearFromYearRow,
            danceMonth = monthFromDateRow,
            danceDay = dayFromDateRow,
            club_name =  danceProperties[1],
            levelsCode = danceProperties[2],
            address1 = danceProperties[3],
            club_logo = danceProperties[9],
            geo = danceProperties[10] + ',' + danceProperties[11],
            directions = danceProperties[12],
            callerPhoto = danceProperties[13],
            cuerPhoto = danceProperties[14],
            contact = danceProperties[15],
            cost = danceProperties[16],
         )

        // Put the pieces of the address into the dance.
        addAddressDetails(
            danceProperties[4],
            danceProperties[5],
            danceProperties[6],
            d
        )

        // Add times.  A dance comment can be included in the same cell.
        val commentIndex = parseDanceStartEnd (danceProperties[8], d)
        d.comment = danceProperties[8].substring(commentIndex)

        // Find the talent.
        val talentString = danceProperties[7]
        val slashPosition = talentString.indexOf('/')

        // A slash, if present separates caller and cuer.
        if (slashPosition<0) d.caller = talentString
        else
        {
            d.caller = talentString.substring(0, slashPosition)
            d.cuer = talentString.substring(slashPosition+1)
        }


        danceobjectlist.add(d)
    }

    return danceobjectlist
}

/* Return a List<Dance> with all dances or just one Dance with error info
   masquerading as a Dance.
 */
@Composable
fun danceList(): List<Dance>
{
    // Get the entire spreadsheet as a list of Strings.
    val csvrows = readSpreadsheetRows(LocalContext.current.resources)

    // The first row might have been generated as an error indication.
    val firstRow = csvrows.first()
    if (firstRow.startsWith("ERROR", true))
    {
        // Failed to open the .csv (spreadsheet) file with the dances.
        // The row has three spreadsheet cells, A1 B1 and C1.
        // Make one "Dance" which is really an error message.
        val now  = LocalDate.now()
        val time = LocalTime.now()

        // Split the error explanation into pieces...
        val commaOne = firstRow.indexOf(',', 0)
        val commaTwo = firstRow.indexOf(',', commaOne+1)
        val commaThree = firstRow.indexOf(',', commaTwo)
        val errorFromOS = firstRow.substring(commaThree+1)
        val fileWanted = firstRow.substring(commaOne+1, commaTwo)

        val errorDance = Dance(0,
            now.year, now.monthValue,  now.dayOfMonth,
            time.hour,time.minute, time.hour, time.minute,
            club_name = errorFromOS,
            club_logo = "sickface",
            city = fileWanted,
            contact = "Please email iowasquaredance@proton.me",
            caller = "Touch this for details",
            appearance = Appearance.ERROR)

        return listOf(
            errorDance)
    }

    // Parse the .csv file into Dance objects.  Put them in a List and return it.
    val allDances = parseSpreadsheetFile(csvrows)
    return  allDances
}
