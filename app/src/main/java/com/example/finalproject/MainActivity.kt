package com.example.finalproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.finalproject.ui.theme.FinalProjectTheme
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.finalproject.utils.VeryfiApiClient
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinalProjectTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ReceiptCaptureScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ReceiptCaptureScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var apiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToCache(context, it)
            imageUri = uri
            uploadToVeryfi(uri, context, onResponse = { response ->
                apiResponse = response
                isLoading = false
            }, onError = {
                apiResponse = "Error: ${it.message}"
                isLoading = false
            })
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            uploadToVeryfi(it, context, onResponse = { response ->
                apiResponse = response
                isLoading = false
            }, onError = {
                apiResponse = "Error: ${it.message}"
                isLoading = false
            })
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Button to launch camera
        Button(onClick = {
            isLoading = true
            cameraLauncher.launch(null)
        }) {
            Text("Take Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to select from gallery
        Button(onClick = {
            isLoading = true
            galleryLauncher.launch("image/*")
        }) {
            Text("Select from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the selected image
        imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray, RoundedCornerShape(8.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show loading indicator or API response
        if (isLoading) {
            Text("Processing Receipt...", color = Color.Gray)
        } else {
            apiResponse?.let {
                Text("API Response:\n$it", color = Color.Black)
            }
        }
    }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
    file.outputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun uploadToVeryfi(
    uri: Uri,
    context: Context,
    onResponse: (String) -> Unit,
    onError: (Throwable) -> Unit
) {
    try {
        val file = createTempFileFromUri(uri, context)

        VeryfiApiClient.processDocumentWithVeryfi(
            imageFile = file,
            onSuccess = { response ->
                println("Full JSON Response: $response") // Log the full response for debugging

                val gson = Gson()
                val jsonResponse = gson.fromJson(response, Map::class.java)

                val vendor = jsonResponse["bill_to"]?.let { (it as Map<*, *>)["name"] } as? String ?: "Unknown"
                val date = jsonResponse["date"] as? String ?: "Unknown"
                val total = jsonResponse["total"] as? Double ?: "Unknown"
                val subtotal = jsonResponse["subtotal"] as? Double ?: "Unknown"
                val tax = jsonResponse["tax"] as? Double ?: "Unknown"
                val category = jsonResponse["category"] as? String ?: "Unknown"
                val currencyCode = jsonResponse["currency_code"] as? String ?: "Unknown"
                val lineItems = jsonResponse["line_items"] as? List<*> ?: emptyList<Any>()

                // Process line items for detailed breakdown
                val itemsDetails = lineItems.joinToString(separator = "\n") { item ->
                    val itemMap = item as? Map<*, *> ?: return@joinToString "Unknown item"
                    val description = itemMap["description"] as? String ?: "No description"
                    val totalItem = itemMap["total"] as? Double ?: "Unknown total"
                    "$description: $$totalItem"
                }

                // Format the response
                val formattedResponse = """
                    Vendor: $vendor
                    Date: $date
                    Total: $$total
                    Subtotal: $$subtotal
                    Tax: $$tax
                    Category: $category
                    Currency: $currencyCode
                    
                    Items:
                    $itemsDetails
                """.trimIndent()

                onResponse(formattedResponse)
            },
            onError = { error ->
                onError(error)
            }
        )
    } catch (e: Exception) {
        onError(e)
    }
}

// Helper function to create a temporary file from URI
private fun createTempFileFromUri(uri: Uri, context: Context): File {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Cannot open URI: $uri")
    val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    tempFile.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream)
    }
    return tempFile
}

@Preview(showBackground = true)
@Composable
fun ReceiptCaptureScreenPreview() {
    FinalProjectTheme {
        ReceiptCaptureScreen()
    }
}
