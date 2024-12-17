# FinalProject

## **Overview**
The **FinalProject** is an Android budgeting application designed to help users manage their finances, capture receipts, and visualize expenses. It uses modern Android tools like Jetpack Compose, integrates Firebase services, and employs the Veryfi API for receipt OCR functionality.

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
- Manage app settings with a scrollable settings page. (Currently includes placeholders for future options.)

---

## **Usage Instructions**

### **1. Setup**
1. **Download the App**: Install the app on an Android device.
2. **Login/Register**: Use your email and password to create an account or log in.

### **2. Features**

#### **Receipt Capture**
1. Navigate to the **"New"** screen from the bottom navigation bar.
2. Choose:
   - **Take Photo**: Capture a receipt using the camera.
   - **Select from Gallery**: Upload a receipt image.
3. The app processes the image and extracts the receipt details.

#### **Manual Input**
1. Swipe right on the **"New"** screen to access manual input.
2. Select a category, date, and add expense details (items, price, and tax).
3. The app calculates totals automatically.

#### **Visualization**
1. Navigate to the **"Main"** screen.
2. View your budget breakdown through donut charts.

#### **Sign Out**
- Use the "Sign Out" button on the **"Main"** screen to securely log out.

---

## **Technical Implementation**

### **Architecture**
- **Jetpack Compose**: Used for modern, declarative UI design.
- **Navigation**: Compose Navigation handles screen transitions.
- **Firebase**:
  - **Firestore**: Stores user data, including expenses and receipts.
  - **Authentication**: Manages secure login and registration.

### **Third-Party Libraries**
- **Veryfi API**: Performs OCR on receipts to extract financial details.
- **Coil**: Efficient image loading and rendering.
- **YCharts**: Displays spending breakdowns with donut charts.

### **Custom Components**
- **Expandable Bottom Navigation Bar**: A dynamic bar that collapses into an expand button.
- **Manual Data Input**: Allows users to enter expense details manually.

---

## **Challenges**

### **1. Veryfi API Integration**
- **Issue**: Uploading and parsing image data efficiently.
- **Solution**: Created helper methods to extract essential information like tax, subtotal, and line items.

### **2. State Management**
- **Issue**: Managing states for navigation and dynamic UI updates.
- **Solution**: Used `remember` and `LaunchedEffect` to handle state across screens.

### **3. Firebase Integration**
- **Issue**: Storing hierarchical data securely in Firestore.
- **Solution**: Leveraged user authentication IDs for secure data storage paths.

### **4. Chart Visualization**
- **Issue**: Customizing charts to display financial data.
- **Solution**: Used the YCharts library for interactive charts.

---

## **Modifications and Omissions**
- **Features Omitted**:
  - Multi-language support.
  - Advanced filters for receipt data.
- **Enhancements**:
  - Added swiping gestures for better UX.
  - Included animations for smoother transitions.

---

## **Setup for Development**

### **1. Clone the Repository**
Run the following command in a terminal:

git clone <repository-url>
