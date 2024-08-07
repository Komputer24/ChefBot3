package com.example.chefbot3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chefbot3.ui.theme.ChefBot3Theme
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.InputStream

val generativeModel = GenerativeModel(
    // The Gemini 1.5 models are versatile and work with most use cases
    modelName = "gemini-1.5-flash",
    // Access your API key as a Build Configuration variable (see "Set up your API key" above)
    apiKey = "AIzaSyBcKg4EkH_R1c3P2BbNKrCBFwhluiE6ncw"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChefBot3Theme {
                var generatedText by remember { mutableStateOf("Please upload an image") }
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                val context = LocalContext.current

                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    imageUri = uri
                    uri?.let {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    }
                }

                Scaffold(
                    content = {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ChefBot",
                                fontSize = 40.sp,
                            )

                            Button(onClick = { launcher.launch("image/*") }) {
                                Text(text = "Upload Fridge")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(16.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (bitmap != null) {
                                Button(onClick = {
                                    runBlocking {
                                        launch {
                                            val inputContent = content {
                                                image(bitmap!!)
                                                text("If the image you get is of the inside of a fridge, then reply with a possible recipe the person could make with the contents, if its anything else describe the image in 30 words")
                                            }
                                            generatedText = geminiGeneration(inputContent).toString()
                                        }
                                    }
                                }) {
                                    Text(text = "Get Recipe!")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = generatedText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }

    private suspend fun geminiGeneration(prompt: Content): String? {
        val response = generativeModel.generateContent(prompt)
        return response.text
    }
}
