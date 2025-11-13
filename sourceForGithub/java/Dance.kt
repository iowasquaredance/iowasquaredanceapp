package com.dances.dances

import java.time.DateTimeException
import java.time.LocalDate
import java.util.Locale

val months = listOf("January ", "February ", "March ", "April ",
    "May ", "June ", "July ", "August ",
    "September ", "October ", "November ", "December ")

// This enumeration specifies the way a dance event is rendered.
enum class Appearance {
    DEFAULT,   // A dance event entry as originally posted.
    NOTICE,    // Emphasize a change to the event generally, possibly for multiple changes.
    DATE,      // Change the date.
    TIME,      // Change the time.
    PLACE,     // Change the location
    CALLER,    // Change the caller.
    CUER,      // Change the cuer.
    COMMENT,   // Change the comment.  Can add a comment if not originally provided.
    LEVEL,     // Change the level, z.B. will not dance plus.
    COST,      // Change the cost line.
    CANCEL,    // Cancellation.
    ERROR,     // App error.
}

val ordinalTest: Int = Appearance.ERROR.ordinal

// This class is a square dance event, a meeting of people who dance together at a specific time,
// sponsored by a club, under the direction of leaders referred to as "callers" and "cuers".

class Dance// Dance data from the server should be perfect.  Even so, tolerate some errors...

/*
Find the day of the week corresponding to the date.  Make it a lower case string.
Capitalize the first letter.
Tolerate an invalid day of the month, z.B. April 31st.
Supplied by server for key.  Unique, not 0, not necessary sequential or ascending.
This may specify more than one person who together call the dance.
Similarly, this may specify more than one person, possibly using a business name.
Special dances, for example "Beach Party".

Full confession:  I don't understand these constructors.  This did not work:
            club_logo = danceProperties[9],
even though the debugger showed the right side present.
It has something to do with val and var and         this.club_logo = club_logo

*/

    (
    val key: Int = 0,// (maybe) Every dance should have a unique sequence number.
    var danceYear: Int,
    var danceMonth: Int,
    var danceDay: Int,
    beginHour: Int = 0,
    beginMinute: Int = 0,
    endHour: Int = 0,
    endMinute: Int = 0,
    val club_name: String = "(club name)",
    val club_logo: String = "couple",
    levelsCode: String = "",
    var address1: String = "",
    var address2: String = "",
    var address3: String = "",
    var city: String = "",  // Can include a state.
    val directions: String = "",
    val geo: String = "",
    var caller: String = "(Call ahead)",
    var callerPhoto: String = "",
    var cuer: String = "",
    var cuerPhoto: String = "",
    var comment: String = "",
    var cost: String = "",
    var contact: String = "",
    var appearance: Appearance = Appearance.DEFAULT
) {
    var danceMonthText: String
    var dayOfWeek: String   // Variable to permit exception handling (in Constructor)
    var danceStartHour: Int = beginHour
    var danceStartMinute : Int = beginMinute
    var danceEndHour: Int = endHour
    var danceEndMinute : Int = endMinute
//    var club_logo: String = ""
    var levelsCode: String = ""
//    val levels_logo: String
   // now directions var map_filename: String   = "" //  File with image of map to location.

    init {
//        this.club_logo = club_logo
        this.levelsCode = levelsCode.uppercase()
    //    this.map_filename = map_filename.lowercase()
  //      this.callerPhoto = callerPhoto
    //    this.cuerPhoto = cuerPhoto

        // Make the day of week text...
        try {
            val dw = (LocalDate.of(danceYear, danceMonth, danceDay).dayOfWeek).toString().lowercase()
            this.dayOfWeek = dw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } catch (e: DateTimeException) {
            this.dayOfWeek = "???"
        }

        // Make month text.
        if (danceMonth in 1..12) {
            danceMonthText = months[danceMonth - 1]
        } else {
            danceMonthText = "! Invalid month = "  + danceMonth.toString() + " ? "
        }
    }
}