# FinalProject

## **Overview**
The **FinalProject** is an Android budgeting application that helps users manage their expenses, capture receipts, and track spending visually. Built using **Jetpack Compose**, it integrates **Firebase** for authentication and storage, and leverages the **Veryfi API** for receipt OCR processing.

---

## **Features**

### **1. Authentication**
- Secure login and registration using Firebase Authentication.
- Personalized access to budgeting features.

### **2. Receipt Capture**
- **Camera Integration**: Capture receipts directly from the app.
- **Gallery Import**: Upload receipts from the device's gallery.
- **OCR Functionality**: Extracts financial details using the Veryfi API:
  - Categories
  - Date
  - Total, Tax, Subtotal
  - Line items (e.g., item name and price)

### **3. Budgeting Tools**
- **Category-Based Spending**: Group expenses into categories like Housing, Food, Transportation, and Entertainment.
- **Interactive Charts**: Visualize your spending using donut charts.
- **Manual Data Input**: Add custom expense data, including categories, amounts, and taxes.

### **4. User Navigation**
- Intuitive navigation with an expandable/collapsible bottom navigation bar.
- Fixed top bar displaying personalized greetings.

### **5. Settings**
- Manage app settings with a scrollable settings page.

---

## **Usage Instructions**

### **1. Setup**
1. **Download the App**: Install the app on an Android device from this repo.
2. **Login/Register**: Use your email and password to create an account or log in.

### **2. Walkthrough**

#### **Login/Register**
- Use your email and password to sign up or log in securely.

---

#### **Capture Receipts**
1. Go to the **"New"** screen using the bottom navigation bar.
2. Choose one of the following options:
   - **Take Photo**: Capture a new receipt using the device's camera.
   - **Select from Gallery**: Upload an existing image of a receipt from your device.
3. The app processes the image using OCR and extracts details such as:
   - **Categories**
   - **Date**
   - **Total, Tax, and Subtotal**
   - **Line Items**: Individual item names and prices.

---

#### **Manual Expense Input**
1. Swipe right on the **"New"** screen to access the manual input form.
2. Enter the following details:
   - **Category**: Select the category of the expense.
   - **Date**: Pick the date of the transaction.
   - **Item Details**: Input item names, prices, and tax amounts.
3. The app calculates totals automatically and saves the data.

---

#### **View Spending**
1. Navigate to the **"Main"** screen using the bottom navigation bar.
2. The following information is displayed:
   - **Donut Charts**: Visual breakdown of spending by category.
   - **Budget Overview**: Visualize the total budget used and remaining balance.

---

#### **Settings**
1. Navigate to the **"Settings"** screen.
2. Perform the following actions:
   - **Set Monthly Budget**: Input a desired monthly budget.
   - **Toggle Theme**: Switch between light and dark mode.
   - **Sign Out**: Log out of your account securely.

---

## **Technical Implementation**

### **Architecture**
- **Jetpack Compose**: Modern declarative UI.
- **Navigation Component**: Handles seamless navigation between screens.
- **Firebase**:
   - **Authentication**: Manages user login.
   - **Firestore**: Stores user data, receipts, and preferences.

### **Third-Party Libraries**
| **Library**       | **Purpose**                     |
|--------------------|---------------------------------|
| **Veryfi API**     | Receipt OCR and data extraction |
| **YCharts**        | Interactive chart visualization |
| **Firebase**       | Authentication & Firestore      |

---

## **Functions in MainActivity.kt**

### **Main Entry Point**
1. **`MainActivity.onCreate`**  
   Initializes the app, sets the theme, and launches the login screen.

---

### **Screens**
2. **`ReceiptCaptureScreen`**  
   Allows users to capture or upload a receipt image and process it using the Veryfi API.

3. **`MainAppNav`**  
   Sets up the main navigation structure for the app, including the top and bottom navigation bars.

4. **`MainScreen`**  
   Displays spending charts, filters, and receipt data.

5. **`SettingsScreen`**  
   Provides options for budget input, theme selection, and logout.

6. **`NewScreen`**  
   Offers swipeable navigation between receipt capture, manual data input, and instructions.

---

### **UI Components**
7. **`FixedTopBar`**  
   A top bar displaying greetings and app-level actions.

8. **`ExpandableBottomNavigationBar`**  
   Bottom navigation bar with expandable/collapsible behavior.

9. **`ExpandButton`**  
   Displays a button to expand or minimize the navigation bar.

10. **`PageIndicator`**  
    Visual indicator for pages in swipeable navigation.

11. **`CardSection`**  
    A reusable card component that displays a title and content.

12. **`BudgetPieChart`**  
    Visualizes spending with a color-coded pie chart.

13. **`BudgetBreakdownDonutChart`**  
    Displays a donut chart for detailed spending breakdown.

14. **`DropdownMenuBox`**  
    Provides a dropdown menu with selectable items.

15. **`SimpleDropdownMenu`**  
    Dropdown menu for simple selection with placeholders.

16. **`DropdownMenuField`**  
    A field with dropdown options for dates or categories.

17. **`PhotoGalleryScreen`**  
    Upload receipts via the camera or gallery.

18. **`ManualDataInputScreen`**  
    Enables manual transaction entry with item, tax, and total details.

19. **`SwipeInstructionPage`**  
    Displays swipe instructions for navigation between pages.

20. **`AnimatedCheckmark`**  
    Renders a success animation with a checkmark.

---

### **Utilities**
21. **`randomColor`**  
    Generates random colors for chart slices.

22. **`saveBitmapToCache`**  
    Saves a bitmap image to the app's cache directory.

23. **`createTempFileFromUri`**  
    Converts a URI to a temporary file.

24. **`formatDate`**  
    Formats a date string into `YYYY-MM-DD` format.

---

### **Data Processing**
25. **`uploadToVeryfi`**  
    Uploads a receipt image to the Veryfi API for OCR processing.

26. **`saveFormattedDataToFirebase`**  
    Saves extracted receipt data to Firestore.

27. **`fetchData`** (Inside `MainScreen`)  
    Fetches receipts data from Firebase to populate charts.

28. **`fetchBudget`** (Inside `MainScreen`)  
    Fetches and calculates the user’s budget usage.

29. **`deleteReceipt`** (Inside `MainScreen`)  
    Deletes a receipt document from Firestore.

---

## **Challenges Faced**
1. **Veryfi Integration**  
   - **Issue**: Efficiently processing image data and extracting receipt details.  
   - **Solution**: Implemented helper methods to handle receipt parsing and ensure accurate extraction of financial data.

2. **State Management**  
   - **Issue**: Maintaining UI responsiveness when navigating between screens and processing data dynamically.  
   - **Solution**: Used `remember` and `LaunchedEffect` for state persistence and real-time UI updates.

3. **Firebase Security**  
   - **Issue**: Ensuring secure storage and access of hierarchical user data.  
   - **Solution**: Leveraged user authentication IDs to create isolated and secure paths in Firestore.

---

## **Future Enhancements**
- **Multi-Language Support**: Allow users to switch between languages for better accessibility.
- **Advanced Filters**: Add advanced filtering options for receipts based on category, date range, and keywords.
- **Recurring Expense Tracking**: Introduce support for automatically tracking recurring expenses.

---

## **Contributors**
- **Brandon Dunegan** - Developer and Maintainer.
- **Jewoo Lee** - Developer and Maintainer.
- **Ethan Liu** - Developer and Maintainer.

---

