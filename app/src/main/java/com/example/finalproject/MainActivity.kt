package com.example.finalproject

// Core Android components
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap

// Compose foundation and layout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// Material design
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons

// Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Navigation
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Third-party libraries
import coil.compose.rememberAsyncImagePainter
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.DonutPieChart
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.example.finalproject.utils.VeryfiApiClient
import com.google.gson.Gson
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import com.example.finalproject.ui.theme.Purple40
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.app.DatePickerDialog
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.common.model.Point
import co.yml.charts.axis.AxisData
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



// Kotlin coroutine
import kotlinx.coroutines.delay
import java.io.File



// ========== Main Entry Point ==========
/**
 * MainActivity:
 * The entry point of the app that sets up the starting screen using Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Set the content view using Jetpack Compose
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }


            LaunchedEffect(Unit) {
                loadThemePreferenceFromFirebase { loadedTheme ->
                    isDarkTheme = loadedTheme
                }
            }

            FinalProjectTheme(darkTheme = isDarkTheme) {
                // Load the Login Screen as the starting UI
                LoginScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { newTheme ->
                        isDarkTheme = newTheme
                        saveThemePreferenceToFirebase(newTheme) { success ->
                            if (success) println("Theme saved successfully!")
                            else println("Failed to save theme.")
                        }
                    }
                )
            }
        }
    }
}

/**
 * ReceiptCaptureScreen:
 * A composable function that allows users to capture or upload a receipt image.
 * It processes the image by uploading it to the Veryfi API for OCR extraction.
 *
 * Features:
 * - Capture an image using the device's camera.
 * - Upload an image from the gallery.
 * - Display the selected image.
 * - Show API response or error message after processing the image.
 *
 * @param modifier A Modifier for customizing the layout of the composable (default is empty).
 */
@Composable
fun ReceiptCaptureScreen(
    modifier: Modifier = Modifier
) {
    // Context required for accessing cache and other resources
    val context = LocalContext.current

    // State to store the URI of the selected or captured image
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // State to store the API response or error message
    var apiResponse by remember { mutableStateOf<String?>(null) }

    // State to track whether the image processing is loading
    var isLoading by remember { mutableStateOf(false) }

    // Launcher for capturing an image using the device camera
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // If a bitmap is successfully captured, save it to cache and upload it
        bitmap?.let {
            val uri = saveBitmapToCache(context, it)
            imageUri = uri
            uploadToVeryfi(
                uri,
                context,
                onResponse = { response ->
                    apiResponse = response
                    isLoading = false
                },
                onError = {
                    apiResponse = "Error: ${it.message}"
                    isLoading = false
                }
            )
        }
    }

    // Launcher for selecting an image from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // If a URI is selected, process the image
        uri?.let {
            imageUri = it
            uploadToVeryfi(
                it,
                context,
                onResponse = { response ->
                    apiResponse = response
                    isLoading = false
                },
                onError = {
                    apiResponse = "Error: ${it.message}"
                    isLoading = false
                }
            )
        }
    }

    // Layout: Column to display buttons, image, and processing status
    Column(
        modifier = modifier
            .fillMaxSize() // Makes the composable take the full screen size
            .padding(20.dp) // Adds padding around the content
            .verticalScroll(rememberScrollState()), // Enables vertical scrolling if content overflows
        horizontalAlignment = Alignment.CenterHorizontally, // Center content horizontally
        verticalArrangement = Arrangement.Top // Align content at the top
    ) {
        // Spacer to add vertical space at the top
        Spacer(modifier = Modifier.height(50.dp))

        // Button to capture an image using the camera
        Button(onClick = {
            isLoading = true // Set loading state
            cameraLauncher.launch(null) // Launch camera intent
        }) {
            Text("Take Photo") // Button label
        }

        // Spacer to separate buttons
        Spacer(modifier = Modifier.height(20.dp))

        // Button to select an image from the gallery
        Button(onClick = {
            isLoading = true // Set loading state
            galleryLauncher.launch("image/*") // Launch gallery intent for image selection
        }) {
            Text("Select from Gallery") // Button label
        }

        // Spacer to separate content
        Spacer(modifier = Modifier.height(20.dp))

        // Display the selected or captured image
        imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri), // Asynchronously load the image from URI
                contentDescription = "Selected Image", // Accessibility description
                modifier = Modifier
                    .size(200.dp) // Sets the size of the image
                    .background(Color.LightGray, RoundedCornerShape(10.dp)) // Adds background and rounded corners
            )
        }

        // Spacer to separate the image and processing status
        Spacer(modifier = Modifier.height(20.dp))

        // Display a loading message or API response based on the state
        if (isLoading) {
            Text("Processing Receipt...", color = Color.Gray) // Loading indicator
        } else {
            apiResponse?.let {
                Text("API Response:\n$it", color = Color.Black) // Display API response or error message
            }
        }
    }
}

/**
 * saveBitmapToCache:
 * Saves a given bitmap image to the app's cache directory and returns its URI.
 *
 * This function is useful for temporarily storing image files (e.g., captured photos)
 * and obtaining a sharable URI for further processing or uploads.
 *
 * @param context The context used to access the app's cache directory and file provider.
 * @param bitmap The Bitmap object to be saved as a JPEG file.
 * @return Uri The URI of the saved file, which can be used to share or process the image further.
 */
fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    // Create a unique file name using the current system time
    val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")

    // Open the file's output stream to write the bitmap data
    file.outputStream().use { outputStream ->
        // Compress the bitmap into JPEG format with 100% quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    }

    // Return the URI of the saved file using FileProvider
    // This ensures the file can be shared securely with other components or apps
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}


/**
 * uploadToVeryfi:
 * Processes a receipt image using the Veryfi API for OCR extraction, formats the extracted data,
 * and uploads it to Firebase.
 *
 * This function handles:
 * - File creation from a URI.
 * - Veryfi API call to extract receipt data (category, total, line items, etc.).
 * - Data formatting, including subtotal, tax, and total calculations.
 * - Uploading the formatted data to Firebase.
 *
 * @param uri The URI of the image file to be processed.
 * @param context The context used to access the file system and resources.
 * @param onResponse Callback invoked on successful data upload with a success message.
 * @param onError Callback invoked if an error occurs during processing or upload.
 */
private fun uploadToVeryfi(
    uri: Uri,
    context: Context,
    onResponse: (String) -> Unit, // Success callback
    onError: (Throwable) -> Unit  // Error callback
) {
    try {
        // Step 1: Convert the URI to a temporary file
        val file = createTempFileFromUri(uri, context)

        // Step 2: Call Veryfi API to process the document
        VeryfiApiClient.processDocumentWithVeryfi(
            imageFile = file, // Image file to upload
            onSuccess = { response ->
                // Parse the JSON response from the API
                val gson = Gson()
                val jsonResponse = gson.fromJson(response, Map::class.java)

                // Extract "category" from the JSON response, default to "Unknown" if missing
                val category = jsonResponse["category"] as? String ?: "Unknown"

                // Extract "currency_code", default to "Unknown"
                val currencyCode = jsonResponse["currency_code"] as? String ?: "Unknown"

                // Extract and format the "date" field
                val dateTime = jsonResponse["date"] as? String ?: "Unknown"
                val formattedDate = formatDate(dateTime) // Format to YYYY-MM-DD

                // Step 3: Extract line items from the receipt
                val lineItems = jsonResponse["line_items"] as? List<*>
                val extractedItems = lineItems?.map { item ->
                    val itemMap = item as? Map<*, *> // Cast each item to a Map
                    val name = itemMap?.get("description") as? String ?: "No description"
                    val price = itemMap?.get("total") as? Double ?: 0.0
                    mapOf("name" to name, "price" to price)
                } ?: emptyList()

                // Step 4: Calculate subtotal, tax, and total
                val subtotal = extractedItems.sumOf { (it["price"] as Double?) ?: 0.0 } // Sum up item prices
                val tax = jsonResponse["tax"] as? Double ?: 0.0 // Extract tax or default to 0.0
                val total = jsonResponse["total"] as? Double ?: (subtotal + tax) // Extract total or calculate

                // Step 5: Format the extracted data into a structured map
                val formattedData = mapOf(
                    "category" to category,
                    "date" to formattedDate, // Formatted date
                    "items" to extractedItems,
                    "subtotal" to subtotal,
                    "tax" to tax,
                    "total" to total
                )

                // Step 6: Save the formatted data to Firebase
                saveFormattedDataToFirebase(formattedData) { success ->
                    if (success) {
                        // Invoke success callback
                        onResponse("Data uploaded successfully!")
                    } else {
                        // Invoke error callback if saving fails
                        onError(Exception("Failed to save data."))
                    }
                }
            },
            onError = { error ->
                // Step 7: Handle errors returned by the Veryfi API
                onError(error)
            }
        )
    } catch (e: Exception) {
        // Handle any unexpected exceptions
        onError(e)
    }
}

// Helper Function to Format Date
private fun formatDate(dateTime: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = inputFormat.parse(dateTime)
        outputFormat.format(parsedDate ?: dateTime)
    } catch (e: Exception) {
        dateTime // Return original if formatting fails
    }
}

/**
 * saveFormattedDataToFirebase:
 * Saves the provided receipt data to Firebase Firestore under the authenticated user's collection.
 *
 * This function:
 * - Checks if the user is authenticated.
 * - Saves the data to a "receipts" sub-collection within the user's document.
 * - Invokes a callback to notify the completion status.
 *
 * @param data A map containing the formatted receipt data (e.g., category, items, totals).
 * @param onComplete Callback that returns a Boolean indicating success (true) or failure (false).
 */
