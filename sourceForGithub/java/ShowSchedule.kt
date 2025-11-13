package com.dances.dances

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import com.dances.danceschedule.com.example.dancesapi29.danceList
import net.iowasquaredance.schedule.BuildConfig
import net.iowasquaredance.schedule.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*
import java.util.Calendar
import java.util.Date

// Format the date of a dance as mm/dd/year
private fun dateAsNumbers (dance: Dance) : String
{
    return  dance.danceMonth.toString() + "/" +
            dance.danceDay  .toString() + "/" +
            dance.danceYear. toString()
}


// Is the date provided earlier than today?
fun isInThePast (y: Int, m:Int, d:Int = 1) : Boolean
{
    try {
        val now = LocalDate.now()
        val event = LocalDate.of(y, m, d)
        return event < now
    } catch (e:Exception) {
        // return false
    }
    return false
}

// The background color behind the day varies according to month.
private fun dayColor (year: Int, month: Int, day: Int) : Color
{
    // Turn invalid months into white.
    if (month <1 || month>12 ) { return Color.White  }

    val startColors = arrayOf(  // For 2D, don't use floatArrayOf
        floatArrayOf(360F, 70F, 90F), // January
        floatArrayOf( 60F, 70F, 90F), // February
        floatArrayOf(240F, 70F, 90F), // March
        floatArrayOf(120F, 70F, 90F), // April
        floatArrayOf(280F, 70F, 90F), // May
        floatArrayOf(180F, 70F, 90F), // June
        floatArrayOf( 90F, 70F, 90F), // July
        floatArrayOf(250F, 78F, 90F), // August
        floatArrayOf(140F, 70F, 90F), // September
        floatArrayOf( 40F, 60F, 80F), // October
        floatArrayOf(330F, 70F, 90F), // November
        floatArrayOf(  0F, 70F, 90F)  // December (make grayscale)
    )

    val month0origin = month-1
    val day0origin = day-1

    val hue = startColors[month0origin,][0]
    val saturation =  1.0F
    // Slightly vary the day color within the month.
    val lightnessStart = startColors[month0origin][1]
    val lightnessDailyChange = (startColors[month0origin][2] - startColors[month0origin][1]) / 31F
    val lightnessForDay = lightnessStart + (lightnessDailyChange * day0origin)

// https://developer.android.com/reference/androidx/core/graphics/ColorUtils
    val cc = floatArrayOf(hue, saturation, lightnessForDay/100F)
    var colorInt = ColorUtils.HSLToColor(cc)
    // For December, use the background color which is probably light grey.
    if (month0origin>=11) colorInt = colorInt and 0xFFFFFF
    return  Color(colorInt)
}

// Return the color of this day unless in the past.

private fun dayColorOrPastColor (year: Int, month: Int, day: Int) : Color
{
    return if (isInThePast(year, month, day))
        Color.Gray else
        dayColor(year, month, day)

}
// Put a string on one line, downsizing the font if necessary to fit.
// https://medium.com/@ravishankar.ahirwar/jetpack-compose-now-supports-auto-sizing-text-with-autosize-in-basictext-5f82c1f74a7b
// https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/TextAutoSize
@Composable
fun OneRowString (
    t: String,
    maxFont: TextUnit = 28.sp,
    placement: Arrangement.Horizontal = Arrangement.Start,
)
{
    //   val placement = Arrangement.Start
    if (t.isNotBlank()) {
        Row(
            horizontalArrangement = placement,
            modifier = Modifier
                //       .background(Color.Cyan)
                .fillMaxWidth(1F)  // The row spans the Card horizontally.
        ) {
            // https://medium.com/@ravishankar.ahirwar/jetpack-compose-now-supports-auto-sizing-text-with-autosize-in-basictext-5f82c1f74a7b
            BasicText(
                text = t,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(maxFontSize = maxFont
                )
            )
        }
    }
}

@Composable
private fun ShowScheduleMetadata (d: List<Dance>)
{
    val dc = d.count()
    val firstDate = dateAsNumbers(d.first())
    val lastDate =dateAsNumbers(d.last())
    val danceCounts = "$dc dances from $firstDate to $lastDate"
    OneRowString(danceCounts, 26.sp, placement = Arrangement.Center)
}

enum class screens {
    SCHEDULE,
    DETAILS,
    DIRECTIONS,
    ABOUT,
}

