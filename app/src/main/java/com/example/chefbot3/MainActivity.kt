package com.example.chefbot3

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

val generativeModel = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "AIzaSyBcKg4EkH_R1c3P2BbNKrCBFwhluiE6ncw"
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChefBot3Theme {
                var generatedText by remember { mutableStateOf("Upload the contents of your fridge to get a possible recipe!") }
                var imageUri by remember { mutableStateOf<Uri?>(null) }
                var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                val context = LocalContext.current

                val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    imageUri = uri
                    uri?.let {
                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    }
                }

                val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { capturedBitmap: Bitmap? ->
                    capturedBitmap?.let {
                        bitmap = it
                        saveToGallery(context, it)
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "ChefBot") }
                        )
                    },
                    content = {
                        Column(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp) // Increased height for a bigger button
                            ) {
                                Text(
                                    text = "Upload Fridge",
                                    fontSize = 15.sp // Increase font size
                                )
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
                                                text("If the image you get is of the inside of a fridge, then reply with a possible recipe the person could make with the contents as well as the steps to do it, around 150 words, if it's anything else, mention how the user must have mistakenly uploaded the wrong image and describe what the image is displaying instead in 60 words")
                                            }
                                            generatedText = geminiGeneration(inputContent).toString()
                                        }
                                    }
                                }) {
                                    Text(text = "Get Recipe!")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Making the response scrollable
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = generatedText,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    },
                    bottomBar = {
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(text = "Take Picture")
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

    private fun saveToGallery(context: Context, bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ChefBot_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ChefBot")
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
    }
}
