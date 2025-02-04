package com.niecodzienny.cellscanner

class BatteryQrDecoder {
    companion object {
        // Dozwolone długości kodów – dodano 25, aby obsłużyć niektóre kody DataMatrix
        private val VALID_LENGTHS = setOf(19, 24, 25)

        /**
         * Waliduje kod QR lub DataMatrix.
         * Kod powinien mieć długość 24 (pełny), 19 (recyklingowy) lub 25 (alternatywny DataMatrix).
         * 4-ty znak określa typ produktu.
         */
        fun validateCode(code: String): Boolean {
            if (code.length !in VALID_LENGTHS) {
                return false
            }
            val productType = code.getOrNull(3)?.uppercaseChar()
            if (productType !in listOf('C', 'P', 'M')) {
                return false
            }
            return true
        }

        /**
         * Dekoduje informacje z kodu.
         *
         * Struktura kodu:
         * - Znaki 1-3: Vendor Code
         * - 4-ty znak: Product Type (C: ogniwo, P: pakiet, M: moduł)
         * - 5-ty znak: Cell Chemistry (np. B: LiFePO4)
         * - Znaki 6-7: Specification Code (np. model, pojemność, napięcie)
         * - Znaki 8-13: Traceability Code (unikalny identyfikator produkcji)
         * - 14-ty znak: Factory Location (np. J: Jingmen, H: Huizhou)
         * - Znaki 15-17: Production Date (3-znakowy kod)
         * - Znaki 18-24: Cell Serial Number
         * - Jeśli kod zawiera więcej niż 24 znaki, dodatkowe dane są traktowane jako "Additional Info"
         */
        fun decodeInformation(code: String): Map<String, String> {
            val result = mutableMapOf<String, String>()
            result["Vendor Code"] = code.substring(0, 3)
            result["Product Type"] = when (code[3].uppercaseChar()) {
                'C' -> "Battery Cell"
                'P' -> "Battery Pack"
                'M' -> "Battery Module"
                else -> "Unknown"
            }
            result["Cell Chemistry"] = when (code[4].uppercaseChar()) {
                'B' -> "LiFePO4"
                else -> "Unknown"
            }
            result["Specification Code"] = code.substring(5, 7)
            result["Traceability Code"] = code.substring(7, 13)
            result["Factory Location"] = when (code[13].uppercaseChar()) {
                'J' -> "Jingmen"
                'H' -> "Huizhou"
                else -> "Unknown"
            }
            result["Production Date"] = decodeProductionDate(code.substring(14, 17))
            result["Cell Serial Number"] = code.substring(17, minOf(code.length, 24))
            if (code.length > 24) {
                result["Additional Info"] = code.substring(24)
            }
            return result
        }

        /**
         * Dekoduje datę produkcji z 3-znakowego kodu.
         *
         * Rok:
         * - Jeśli znak to cyfra ('0'-'9'): rok = 2010 + (cyfra)
         * - Jeśli litera ('A'-'Z'): rok = 2010 + (litera - 'A' + 10)
         *
         * Miesiąc:
         * - Jeśli znak to cyfra ('1'-'9'): miesiąc = cyfra
         * - Jeśli litera ('A'-'C'): miesiąc = litera - 'A' + 10 (A → 10, B → 11, C → 12)
         *
         * Dzień:
         * - Jeśli znak to cyfra ('1'-'9'): dzień = cyfra
         * - Jeśli litera ('A'-'V'): dzień = litera - 'A' + 10 (A → 10, …, V → 31)
         */
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
}
