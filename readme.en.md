---

## English Version

```md
# CellScanner

CellScanner is an open-source mobile application for Android designed to scan **QR** and **DataMatrix** codes placed on LFP cells (and more). 🔍  
These codes are either engraved or printed by the cell manufacturers and contain detailed information that enables authenticity verification, production tracking, and individual cell identification.

## What is this application?
The app was created to:
- Scan QR and DataMatrix codes found on cells, battery packs, or modules,
- Decode the information contained within them, such as:
  - **Vendor Code** – supplier code,
  - **Product Type** – product type (e.g., cell, pack, module),
  - **Cell Chemistry** – cell chemistry (e.g., LiFePO4),
  - **Specification Code** – specification code (e.g., model, capacity, voltage),
  - **Traceability Code** – code enabling full production traceability,
  - **Factory Location** – factory location,
  - **Production Date** – production date,
  - **Cell Serial Number** – cell serial number,
  - **Additional Info** – extra information (e.g., voltage, capacity, or cell model), if available.

The codes are printed on the cell casings, allowing manufacturers and users to verify quality and identify cells used in energy storage systems. ⚡

## How does the application work?
### Scanning:
- The app uses the **CameraX** library along with **ML Kit (Barcode Scanning)** to detect QR and DataMatrix codes.
- The scanning area (analyzed by ML Kit) is centered on the screen, and a displayed frame (220 dp × 220 dp, enlarged by 10%) indicates this area.  
  Once the first code is scanned, the scanning stops to prevent multiple reads.

### Decoding:
- After detecting a code, the app verifies its correctness and decodes the embedded information.
- The detailed production date decoding algorithm interprets a 3-character code:
  - **Year:**  
    If the character is a digit ('0'-'9'): year = 2010 + digit;  
    if it's a letter ('A'-'Z'): year = 2010 + (letter - 'A' + 10).
  - **Month:**  
    If the character is a digit ('1'-'9'): month = digit;  
    if it's a letter ('A'-'C'): month = letter - 'A' + 10 (A → 10, B → 11, C → 12).
  - **Day:**  
    If the character is a digit ('1'-'9'): day = digit;  
    if it's a letter ('A'-'V'): day = letter - 'A' + 10 (A → 10, …, V → 31).

- The decoding results are displayed on a dimmed background with a 50% larger font – with **Cell Chemistry** and **Production Date** highlighted in bold.

### Switching Lenses:
- The app allows switching between the rear camera, telephoto (this functionality requires further refinement), and front camera. 🔄

### Support and Additional Features:
- **Support Me:**  
  A button redirects to the support page: [Suppi](https://suppi.pl/gpietrzak). ❤️
- **Repository:**  
  The link in the top-right corner opens the GitHub repository: [CellScanner on GitHub](https://github.com/gpietrzak-pl/CellScanner).
- **Creator's Website:**  
  [gpietrzak.pl](https://gpietrzak.pl/).  
- The app is open-source – feel free to check out its code and learn how it works.

## Requirements
- **Operating System:** Android 5.0 (Lollipop) or newer.
- **Libraries:**
  - CameraX (with Camera2 interop)
  - ML Kit (Barcode Scanning)
- **Permissions:**  
  The app requires permission to use the camera.

## Permissions
- **Camera:**  
  The app requests permission to access the camera in order to scan QR and DataMatrix codes.

## Support
You can support the development of the app via:
- [Suppi (Support Me)](https://suppi.pl/gpietrzak)
- [PayPal](https://paypal.me/gpietrzak)
- [Revolut](https://revolut.me/niecodzienny)

Also, subscribe to my YouTube channel: [NieCodzienny Majsterkuje](https://www.youtube.com/@NieCodziennyMajsterkuje)

## App Version
Current version: **1.0.0**

## How to Run the App
1. Clone the repository:
   ```bash
   git clone https://github.com/gpietrzak-pl/CellScanner.git
2. Open the project in Android Studio.
3. Build and run the app on a device or emulator.
4. Ensure that the app has camera access permission.
Tasks for Further Improvement
Task 2: Improve the lens switching functionality.
Currently, switching to the front camera works, but the telephoto (selecting the rear camera with the highest focal length) requires further refinement.
Task 3: Extend support for DataMatrix codes.
If the structure of DataMatrix codes differs from the one described, extend the decoding logic in BatteryQrDecoder accordingly.
Task 4: Align the scanning area (analyzed by ML Kit) with the displayed frame in the interface.
© 2025 gpietrzak
