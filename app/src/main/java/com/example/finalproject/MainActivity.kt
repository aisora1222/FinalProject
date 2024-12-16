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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap

// Compose foundation and layout
import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items


// Material design
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown

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
import android.app.DatePickerDialog
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Kotlin coroutine
import kotlinx.coroutines.delay
import java.io.File



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
            .padding(20.dp)
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

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            isLoading = true
            galleryLauncher.launch("image/*")
        }) {
            Text("Select from Gallery")
        }

        Spacer(modifier = Modifier.height(20.dp))

        imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .size(200.dp)
                    .background(Color.LightGray, RoundedCornerShape(10.dp))
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

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
        // Path that the data is being stored in
        // (essentially the database schema, this will need to get updated once we introduce categories)
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
                val gson = Gson()
                val jsonResponse = gson.fromJson(response, Map::class.java)

                // Extracting only the required fields
                val category = jsonResponse["category"] as? String ?: "Unknown"
                val currencyCode = jsonResponse["currency_code"] as? String ?: "Unknown"
                val date = jsonResponse["date"] as? String ?: "Unknown"

                val lineItems = jsonResponse["line_items"] as? List<*>
                val extractedItems = lineItems?.map { item ->
                    val itemMap = item as? Map<*, *>
                    val name = itemMap?.get("description") as? String ?: "No description"
                    val price = itemMap?.get("total") as? Double ?: 0.0
                    mapOf("name" to name, "price" to price)
                } ?: emptyList()

                val subtotal = extractedItems.sumOf { (it["price"] as Double?) ?: 0.0 }
                val tax = jsonResponse["tax"] as? Double ?: 0.0
                val total = jsonResponse["total"] as? Double ?: subtotal + tax

                // Formatting the extracted data
                val formattedData = mapOf(
                    "category" to category,
                    "currency_code" to currencyCode,
                    "date" to date,
                    "items" to extractedItems,
                    "subtotal" to subtotal,
                    "tax" to tax,
                    "total" to total
                )

                // Push extracted data to Firebase
                saveFormattedDataToFirebase(formattedData)

                // Provide feedback to the UI
                val formattedResponse = """
                    Category: $category
                    Currency Code: $currencyCode
                    Date: $date
                    Items:
                    ${extractedItems.joinToString("\n") { "- ${it["name"]}: $${it["price"]}" }}
                    Subtotal: $${"%.2f".format(subtotal)}
                    Tax: $${"%.2f".format(tax)}
                    Total: $${"%.2f".format(total)}
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
private fun saveFormattedDataToFirebase(data: Map<String, Any>) {
    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId != null) {
        firestore.collection("users").document(userId).collection("receipts")
            .add(data)
            .addOnSuccessListener { documentReference ->
                println("Document saved with ID: ${documentReference.id}")
            }
            .addOnFailureListener { error ->
                println("Failed to save document: $error")
            }
    } else {
        println("User is not authenticated. Cannot save data.")
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
    var isExpanded by remember { mutableStateOf(true) } // Track bottom bar state

    Scaffold(
        topBar = { FixedTopBar(userEmail) },
        bottomBar = {
            if (isExpanded) {
                ExpandableBottomNavigationBar(
                    navController = navController,
                    onMinimize = { isExpanded = false }
                )
            } else {
                ExpandButton(onExpand = { isExpanded = true })
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(
                bottom = if (isExpanded) innerPadding.calculateBottomPadding() else 0.dp
            )
        ) {
            NavigationGraph(navController, userEmail, onSignOut)
        }
    }
}

@Composable
fun ExpandableBottomNavigationBar(
    navController: NavHostController,
    onMinimize: () -> Unit
) {
    Surface {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Magenta)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Tabs
                BottomNavigationTab("Main", navController, "main")
                BottomNavigationTab("New", navController, "new")
                BottomNavigationTab("Settings", navController, "settings")

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = Color.Green,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onMinimize() }
                )
            }
        }
    }
}

@Composable
fun ExpandButton(onExpand: () -> Unit) {
    // Show a small button in the bottom-right corner to expand
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Expand",
            tint = Color.Green,
            modifier = Modifier
                .size(36.dp)
                .clickable { onExpand() }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedTopBar(userEmail: String) {
    TopAppBar(
        title = {
            Text(
                text = "Hello, $userEmail",
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Magenta
        ),
        modifier = Modifier.fillMaxWidth()
    )
}


@Composable
fun BottomNavigationTab(label: String, navController: NavHostController, route: String) {
    Text(
        text = label,
        modifier = Modifier
            .padding(20.dp)
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
    val categories = listOf("Housing", "Food", "Transportation", "Entertainment")
    var selectedCategory by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text("Hello, $userEmail", fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        Text("Select a Spending Category:")

        Spacer(modifier = Modifier.height(10.dp))

        CategorySelectionDropdown(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it } // Update selected category
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedCategory.isNotEmpty()) {
            Text("You selected: $selectedCategory", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            onSignOut()
        }) {
            Text("Sign Out")
        }
    }
}

@Composable
fun CategorySelectionDropdown(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        // Show selected category or default prompt
        Text(
            text = selectedCategory.ifEmpty { "Select a Category" },
            color = Color.Black
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(text = category) }, // Wrap `Text` in a lambda for `text` parameter
                    onClick = {
                        onCategorySelected(category) // Invoke callback with selected category
                        expanded = false // Close dropdown
                    }
                )
            }
        }
    }
}

@Composable
fun ExampleChart(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = 10.dp)
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
/*@Composable
fun NewScreen() {
    //Here we should add the logic to add a new receipt to the database (picture or manual)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("This is where we are gonna add receipts")
        ReceiptCaptureScreen()
    }
}

 */

@Composable
fun NewScreen() {
    var currentPage by remember { mutableStateOf(1) }
    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PageIndicator(currentPage)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (offsetX > 100) {
                                    currentPage = (currentPage - 1).coerceAtLeast(0)
                                } else if (offsetX < -100) {
                                    currentPage = (currentPage + 1).coerceAtMost(2)
                                }
                                offsetX = 0f
                            },
                            onDrag = { _, dragAmount ->
                                offsetX += dragAmount.x
                            }

                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (currentPage) {
                    0 -> PhotoGalleryScreen()
                    1 -> SwipeInstructionPage()
                    2 -> ManualDataInputScreen()
                }
            }
        }
    }
}

@Composable
fun PageIndicator(currentPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .background(Color.White)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(4.dp)
                    .background(
                        color = if (index == currentPage) Color.Magenta else Color.LightGray,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun SwipeInstructionPage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Swipe left for Camera/Gallery\nSwipe right for Manual Input",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}
@Composable
fun AnimatedCheckmark(modifier: Modifier = Modifier) {
    val checkmarkProgress = remember { Animatable(0f) }

    val customEasing: (Float) -> Float = { fraction ->
        fraction * fraction * (3 - 2 * fraction)
    }

    LaunchedEffect(Unit) {
        checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = customEasing)
        )
    }

    Canvas(
        modifier = modifier
            .size(150.dp)
            .background(Color.Green, shape = CircleShape)
            .padding(20.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        drawCircle(
            color = Color.Green,
            radius = canvasWidth / 2
        )

        val progress = checkmarkProgress.value
        if (progress > 0) {
            val checkmarkPath = Path().apply {
                moveTo(canvasWidth * 0.3f, canvasHeight * 0.55f)
                lineTo(canvasWidth * 0.45f, canvasHeight * 0.7f)
                lineTo(canvasWidth * 0.7f, canvasHeight * 0.4f)
            }

            val pathMeasure = PathMeasure()
            pathMeasure.setPath(checkmarkPath, false)

            val partialPath = Path()
            pathMeasure.getSegment(
                0f,
                pathMeasure.length * progress,
                partialPath,
                true
            )

            drawPath(
                path = partialPath,
                color = Color.White,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun PhotoGalleryScreen() {
    val context = LocalContext.current
    var isUploadComplete by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToCache(context, it)
            isLoading = true
            uploadToVeryfi(uri, context, onResponse = {
                isLoading = false
                isUploadComplete = true
            }, onError = {
                isLoading = false
            })
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            isLoading = true
            uploadToVeryfi(it, context, onResponse = {
                isLoading = false
                isUploadComplete = true
            }, onError = {
                isLoading = false
            })
        }
    }

    LaunchedEffect(isUploadComplete) {
        if (isUploadComplete) {
            kotlinx.coroutines.delay(3000)
            isUploadComplete = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (isUploadComplete) {
            AnimatedCheckmark()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = {
                    cameraLauncher.launch(null)
                }) {
                    Text("Take Photo")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Select from Gallery")
                }
            }
        }
    }
}
@Composable
fun SimpleDropdownMenu(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
            .clickable { expanded = !expanded } // Fixed clickable
            .padding(10.dp)
    ) {
        // Display the currently selected item or default placeholder
        Text(
            text = selectedItem.ifEmpty { "Select an Option" }, // Handle empty selected item
            color = Color.Black
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) }, // Wrap the `Text` in a lambda
                    onClick = {
                        onItemSelected(item) // Invoke callback with selected item
                        expanded = false // Close dropdown
                    }
                )
            }
        }
    }
}

@Composable
fun ManualDataInputScreen() {
    val categories = listOf(
        "Advertising & Marketing", "Automotive", "Bank Charges & Fees",
        "Legal & Professional Services", "Insurance", "Meals & Entertainment",
        "Office Supplies & Software", "Taxes & Licenses", "Travel",
        "Rent & Lease", "Repairs & Maintenance", "Payroll", "Utilities",
        "Job Supplies", "Grocery"
    )

    var selectedCategory by remember { mutableStateOf("Select a Category") }
    val items = remember { mutableStateListOf(Pair("", "")) }
    var tax by remember { mutableStateOf("") }
    var total by remember { mutableStateOf(0.0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Date Picker State
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf("Select a Date") }
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Context for DatePickerDialog
    val context = LocalContext.current

    // DatePickerDialog Trigger
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = dateFormatter.format(calendar.time)
                showDialog = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Update Total Calculation
    LaunchedEffect(items, tax) {
        val itemPrices = items.mapNotNull { it.second.toDoubleOrNull() }.sum()
        val taxAmount = tax.toDoubleOrNull() ?: 0.0
        total = itemPrices + taxAmount
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Category Dropdown
            Text("Select a Category:")
            SimpleDropdownMenu(
                items = categories,
                selectedItem = selectedCategory,
                onItemSelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            Text("Select Date:")
            OutlinedTextField(
                value = selectedDate,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDialog = true }, // Open DatePickerDialog
                label = { Text("Date (YYYY-MM-DD)") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Items Input
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
                        onValueChange = { updatedName ->
                            items[index] = updatedName to price
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Item Name") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { updatedPrice ->
                            items[index] = name to updatedPrice
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Price") }
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
                onValueChange = { tax = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tax") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total
            Text("Total: $${"%.2f".format(total)}")

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    val data = mapOf(
                        "category" to selectedCategory,
                        "date" to selectedDate,
                        "items" to items.map { mapOf("name" to it.first, "price" to it.second) },
                        "tax" to tax,
                        "total" to total.toString()
                    )
                    saveFormattedDataToFirebase(data)
                    isSubmitting = false
                    showSuccessAnimation = true
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
                    .width(150.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit")
                }
            }
        }
    }
}


//Settings Screen ---------------------------------------------------------------------------------
data class Setting(val title: String, val description: String)

@Composable
fun SettingsScreen() {
    // Generate 100 fake settings
    val settingsList = List(100) { index ->
        Setting(title = "Setting ${index + 1}", description = "Description for Setting ${index + 1}")
    }



    // LazyColumn to display settings
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(settingsList) { setting ->
            SettingItem(title = setting.title, description = setting.description)
        }
    }

}

@Composable
fun SettingItem(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall)
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
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign in or Sign up with Firebase:")
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(10.dp))

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
            Spacer(modifier = Modifier.height(10.dp))
            Text("Error: $it", color = MaterialTheme.colorScheme.error)
        }
    }
}



// Fairly useless, but keeping at the bottom as a point of reference for the other two screens.
// This will be deleted by the time everything is finished
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
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hello, $userEmail")
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter some data") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

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

        Spacer(modifier = Modifier.height(20.dp))

        Text("Data from Firestore:")
        Spacer(modifier = Modifier.height(10.dp))

        // Display the data in a list
        dataList.forEach { item ->
            Text("- $item")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            onSignOut() // Navigate back to the Sign-In screen
        }) {
            Text("Sign Out")
        }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}