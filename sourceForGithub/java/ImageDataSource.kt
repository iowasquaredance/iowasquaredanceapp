package com.dances.dances

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.iowasquaredance.schedule.R


/*  --------------------------------------------------
    Add an image to a composable screen.

    The first argument is the name of an image file in (or under) the
    assets directory (see Android Studio, project window in left pane).

    The second argument is the compile-time constant (similar to C++ constexpr)
    representing an image under res/drawable that will be shown in case of a
    problem first argument.

    Example call:
       addImage("talent/DougMcCart.webp", R.drawable.microphone)

    ---------------------------------------------------
    This function uses the @composable function AsyncImage to add
    an image to whatever screen the caller is making.  Images can
    be located in many places including the internet.  But for our
    purposes the images are in directories named "assets" and
    "res/drawable".  The tradeoffs between these two accessible locations
    are discussed here:
    https://www.geeksforgeeks.org/assets-folder-in-android/

    AsyncImage has the desirable feature that if an image cannot
    be accessed an alternative will be shown instead.  The alternative
    is in the "drawable" folder whose contents are packaged in the APP distribution
    file (.apk) with the program executable.  This image alternative is
    1) very likely to be present because it's distributed with the executable, and
    2) is accessed by a compile-time constant of the form R.drawable.<name>

    ---------------------------------------------------
    AsyncImage is from the library named "coil".
    https://developer.android.com/codelabs/basic-android-kotlin-compose-load-images#2

    As installed, Android Studio doesn't know about AsyncImage so expect
    to manually add import  to .kt files and also add
        implementation("io.coil-kt:coil-compose:2.4.0")
    to build.gradle.kts (Module:app)

    ---------------------------------------------------
    This function assumes that the primary image is within the Android Studio
    project, on local storage, rather than being fetched from the internet at run-time.
    Most documentation of AsyncImage show the latter.  Access to the assets
    folder is different!

    Two important lines in the call to AsyncImage...

        .data("file:///android_asset/talent/DougMcCart.webp")
        .build()   // ESSENTIAL!  Crashes if omitted.

        Three /// are vital.
        The substring android_asset is a MAGIC literal with a special
        meaning in Android.  It is the path to the assets folder.
        Read this and weep...
        https://stackoverflow.com/questions/13638892/where-is-the-path-file-android-asset-documented

    from AI via duckduckgo
    In Jetpack Compose, "android_asset" refers to a directory in an Android
    app's file structure where you can store raw asset files, such as images
    or SVG files.
 */

@Composable
fun AddImageWithAlternate (
    firstChoice: String,  // for example "talent/DougMcCart.webp"
    secondChoice:Int,     // for example R.drawable.microphone
    size: Dp)             // for example 120.Dp
{
    val imageInAssetsFolder = "file:///android_asset/" + firstChoice
    val failureGraphic = painterResource(secondChoice)

    /* WARNING!  AsyncImage is fragile. Modify with care and
       always check to see whether screen scrolling backwards still works.

       Also, this needs to be in build.grad.kts (Module :app)
           implementation("io.coil-kt:coil-compose:2.4.0")

     */

    AsyncImage (
        contentDescription = "caller photo",  // required?  apparently so.
        modifier = Modifier
            .height(size),
 //           .align(Alignment.CenterHorizontally)
  //          .fillMaxWidth(),
        error = failureGraphic,  // show instead if error.
        fallback = failureGraphic, // show if image resource is null
        placeholder = failureGraphic, // ESSENTIAL!  somehow this fixes scrolling problem?
        model = ImageRequest.Builder(context = LocalContext.current)
           .data(imageInAssetsFolder)
      //      /* still scrolling problem ??  no problem here */   .data("file:///android_asset/talent/DougMcCart.webp")
            // no   .data("file:///assets/talent/DougMcCart.webp")
            .build()   // ESSENTIAL!  Crashes if omitted.
    )
}

// This enumeration classifies the type of images the app shows...
enum class danceInfoType
{
    CLUB_LOGO,      // A club's logo.
    MAP,            // An offline map to the dance location.  Multiple locations possible for a club!
    TALENT_PHOTO    // Picture of a caller or cuer, possibly of one person doing both.
}