var currentDance: Dance = Dance(2025, 1, 1, 1)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowSchedule() {
    val activity = LocalActivity.current as Activity

    // Ask compose to make a variable that it monitors and which
    // does not change upon every call to this function.  (like static in C).
    var scheduleScreenSwitch = remember { mutableStateOf(0) }
 /*   val nextScreenRequest = fun (next: screens) {
        scheduleScreenSwitch.value=next
    } */
//    val mut = remember { mutableStateOf(nextScreenRequest) }
    // What to show, the full schedule or app info?
    if (scheduleScreenSwitch.value == 2) {
        ShowAboutApp( appAboutDismissLamda =   { scheduleScreenSwitch.value = 0 })
        // how did this work without a return here?
        return
    }
    // Add to switch to offline map image.
    else
    {
        if (scheduleScreenSwitch.value == 3)
        {
            ShowDirections   ( directionsDismissLamda = { scheduleScreenSwitch.value=0}, currentDance.directions)
            return   // this gets me to the offline map function.  but why not needed above?
        }
    }

    // Think of what follows as an ELSE of the previous IF.  Its THEN does not flow through to here.
    val buttonTextSize = 18.sp
    val buttonTextWeight = FontWeight.Bold

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        //          color = MaterialTheme.colorScheme.background
    )
    {
        Column(
            // Centers the top buttons.
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                //          .background(Color.Gray)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            /*****************
             * Get a list of all dances.
             * Hopefully, all errors will have been caught by
             * the danceList function.  At minimum, one Dance
             * object will be returned, even if an error occurred.
             */
            val allDances = danceList()
            ShowScheduleMetadata(allDances)

            Row (
                horizontalArrangement = Arrangement.Center,

                //       verticalAlignment = Alignment.Bottom,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
          //          .background(color = Color.DarkGray)
                    .fillMaxWidth()
           // no effect         .clip(shape = RoundedCornerShape(20.dp))
                )
            {
               OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.DarkGray,
                        //                    contentColor = Color(0XFF01800A)
                    ),
                    onClick =
                        {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "message/rfc822"
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("iowasquaredance@proton.me"))
                                putExtra(Intent.EXTRA_SUBJECT, "Dance app")
                                putExtra(Intent.EXTRA_TEXT, "comments about the app...")
                            }

                            activity.startActivity(Intent.createChooser(intent, "Send Email"))

                        }
                )
                {
                    Text(
                        "Contact",
                        fontSize = buttonTextSize,
                        fontWeight = buttonTextWeight
                    )
                }

                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.LightGray,
                        contentColor = Color.DarkGray,
                        //               contentColor = Color(0XFF01800A)
                    ),
                    // Ask for the "About" screen.
                    onClick = { scheduleScreenSwitch.value = 2 }
                )
                {
                    Text(
                        "About",
                        fontSize = buttonTextSize,
                        fontWeight = buttonTextWeight
                    )
                }
            }

            // LazyColumn is very important.  It implements scrolling behavior for dances.
            LazyColumn(
                Modifier
                    // This keeps the schedule away from the top and bottom parts of the screen.
                    .windowInsetsPadding(WindowInsets.safeDrawing),

                // This keeps a left and right margin so schedule is not on the curved glass sides.
                contentPadding = PaddingValues(horizontal = 8.dp),
                //      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            )
            {

//  complains about @Composable              val allDances = danceList()
                var monthHeader = 0

                /*************************
                 * ITERATE DANCES...
                 */
                for (d in allDances)
                {
                    val thisMonth = 12*d.danceMonth+d.danceYear  // Change if year or month changes
                    if (thisMonth != monthHeader)
                    {
                        stickyHeader { MonthCard(
                            d,
                            modifier = Modifier.padding(vertical = 1.dp))
                        }
                        monthHeader = thisMonth

                    }
                    item { DanceCard(
                        modifier = Modifier.padding(vertical = 1.dp),
                        dance = d,
                        mainCardSwitch = scheduleScreenSwitch
                     //   danceCardSwitch = danceCardSwitch
                    )

                    }
                }
            }
        } // of schedule display column
    } // of surface
}


