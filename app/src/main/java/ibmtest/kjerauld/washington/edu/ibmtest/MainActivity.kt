package ibmtest.kjerauld.washington.edu.ibmtest

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.ibm.watson.developer_cloud.discovery.v1.Discovery
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions
import android.os.StrictMode
import android.util.Log
import com.ibm.watson.developer_cloud.personality_insights.v3.model.ProfileOptions
import com.ibm.watson.developer_cloud.util.GsonSingleton
import com.ibm.watson.developer_cloud.personality_insights.v3.PersonalityInsights
import com.ibm.watson.developer_cloud.personality_insights.v3.model.ConsumptionPreferencesCategory
import com.ibm.watson.developer_cloud.personality_insights.v3.model.Content
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.Reader

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
        //val service = Discovery("2017-11-07", "5f3becf6-32ee-43ac-bbbd-2ac42ef7668c", "6fXcNSP88OWu")

        //initialize UI parameters
        val textView = findViewById(R.id.textView2) as TextView
        val editText = findViewById<View>(R.id.editText) as EditText
        val button = findViewById<View>(R.id.button) as Button

        //val intent : Intent = Intent(this, ToneChart::class.java)
        //startActivity(intent)

        button.setOnClickListener() {
            val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
            StrictMode.setThreadPolicy(policy)

            textView.setText(editText.getText())

            val currentText: String = editText.getText().toString()

            val myService = ToneAnalyzer("2017-09-21")
            myService.setUsernameAndPassword("5f3becf6-32ee-43ac-bbbd-2ac42ef7668c", "6fXcNSP88OWu")

            val toneOptions = ToneOptions.Builder().text(currentText).build()
            val tone = myService.tone(toneOptions).execute()

            val tone_values = tone.documentTone.tones

            Log.i("TONES", tone_values.toString())
            // holds an Array of all detected tones
            val tone_holder = ArrayList<Array<String>>()
            var toneString = "Tones Detected: "
            for (a in 0..(tone_values.size - 1)) {
                val arr : Array<String> = arrayOf(tone_values[a].toneName, tone_values[a].score.toString())
                tone_holder.add(arr)
                toneString += tone_values[a].toneName + " "
            }

            val intent : Intent = Intent(this, ToneChart::class.java)
            intent.putExtra("tones", tone_holder)
            startActivity(intent)
        }

        val textViewPer = findViewById(R.id.textView3) as TextView
        val editTextPer = findViewById<View>(R.id.editText3) as EditText
        val buttonPer = findViewById<View>(R.id.button2) as Button

        buttonPer.setOnClickListener() {
            val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
            StrictMode.setThreadPolicy(policy)

            var profiler = ""
            val service = PersonalityInsights("2017-10-13")
            service.setUsernameAndPassword("a2a3e22d-e572-4884-9598-ef6e5d334619", "prGs1xnZOB8r")

            try {
                // file currently at /sdcard/profile.json at my device
                val jReader: Reader = FileReader(editTextPer.getText().toString())
                val myData = GsonSingleton.getGson().fromJson(jReader, Content::class.java)
                val options = ProfileOptions.Builder()
                        .content(myData).consumptionPreferences(true)
                        .rawScores(true).build()
                val profile = service.profile(options).execute()
                //System.out.println(profile)

                val consumption : HashMap<String, Map<String, Double>> = HashMap()
                val values : HashMap<String, ArrayList<Double>> = HashMap()
                val needs : HashMap<String, ArrayList<Double>> = HashMap()
                val personality : HashMap<HashMap<String, ArrayList<Double>>, HashMap<String, ArrayList<Double>>>  = HashMap()

                /**
                 * This processes the data for consumption and puts it into a map for easy access
                 */
                for (a in 0..profile.consumptionPreferences.size - 1){
                    val category : String = profile.consumptionPreferences[a].consumptionPreferenceCategoryId
                    val items : HashMap<String, Double> = HashMap<String, Double>()

                    for (b in 0..profile.consumptionPreferences[a].consumptionPreferences.size - 1){
                        items.put(profile.consumptionPreferences[a].consumptionPreferences[b].name,
                                profile.consumptionPreferences[a].consumptionPreferences[b].score)
                    }

                    consumption.put(category, items)
                }

                /**
                 * This processes the data for 'values' and puts it into a map with the value name
                 * as the key and raw score & percentile as values
                 */
                for(a in 0..profile.values.size - 1){
                    val value : String = profile.values[a].name
                    val percentile : Double = profile.values[a].percentile
                    val raw : Double = profile.values[a].rawScore
                    values.put(value, arrayListOf(percentile * 100.0, raw * 100.0))
                }

                println("VALUES MAIN" + values)

                /**
                 * This processes the data for 'needs' and puts it into a map with the value name
                 * as the key and raw score & percentile as values
                 */
                for (a in 0..profile.needs.size - 1){
                    val need : String = profile.needs[a].name
                    val percentile : Double = profile.needs[a].percentile
                    val raw : Double = profile.needs[a].rawScore

                    needs.put(need, arrayListOf(percentile * 100.0, raw * 100.0))
                }

                /**
                 * This processes the data for the 'personality' category.
                 */
                for (a in 0..profile.personality.size - 1){
                    val topLevel : HashMap<String, ArrayList<Double>> = HashMap()
                    val temp : HashMap<String, ArrayList<Double>> = HashMap()

                    val p = profile.personality[a].name
                    val child = profile.personality[a].children

                    topLevel.put(p, arrayListOf(profile.personality[a].percentile * 100.0, profile.personality[a].rawScore * 100.0))
                    for (b in 0..child.size - 1){

                        val name : String = child[b].name
                        val percentile : Double = child[b].percentile
                        val raw : Double = child[b].rawScore

                        temp.put(name, arrayListOf(percentile * 100.0, raw * 100.0))
                    }
                    personality.put(topLevel, temp)
                }

                // Attaches all the data maps to send with the intent to the overview screen
                val bundle : Bundle = Bundle()
                bundle.putSerializable("consumption", consumption)
                bundle.putSerializable("values", values)
                bundle.putSerializable("needs", needs)
                bundle.putSerializable("personality", personality)
                bundle.putLong("words", profile.wordCount)

                val intent : Intent = Intent(this, PersonalityOverview::class.java)
                intent.putExtras(bundle)
                startActivity(intent)

                // Shows how well certain personality traits match a given user in terms of percentage
                println("Personality: ")
                //val personality = profile.personality
               // println(personality)

                profiler = profile.toString()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }

            // Right now just outputs the entire profile. Isn't the most useful display. Just a placeholder.
            //textViewPer.setText(profiler)
        }

    }
}

// starting of implementation for JSON Writer
class message {
    private var content: String = ""
    private var contentType: String = "text/plain"
    private var language: String = "en"
}