private fun saveFormattedDataToFirebase(
    data: Map<String, Any>,           // Receipt data to be saved
    onComplete: (Boolean) -> Unit     // Callback function to notify completion
) {
    // Step 1: Get an instance of Firestore
    val firestore = FirebaseFirestore.getInstance()

    // Step 2: Get the currently authenticated user's ID
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Step 3: Check if the user is authenticated
    if (userId != null) {
        // Reference the user's "receipts" collection in Firestore
        firestore.collection("users")
            .document(userId)               // Access the document corresponding to the user's ID
            .collection("receipts")         // Access or create the "receipts" sub-collection
            .add(data)                      // Add the receipt data as a new document
            .addOnSuccessListener {
                // Document successfully saved to Firestore
                println("Document saved successfully!")
                onComplete(true)            // Invoke callback with success status
            }
            .addOnFailureListener {
                // Document failed to save to Firestore
                println("Failed to save document: ${it.message}")
                onComplete(false)           // Invoke callback with failure status
            }
    } else {
        // If the user is not authenticated, log an error
        println("User is not authenticated.")
        onComplete(false)                   // Invoke callback with failure status
    }
}



/**
 * createTempFileFromUri:
 * Creates a temporary file in the app's cache directory from a given URI.
 * This function is useful for converting content URIs (e.g., selected images) into
 * accessible File objects for further processing, such as uploading to an API.
 *
 * @param uri The URI of the file or content to be converted into a temporary file.
 * @param context The context used to access the content resolver and cache directory.
 * @return File A temporary File object created from the content of the URI.
 *
 * @throws IllegalArgumentException If the input stream for the given URI cannot be opened.
 */
private fun createTempFileFromUri(uri: Uri, context: Context): File {
    // Step 1: Access the ContentResolver to read the content of the URI
    val contentResolver = context.contentResolver

    // Step 2: Open an input stream from the URI
    val inputStream = contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("Cannot open URI: $uri") // Throw an exception if input stream fails

    // Step 3: Create a temporary file in the app's cache directory
    val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

    // Step 4: Write the content of the input stream to the temporary file
    tempFile.outputStream().use { outputStream ->
        inputStream.copyTo(outputStream) // Copy all bytes from the input stream to the output stream
    }

    // Step 5: Return the temporary file
    return tempFile
}


/**
 * MainAppNav:
 * A composable function that sets up the main navigation structure of the app.
 *
 * It includes:
 * - A fixed top bar displaying a greeting.
 * - A conditionally expandable bottom navigation bar for navigation.
 * - A main content area that hosts the navigation graph for different screens.
 *
 * Features:
 * - Tracks the expansion state of the bottom navigation bar.
 * - Allows dynamic budget value updates.
 *
 * @param userEmail The email of the logged-in user, displayed in the top bar.
 * @param onSignOut A callback function to handle user sign-out.
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppNav(userEmail: String, onSignOut: () -> Unit, isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    // Creates a NavController to manage navigation between different screens
    val navController = rememberNavController()

    // State to track whether the bottom navigation bar is expanded or minimized
    var isExpanded by remember { mutableStateOf(true) }

    // State to store the user's current budget
    var budget by remember { mutableStateOf("") }

    // Scaffold defines the top bar, bottom bar, and main content area
    Scaffold(
        // Top bar that displays a greeting with the user's email
        topBar = { FixedTopBar(userEmail) },

        // Bottom bar that conditionally displays either the navigation bar or an expand button
        bottomBar = {
            if (isExpanded) {
                // Expanded bottom navigation bar with navigation tabs
                ExpandableBottomNavigationBar(
                    navController = navController,
                    onMinimize = { isExpanded = false } // Callback to minimize the navigation bar
                )
            } else {
                // Button to expand the bottom navigation bar when minimized
                ExpandButton(onExpand = { isExpanded = true })
            }
        }
    ) { innerPadding ->
        // Content area that adjusts its padding based on the visibility of the bottom bar
        Box(
            modifier = Modifier.padding(
                bottom = if (isExpanded) innerPadding.calculateBottomPadding() else 0.dp
            )
        ) {
            // Displays the navigation graph with dynamic budget updates
            NavigationGraph(
                navController = navController,       // NavController for screen navigation
                userEmail = userEmail,               // User email passed to screens
                onSignOut = onSignOut,               // Sign-out callback
                budget = budget,                     // Current budget value
                onBudgetChange = { budget = it },     // Callback to update the budget state
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange
            )
        }
    }
}


/**
 * ExpandableBottomNavigationBar:
 * A composable function that displays a bottom navigation bar with navigation tabs
 * and a minimize button. This bar allows users to navigate between app sections and
 * can be minimized using the provided callback.
 *
 * Features:
 * - Three navigation tabs: "Main", "New", and "Settings".
 * - A minimize button represented by a down arrow icon.
 *
 * @param navController The NavHostController used to handle navigation between screens.
 * @param onMinimize A callback function invoked when the minimize button is clicked.
 */
@Composable
fun ExpandableBottomNavigationBar(
    navController: NavHostController, // NavController to handle navigation
    onMinimize: () -> Unit // Callback to minimize the bottom navigation bar
) {
    // Surface provides a material design container for the bottom navigation bar
    Surface (
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), // Clip content to rounded corners
        color = Color(0xFF6e7885), // Background color for the Surface
        shadowElevation = 4.dp // Optional: Adds a shadow for elevation effect
    ){
        Column {
            // Row: Contains navigation tabs and the minimize button
            Row(
                modifier = Modifier
                    .fillMaxWidth() // Makes the row take up the full width of the screen
                    //.background(Purple40) // Sets the background color to magenta
                    .padding(vertical = 10.dp), // Adds vertical padding around the row
                horizontalArrangement = Arrangement.SpaceEvenly, // Evenly space the tabs horizontally
                verticalAlignment = Alignment.CenterVertically // Vertically align the content to center
            ) {
                // Navigation tab for "Main" screen
                BottomNavigationTab("Main", navController, "main")

                // Navigation tab for "New" screen
                BottomNavigationTab("New", navController, "new")

                // Navigation tab for "Settings" screen
                BottomNavigationTab("Settings", navController, "settings")

                // Minimize button: Downward arrow icon
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown, // Default down arrow icon
                    contentDescription = "Minimize", // Accessibility description for screen readers
                    tint = Color.DarkGray, // Sets the color of the icon to green
                    modifier = Modifier
                        .size(36.dp) // Sets the size of the icon
                        .clickable { onMinimize() } // Makes the icon clickable to trigger the minimize callback
                )
            }
        }
    }
}

/**
 * ExpandButton:
 * A composable function that displays a small button with an upward arrow icon.
 * It is used to expand the bottom navigation bar when minimized.
 *
 * Features:
 * - Positioned at the bottom-right corner of the screen.
 * - Contains an icon (upward arrow) to indicate the expand action.
 * - Triggers a callback function when clicked to notify the parent composable.
 *
 * @param onExpand A callback function invoked when the button is clicked to expand the navigation bar.
 */
@Composable
fun ExpandButton(onExpand: () -> Unit) {
    // Row container to position the expand button
    Row(
        modifier = Modifier
            .fillMaxWidth() // Makes the Row span the full width of the screen
            .padding(10.dp), // Adds padding to position the button away from the edges
        horizontalArrangement = Arrangement.End // Aligns the button to the far right of the Row
    ) {
        // Icon representing the "expand" action
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp, // Default upward arrow icon
            contentDescription = "Expand", // Provides accessibility support for screen readers
            tint = Color.DarkGray, // Sets the color of the icon to green
            modifier = Modifier
                .size(36.dp) // Sets the size of the icon to 36dp
                .clickable { onExpand() } // Makes the icon clickable and triggers the callback
        )
    }
}

/**
 * FixedTopBar:
 * A composable function that displays a fixed top app bar with a greeting message for the user.
 *
 * Features:
 * - Displays a personalized greeting with the user's email.
 * - Uses a fixed layout that spans the full width of the screen.
 * - Styled with a magenta background and white text.
 *
 * @param userEmail The email of the logged-in user, displayed as part of the greeting message.
 */
@OptIn(ExperimentalMaterial3Api::class) // Required to use Material3's TopAppBar API
@Composable
fun FixedTopBar(userEmail: String) {
    Surface( // Wrap the TopAppBar inside a Surface to apply rounded corners
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)), // Rounded bottom corners
        color = Color(0xFF6e7885), // Sets the background color
        shadowElevation = 4.dp // Optional: Adds elevation for a shadow effect
    ) {
        // TopAppBar: A material component that displays the app bar at the top of the screen
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically, // Vertically align icon and text
                    horizontalArrangement = Arrangement.Center, // Center content horizontally
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon placed to the left of the text
                    Icon(
                        imageVector = Icons.Filled.Delete, // Replace with any icon you prefer
                        contentDescription = "App Icon", // Accessibility description
                        modifier = Modifier
                            .size(24.dp) // Set icon size
                            .padding(end = 8.dp) // Add spacing between the icon and text
                    )

                    // Text composable to display the app name
                    Text(
                        text = "WasteWise", // App name
                        style = MaterialTheme.typography.titleLarge, // Text style
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(end = 8.dp)
                    )

                    Icon(
                        imageVector = Icons.Filled.ShoppingCart, // Replace with any icon you prefer
                        contentDescription = "App Icon", // Accessibility description
                        modifier = Modifier
                            .size(24.dp) // Set icon size
                            .padding(end = 8.dp) // Add spacing between the icon and text
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent // Sets the background color of the app bar to magenta
            ),
            modifier = Modifier.fillMaxWidth() // Makes the TopAppBar span the full width of the screen
        )
    }
}

