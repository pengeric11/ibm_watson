package ibmtest.kjerauld.washington.edu.ibmtest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.ibm.watson.developer_cloud.discovery.v1.Discovery
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions
import android.os.StrictMode
import com.ibm.watson.developer_cloud.personality_insights.v3.model.ProfileOptions
import com.ibm.watson.developer_cloud.util.GsonSingleton
import com.ibm.watson.developer_cloud.personality_insights.v3.PersonalityInsights
import com.ibm.watson.developer_cloud.personality_insights.v3.model.Content
import com.ibm.watson.developer_cloud.personality_insights.v3.model.ContentItem
import org.json.JSONArray
import java.io.*

// IMPORTANT NOTES: Steps to Upload File to Android Studio...
//  For personality tests to work you will need a profile stored somewhere on your device.
//  I personally move it to the SD Card. You can do that by running an emulator and then in
//  Android Studio going ot "View -> Tool Windows -> Device File Explorer" and uploading it form
//  there. You can then type "/sdcard/profile.json" in the editText for the personality test. It
//  as of right now is outputting useful information in the Logcat so you can see it and decide
//  what to do with it.
//
//  For tone analysis all you have to do is enter text and if it predicts a tone it will
//  tell you which ones. I have the APIs and everything setup and working. 

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // in the constructor
        val service = Discovery("2017-11-07", "5f3becf6-32ee-43ac-bbbd-2ac42ef7668c", "6fXcNSP88OWu")

        //initialize UI parameters
        val fileLoc = findViewById(R.id.textView) as TextView
        val buttonLoc = findViewById(R.id.button3) as Button

        val textView = findViewById(R.id.textView2) as TextView
        val editText = findViewById<View>(R.id.editText) as EditText
        val button = findViewById<View>(R.id.button) as Button

        val textViewPer = findViewById(R.id.textView3) as TextView
        val editTextPer = findViewById<View>(R.id.editText3) as EditText
        val buttonPer = findViewById<View>(R.id.button2) as Button

        button.setOnClickListener() {
            val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
            StrictMode.setThreadPolicy(policy)

            textView.setText(editText.getText())

            val currentText: String = editText.getText().toString()

            if(currentText != "") {

                val myService = ToneAnalyzer("2017-09-21")
                myService.setUsernameAndPassword("5f3becf6-32ee-43ac-bbbd-2ac42ef7668c", "6fXcNSP88OWu")

                val toneOptions = ToneOptions.Builder().text(currentText).build()
                val tone = myService.tone(toneOptions).execute()

                val tone_values = tone.documentTone.tones
                // holds an Array of all detected tones
                val tone_holder = ArrayList<String>()
                var toneString = "Tones Detected: "
                for (a in 0..(tone_values.size - 1)) {
                    tone_holder.add(tone_values[a].toneName)
                    toneString += tone_values[a].toneName + " "
                }
                try {
                    val output = BufferedWriter(FileWriter(editTextPer.getText().toString(), true))
                    output.append(currentText)
                    output.newLine()
                    output.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

                // Shows predicted tones. TODO: Different messages if tone is empty
                if (tone_holder.size == 0) {
                    textView.setText("No Tones Found From Given Input")
                } else {
                    textView.setText(toneString)
                }
            }
        }

        buttonPer.setOnClickListener() {
            val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
            StrictMode.setThreadPolicy(policy)

            var profiler = ""
            val service = PersonalityInsights("2017-10-13")
            service.setUsernameAndPassword("a2a3e22d-e572-4884-9598-ef6e5d334619", "prGs1xnZOB8r")

            try {
                // Text file implementation (works better than JSON)
                //  File currently at /sdcard/mytest/txt on my device
                val filer = deviceReader(editTextPer.getText().toString())

                // sets up Profile and executes the call to IBM Watson
                val options = ProfileOptions.Builder()
                        .text(filer)
                        .rawScores(true).build()
                val profile = service.profile(options).execute()

                // Just gives times and days of week. Probably not too useful in our analysis
                println("Behavior: ")
                val behavior = profile.behavior
                println(behavior)

                // Predicts what a user likes in terms of books, cars, movies, habits, etc... Could be useful and easy to write displays for
                println("Consumption Preferences: ")
                val consumptionPrefs = profile.consumptionPreferences
                println(consumptionPrefs)

                // Shows how much a user needs things like "Love" and "Stability" in their life
                println("Needs: ")
                val needs = profile.needs
                println(needs)

                // Shows how well certain personality traits match a given user in terms of percentage
                println("Personality: ")
                val personality = profile.personality
                println(personality)

                // Shows values the person is predicted to have and the percentage it is true for them
                println("Values: ")
                val theirValues = profile.values
                println(theirValues)

                // Shows words analyzed
                println("Word Count: ")
                val wordCount = profile.wordCount
                println(wordCount)

                // Seems to always appear as null in my tests
                println("Word Count Message: ")
                val wordCountMessage = profile.wordCountMessage
                println(wordCountMessage)

                profiler = profile.toString()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            // Right now just outputs the entire profile. Isn't the most useful display. Just a placeholder.
            textViewPer.setText(profiler)
        }

        buttonLoc.setOnClickListener() {
            fileLoc.setText(editTextPer.getText().toString())
        }

    }

    // Reads a device from a given file name and stores it as a String to return
    fun deviceReader(fileName: String): String {
        val myFile: File? = File(fileName)
        if(myFile == null) {
            return "nullFile"
        }

        val bufferedReader: BufferedReader = myFile.bufferedReader()

        val inputString = bufferedReader.use { it.readText() }


        return inputString
    }
}

