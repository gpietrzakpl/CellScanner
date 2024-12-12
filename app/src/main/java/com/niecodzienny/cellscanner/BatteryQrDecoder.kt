package com.niecodzienny.cellscanner

class BatteryQrDecoder {
    companion object {
        private const val VALID_CODE_LENGTH_FULL = 24
        private const val VALID_CODE_LENGTH_RECYCLED = 19

        /**
         * Validates the QR code.
         * @param code Code to validate.
         * @return True if the code is valid, false otherwise.
         */
        fun validateCode(code: String): Boolean {
            // Check length
            if (code.length != VALID_CODE_LENGTH_FULL && code.length != VALID_CODE_LENGTH_RECYCLED) {
                return false
            }

            // Check 4th character for product type
            val productType = code.getOrNull(3)?.uppercaseChar()
            if (productType !in listOf('C', 'P', 'M')) {
                return false
            }

            return true
        }

        /**
         * Decodes information from the QR code.
         * @param code Validated full QR code.
         * @return Decoded information as a map.
         */
        fun decodeInformation(code: String): Map<String, String> {
            val result = mutableMapOf<String, String>()

            // Decode vendor code (1-3)
            result["Vendor Code"] = code.substring(0, 3)

            // Decode product type (4th character)
            result["Product Type"] = when (code[3].uppercaseChar()) {
                'C' -> "Battery Cell"
                'P' -> "Battery Pack"
                'M' -> "Battery Module"
                else -> "Unknown"
            }

            // Decode cell chemistry (5th character)
            result["Cell Chemistry"] = when (code[4].uppercaseChar()) {
                'B' -> "LiFePO4"
                else -> "Unknown"
            }

            // Decode cell specification code (6-7)
            result["Specification Code"] = code.substring(5, 7)

            // Decode traceability code (8-13)
            result["Traceability Code"] = code.substring(7, 13)

            // Decode factory address (14th character)
            result["Factory Address"] = when (code[13].uppercaseChar()) {
                'J' -> "Jingmen"
                'H' -> "Huizhou"
                else -> "Unknown"
            }

            // Decode production date (15-17)
            result["Production Date"] = decodeProductionDate(code.substring(14, 17))

            // Decode cell serial number (18+)
            result["Cell Serial Number"] = code.substring(17)

            return result
        }

        /**
         * Decodes production date from the date code.
         * @param dateCode The 3-character production date code.
         * @return Decoded production date as a string.
         */
        private fun decodeProductionDate(dateCode: String): String {
            val yearChar = dateCode[0].uppercaseChar()
            val monthChar = dateCode[1].uppercaseChar()
            val dayChar = dateCode[2].uppercaseChar()

            val year = when (yearChar) {
                in '1'..'9' -> 2010 + yearChar.digitToInt()
                '0' -> 2020
                in 'A'..'Z' -> 2020 + (yearChar - 'A' + 1)
                else -> -1
            }

            val month = when (monthChar) {
                in '1'..'9' -> monthChar.digitToInt()
                'A' -> 11
                'B' -> 12
                else -> -1
            }

            val day = when (dayChar) {
                in '1'..'9' -> dayChar.digitToInt()
                '0' -> 10
                in 'A'..'Z' -> 10 + (dayChar - 'A' + 1)
                else -> -1
            }

            return if (year != -1 && month != -1 && day != -1) {
                "$year-$month-$day"
            } else {
                "Invalid Date"
            }
        }
    }
}