/**
 * BottomNavigationTab:
 * A composable function that represents an individual clickable navigation tab.
 * It displays a text label and navigates to the specified route when clicked.
 *
 * Features:
 * - Displays the tab label as text.
 * - Adds padding around the label for better spacing.
 * - Uses the provided NavController to navigate to a specific route.
 *
 * @param label The text label displayed on the tab.
 * @param navController The NavHostController used to handle navigation between screens.
 * @param route The destination route to navigate to when the tab is clicked.
 */
@Composable
fun BottomNavigationTab(
    label: String,                // The text label for the navigation tab
    navController: NavHostController, // The NavController responsible for managing navigation
    route: String                 // The route to navigate to when the tab is clicked
) {
    // Text composable to display the label as a clickable navigation tab
    Text(
        text = label, // Sets the text content to the provided label
        modifier = Modifier
            .padding(20.dp) // Adds uniform padding around the label for better spacing
            .clickable {
                // Navigate to the specified route when the text is clicked
                navController.navigate(route)
            }
    )
}

/**
 * NavigationGraph:
 * A composable function that defines the navigation graph for the app.
 *
 * This function sets up navigation routes (destinations) using the provided NavController.
 * It manages transitions between screens like Main, New, and Settings screens.
 *
 * Features:
 * - Uses the NavHost to set the navigation structure.
 * - Defines three composable destinations: "main", "new", and "settings".
 * - Passes user-specific data such as email and callbacks to screens where required.
 *
 * @param navController The NavHostController used to navigate between destinations.
 * @param userEmail The email of the logged-in user, passed to screens for personalization.
 * @param onSignOut A callback function invoked when the user signs out.
 * @param budget The current budget string to be displayed or managed.
 * @param onBudgetChange A callback function to update the budget string when changed.
 */
@Composable
fun NavigationGraph(
    navController: NavHostController, // NavController to manage navigation
    userEmail: String,                // Email of the logged-in user
    onSignOut: () -> Unit,            // Callback for user sign-out
    budget: String,                   // Current budget value
    onBudgetChange: (String) -> Unit,  // Callback to update the budget
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    // NavHost: Defines the navigation graph and sets the starting destination
    NavHost(
        navController = navController, // Controller to handle navigation actions
        startDestination = "new"       // The initial route to display when the app starts
    ) {
        // Main screen destination
        composable("main") {
            MainScreen(userEmail, onSignOut) // Pass userEmail and onSignOut callback to MainScreen
        }

        // New screen destination
        composable("new") {
            NewScreen() // Displays the NewScreen composable
        }

        // Settings screen destination
        composable("settings") {
            SettingsScreen(userEmail, onSignOut, navController, isDarkTheme, onThemeChange) // Pass userEmail, onSignOut, and NavController
        }
    }
}

/**
 * MainScreen:
 * A composable function that displays a greeting, user spending data in a pie chart,
 * and a sign-out button.
 *
 * Features:
 * - Fetches receipts data from Firebase Firestore for the logged-in user.
 * - Aggregates spending data by category and displays it in a pie chart.
 * - Includes a loading indicator while fetching data.
 * - Allows users to sign out of their account.
 *
 * @param userEmail The email of the logged-in user, displayed as part of the greeting.
 * @param onSignOut A callback function invoked when the user clicks the "Sign Out" button.
 */
@Composable
fun MainScreen(userEmail: String, onSignOut: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Chart Data
    var pieChartData by remember { mutableStateOf<List<PieChartData.Slice>>(emptyList()) }
    var lineChartData by remember { mutableStateOf<LineChartData?>(null) }

    // UI States
    var isLoading by remember { mutableStateOf(true) }
    var totalSpending by remember { mutableStateOf(0.0) }
    var chartType by remember { mutableStateOf("PIE") } // Default to PIE chart

    // Filters
    var showFilters by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Helper: Show Toast
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Date Picker
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Fetch Filtered Data
    fun fetchFilteredData() {
        if (selectedCategory.isEmpty()) {
            showToast("Category is required.")
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        if (startDate.isEmpty() && endDate.isEmpty()) {
            val today = sdf.format(calendar.time)
            calendar.add(Calendar.MONTH, -1)
            startDate = sdf.format(calendar.time)
            endDate = today
        }

        isLoading = true
        println("Fetching data for category: $selectedCategory between $startDate and $endDate")

        firestore.collection("users")
            .document(userId ?: "")
            .collection("receipts")
            .get()
            .addOnSuccessListener { result ->
                println("Total documents fetched: ${result.size()}")

                val linePoints = mutableListOf<Pair<String, Float>>()
                val categoryMap = mutableMapOf<String, Double>()
                var total = 0.0

                for (document in result.documents) {
                    val category = document.getString("category") ?: "Other"
                    val date = document.getString("date") ?: ""
                    val amount = document.getDouble("total") ?: 0.0

                    println("Document: category=$category, date=$date, total=$amount")

                    if (date >= startDate && date <= endDate) {
                        if (category == selectedCategory) {
                            linePoints.add(Pair(date, amount.toFloat()))
                        }
                        categoryMap[category] = categoryMap.getOrDefault(category, 0.0) + amount
                        total += amount
                    }
                }

                println("LinePoints: $linePoints")
                println("CategoryMap: $categoryMap")

                totalSpending = total

                if (linePoints.isNotEmpty()) {
                    chartType = "LINE"
                    println("Chart Type: LINE")
                    val sortedPoints = linePoints.sortedBy { it.first }
                    val lineDataPoints = sortedPoints.mapIndexed { index, (_, value) ->
                        Point(index.toFloat(), value)
                    }

                    lineChartData = LineChartData(
                        linePlotData = LinePlotData(
                            lines = listOf(Line(dataPoints = lineDataPoints))
                        )
                    )
                } else if (categoryMap.isNotEmpty()) {
                    chartType = "PIE"
                    println("Chart Type: PIE")
                    pieChartData = categoryMap.map { (k, v) ->
                        PieChartData.Slice(k, v.toFloat(), randomColor())
                    }
                } else {
                    chartType = "EMPTY"
                    println("Chart Type: EMPTY")
                }

                isLoading = false
            }
            .addOnFailureListener { exception ->
                println("Error fetching data: ${exception.message}")
                showToast("Failed to fetch data.")
                isLoading = false
            }
    }

    // Scaffold Layout
    Scaffold(topBar = { FixedTopBar(userEmail) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Filter Button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { showFilters = !showFilters }) {
                    Text(if (showFilters) "Hide Filters" else "Filter")
                }
            }

            // Filters
            if (showFilters) {
                Column {
                    Text("Select Category:")
                    SimpleDropdownMenu(
                        items = listOf("Job Supplies", "Grocery", "Utilities", "Rent", "Entertainment"),
                        selectedItem = selectedCategory,
                        onItemSelected = { selectedCategory = it }
                    )

                    Row {
                        Text("Start Date: $startDate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showDatePicker { startDate = it } }) { Text("Pick Start Date") }
                    }

                    Row {
                        Text("End Date: $endDate")
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showDatePicker { endDate = it } }) { Text("Pick End Date") }
                    }

                    Button(onClick = { fetchFilteredData() }) {
                        Text("Apply")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Spending
            Text("Total Spending: $${"%.2f".format(totalSpending)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            // Charts
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                println("Chart Type: $chartType")
                println("PieChartData Size: ${pieChartData.size}")
                println("LineChartData Lines: ${lineChartData?.linePlotData?.lines?.size ?: 0}")

                when (chartType) {
                    "LINE" -> {
                        println("Rendering Line Chart...")
                        lineChartData?.let {
                            LineChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                lineChartData = it
                            )
                        }
                    }
                    "PIE" -> {
                        println("Rendering Pie Chart...")
                        if (pieChartData.isNotEmpty()) {
                            BudgetPieChart(pieChartData)
                        } else {
                            Text("No data available.", color = Color.Gray)
                        }
                    }
                    "EMPTY" -> {
                        println("Rendering EMPTY State...")
                        Text("No data available for the selected filters.", color = Color.Gray)
                    }
                    else -> {
                        println("Unexpected Chart Type: $chartType")
                        Text("Unexpected chart type.", color = Color.Red)
                    }
                }
            }
        }
    }
}


/**
 * randomColor:
 * Generates a random color with fully opaque alpha.
 *
 * Features:
 * - Randomly generates red, green, and blue components.
 * - Returns a `Color` object using these random values.
 *
 * @return Color A randomly generated color with RGBA components.
 */
fun randomColor(): Color {
    return Color(
        red = (0..255).random() / 255f,
        green = (0..255).random() / 255f,
        blue = (0..255).random() / 255f,
        alpha = 1f
    )
}



/**
 * CategorySelectionDropdown:
 * A composable function that displays a dropdown menu for selecting a category.
 *
 * Features:
 * - Displays a list of selectable categories.
 * - Shows the currently selected category.
 * - Invokes a callback when a category is selected.
 *
 * @param categories A list of category strings to display in the dropdown.
 * @param selectedCategory The currently selected category, displayed as the dropdown title.
 * @param onCategorySelected A callback function invoked when the user selects a category.
 */