@Composable
private fun ShowOneDance(dance: Dance, danceCardSwitch: MutableState<Int>) {
     // Is this dance cancelled?  If so, change the text to strikethrough.
     val cancelOrNot = if (dance.appearance == Appearance.CANCEL)
        TextStyle(textDecoration = TextDecoration.LineThrough) else
        TextStyle(textDecoration = TextDecoration.None)

    val bc = if (dance.appearance == Appearance.CANCEL ||
                 dance.appearance == Appearance.ERROR)
                    Color.Red else Color.DarkGray

    val card_background_color =
        if (isInThePast(dance.danceYear, dance.danceMonth, dance.danceDay) &&
            dance.appearance != Appearance.ERROR)
            Color.Gray else MaterialTheme.colorScheme.surfaceVariant

    Card(border = BorderStroke(2.dp,bc),
        colors = CardDefaults.cardColors(
            containerColor = card_background_color ),
                modifier = Modifier
     // ? This does not set the color       .background(color = Color.DarkGray)
            .clickable (
// This google search is helpful:  jetpack onclick contains call to composable
// https://stackoverflow.com/questions/78166970/call-a-composable-function-from-an-onclick-that-is-in-a-composable/78166990#78166990https://stackoverflow.com/questions/78166970/call-a-composable-function-from-an-onclick-that-is-in-a-composable/78166990#78166990
            /* This assignment statement deserves a long explanation...
            *  This program uses a GUI library named Jetpack Compose.  It's attraction for me is
            *  the absence of .xml to format screens.  Another (mostly) advantage is that
            *  a change to the data being displayed triggers an update to the associated screen.
            *  This dataset is called the "context".
            *
            *  How is this implemented underneath?  In source, "@Composable" is an "annotation" attached to
            *  functions that lay out screen content.  The compiler generates additional code to make
            *  the screen visible and keep that screen consistent with its context.
            *  That is the reason for an important rule:  a composable function can only be called
            *  from another composable function.
            *
            *  The code block invoked by onClick does not have the composable attribute.
            *  This unfortunately, even if the caller is annotated with @Composable.
            *  Therefore you can't put a call to something composable like AlertDialog in onClick!
            *
            *  So the time sequence associated with touching the "DanceCard" is:
            *  - onClick runs.  It sets a flag in the context to request the
            *    dance details screen the next time DanceCard is called.
            *  - changing that value alerts the underlying compose code that it should
            *    go ahead and call DanceCard again.
            *  - so this next time danceCardSwitch.value = 1 and the dance details are shown.
            *
            *  This convoluted flow is, according to stackoverflow, just the way it's done.
            * */
            onClick = { danceCardSwitch.value = 1  }
        )
    )
    {

        // Show the word cancelled if this is a cancellation notice.
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()  // rows extend to right screen edge.
                .background(Color.Yellow)
                .padding(0.dp)

        ) {
            if (dance.appearance == Appearance.CANCEL) {
                Text(
                    text = "CANCELLED!",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    fontSize = 32.sp,
              //      autoSize = TextAutoSize.StepBased(maxFontSize = 32.sp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            else    if (dance.appearance == Appearance.ERROR) {
                    Text(
                        text = "APP ERROR!",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 32.sp,
                        //      autoSize = TextAutoSize.StepBased(maxFontSize = 32.sp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()  // rows extend to right screen edge.
    // only use to see child widget positioning                    .background(Color.Yellow)
              //  .padding(0.dp)
        ) {
            // Make the day of month on top and the day of the week underneath.
            // Limit the column's width to the size needed for "Wed" using widthSP = w.dp
            // Make the width of the two rows match that size using .fillMaxWidth()
            // Center within the row using horizontalArrangement = Arrangement.Center
            // Set color using .background(daycolor)

            val fs = 28.0                  // * Font size desired.
            val w = (0.6f*fs*3F).toInt()   // * Make a value representing
            val widthSP = w.dp             // * size needed for three characters.

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 4.dp)  // Left letter of day of week not touching border.
                    .width(widthSP)

            )
            {
                val daycolor = dayColorOrPastColor(dance.danceYear, dance.danceMonth, dance.danceDay)

                Row(  // for the numeric dance day of the month
                    horizontalArrangement = Arrangement.Center ,
                    modifier = Modifier
                        .background(daycolor)
                        .fillMaxWidth()
            // no effect            .align(Alignment.CenterHorizontally)
                )
                {
                    // Show the day (numeric) of the dance.
                    // Use a monospace font to take up the same amount of
                    // space for two digits.
                    Text(
                        text = dance.danceDay.toString() /*.padStart(
                            2,
                            ' '
                        )*/,
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = cancelOrNot,
                        textAlign = TextAlign.Center
                    )
                }

                Row(  // for the day of the week
                    horizontalArrangement = Arrangement.Center ,

                    modifier = Modifier
                    //                 .background(Color.Red),
            //            .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .background(daycolor)
                ) {
                    Text(
                        modifier = Modifier
                            .padding(all = 0.dp), // Has some effect but still too much space around the text.
                        text = dance.dayOfWeek.substring(0, 3),
                        fontSize = 24.sp,
                        style = cancelOrNot,
                    )
                }
            }
            Column (
                modifier = Modifier
                //            .background(color = Color.Blue),

            ){  // for the club image
                val logo_to_show = if (dance.appearance != Appearance.CANCEL) dance.club_logo else "ohmyface"
                /* YIKES!  somehow this is breaking screen scrolling backwards !! ?? */
               DanceInformationImage(danceInfoType.CLUB_LOGO, logo_to_show, 70.dp)

            }
            Column(  // for the club name and city
                modifier = Modifier
       //             .background(color = Color.Blue),
            )
            {
                BasicText(
                    text = dance.club_name,
                    maxLines = 1,
                    style = cancelOrNot,
                    autoSize = TextAutoSize.StepBased(maxFontSize = 28.sp)
                )

                Row(
                    verticalAlignment = Alignment.Bottom   // line up different fonts to bottom.
                ) {  // for the dance time and city
                    val danceTime =
                        " " + dance.danceStartHour.toString() + ":" + dance.danceStartMinute.toString()
                            .padStart(2, '0')
                    Text(text = danceTime, fontSize = 24.sp, style = cancelOrNot)
                    Text(text = " in ", fontSize = 20.sp,)
                    Text(
                        text = dance.city,
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = cancelOrNot,
                    )
                }
            }
        }
        // Truncate information if this dance already happened or is cancelled.
        if ( ! isInThePast(dance.danceYear, dance.danceMonth, dance.danceDay)
            && dance.appearance != Appearance.CANCEL)
        {
            var talent = dance.caller
            if (dance.cuer.isNotBlank()) talent = dance.caller + " / " + dance.cuer
            BasicText(
                text = talent,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(maxFontSize = 28.sp),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Is there a comment line?
            if (dance.comment.isNotBlank())
                Text(
                    text = dance.comment,
                    fontSize = 24.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    overflow = TextOverflow.Ellipsis
                )
        }
    }
}

@Composable
private fun DanceCard(
    dance: Dance, modifier: Modifier, mainCardSwitch: MutableState<Int>)
 {
    // Ask compose to make a variable that it monitors and which
    // does not change upon every call to this function.  (like static in C).
    val danceCardSwitch = remember { mutableStateOf(0) }

    // What to show, the full schedule or the details of one dance?
    if (danceCardSwitch.value==3) mainCardSwitch.value=3  // pass the switch request up.

    if (danceCardSwitch.value == 1) {
     /*   ShowDanceDetails(
            // Pass a function that is run when the user clicks OK or outside the details box.
            danceDetailsDismissLamda = {
                // Change the static variable to reshow the dance schedule.
                danceCardSwitch.value = 0
                               },
            dance = dance,
            danceCardSwitch) */
         ShowDanceDetails(
            // Pass a function that is run when the user clicks OK or outside the details box.
            danceDetailsDismissLamda = {
                // Change the static variable to reshow the dance schedule.
                danceCardSwitch.value = 0
                               },
            dance = dance,
            danceCardSwitch)
   }

    // Think of what follows as an ELSE of the previous IF.  Its THEN does not flow through to here.
    ShowOneDance(dance, danceCardSwitch)
}


@Composable
private fun MonthCard(dance: Dance, modifier: Modifier) {

    /* The month row can have one of three colors:
       - if month is this month or later, the color of the first day of this month.
       - if month is in the past, grey.
       - but red if something is very wrong.
     */

    // Find the first day of the current month.
    var colorOfCard = dayColor (dance.danceYear, dance.danceMonth, 1)

    // Change color for months preceding this one...
    try {
        val firstOfNowMonth = LocalDate.of(LocalDate.now().year, LocalDate.now().month, 1)
        val firstOfDanceMonth = LocalDate.of(dance.danceYear, dance.danceMonth, 1)
        if (firstOfNowMonth > firstOfDanceMonth) colorOfCard = Color.Gray
    } catch (e: Exception) { colorOfCard = Color.Red}

    Card(border = BorderStroke(2.dp,Color.DarkGray),
        modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()  // rows extend to right screen edge.
                .background(colorOfCard)
        ) {
            Text(dance.danceMonthText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(text = dance.danceYear.toString(), fontSize = 28.sp)
        }
    }
}

// Format and put dance information onto a Jetpack compose "Card".

@Composable
private fun ShowDanceDetailsCard(
    danceDetailsDismissLamda: (next: Int) -> Unit,  // Kotlin for dummies:  this is a function pointer.
    dance: Dance,
    screenSwitcher: MutableState<Int>) {
    val activity = LocalActivity.current as Activity

    currentDance = dance  // Set global to the dance being shown.

    Card(
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
    )
    {

    Row (
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            //    .background(Color.DarkGray)
            .fillMaxWidth(1F)  // The row spans the Card horizontally.
        //   .padding(12.dp)  // Add space above and below the Text.

    )
    {
        DanceInformationImage(danceInfoType.CLUB_LOGO, dance.club_logo)

        // Map the levels code string to an image...
        val levelsINT = arrayOf(
            R.drawable.mainstream,
            R.drawable.mainstream,
            R.drawable.plus,
            R.drawable.mainstream_plus,
            R.drawable.rounds_one,
            R.drawable.mainstream_rounds,
            R.drawable.plus_rounds,
            R.drawable.mainstream_plus
        )

        // Map the levels code string to a textual description...
        val levelText = arrayOf(
            "",
            "Mainstream",
            "Plus",
            "Mainstream, Plus",
            "Rounds",
            "Mainstream, Rounds",
            "Plus, Rounds",
            "Mainstream, Plus, Rounds"
        )
        // Make a 3-bit int according to the letters M, P, and R.
        var levelsSelector = 0
        val levelsCodeLetters = dance.levelsCode.uppercase()

        if (levelsCodeLetters.indexOf('M') >=0) levelsSelector = levelsSelector or 0x1
        if (levelsCodeLetters.indexOf('P') >=0) levelsSelector = levelsSelector or 0x2
        if (levelsCodeLetters.indexOf('R') >=0) levelsSelector = levelsSelector or 0x4

        val levels_logo_handle = levelsINT[levelsSelector]
        val levelCodeInterpreted = levelText[levelsSelector]

        // Column for caller and name underneath...
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .align(Alignment.CenterVertically)
        )
        { // Levels icon (boxes, circles)
            Image(
                //       painter = it,
                painter = painterResource(id = levels_logo_handle),
                // correct but hard coded name                        painter = painterResource(id = R.drawable.ankeny),
                contentDescription = levelCodeInterpreted,
                // no, want the default of .Fit    contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(100.dp)
                    .width(100.dp)
                    .padding(8.dp)
                    .align(Alignment.CenterHorizontally)
                //            .fillMaxWidth()
                //          .background(Color.Green)
                //    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                levelCodeInterpreted, textAlign = TextAlign.Center,
                modifier = Modifier
                    //      .height(120.dp)
                    .align(Alignment.CenterHorizontally)
                //     .fillMaxWidth()  // important
            )
        } // of column

    } // of Row

    HorizontalDivider(thickness = 4.dp, color = Color.DarkGray)

    OneRowString(dance.club_name, placement = Arrangement.Center)
    Spacer(Modifier.height(6.dp))
    OneRowString(dance.address1, 26.sp, placement = Arrangement.Center)
    OneRowString(dance.address2, 26.sp, placement = Arrangement.Center)
    OneRowString(dance.address3, 26.sp, placement = Arrangement.Center)
    OneRowString(dance.city, 26.sp, placement = Arrangement.Center)
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(thickness = 4.dp, color = Color.DarkGray)

    val date_line = dance.dayOfWeek + "  " +
            dance.danceMonthText + dance.danceDay.toString() + " " +
            dance.danceYear
    OneRowString(date_line, 26.sp, placement = Arrangement.Center)

    val danceTime = " " + dance.danceStartHour.toString() +
            ":" + dance.danceStartMinute.toString().padStart(2, '0') +
            " to " + dance.danceEndHour.toString() +
            ":" + dance.danceEndMinute.toString().padStart(2, '0')

    OneRowString(danceTime, 26.sp, placement = Arrangement.Center)
    OneRowString(dance.cost, 26.sp, placement = Arrangement.Center)
    OneRowString(dance.comment, 26.sp, placement = Arrangement.Center)
    OneRowString(dance.contact, 26.sp, placement = Arrangement.Center)
    Spacer(Modifier.height(10.dp))

    HorizontalDivider(thickness = 4.dp, color = Color.DarkGray)

    // Caller and cuer..
    if (dance.appearance != Appearance.ERROR) {
        ShowTalent(dance.callerPhoto, dance.caller,
            dance.cuerPhoto,   dance.cuer)
    }

    // Must put buttons in a row, otherwise they overlap.
    Row (
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        // Center the buttons (subtle because they almost fill the card).
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    {
        val buttonTextSize = 18.sp
        val buttonTextWeight = FontWeight.Bold

        OutlinedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray,
                contentColor = Color.Black
            ),
            // original        onClick = { onDismissRequest() }
            onClick = {
                // Yuck, Android Studio warns
                // "Try catch is not supported around composable function invocations."
                //          val isMapIntentAvailable = mapIntent.resolveActivity(LocalContext.current.packageManager)
                try {
                    // https://developers.google.com/maps/documentation/urls/android-intents#kotlin
//                              val gmmIntentUri = Uri.parse("geo:" + dance.geo + "?z=16")
                    val gmmIntentUri =
                        Uri.parse("geo:0,0?q=" + dance.geo + "(" + dance.address1 + ")")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    activity.startActivity(mapIntent)
                    //   /* this works to get to the catch  */   throw IOException ("deliberate to test catch")
//original                    onDismissRequest()
                //    screenSwitcher.value = 3  // here just for testing error handling.
                } catch (e: Exception) {
                    screenSwitcher.value = 3  // ask for backup map screen.
                }
            }
        )
        { Text("Map", fontSize = buttonTextSize, fontWeight = buttonTextWeight) }

        OutlinedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray,
                contentColor = Color.Black
            ),
            onClick = {
                //    onDismissRequest()
                // Send dance day to whatever calendar app might be available in this phone.
                // https://developer.android.com/identity/providers/calendar-provider#intents
                // Although I find no documentation, apparently months are 0 origin.
                // Assume all times given are PM.
                val startMillis: Long = Calendar.getInstance().run {
                    set(
                        dance.danceYear, dance.danceMonth - 1, dance.danceDay,
                        dance.danceStartHour + 12, dance.danceStartMinute
                    )
                    timeInMillis
                }
                val endMillis: Long = Calendar.getInstance().run {
                    set(
                        dance.danceYear, dance.danceMonth - 1, dance.danceDay,
                        dance.danceEndHour + 12, dance.danceEndMinute
                    )
                    timeInMillis
                }
                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    .putExtra(CalendarContract.Events.TITLE, "Dance")
                    .putExtra(CalendarContract.Events.DESCRIPTION, dance.club_name)
        // just shows a comma            .putExtra(CalendarContract.Events.EVENT_LOCATION, dance.geo)
            //        .putExtra("lat", "40.711096")
               //     .putExtra("long", "-91.230198")
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, dance.city)
                activity.startActivity(intent)
            }
        )
        { Text("Reminder", fontSize = buttonTextSize, fontWeight = buttonTextWeight) }

        OutlinedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray,
                contentColor = Color(0XFF01800A)
            ),
            onClick = { danceDetailsDismissLamda(0) }
        )
        { Text("OK", fontSize = buttonTextSize, fontWeight = buttonTextWeight) }

    }
    }  // of Row lambda
    //  }  // of body of Column lambda
}  // of Card lambda



