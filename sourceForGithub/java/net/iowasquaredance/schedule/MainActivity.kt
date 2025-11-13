/*
 This is the top-level Kotlin file for the app.

 The identifier "DanceScheduleTheme" is somehow tied to the name of the
 Android Studio project.  Isolate code using that name in this file.

 The bulk of the app's code is in ShowSchedule.kt.  Why?  Because
 as of August 2024 the Google Play Store requires new apps to run over
 Android 14 (API level 34).  But as of May 2025, only 43% of phones are
 at that level.  Therefore I want two version of the app, one that can
 be published by Google but another at a lower level for dancers with
 older phones.
 */
package net.iowasquaredance.schedule


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dances.dances.ShowSchedule
import net.iowasquaredance.schedule.ui.theme.DancesAPI29Theme

// I keep this short to isolate some uses of "dancesapi29".

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Here instead of using "trailing lamda" syntax shortcut, I explicitly
        // code that "content" is a formal parameter of "setContent" and that
        // it takes a function pointer (lamda).
        setContent  ( // Warning:  compiler does not like ( on the next line instead of here.
            // Within library code, "content" is specified as @Composable.
            content =
                {
                    DancesAPI29Theme {  ShowSchedule()  }
                }
        )
    }
}