/*
    Add an image to supplement a dance announcement.

    Provide the type of image to be shown, club logo, offline map, or person.
    Provide the image name but without the enclosing directory name.

    I've chosen to put images of the "types" just mentioned into three
    subdirectories under "assets".  This function hides that implementation detail.
 */

@Composable
fun DanceInformationImage (
    type: danceInfoType,
    name: String,
    size: Dp = 120.dp)
{
    // Each type of image is in a different directory under assets.
    var directory = "clubs"
    var alternate = R.drawable.couplecolor

    if (type==danceInfoType.MAP)
    {
        directory = "maps"
        alternate = R.drawable.iowa_outline
    }

    else if (type==danceInfoType.TALENT_PHOTO)
    {
        directory = "talent"
        alternate = R.drawable.microphone
    }

    // Prefix the directory in front of the name.
    // The function being called, "AddImageWithAlternate" adds
    // additional text to assemble the final name passed to AsyncImage.
    val fullName = directory + '/' + name
    /* YIKES!  somehow this is breaking screen scrolling back in time !! ?? */
    AddImageWithAlternate(fullName, alternate, size)
}

// Show the caller and, if provided the cuer - photo and name underneath.
@Composable
fun ShowTalent (
    callerFileName: String, callerName: String,
    cuerFileName:   String, cuerName  : String)
{
     Row (
        modifier = Modifier
            // .fillMaxWidth()
            .padding(horizontal = 4.dp)
        //        .background(Color.DarkGray)
        ,horizontalArrangement = Arrangement.SpaceEvenly)
     {
         // Column for caller and name underneath...
         Column(
             modifier = Modifier
                 .fillMaxWidth()
                 .weight(0.5f)
         )
         { // Caller
             DanceInformationImage(danceInfoType.TALENT_PHOTO, callerFileName)

             Text(
                 callerName, textAlign = TextAlign.Center,
                 modifier = Modifier
                     //      .height(120.dp)
                     //       .align(Alignment.CenterHorizontally)
                //     .fillMaxWidth()  // important
             )
         } // of caller column

         // Sometimes there is no cuer or the caller also cues.
         if (cuerName.isNotBlank()) {
             // Column for cuer and name underneath...
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .weight(0.5f)
             )
             { // Cuer
                 DanceInformationImage(danceInfoType.TALENT_PHOTO, cuerFileName)
                 //       ShowTalent("manning.jpg", "Tom Manning")  // temporarily, all callers look like Tom

                 Text(
                     cuerName, textAlign = TextAlign.Center,
                     modifier = Modifier
                         //      .height(120.dp)
                         //       .align(Alignment.CenterHorizontally)
                      //   .fillMaxWidth()  // important
                 )
             } // of cuer column
         } // of if there is a cuer
     }  // of Row for talent
}


@Composable
fun LandscapeImage (
    firstChoice: String,  // for example "talent/DougMcCart.webp"
    secondChoice:Int,     // for example R.drawable.microphone
    size: Dp = 300.dp)             // for example 120.Dp
{
    val imageInAssetsFolder = "file:///android_asset/" + firstChoice
    val failureGraphic = painterResource(secondChoice)

    /* WARNING!  AsyncImage is fragile. Modify with care and
       always check to see whether screen scrolling backwards still works.

       Also, this needs to be in build.grad.kts (Module :app)
           implementation("io.coil-kt:coil-compose:2.4.0")

     */

    AsyncImage (
        contentDescription = "caller photo",  // required?  apparently so.
        modifier = Modifier
            .height(size),

        //           .align(Alignment.CenterHorizontally)
        //          .fillMaxWidth(),
        error = failureGraphic,  // show instead if error.
        fallback = failureGraphic, // show if image resource is null
        placeholder = failureGraphic, // ESSENTIAL!  somehow this fixes scrolling problem?
        // needed?      contentScale = ContentScale.Fit,

        model = ImageRequest.Builder(context = LocalContext.current)
            /* ok but experiment, causing scrolling problem?  YES */         .data(imageInAssetsFolder)
            //      /* still scrolling problem ??  no problem here */   .data("file:///android_asset/talent/DougMcCart.webp")
            // no   .data("file:///assets/talent/DougMcCart.webp")
            .build()   // ESSENTIAL!  Crashes if omitted.
    )
}