// Return true if the phone is positioned as portrait, false if landscape.

@Composable
fun inPortrait () : Boolean
{
    // Fetching current app configuration
    val configuration = LocalConfiguration.current

    // When orientation is Landscape
    when (configuration.orientation)
    {
        Configuration.ORIENTATION_LANDSCAPE -> { return false }

        // Portrait orientation
        else -> { return true }
    }
}

/*
    Show all available information about a dance.

    For most of this app, Android handles screen contents when the phone is
    held in portrait position or landscape position.  However when dance details
    are being shown, a Dialog is best for Portrait but is not suited for
    landscape.  This function implements a different technique for landscape.
 */

@Composable
fun ShowDanceDetails (
    danceDetailsDismissLamda: (next: Int) -> Unit,  // Kotlin for dummies:  this is a function pointer.
    dance: Dance,
    screenSwitcher: MutableState<Int>)
{
    if (inPortrait())
    {
        Dialog(onDismissRequest = { danceDetailsDismissLamda(0) })
        {
            ShowDanceDetailsCard(danceDetailsDismissLamda, dance, screenSwitcher)
        }
    }

    // For landscape, do not use a dialog box.
    else ShowDanceDetailsCard(danceDetailsDismissLamda, dance, screenSwitcher)
}

@Composable
fun ShowAboutApp (
    appAboutDismissLamda: (next: Int) -> Unit,
    )
{

Dialog(onDismissRequest = { appAboutDismissLamda(0 /* next screen */) })
{
val danceCardContext = remember { mutableStateOf(false) }
Card(
shape = RoundedCornerShape(4.dp),
modifier = Modifier
    .fillMaxWidth()
//    .height(600.dp)  // if absent, card grows to accommodate contents
)
{
OneRowString("Version 1.0", placement = Arrangement.Center)
//OneRowString(BuildConfig.BUILD_TIME, placement = Arrangement.Center)
OneRowString("Although this schedule is correct it ")
OneRowString("does not yet update automatically.", placement = Arrangement.Center)
OneRowString("Please send comments, club", placement = Arrangement.Center)
OneRowString("logos, and photos to James at", placement = Arrangement.Center)
OneRowString("iowasquaredance@proton.me", placement = Arrangement.Center)
Spacer(Modifier.height(6.dp))

    // Prepare build-time for display...
    val currentMillis = BuildConfig.TIMESTAMP  // from build.gradle.kts which generates BuildConfig.java
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    val dateString = dateFormat.format(Date(currentMillis))// Must put buttons in a row, otherwise they overlap.
OneRowString(dateString, placement = Arrangement.Center)

    Row (
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.Bottom,
    // Center the buttons (subtle because they almost fill the card).
    modifier = Modifier.align(Alignment.CenterHorizontally),
)
{
    val buttonTextSize = 18.sp
    val buttonTextWeight = FontWeight.Bold

   OutlinedButton(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.LightGray,
            contentColor = Color(0XFF01800A)
        ),
        onClick = {  appAboutDismissLamda(0 /* want to set next screen */) }
    )
    { Text("OK", fontSize = buttonTextSize, fontWeight = buttonTextWeight) }


}  // of Row lambda
//  }  // of body of Column lambda
}  // of Card lambda
}  // of Dialog
}  // of function



