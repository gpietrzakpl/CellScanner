package com.niecodzienny.cellscanner

object DataMatrixDecoder {

    // Minimalna długość kodu (może wymagać dostosowania)
    private const val MIN_CODE_LENGTH = 10

    // Zakładane minimalne długości początkowych pól (do weryfikacji!)
    private const val VENDOR_CODE_LENGTH = 3
    private const val PRODUCT_TYPE_LENGTH = 1
    private const val PRODUCTION_DATE_LENGTH = 3

    fun decodeInformation(code: String): Map<String, String>? {
        if (code.length < MIN_CODE_LENGTH) {
            return null // Kod za krótki
        }

        var currentIndex = 0
        // Próba wyodrębnienia podstawowych informacji
        val vendorCode = try {
            code.substring(currentIndex, currentIndex + VENDOR_CODE_LENGTH)
        } catch (e: StringIndexOutOfBoundsException) {
            return null // Nie udało się wyodrębnić
        }
        currentIndex += VENDOR_CODE_LENGTH


        val productType = try {
            code.substring(currentIndex, currentIndex + PRODUCT_TYPE_LENGTH)
        }
        catch (e: StringIndexOutOfBoundsException){
            return null
        }
        currentIndex += PRODUCT_TYPE_LENGTH


        val productionDateCode = try {
            code.substring(currentIndex, currentIndex + PRODUCTION_DATE_LENGTH)
        }  catch (e: StringIndexOutOfBoundsException) {
            return null
        }
        currentIndex += PRODUCTION_DATE_LENGTH

        // Reszta kodu jako "Additional Info"
        val additionalInfo = if (code.length > currentIndex) {
            code.substring(currentIndex)
        } else {
            null
        }

        val result = mutableMapOf<String, String>()
        result["Vendor Code"] = vendorCode
        result["Product Type"] = when (productType) {
            "C" -> "Battery Cell"
            "P" -> "Battery Pack"
            "M" -> "Battery Module"
            else -> "Unknown"
        }
        result["Production Date"] = decodeProductionDate(productionDateCode)
        additionalInfo?.let { result["Additional Info"] = it } // Dodaj, jeśli istnieje

        // Te pola ustawiamy na "Unknown", bo nie wiemy, jak je wyodrębnić
        result["Cell Chemistry"] = "Unknown"
        result["Specification Code"] = "Unknown"
        result["Traceability Code"] = "Unknown"
        result["Factory Location"] = "Unknown"
        result["Cell Serial Number"] = "Unknown"

        return result
    }
    private fun decodeProductionDate(dateCode: String): String {
        if (dateCode.length != 3) return "Invalid Date"
        val yearChar = dateCode[0]
        val monthChar = dateCode[1]
        val dayChar = dateCode[2]

        val year = when (yearChar) {
            in '0'..'9' -> 2010 + (yearChar - '0')
            in 'A'..'Z' -> 2010 + (yearChar - 'A' + 10)
            else -> -1
        }
        val month = when (monthChar) {
            in '1'..'9' -> monthChar.digitToInt()
            in 'A'..'C' -> monthChar - 'A' + 10
            else -> -1
        }
        val day = when (dayChar) {
            in '1'..'9' -> dayChar.digitToInt()
            in 'A'..'V' -> dayChar - 'A' + 10
            else -> -1
        }
        return if (year != -1 && month != -1 && day != -1) "$year-$month-$day" else "Invalid Date"
    }
}