@Composable
fun CategorySelectionDropdown(
    categories: List<String>,             // List of category options
    selectedCategory: String,             // Currently selected category
    onCategorySelected: (String) -> Unit  // Callback invoked with the selected category
) {
    // State to track whether the dropdown menu is expanded or collapsed
    var expanded by remember { mutableStateOf(false) }

    // Box: Wraps the dropdown trigger and provides visual styling
    Box(
        modifier = Modifier
            .fillMaxWidth() // Takes up the full width of the parent container
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp)) // Adds a gray border with rounded corners
            .clickable { expanded = !expanded } // Toggles dropdown expansion on click
            .padding(16.dp) // Adds padding inside the box for better spacing
    ) {
        // Display the selected category or a default prompt if none is selected
        Text(
            text = selectedCategory.ifEmpty { "Select a Category" }, // Fallback text when no category is selected
            color = Color.Black // Sets the text color to black
        )

        // DropdownMenu: Displays the list of category options
        DropdownMenu(
            expanded = expanded,               // Controls whether the dropdown is visible
            onDismissRequest = { expanded = false } // Closes the dropdown when dismissed
        ) {
            // Loop through the list of categories and create a menu item for each
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(text = category) }, // Displays the category name as a menu item
                    onClick = {
                        onCategorySelected(category) // Invoke the callback with the selected category
                        expanded = false // Close the dropdown menu after selection
                    }
                )
            }
        }
    }
}


/**
 * BudgetPieChart:
 * A composable function that displays a pie chart along with a legend showing category labels and colors.
 *
 * Features:
 * - Uses PieChartData to create a chart with slices.
 * - Configures the chart to display without labels on the slices.
 * - Displays a color-coded legend alongside the pie chart.
 *
 * @param slices A list of PieChartData.Slice objects, each representing a segment in the pie chart,
 *               with a label, value, and color.
 */
@Composable
fun BudgetPieChart(slices: List<PieChartData.Slice>) {
    // Create pie chart data using the list of slices
    val pieChartData = PieChartData(
        slices = slices,           // The slices represent chart data
        plotType = PlotType.Pie    // Specifies the chart type as "Pie"
    )

    // Pie chart configuration (e.g., animations, alpha)
    val pieChartConfig = PieChartConfig(
        showSliceLabels = false,    // Disables labels on the pie chart slices
        isAnimationEnable = true,   // Enables animation for slice appearance
        activeSliceAlpha = 0.8f, // Sets transparency for active slices
        backgroundColor = Color.Transparent
    )

    // Row: Contains the pie chart on the left and the legend on the right
    Row(
        modifier = Modifier
            .fillMaxWidth()                // Makes the Row span the full width of the parent container
            .padding(16.dp),               // Adds padding around the entire Row
        horizontalArrangement = Arrangement.Center, // Centers content horizontally
        verticalAlignment = Alignment.CenterVertically // Aligns content vertically in the center
    ) {
        // Pie Chart on the left
        PieChart(
            modifier = Modifier
                .size(200.dp)              // Sets a fixed size for the pie chart
                .padding(end = 16.dp),     // Adds space between the pie chart and the legend
            pieChartData = pieChartData,   // The chart data
            pieChartConfig = pieChartConfig // The configuration for the pie chart
        )

        // Legend on the right side of the pie chart
        Column(
            verticalArrangement = Arrangement.Center,    // Centers legend items vertically
            horizontalAlignment = Alignment.Start        // Aligns legend items to the start
        ) {
            // Loop through each slice to create legend items
            slices.forEach { slice ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp), // Adds spacing between legend items
                    verticalAlignment = Alignment.CenterVertically // Aligns items vertically
                ) {
                    // Box: Displays a small color dot for the slice
                    Box(
                        modifier = Modifier
                            .size(12.dp)                         // Sets the size of the color dot
                            .background(slice.color, shape = CircleShape) // Uses slice color with a circular shape
                    )
                    Spacer(modifier = Modifier.width(8.dp))       // Adds horizontal space between dot and text

                    // Text: Displays the label for the slice
                    Text(
                        text = slice.label,                      // Category name
                        fontSize = 14.sp,                        // Sets text size to 14sp
                        color = MaterialTheme.colorScheme.onSurface                     // Sets text color to black
                    )
                }
            }
        }
    }
}


@Composable
fun BudgetBreakdownDonutChart() {
    val totalBudget = 5000f // Total budget in dollars

    // Donut Chart Data
    val donutChartData = PieChartData(
        slices = listOf(
            PieChartData.Slice("Housing", 1750F, Color(0xFF5F0A87)),
            PieChartData.Slice("Food", 1250f, Color(0xFF20BF55)),
            PieChartData.Slice("Transportation", 750f, Color(0xFFEC9F05)),
            PieChartData.Slice("Entertainment", 500f, Color(0xFFF53844)),
            PieChartData.Slice("Savings", 750f, Color(0xFF0496FF))
        ),
        plotType = PlotType.Donut
    )

    // Donut Chart Configuration
    val donutChartConfig = PieChartConfig(
        labelVisible = true,
        strokeWidth = 120f,
        labelColor = Color.White,
        labelFontSize = 14.sp,
        activeSliceAlpha = 0.9f,
        isAnimationEnable = true,
        chartPadding = 25,
        labelType = PieChartConfig.LabelType.VALUE,
    )

    // State to hold the currently selected slice
    var selectedSlice by remember { mutableStateOf<PieChartData.Slice?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Donut Pie Chart
        DonutPieChart(
            modifier = Modifier.fillMaxSize(),
            pieChartData = donutChartData,
            pieChartConfig = donutChartConfig,
            onSliceClick = { slice ->
                selectedSlice = slice
            }
        )

        // Centered Total Budget and Selected Slice Details
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Total Budget Display (Always Fixed)
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "$${totalBudget.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Selected Slice Details
            selectedSlice?.let { slice ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = slice.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Text(
                    text = "$${slice.value.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = slice.color
                )
            }
        }
    }
}

/**
 * NewScreen:
 * A composable function that displays a swipeable multi-page interface.
 *
 * Features:
 * - Allows users to swipe left and right to navigate between three pages:
 *   1. PhotoGalleryScreen
 *   2. SwipeInstructionPage
 *   3. ManualDataInputScreen
 * - Displays a page indicator at the top to show the current page.
 * - Uses drag gestures to detect swipes and update the current page.
 *
 * @note Pages are defined as `PhotoGalleryScreen`, `SwipeInstructionPage`, and `ManualDataInputScreen`.
 */
@Composable
fun NewScreen() {
    // State to track the current page (0, 1, or 2)
    var currentPage by remember { mutableStateOf(1) }

    // State to track the horizontal drag offset
    var offsetX by remember { mutableStateOf(0f) }

    // Box to hold the entire screen layout
    Box(
        modifier = Modifier
            .fillMaxSize()                 // Make the container take up the full screen
            .background(Color.White)       // Set the background color to white
    ) {
        // Column: Organizes the page indicator and the swipeable content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // PageIndicator: Displays the current page at the top of the screen
            PageIndicator(currentPage)

            // Swipeable content area
            Box(
                modifier = Modifier
                    .fillMaxSize()            // Make the box take the available space
                    .weight(1f)               // Allow the box to take up most of the vertical space
                    .pointerInput(Unit) {
                        // Detect drag gestures
                        detectDragGestures(
                            onDragEnd = {
                                // Determine page change based on drag offset
                                if (offsetX > 100) {
                                    // Swipe to the previous page (drag right)
                                    currentPage = (currentPage - 1).coerceAtLeast(0)
                                } else if (offsetX < -100) {
                                    // Swipe to the next page (drag left)
                                    currentPage = (currentPage + 1).coerceAtMost(2)
                                }
                                offsetX = 0f // Reset drag offset after swipe
                            },
                            onDrag = { _, dragAmount ->
                                // Track horizontal drag offset
                                offsetX += dragAmount.x
                            }
                        )
                    },
                contentAlignment = Alignment.Center // Align the content in the center of the box
            ) {
                // Show different content based on the current page
                when (currentPage) {
                    0 -> PhotoGalleryScreen()       // Page 0: Photo gallery
                    1 -> SwipeInstructionPage()     // Page 1: Swipe instructions
                    2 -> ManualDataInputScreen()    // Page 2: Manual data input
                }
            }
        }
    }
}


/**
 * PageIndicator:
 * A composable function that displays a visual indicator for the current page.
 *
 * Features:
 * - Shows a row of circular dots.
 * - Highlights the active dot corresponding to the current page.
 * - Supports up to three pages (indicated by three dots).
 *
 * @param currentPage The index of the currently active page (0-based).
 */
@Composable
fun PageIndicator(currentPage: Int) {
    // Row: Arranges the page indicator dots horizontally
    Row(
        modifier = Modifier
            .fillMaxWidth()                // Makes the row take the full screen width
            .padding(top = 20.dp)          // Adds padding to the top
            .background(Color.White)       // Sets a white background for the row
            .padding(vertical = 10.dp),    // Adds vertical padding to center the indicators
        horizontalArrangement = Arrangement.Center // Centers the dots horizontally
    ) {
        // Repeat 3 times to draw three dots for pages
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp) // Sets the size of each dot
                    .padding(4.dp) // Adds spacing between dots
                    .background(
                        color = if (index == currentPage) Color.Magenta else Color.LightGray, // Highlights the current page
                        shape = CircleShape // Makes the dot circular
                    )
            )
        }
    }
}

/**
 * SwipeInstructionPage:
 * A composable function that displays swipe instructions to the user.
 *
 * Features:
 * - Provides clear instructions for navigating between pages using swipe gestures.
 * - Uses a centered text with gray color and multi-line formatting for readability.
 *
 * Visual:
 * - "Swipe left for Camera/Gallery"
 * - "Swipe right for Manual Input"
 */
