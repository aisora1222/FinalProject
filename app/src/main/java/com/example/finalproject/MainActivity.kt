package com.example.finalproject

import android.annotation.SuppressLint
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import com.example.finalproject.utils.VeryfiApiClient
import com.google.gson.Gson

import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import co.yml.charts.common.model.PlotType
import co.yml.charts.ui.piechart.charts.DonutPieChart
import co.yml.charts.ui.piechart.charts.PieChart
import co.yml.charts.ui.piechart.models.PieChartConfig
import co.yml.charts.ui.piechart.models.PieChartData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinalProjectTheme {
                LoginScreen()
//                Scaffold(
//                    modifier = Modifier.fillMaxSize()
//                ) { innerPadding ->
//                    ReceiptCaptureScreen(modifier = Modifier.padding(innerPadding))
//                }
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
        Spacer(modifier = Modifier.height(50.dp))

        Button(onClick = {
            isLoading = true
            cameraLauncher.launch(null)
        }) {
            Text("Take Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            isLoading = true
            galleryLauncher.launch("image/*")
        }) {
            Text("Select from Gallery")
        }

        Spacer(modifier = Modifier.height(16.dp))

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


fun saveRawJsonToFirestore(jsonString: String, ) {
    val firestore = FirebaseFirestore.getInstance()
    val documentData = mapOf("raw_json" to jsonString)

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId != null) {
        firestore.collection("users").document(userId).collection("userData")
            .add(documentData)
            .addOnSuccessListener { documentReference ->
                println("Document saved with ID: ${documentReference.id}")
            }
            .addOnFailureListener { error ->
                println("Failed to save document: $error")
            }
    }
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
                saveRawJsonToFirestore(response)
                println("Full JSON Response: $response") 

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

                val itemsDetails = lineItems.joinToString(separator = "\n") { item ->
                    val itemMap = item as? Map<*, *> ?: return@joinToString "Unknown item"
                    val description = itemMap["description"] as? String ?: "No description"
                    val totalItem = itemMap["total"] as? Double ?: "Unknown total"
                    "$description: $$totalItem"
                }

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


//Brandons Code
@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppNav(userEmail: String, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    var isExpanded by remember { mutableStateOf(false) }


    Scaffold(
        //User Greeting
        topBar = { FixedTopBar(userEmail) },
        //Bottom Navigation Bar (Main, New, Settings)
        bottomBar = { ExpandableBottomNavigationBar(navController, isExpanded, onExpandToggle = { isExpanded = !isExpanded }, userEmail, onSignOut) }
    ) {
        NavigationGraph(navController, userEmail, onSignOut)
    }
}

@Composable
fun ExpandableBottomNavigationBar(
    navController: NavHostController,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    userEmail: String,
    onSignOut: () -> Unit
) {
    Surface() {
        Column {
            // Expand/Collapse Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand/Collapse"
                    )
                }
            }

            // Navigation Row Visibility
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(50)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(50)) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Magenta)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation Tabs
                    BottomNavigationTab("Main", navController, "main")
                    BottomNavigationTab("New", navController, "new")
                    BottomNavigationTab("Settings", navController, "settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedTopBar(userEmail: String) {
    Surface() {
        TopAppBar(
            title = { Text(
                text = "Hello, $userEmail",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Magenta)
                    .padding(vertical = 8.dp),
            ) }
        )
    }
}

@Composable
fun BottomNavigationTab(label: String, navController: NavHostController, route: String) {
    Text(
        text = label,
        modifier = Modifier
            .padding(16.dp)
            .clickable { navController.navigate(route) }

    )
}

