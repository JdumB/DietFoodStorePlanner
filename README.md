======================================================
Diet Food Store Planner
======================================================

1. SOURCE CODE ACCESS
The latest source code for this project is hosted on GitHub. 
Link: https://github.com/JdumB/DietFoodStorePlanner

2. TOOLS & PREREQUISITES REQUIRED
To execute and build the source code, 
- Android Studio
- Java Development Kit (JDK) 11 or higher

3. LIBRARIES & DEPENDENCIES
The project utilizes the following core libraries (automatically downloaded via Gradle upon syncing):
- AndroidX Biometric for fingerprint authentication.
- Firebase for Realtime Database integration.
- Java Cryptography Extension for AES-256 Client-Side Encryption (Native to Java).

4. EXECUTION INSTRUCTIONS
Step 1: Download or clone the repository from the GitHub link provided above.
Step 2: Open Android Studio, select "Open", and navigate to the extracted "DietFoodStorePlanner" folder.
Step 3: Allow Gradle to sync and download the required dependencies.
Step 4: Click the green "Run 'app'" button (Shift + F10) to deploy the application to your emulator or connected device.

5. DATABASE & DATASET REPRODUCTION
This project relies on a Firebase Realtime Database. For the examiner to fully replicate the environment or inspect the encrypted data structure:
- A complete export of the database is included in the GitHub repository as "dietplannertest-default-rtdb-export.json".
- To replicate the backend, create a new project at https://console.firebase.google.com/.
- Enable the "Realtime Database" service.
- Click the three dots (⋮) in the database data viewer and select "Import JSON". Upload the provided .json file.
- Download your new "google-services.json" file from Firebase settings and place it inside the "app/" folder of the Android Studio project before compiling.