@Composable
fun SwipeInstructionPage() {
    // Box: A container to center the instruction text on the screen
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),                   // Makes the Box take up the full screen size
        contentAlignment = Alignment.Center // Centers the content inside the Box
    ) {
        // Text: Displays the swipe instructions in the center
        Text(
            text = "Swipe left for Camera/Gallery\nSwipe right for Manual Input", // Instructional text
            textAlign = TextAlign.Center,   // Aligns the text content to the center
            color = Color.Gray              // Sets the text color to gray for a subtle look
        )
    }
}

/**
 * AnimatedCheckmark:
 * A composable function that draws an animated checkmark inside a green circular background.
 *
 * Features:
 * - Uses a custom easing function for smooth animation.
 * - Animates the checkmark drawing progressively using a PathMeasure.
 * - Draws a partial checkmark based on the animation progress.
 *
 * @param modifier A Modifier to customize the size, position, and layout of the composable.
 */
@Composable
fun AnimatedCheckmark(modifier: Modifier = Modifier) {
    // Animatable state to track the progress of the checkmark animation
    val checkmarkProgress = remember { Animatable(0f) }

    // Custom easing function for smooth acceleration and deceleration
    val customEasing: (Float) -> Float = { fraction ->
        fraction * fraction * (3 - 2 * fraction) // Smoothstep easing
    }

    // Launch animation when the composable is first launched
    LaunchedEffect(Unit) {
        checkmarkProgress.animateTo(
            targetValue = 1f, // Animate progress to 1 (100%)
            animationSpec = tween(
                durationMillis = 1000, // Animation duration: 1 second
                easing = customEasing  // Use custom easing function
            )
        )
    }

    // Canvas to draw the green circle and the animated checkmark
    Canvas(
        modifier = modifier
            .size(150.dp)                             // Sets the canvas size to 150dp
            .background(Color.Green, shape = CircleShape) // Adds a green circular background
            .padding(20.dp)                           // Adds padding around the circle
    ) {
        // Canvas dimensions
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Step 1: Draw the circular green background
        drawCircle(
            color = Color.Green,               // Circle color
            radius = canvasWidth / 2           // Circle radius (half the width of the canvas)
        )

        // Step 2: Checkmark drawing logic
        val progress = checkmarkProgress.value // Current animation progress (0.0 to 1.0)

        if (progress > 0) { // Draw the checkmark only if animation has started
            // Define the path for the checkmark
            val checkmarkPath = Path().apply {
                moveTo(canvasWidth * 0.3f, canvasHeight * 0.55f) // Start point
                lineTo(canvasWidth * 0.45f, canvasHeight * 0.7f) // Middle point of the checkmark
                lineTo(canvasWidth * 0.7f, canvasHeight * 0.4f)  // End point of the checkmark
            }

            // Step 3: Measure and animate the path
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(checkmarkPath, false) // Measure the defined checkmark path

            val partialPath = Path()
            pathMeasure.getSegment(
                0f, // Start at the beginning of the path
                pathMeasure.length * progress, // Animate the path up to the current progress
                partialPath, // Destination path to draw
                true // Move to the start position
            )

            // Step 4: Draw the animated checkmark path
            drawPath(
                path = partialPath,                        // The partially animated path
                color = Color.White,                       // Checkmark color
                style = Stroke(
                    width = 8.dp.toPx(),                  // Stroke width for the checkmark
                    cap = StrokeCap.Round                 // Rounded ends for a smoother appearance
                )
            )
        }
    }
}


/**
 * PhotoGalleryScreen:
 * A composable function that allows users to upload an image via the camera or gallery.
 *
 * Features:
 * - Provides buttons to launch the device's camera or gallery.
 * - Uploads the selected image to a Veryfi API.
 * - Displays an animated checkmark on successful upload and a loading spinner while uploading.
 *
 * Behavior:
 * - Shows a "Take Photo" button to capture an image using the camera.
 * - Shows a "Select from Gallery" button to pick an image from the device gallery.
 * - Displays a progress indicator while uploading the image.
 * - Shows an animated checkmark for 3 seconds upon successful upload.
 */
@Composable
fun PhotoGalleryScreen() {
    // Local context to access system resources like cache directory
    val context = LocalContext.current

    // State to track whether the upload is complete
    var isUploadComplete by remember { mutableStateOf(false) }

    // State to track whether an upload is currently in progress
    var isLoading by remember { mutableStateOf(false) }

    // Camera launcher to capture an image and return a bitmap
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // If a valid bitmap is captured
        bitmap?.let {
            val uri = saveBitmapToCache(context, it) // Save the bitmap to cache and get its URI
            isLoading = true // Start the loading state
            uploadToVeryfi(
                uri = uri,
                context = context,
                onResponse = {
                    isLoading = false // Stop loading on success
                    isUploadComplete = true // Mark the upload as complete
                },
                onError = {
                    isLoading = false // Stop loading on error
                }
            )
        }
    }

    // Gallery launcher to pick an image and return a URI
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // If a valid URI is returned
        uri?.let {
            isLoading = true // Start the loading state
            uploadToVeryfi(
                uri = uri,
                context = context,
                onResponse = {
                    isLoading = false // Stop loading on success
                    isUploadComplete = true // Mark the upload as complete
                },
                onError = {
                    isLoading = false // Stop loading on error
                }
            )
        }
    }

    // Effect to reset the "Upload Complete" state after showing the checkmark for 3 seconds
    LaunchedEffect(isUploadComplete) {
        if (isUploadComplete) {
            kotlinx.coroutines.delay(3000) // Wait for 3 seconds
            isUploadComplete = false // Reset the upload complete state
        }
    }

    // Box: Acts as the container for the screen content
    Box(
        modifier = Modifier
            .fillMaxSize() // Make the Box take up the full screen
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp), // Add padding to the content
        contentAlignment = Alignment.Center // Center content inside the Box

    ) {
        when {
            // Show a loading spinner if the upload is in progress
            isLoading -> {
                CircularProgressIndicator()
            }
            // Show an animated checkmark if the upload is complete
            isUploadComplete -> {
                AnimatedCheckmark()
            }
            // Default view: Show camera and gallery buttons
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Button to launch the camera
                    Button(onClick = {
                        cameraLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, // Theme's primary color
                            contentColor = MaterialTheme.colorScheme.onPrimary // Text color for primary background
                        )
                    ) {
                        Text("Take Photo")
                    }

                    // Spacer to add vertical space between buttons
                    Spacer(modifier = Modifier.height(20.dp))

                    // Button to open the gallery and select an image
                    Button(onClick = {
                        galleryLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, // Theme's primary color
                            contentColor = MaterialTheme.colorScheme.onPrimary // Text color for primary background
                        )
                    ) {
                        Text("Select from Gallery")
                    }
                }
            }
        }
    }
}

/**
 * SimpleDropdownMenu:
 * A composable function that displays a simple dropdown menu with selectable items.
 *
 * Features:
 * - Displays a list of options when expanded.
 * - Highlights the currently selected item or shows a placeholder if none is selected.
 * - Closes the dropdown after selecting an item.
 *
 * @param items A list of strings representing the menu options.
 * @param selectedItem The currently selected item to be displayed in the dropdown trigger.
 * @param onItemSelected A callback function invoked when the user selects an item.
 */
@Composable
fun SimpleDropdownMenu(
    items: List<String>,              // List of options to display in the dropdown
    selectedItem: String,             // Currently selected item
    onItemSelected: (String) -> Unit  // Callback to notify when an item is selected
) {
    // State to track whether the dropdown is expanded or collapsed
    var expanded by remember { mutableStateOf(false) }

    // Box: Acts as the dropdown trigger and container for the dropdown menu
    Box(
        modifier = Modifier
            .wrapContentSize()                          // Adjusts size to wrap its content
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)) // Adds a gray border with rounded corners
            .clickable { expanded = !expanded }         // Toggles dropdown visibility when clicked
            .padding(10.dp)                             // Adds padding inside the box
    ) {
        // Display the currently selected item or a placeholder text
        Text(
            text = selectedItem.ifEmpty { "Select an Option" }, // Show placeholder if no item is selected
            color = MaterialTheme.colorScheme.onSurface         // Sets text color to black
        )

        // DropdownMenu: Displays the list of selectable items
        DropdownMenu(
            expanded = expanded,                     // Controls visibility of the dropdown menu
            onDismissRequest = { expanded = false }  // Closes the menu when dismissed
        ) {
            // Loop through each item in the provided list
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item,
                        color = MaterialTheme.colorScheme.onSurface) },   // Display each item as a menu option
                    onClick = {
                        onItemSelected(item)       // Invoke callback with the selected item
                        expanded = false           // Collapse the dropdown after selection
                    }
                )
            }
        }
    }
}


/**
 * ManualDataInputScreen:
 * A composable function that allows users to manually input transaction details.
 *
 * Features:
 * - Category selection via dropdown.
 * - Date selection using dropdown menus for year, month, and day.
 * - Itemized input for multiple items with names and prices.
 * - Tax input and automatic total calculation.
 * - Validation to ensure all required fields are filled before submission.
 * - Animated success checkmark displayed upon successful submission.
 *
 * State Management:
 * - Tracks selected category, tax amount, total, and dynamically added items.
 * - Displays a loading indicator and success animation during submission.
 */