@Composable
fun ShowDirections (
    directionsDismissLamda: (next: Int) -> Unit,
    filename: String,
    )
{
    Dialog(
        onDismissRequest = { directionsDismissLamda(0 /* next screen */) },
        // This expands the width of the Dialog.  Important in landscape orientation.
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
    {
        Card(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .padding(20.dp).fillMaxWidth()
        )
        {
            val toResources = LocalContext.current.resources
            val fileNameOfDirections =  "maps/$filename" //"maps/ankeny.txt" //
            val openResult = runCatching() {
                toResources.assets.open(fileNameOfDirections)
                    .bufferedReader()
                    .use { it.readLines() }
            }

        val directionsInList = openResult.getOrNull()

        if (directionsInList != null) {
            for (line in directionsInList)
            {
                // Regex from AI assist:  "kotlin convert string to sentences"
                val sentences = line.split(Regex("(?<=[.!?])\\s*"))
                for (s in sentences)
                    BasicText(s,autoSize = TextAutoSize.StepBased(maxFontSize = 24.sp))
            }
         } else BasicText("No directions available for $fileNameOfDirections" )

 //       Spacer(Modifier.height(6.dp))

        // Must put buttons in a row, otherwise they overlap.
        Row (
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
            // Center the buttons (subtle because they almost fill the card).
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
            {
                val buttonTextSize = 18.sp
                val buttonTextWeight = FontWeight.Bold

               OutlinedButton(
                colors = ButtonDefaults.buttonColors(
                containerColor = Color.LightGray,
                contentColor = Color(0XFF01800A)
        ),
        onClick = {
            directionsDismissLamda(2 /* want to set next screen */)
        }
    )
    { Text("OK", fontSize = buttonTextSize, fontWeight = buttonTextWeight) }


}  // of Row lambda
//  }  // of body of Column lambda
}  // of Card lambda
}  // of Dialog
}  // of function