@Composable
fun NavigationGraph(navController: NavHostController, userEmail: String, onSignOut: () -> Unit) {

    NavHost(navController, startDestination = "new") {
        composable("main") { MainScreen(userEmail, onSignOut) }
        composable("new") { NewScreen() }
        composable("settings") { SettingsScreen() }
    }
}
//Main Screen ---------------------------------------------------------------------------------
@Composable
fun MainScreen(userEmail: String, onSignOut: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    var inputText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var dataList by remember { mutableStateOf<List<String>>(emptyList()) }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    // Fetch data from Firestore collection
    LaunchedEffect(Unit) {
        if (userId != null) {
            firestore.collection("users").document(userId).collection("userData")
                .get()
                .addOnSuccessListener { result ->
                    dataList = result.documents.mapNotNull { it.getString("userInput") }
                }
                .addOnFailureListener { e ->
                    statusMessage = "Error fetching data: ${e.localizedMessage}"
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Text("Hello, $userEmail")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter some data") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (userId != null) {
                firestore.collection("users").document(userId).collection("userData")
                    .add(mapOf("userInput" to inputText))
                    .addOnSuccessListener {
                        statusMessage = "Data submitted successfully!"
                        // Refresh the data list after submission
                        firestore.collection("users").document(userId).collection("userData")
                            .get()
                            .addOnSuccessListener { result ->
                                dataList = result.documents.mapNotNull { it.getString("userInput") }
                            }
                    }
                    .addOnFailureListener { e ->
                        statusMessage = "Error submitting data: ${e.localizedMessage}"
                    }
            }
        }) {
            Text("Submit to Firestore")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Data from Firestore:")
        Spacer(modifier = Modifier.height(8.dp))

        // Display the data in a list
        dataList.forEach { item ->
            Text("- $item")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            onSignOut() // Navigate back to the Sign-In screen
        }) {
            Text("Sign Out")
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Total Budget Breakdown",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                BudgetTotalPieChart()
            }
            item {
                Text(
                    text = "Detailed Budget Breakdown",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                BudgetBreakdownDonutChart()
            }
            // Add more charts or components here
            items(10) { index -> // Example: Adding multiple charts dynamically
                ExampleChart(title = "Chart $index")
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ExampleChart(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Replace with your chart component
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.Gray, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Chart Content")
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = slice.label,
                    fontSize = 16.sp,
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

@Composable
fun BudgetTotalPieChart() {
    val totalBudget = 5000f // Total budget in dollars
    val currentBudget = 3000f // Current budget in dollars

    // Donut Chart Data
    val totalPieChartData = PieChartData(
        slices = listOf(
            PieChartData.Slice("Total", totalBudget, Color(0xFF5F0A87)),
            PieChartData.Slice("Current", currentBudget, Color(0xFF03A9F4)),
        ),
        plotType = PlotType.Pie
    )

    // Donut Chart Configuration
    val totalPieChartConfig = PieChartConfig(
        labelVisible = true,
        strokeWidth = 120f,
        labelColor = Color.White,
        labelFontSize = 14.sp,
        activeSliceAlpha = 0.9f,
        isAnimationEnable = true,
        chartPadding = 25,
        labelType = PieChartConfig.LabelType.VALUE,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Total Pie Chart
        PieChart(
            modifier = Modifier.fillMaxSize(),
            pieChartData = totalPieChartData,
            pieChartConfig = totalPieChartConfig,
        )
    }
}

//New Screen ---------------------------------------------------------------------------------
@Composable
fun NewScreen() {
    //Here we should add the logic to add a new receipt to the database (picture or manual)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("This is where we are gonna add receipts")
        ReceiptCaptureScreen()
    }
}

//Settings Screen ---------------------------------------------------------------------------------
@Composable
fun SettingsScreen() {
    // Generate 100 fake settings
    val settingsList = List(100) { index ->
        "Setting ${index + 1}" to "Description for Setting ${index + 1}"
    }

    // LazyColumn to display settings
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(settingsList) { setting ->
            SettingItem(title = setting.first, description = setting.second)
        }
    }
}

@Composable
fun SettingItem(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title,)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description,)
        }
    }
}

//Ethan's Code

@Composable
fun LoginScreen() {
    val auth = FirebaseAuth.getInstance()
    var user by remember { mutableStateOf(auth.currentUser) }

    if (user == null) {
        AuthScreen { user = auth.currentUser }
    } else {
        MainAppNav(
            userEmail = user!!.email ?: "Unknown",
            onSignOut = { user = null }
        )
        /*
        FirestoreTestScreen(
            userEmail = user!!.email ?: "Unknown",
            onSignOut = { user = null }
        )

         */
    }
}

@Composable
fun AuthScreen(onAuthComplete: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign in or Sign up with Firebase:")
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Button
        Button(onClick = {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onAuthComplete()
                    } else {
                        errorMessage = task.exception?.localizedMessage
                    }
                }
        }) {
            Text("Sign In")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sign Up Button
        TextButton(onClick = {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onAuthComplete()
                    } else {
                        errorMessage = task.exception?.localizedMessage
                    }
                }
        }) {
            Text("Sign Up")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}


@Composable
fun FirestoreTestScreen(userEmail: String, onSignOut: () -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    var inputText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var dataList by remember { mutableStateOf<List<String>>(emptyList()) }

    // Fetch data from Firestore collection
    LaunchedEffect(Unit) {
        firestore.collection("testCollection")
            .get()
            .addOnSuccessListener { result ->
                dataList = result.documents.mapNotNull { it.getString("userInput") }
            }
            .addOnFailureListener { e ->
                statusMessage = "Error fetching data: ${e.localizedMessage}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hello, $userEmail")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter some data") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            firestore.collection("testCollection")
                .add(mapOf("userInput" to inputText))
                .addOnSuccessListener {
                    statusMessage = "Data submitted successfully!"
                    // Refresh the data list after submission
                    firestore.collection("testCollection")
                        .get()
                        .addOnSuccessListener { result ->
                            dataList = result.documents.mapNotNull { it.getString("userInput") }
                        }
                }
                .addOnFailureListener { e ->
                    statusMessage = "Error submitting data: ${e.localizedMessage}"
                }
        }) {
            Text("Submit to Firestore")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Data from Firestore:")
        Spacer(modifier = Modifier.height(8.dp))

        // Display the data in a list
        dataList.forEach { item ->
            Text("- $item")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            onSignOut() // Navigate back to the Sign-In screen
        }) {
            Text("Sign Out")
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}