@Composable
fun ManualDataInputScreen() {
    // List of predefined categories
    val categories = listOf(
        "Advertising & Marketing", "Automotive", "Bank Charges & Fees",
        "Legal & Professional Services", "Insurance", "Meals & Entertainment",
        "Office Supplies & Software", "Taxes & Licenses", "Travel",
        "Rent & Lease", "Repairs & Maintenance", "Payroll", "Utilities",
        "Job Supplies", "Grocery"
    )

    // State variables for managing inputs and UI states
    var selectedCategory by remember { mutableStateOf("Select a Category") }
    val items = remember { mutableStateListOf(Pair("", "")) } // List of items (name, price)
    var tax by remember { mutableStateOf("") }                // Tax input
    var total by remember { mutableStateOf(0.0) }             // Total calculation
    var isSubmitting by remember { mutableStateOf(false) }    // Submission state
    var showSuccessAnimation by remember { mutableStateOf(false) } // Success animation flag

    val context = LocalContext.current

    // Date Selection Logic
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    val years = (2000..2030).map { it.toString() }
    val months = (1..12).map { it.toString().padStart(2, '0') }
    var selectedYear by remember { mutableStateOf(currentYear.toString()) }
    var selectedMonth by remember { mutableStateOf(currentMonth.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(currentDay.toString().padStart(2, '0')) }

    // Calculate days in the selected month dynamically
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val month = selectedMonth.toInt()
        val year = selectedYear.toInt()
        when (month) {
            1, 3, 5, 7, 8, 10, 12 -> (1..31).map { it.toString().padStart(2, '0') }
            4, 6, 9, 11 -> (1..30).map { it.toString().padStart(2, '0') }
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) {
                (1..29).map { it.toString().padStart(2, '0') } // Leap year
            } else {
                (1..28).map { it.toString().padStart(2, '0') }
            }
            else -> emptyList()
        }
    }

    // Auto-calculate total when items or tax are updated
    LaunchedEffect(items, tax) {
        val itemPrices = items.mapNotNull { it.second.toDoubleOrNull() }.sum()
        val taxAmount = tax.toDoubleOrNull() ?: 0.0
        total = itemPrices + taxAmount
    }

    // Reset all fields to initial state
    fun resetFields() {
        selectedCategory = "Select a Category"
        selectedYear = currentYear.toString()
        selectedMonth = currentMonth.toString().padStart(2, '0')
        selectedDay = currentDay.toString().padStart(2, '0')
        items.clear()
        items.add(Pair("", ""))
        tax = ""
        total = 0.0
        isSubmitting = false
    }

    // Main layout scaffold
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        if (showSuccessAnimation) {
            // Display animated checkmark on successful submission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                AnimatedCheckmark()
            }

            // Hide animation after delay and reset fields
            LaunchedEffect(Unit) {
                delay(2000)
                showSuccessAnimation = false
                resetFields()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                // Category Selection
                Text("Select a Category:")
                SimpleDropdownMenu(
                    items = categories,
                    selectedItem = selectedCategory,
                    onItemSelected = { selectedCategory = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection
                Text("Select Year:",)
                DropdownMenuField(years, selectedYear) { selectedYear = it }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Month:")
                DropdownMenuField(months, selectedMonth) { selectedMonth = it }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Day:")
                DropdownMenuField(daysInMonth, selectedDay) { selectedDay = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Item Input Fields
                Text("Enter Items:")
                items.forEachIndexed { index, (name, price) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { updatedName -> items[index] = updatedName to price },
                            modifier = Modifier
                                .weight(1f),
                            label = { Text("Item Name", color = MaterialTheme.colorScheme.onSurface) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = price,
                            onValueChange = { updatedPrice ->
                                val regex = Regex("^\\d{0,10}(\\.\\d{0,2})?\$")
                                if (updatedPrice.matches(regex) || updatedPrice.isEmpty()) {
                                    items[index] = name to updatedPrice
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Price", color = MaterialTheme.colorScheme.onSurface) },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { items.removeAt(index) }) {
                            Text("Remove")
                        }
                    }
                }
                Button(
                    onClick = { items.add("" to "") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add Item")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tax Input
                Text("Tax:")
                OutlinedTextField(
                    value = tax,
                    onValueChange = { updatedTax ->
                        val regex = Regex("^\\d{0,10}(\\.\\d{0,2})?\$") // Regex: up to 4 digits and 2 decimal places
                        if (updatedTax.matches(regex) || updatedTax.isEmpty()) {
                            tax = updatedTax
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tax", color = MaterialTheme.colorScheme.onSurface) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Total Display
                Text("Total: $${"%.2f".format(total)}")

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button with Validation
                Button(
                    onClick = {
                        val hasEmptyItem = items.any { it.first.isBlank() || it.second.isBlank() }
                        when {
                            selectedCategory == "Select a Category" -> {
                                Toast.makeText(context, "Please select a category.", Toast.LENGTH_SHORT).show()
                            }
                            hasEmptyItem -> {
                                Toast.makeText(context, "Item name and price cannot be empty.", Toast.LENGTH_SHORT).show()
                            }
                            tax.isBlank() -> {
                                Toast.makeText(context, "Tax cannot be empty.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                isSubmitting = true
                                showSuccessAnimation = true
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(150.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit")
                    }
                }
            }
        }
    }
}



/**
 * DropdownMenuField:
 * A composable function that displays a dropdown menu for selecting an item from a list.
 *
 * Features:
 * - Displays a list of items as a dropdown menu.
 * - Highlights the currently selected item.
 * - Closes the menu when an item is selected or dismissed.
 *
 * @param items A list of strings representing the dropdown options.
 * @param selectedItem The currently selected item to display as the dropdown title.
 * @param onItemSelected A callback function invoked when the user selects an item.
 */
@Composable
fun DropdownMenuField(
    items: List<String>,              // List of options for the dropdown
    selectedItem: String,             // Currently selected item
    onItemSelected: (String) -> Unit  // Callback to notify when an item is selected
) {
    // State to track whether the dropdown menu is expanded
    var expanded by remember { mutableStateOf(false) }

    // Box acts as the dropdown trigger and container
    Box(
        modifier = Modifier
            .fillMaxWidth()                               // Makes the Box take up the full width of the parent
            .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)) // Adds a gray border with rounded corners
            .clickable { expanded = !expanded }           // Toggles the dropdown visibility when clicked
            .padding(16.dp)                               // Adds padding inside the dropdown trigger
    ) {
        // Displays the currently selected item
        Text(
            text = selectedItem,                         // Displays the selected item
            color = MaterialTheme.colorScheme.onSurface  // Text color set to black
        )

        // DropdownMenu: Shows the list of selectable items
        DropdownMenu(
            expanded = expanded,                         // Controls the visibility of the dropdown menu
            onDismissRequest = { expanded = false }      // Closes the menu when dismissed
        ) {
            // Iterate through the list of items and display them as menu items
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item,
                        color = MaterialTheme.colorScheme.onSurface) },               // Displays the item text
                    onClick = {
                        onItemSelected(item)             // Invoke the callback with the selected item
                        expanded = false                 // Collapse the dropdown menu after selection
                    }
                )
            }
        }
    }
}



//Settings Screen ---------------------------------------------------------------------------------
//Ethan's Code
/**
 * SettingsScreen:
 * A composable function that displays the settings screen for the app.
 *
 * Features:
 * - Displays user information.
 * - Allows the user to set and save a budget.
 * - Provides theme settings options (placeholder).
 * - Allows the user to log out.
 * - Loads and saves budget information to Firebase.
 * - Shows a success message when the budget is successfully saved.
 *
 * @param userEmail The email of the logged-in user to display on the settings screen.
 * @param onSignOut A callback function triggered when the user logs out.
 * @param navController NavHostController to handle navigation between screens.
 */
@Composable
fun SettingsScreen(
    userEmail: String,               // Logged-in user's email
    onSignOut: () -> Unit,           // Callback for user sign-out
    navController: NavHostController, // Navigation controller
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    // State to manage the budget input
    var budget by remember { mutableStateOf("") }
    // State to show a success message when the budget is saved
    var showSaveSuccess by remember { mutableStateOf(false) }

    // Load the user's budget from Firebase when the screen is first displayed
    LaunchedEffect(Unit) {
        loadBudgetFromFirebase { loadedBudget -> // Callback to retrieve budget from Firebase
            budget = loadedBudget // Update the budget state with the retrieved value
        }
    }

    // Main layout scaffold with a fixed top bar
    Scaffold(
        topBar = { FixedTopBar(userEmail) } // Displays the user's email in the top bar
    ) { innerPadding ->
        Column() {
            Spacer(modifier = Modifier.height(10.dp))
            // LazyColumn: A vertically scrollable list with items spaced evenly
            LazyColumn(
                contentPadding = innerPadding, // Adds padding to prevent overlapping with the top bar
                modifier = Modifier
                    .fillMaxSize()             // Makes the list take up the entire screen
                    .padding(horizontal = 20.dp), // Adds horizontal padding for aesthetics
                verticalArrangement = Arrangement.spacedBy(10.dp) // Spacing between items
            ) {
                // Display user information
                item {
                    UserInformationCard(userEmail)
                }
                // Display the budget selection card
                item {
                    BudgetSelectionCard(
                        budget = budget, // Current budget
                        onBudgetChange = { newBudget -> budget = newBudget }, // Updates budget state
                        onSave = {
                            // Save the budget to Firebase and show success message if successful
                            saveBudgetToFirebase(budget) { success ->
                                if (success) showSaveSuccess = true
                            }
                        }
                    )
                }
                // Placeholder for theme settings card
                item {
                    ThemeSettingsCard(
                        isDarkThemeEnabled = isDarkTheme,
                        onThemeChange = { newTheme ->
                            onThemeChange(newTheme) // Update theme state
                            saveThemePreferenceToFirebase(newTheme) { success ->
                                if (success) println("Theme saved to Firestore.")
                                else println("Failed to save theme to Firestore.")
                            }
                        }
                    )
                }
                // Display logout card
                item {
                    LogoutCard(onSignOut) // Trigger sign-out when pressed
                }
            }
        }



        // Show a success toast message after saving the budget
        if (showSaveSuccess) {
            ToastMessage("Budget saved successfully!") // Displays a toast message
            showSaveSuccess = false // Reset the success state to avoid repeated toasts
        }
    }
}


/**
 * ToastMessage:
 * A composable function that displays a short toast message on the screen.
 *
 * Features:
 * - Uses `Toast` from the Android system to display the provided message.
 * - Triggered when the `message` parameter changes.
 *
 * @param message A string containing the message to display in the toast.
 */
@Composable
fun ToastMessage(message: String) {
    // Get the current context for showing the toast
    val context = LocalContext.current

    // LaunchedEffect: Trigger the toast when the message parameter changes
    LaunchedEffect(message) {
        // Display a short-duration toast message
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}


/**
 * UserInformationCard:
 * A composable function that displays the user's information inside a styled card.
 *
 * Features:
 * - Displays a title and the user's email address.
 * - Uses Material Design's `Card` for styling with rounded corners and elevation.
 *
 * @param userEmail The email address of the user to be displayed on the card.
 */
@Composable
fun UserInformationCard(userEmail: String) {
    // Card: A material container with elevation and rounded corners
    Card(
        modifier = Modifier.fillMaxWidth(),                   // Makes the card span the full width
        shape = RoundedCornerShape(10.dp),                   // Adds rounded corners with a 10.dp radius
        elevation = CardDefaults.cardElevation(4.dp)         // Adds shadow/elevation for a lifted effect
    ) {
        // Column: Organizes the content of the card vertically
        Column(
            modifier = Modifier.padding(20.dp)               // Adds padding around the content
        ) {
            // Title Text: "User Information" styled with bold font
            Text(
                text = "User Information",
                style = MaterialTheme.typography.titleMedium, // Uses MaterialTheme for consistent styling
                fontWeight = FontWeight.Bold                 // Makes the title bold
            )

            Spacer(modifier = Modifier.height(4.dp))         // Adds spacing between title and email text

            // Email Text: Displays the user's email in gray color
            Text(
                text = "Your Email: $userEmail",             // Shows the email address
                color = Color.Gray                          // Sets the text color to gray
            )
        }
    }
}


/**
 * BudgetSelectionCard:
 * A composable function that provides an input field for the user to set their budget
 * and a button to save or update it.
 *
 * Features:
 * - Displays a card with a title, description, budget input field, and a save button.
 * - Allows only numeric input for the budget field.
 * - Updates the budget when the save button is clicked.
 *
 * @param budget The current budget value as a string.
 * @param onBudgetChange A callback invoked when the budget input changes.
 * @param onSave A callback triggered when the user clicks the save button.
 */
@Composable
fun BudgetSelectionCard(
    budget: String,                     // Current budget value
    onBudgetChange: (String) -> Unit,   // Callback to update the budget
    onSave: () -> Unit                  // Callback to save the budget
) {
    // Card: A styled container with elevation and rounded corners
    Card(
        modifier = Modifier.fillMaxWidth(),                   // Makes the card span the full width
        shape = RoundedCornerShape(10.dp),                   // Adds rounded corners with a 10.dp radius
        elevation = CardDefaults.cardElevation(4.dp)         // Adds subtle elevation for a lifted effect
    ) {
        // Column: Arranges the content vertically inside the card
        Column(
            modifier = Modifier
                .fillMaxWidth()                              // Makes the column span the full width
                .padding(20.dp),                             // Adds padding around the content
            verticalArrangement = Arrangement.spacedBy(10.dp) // Spacing between child elements
        ) {
            // Title Text: "Budget Selection"
            Text(
                text = "Budget Selection",                  // Section title
                style = MaterialTheme.typography.titleMedium, // Material design title styling
                fontWeight = FontWeight.Bold                // Makes the text bold
            )

            // Subtitle Text: Provides instructions
            Text(
                text = "Set your budget for expenses.",     // Instructional text
                color = Color.Gray                         // Sets text color to gray
            )

            // Row: Arranges the input field and button horizontally
            Row(
                modifier = Modifier.fillMaxWidth(),         // Makes the row span the full width
                verticalAlignment = Alignment.CenterVertically, // Aligns content vertically in the center
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Spacing between elements in the row
            ) {
                // OutlinedTextField: Input field for budget entry
                OutlinedTextField(
                    value = budget,                        // Current value of the input field
                    onValueChange = { newBudget ->         // Callback when input changes
                        // Ensure only numeric input is allowed
                        if (newBudget.all { it.isDigit() }) {
                            onBudgetChange(newBudget)      // Update the budget state
                        }
                    },
                    label = { Text("Enter Budget") },      // Label shown above the input field
                    placeholder = { Text("0") },          // Placeholder text shown when empty
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number // Restrict keyboard to numbers only
                    ),
                    singleLine = true,                    // Restrict the input to a single line
                    modifier = Modifier.weight(1f)        // Input field takes most of the row space
                )

                // Button: Trigger to save or update the budget
                Button(onClick = { onSave() }) {
                    Text("Update Budget")                 // Button text
                }
            }
        }
    }
}




/**
 * saveBudgetToFirebase:
 * A function that saves the user's budget to a specific document in Firebase Firestore.
 *
 * Features:
 * - Saves the budget value to Firestore under the logged-in user's data.
 * - Ensures that the user is authenticated before attempting to save.
 * - Uses `SetOptions.merge()` to avoid overwriting other fields in the document.
 * - Provides success or failure feedback through a callback.
 *
 * @param budget The budget value (as a string) to be saved to Firestore.
 * @param onComplete A callback function that returns a Boolean indicating success (true) or failure (false).
 */
fun saveBudgetToFirebase(budget: String, onComplete: (Boolean) -> Unit) {
    // Access the Firestore database instance
    val firestore = FirebaseFirestore.getInstance()

    // Retrieve the currently logged-in user's ID
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Check if the user is logged in
    if (userId != null) {
        // Prepare the data to be saved: a map with a "budget" key
        val budgetData = mapOf("budget" to budget)

        // Save the budget data to the user's Firestore document
        firestore.collection("users")                         // Access the "users" collection
            .document(userId)                                 // Target the document named after the user ID
            .collection("userData")                          // Access the "userData" sub-collection
            .document("budget")                              // Target the "budget" document
            .set(budgetData, SetOptions.merge())             // Use merge to avoid overwriting existing fields
            .addOnSuccessListener {
                // Called when the data is successfully saved
                println("Budget saved successfully!")        // Log success message
                onComplete(true)                             // Trigger the callback with success = true
            }
            .addOnFailureListener { e ->
                // Called when an error occurs while saving data
                println("Error saving budget: ${e.message}") // Log the error message
                onComplete(false)                           // Trigger the callback with success = false
            }
    } else {
        // Handle the case where the user is not logged in
        println("User not logged in")                       // Log warning message
        onComplete(false)                                   // Trigger the callback with success = false
    }
}



/**
 * loadBudgetFromFirebase:
 * A function that loads the user's budget value from Firebase Firestore.
 *
 * Features:
 * - Retrieves the budget document for the currently logged-in user.
 * - Provides the loaded budget value via a callback.
 * - Handles errors gracefully and defaults to an empty string if retrieval fails.
 *
 * Path in Firestore:
 * - Collection: "users"
 * - Document: {userId} (unique user document)
 * - Sub-collection: "userData"
 * - Document: "budget"
 *
 * @param onLoaded A callback function that returns the loaded budget as a String.
 */
fun loadBudgetFromFirebase(onLoaded: (String) -> Unit) {
    // Access the Firestore database instance
    val firestore = FirebaseFirestore.getInstance()

    // Retrieve the currently logged-in user's ID
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Check if the user is logged in
    if (userId != null) {
        // Attempt to retrieve the "budget" document under the user's data
        firestore.collection("users")                         // Access the "users" collection
            .document(userId)                                 // Target the document named after the user ID
            .collection("userData")                          // Access the "userData" sub-collection
            .document("budget")                              // Target the "budget" document
            .get()                                           // Fetch the document
            .addOnSuccessListener { document ->
                // Retrieve the "budget" field as a string, defaulting to an empty string if null
                val budget = document.getString("budget") ?: ""
                println("Budget loaded: $budget")            // Log the loaded budget
                onLoaded(budget)                             // Pass the budget to the callback
            }
            .addOnFailureListener {
                // Handle the case where the document retrieval fails
                println("Error loading budget: ${it.message}") // Log the error message
                onLoaded("")                                  // Return an empty string as default
            }
    } else {
        // Handle the case where no user is logged in
        println("User not logged in")                        // Log a message for debugging
        onLoaded("")                                         // Return an empty string as default
    }
}




/**
 * ThemeSettingsCard:
 * A composable function that displays a card allowing the user to toggle between
 * dark and light themes using a switch.
 *
 * Features:
 * - Displays a title and description for theme settings.
 * - Includes a switch to enable or disable dark mode.
 * - State is managed locally to reflect the toggle's position.
 *
 * Note: This implementation only updates local state and does not apply global theme changes.
 */
@Composable
fun ThemeSettingsCard(
    isDarkThemeEnabled: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    // Card: A styled container with rounded corners and elevation
    Card(
        modifier = Modifier.fillMaxWidth(),                   // Makes the card span the full width
        shape = RoundedCornerShape(10.dp),                   // Adds rounded corners with a 10.dp radius
        elevation = CardDefaults.cardElevation(4.dp)         // Adds subtle elevation for a lifted effect
    ) {
        // Row: Arranges the content horizontally with space between elements
        Row(
            modifier = Modifier
                .fillMaxWidth()                              // Makes the row span the full width of the card
                .padding(20.dp),                             // Adds padding around the row content
            verticalAlignment = Alignment.CenterVertically,  // Aligns content vertically in the center
            horizontalArrangement = Arrangement.SpaceBetween // Adds space between the column and switch
        ) {
            // Column: Holds the title and description text
            Column {
                // Title Text: "Theme Settings"
                Text(
                    text = "Theme Settings",                // Displays the title
                    style = MaterialTheme.typography.titleMedium, // Applies MaterialTheme title styling
                    fontWeight = FontWeight.Bold            // Makes the title bold
                )
                Spacer(modifier = Modifier.height(4.dp))     // Adds spacing between title and description

                // Description Text: Provides instructions
                Text(
                    text = "Switch between dark and light",  // Informative description
                    color = Color.Gray                      // Sets the text color to gray
                )
            }

            // Switch: Toggles the dark theme on/off
            Switch(
                checked = isDarkThemeEnabled,               // Reflects the current state of the switch
                onCheckedChange = { onThemeChange(it) } // Updates the state when the switch is toggled
            )
        }
    }
}

fun saveThemePreferenceToFirebase(isDarkTheme: Boolean, onComplete: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId != null) {
        firestore.collection("users")
            .document(userId)
            .collection("userData")
            .document("preferences") // Fixed document for theme preferences
            .set(mapOf("isDarkTheme" to isDarkTheme))
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    } else {
        println("User not logged in.")
        onComplete(false)
    }
}

fun loadThemePreferenceFromFirebase(onLoaded: (Boolean) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId != null) {
        firestore.collection("users")
            .document(userId)
            .collection("userData")
            .document("preferences")
            .get()
            .addOnSuccessListener { document ->
                val isDarkTheme = document.getBoolean("isDarkTheme") ?: false
                onLoaded(isDarkTheme)
            }
            .addOnFailureListener {
                println("Error loading theme preference: ${it.message}")
                onLoaded(false) // Default to false (light theme)
            }
    } else {
        println("User not logged in.")
        onLoaded(false)
    }
}



/**
 * LogoutCard:
 * A composable function that displays a card with logout functionality.
 *
 * Features:
 * - Displays a title and description for logout.
 * - Provides a "Sign Out" button to log the user out using Firebase Authentication.
 * - Calls a callback function after a successful logout.
 *
 * @param onSignOut A callback function triggered when the user clicks the "Sign Out" button.
 */
@Composable
fun LogoutCard(onSignOut: () -> Unit) {
    // Card: A styled container with elevation and rounded corners
    Card(
        modifier = Modifier.fillMaxWidth(),                   // Makes the card span the full width
        shape = RoundedCornerShape(10.dp),                   // Adds rounded corners with a 10.dp radius
        elevation = CardDefaults.cardElevation(4.dp)         // Adds subtle elevation for a lifted effect
    ) {
        // Column: Arranges the content vertically
        Column(
            modifier = Modifier.padding(20.dp),              // Adds padding around the content
            verticalArrangement = Arrangement.spacedBy(10.dp) // Adds spacing between child elements
        ) {
            // Title Text: "Logout"
            Text(
                text = "Logout",                             // Displays the section title
                style = MaterialTheme.typography.titleMedium, // Applies MaterialTheme for consistent styling
                fontWeight = FontWeight.Bold                 // Makes the title bold
            )

            // Description Text: Provides instructions to the user
            Text(
                text = "Sign out from your account.",        // Instructional text
                color = Color.Gray                           // Sets the text color to gray
            )

            Spacer(modifier = Modifier.height(8.dp))         // Adds spacing between description and button

            // Sign Out Button
            Button(
                onClick = {
                    // Perform Firebase sign-out
                    FirebaseAuth.getInstance().signOut()

                    // Trigger the callback to notify the parent composable
                    onSignOut()
                },
                modifier = Modifier.align(Alignment.End)     // Aligns the button to the end (right) of the row
            ) {
                Text("Sign Out")                             // Button text
            }
        }
    }
}



/**
 * LoginScreen:
 * A composable function that manages the login state and displays the appropriate screen.
 *
 * Features:
 * - Displays the `AuthScreen` when the user is not logged in.
 * - Displays the `MainAppNav` when the user is authenticated.
 * - Handles user sign-out by resetting the login state.
 *
 * @note This function uses Firebase Authentication to manage user authentication state.
 */
@Composable
fun LoginScreen(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    // Firebase Authentication instance
    val auth = FirebaseAuth.getInstance()

    // State to hold the currently authenticated user
    var user by remember { mutableStateOf(auth.currentUser) }

    // Check if the user is logged in
    if (user == null) {
        // Display the authentication screen when no user is logged in
        AuthScreen {
            // Update the user state after a successful login
            user = auth.currentUser
        }
    } else {
        // Display the main application navigation screen when the user is logged in
        MainAppNav(
            userEmail = user!!.email ?: "Unknown", // Pass the user's email or "Unknown" if null
            onSignOut = {
                // Sign out the user and reset the state
                auth.signOut()
                user = null

            },
            isDarkTheme = isDarkTheme,
            onThemeChange = onThemeChange
        )

        /*
        Optional: If you want to test Firestore or display a Firestore screen instead of MainAppNav,
        uncomment the following code block:

        FirestoreTestScreen(
            userEmail = user!!.email ?: "Unknown", // Pass the user's email
            onSignOut = {
                auth.signOut()
                user = null
            }
        )
        */
    }
}


/**
 * AuthScreen:
 * A composable function that provides a user interface for signing in or signing up
 * with Firebase Authentication using email and password.
 *
 * Features:
 * - Allows users to input their email and password.
 * - Provides "Sign In" and "Sign Up" options.
 * - Displays an error message if authentication fails.
 * - Invokes a callback function when authentication is successful.
 *
 * @param onAuthComplete A callback function triggered when authentication is successfully completed.
 */
@Composable
fun AuthScreen(onAuthComplete: () -> Unit) {
    // Initialize Firebase Authentication instance
    val auth = FirebaseAuth.getInstance()

    // State variables for email, password, and error message
    var email by remember { mutableStateOf("") }                // Stores the email input
    var password by remember { mutableStateOf("") }             // Stores the password input
    var errorMessage by remember { mutableStateOf<String?>(null) } // Holds an error message when sign-in fails

    val textColor = Color(0xFF000000) // Black text
    val buttonColor = Color(0xFF414e5e) // Blue button color

    val buttonModifier = Modifier
        .fillMaxWidth() // Makes the buttons take up the full width
        .height(48.dp) // Ensures consistent height for both buttons

    // Main layout: Column to arrange components vertically
    Column(
        modifier = Modifier
            .fillMaxSize()                                     // Makes the column take up the full screen
            .padding(35.dp),                                   // Adds padding around the content
        verticalArrangement = Arrangement.Center,             // Centers the content vertically
        horizontalAlignment = Alignment.CenterHorizontally     // Centers the content horizontally
    ) {
        // Header Text
        Text("Welcome to WasteWise", fontSize = 25.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(10.dp))

        Text("Sign in or Sign up with Firebase:")

        Spacer(modifier = Modifier.height(10.dp))             // Adds vertical space

        // Email Input Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = textColor.copy(alpha = 0.7f)) },
            textStyle = LocalTextStyle.current.copy(color = textColor),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = buttonColor,
                unfocusedBorderColor = buttonColor,
            )
        )

        Spacer(modifier = Modifier.height(10.dp))             // Adds vertical space

        // Password Input Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = textColor.copy(alpha = 0.7f)) },
            textStyle = LocalTextStyle.current.copy(color = textColor),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = buttonColor,
                unfocusedBorderColor = buttonColor,
            )
        )

        Spacer(modifier = Modifier.height(20.dp))             // Adds vertical space

        // Sign In Button
        Button(
            onClick = {
                // Firebase Authentication: Sign in with email and password
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Invoke the callback when sign-in is successful
                            onAuthComplete()
                        } else {
                            // Display the error message if sign-in fails
                            errorMessage = task.exception?.localizedMessage
                        }
                    }
            },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = buttonModifier
        ) {
            Text("Sign In", color = Color.White)                                   // Button text
        }

        Spacer(modifier = Modifier.height(10.dp))             // Adds vertical space

        // Sign Up Button
        TextButton(
            onClick = {
                // Firebase Authentication: Create a new user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Invoke the callback when sign-up is successful
                            onAuthComplete()
                        } else {
                            // Display the error message if sign-up fails
                            errorMessage = task.exception?.localizedMessage
                        }
                    }
            },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            modifier = buttonModifier
        ) {
            Text("Sign Up", color = Color.White)                                   // Button text
        }

        // Display Error Message
        errorMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))         // Adds vertical space
            Text(
                text = "Error: $it",                          // Displays the error message
                color = MaterialTheme.colorScheme.error       // Uses the theme's error color
            )
        }
    }
}