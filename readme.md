The English version of the README file can be found here:  
[https://github.com/gpietrzak-pl/CellScanner/edit/master/readme.en.md](https://github.com/gpietrzak-pl/CellScanner/edit/master/readme.en.md)


# CellScanner

CellScanner to otwartoźródłowa aplikacja mobilna na system Android służąca do skanowania kodów **QR** i **DataMatrix** umieszczonych na ogniwach LFP (i nie tylko). 🔍  
Kody te są grawerowane lub nadrukowywane przez producentów ogniw i zawierają szczegółowe informacje, które pozwalają na weryfikację autentyczności, śledzenie produkcji oraz identyfikację poszczególnych ogniw.

## Co to za aplikacja?
Aplikacja została stworzona, aby:
- Skanować kody QR i DataMatrix znajdujące się na ogniwach, pakietach lub modułach baterii,
- Dekodować zawarte w nich informacje, takie jak:
  - **Vendor Code** – kod dostawcy,
  - **Product Type** – typ produktu (np. ogniwo, pakiet, moduł),
  - **Cell Chemistry** – chemia ogniwa (np. LiFePO4),
  - **Specification Code** – kod specyfikacji (np. model, pojemność, napięcie),
  - **Traceability Code** – kod umożliwiający pełną identyfikację produkcji,
  - **Factory Location** – lokalizacja fabryki,
  - **Production Date** – data produkcji,
  - **Cell Serial Number** – numer seryjny ogniwa,
  - **Additional Info** – dodatkowe dane (np. informacje o napięciu, pojemności, modelu ogniwa), jeśli są zawarte.

Kody są nadrukowywane na obudowach ogniw – co umożliwia producentom oraz użytkownikom weryfikację jakości i identyfikację ogniw stosowanych m.in. w systemach magazynowania energii. ⚡

## Jak działa aplikacja?
### Skanowanie:
- Aplikacja wykorzystuje bibliotekę **CameraX** oraz **ML Kit (Barcode Scanning)** do wykrywania kodów QR i DataMatrix.
- Obszar skanowania (analizowany przez ML Kit) jest centralnie wyśrodkowany, a wyświetlana ramka (220 dp × 220 dp) wskazuje ten obszar (powiększona o 10dp ramka służy lepszemu podglądowi obszaru skanowania).  Zadanie wyrównania obszaru skanowania z ramką jest nadal w toku.
  Po zeskanowaniu pierwszego kodu, skanowanie zostaje zatrzymane, co zapobiega wielokrotnemu odczytowi.

### Dekodowanie:
- Po wykryciu kodu, aplikacja weryfikuje jego poprawność i dekoduje zawarte informacje.
- Szczegółowy algorytm dekodowania daty produkcji interpretuje 3-znakowy kod:
  - **Rok:**  
    Jeśli znak to cyfra ('0'-'9'): rok = 2010 + cyfra;  
    jeśli litera ('A'-'Z'): rok = 2010 + (litera - 'A' + 10).
  - **Miesiąc:**  
    Jeśli znak to cyfra ('1'-'9'): miesiąc = cyfra;  
    jeśli litera ('A'-'C'): miesiąc = litera - 'A' + 10 (A → 10, B → 11, C → 12).
  - **Dzień:**  
    Jeśli znak to cyfra ('1'-'9'): dzień = cyfra;  
    jeśli litera ('A'-'V'): dzień = litera - 'A' + 10 (A → 10, …, V → 31).

- Wyniki dekodowania wyświetlane są na przyciemnionym tle z czcionką zwiększoną o 50% – przy czym **Cell Chemistry** oraz **Production Date** są pogrubione.

### Przełączanie obiektywów:
- Aplikacja umożliwia przełączanie między kamerą tylną, teleobiektywem (funkcjonalność wymaga dalszych poprawek) oraz kamerą przednią. 🔄

### Eksport danych i analityka:
- **Eksport danych:**  Aplikacja umożliwia eksport zeskanowanych danych do pliku CSV.  Dane są zapisywane lokalnie, a następnie można je wyeksportować do katalogu Pobrane (Downloads).
- **Archiwizacja danych:**  Możesz zarchiwizować lokalnie zapisane dane.  Spowoduje to zmianę nazwy pliku z logami i utworzenie nowego, pustego pliku na kolejne skany.
- **Firebase Analytics:**  Aplikacja używa Firebase Analytics do zbierania *anonimowych* danych statystycznych, które pomagają w ulepszaniu aplikacji.

### Zbierane dane (Firebase Analytics):
Aplikacja zbiera następujące *anonimowe* informacje:
- **Zdarzenia:**
  - `app_start`:  Pierwsze uruchomienie aplikacji.
  - `app_open`:  Każde otwarcie aplikacji.
  - `unique_code_scanned`:  Zeskanowanie unikalnego kodu (zliczanie unikalnych kodów).
  - `time_between_scans`:  Czas między kolejnymi skanowaniami (w sekundach).
  - `invalid_code_scanned`:  Zeskanowanie nieprawidłowego kodu.
  - `cell_code_scanned`:  Zeskanowanie prawidłowego kodu (zawiera dodatkowe parametry).
  - `cell_code_invalid`: Zeskanowanie kodu, który nie został poprawnie zdekodowany (zawiera dodatkowe parametry i przyczynę).
  - `camera_lens_switched`:  Przełączenie obiektywu kamery.
- **Parametry zdarzeń (dla `cell_code_scanned` i `cell_code_invalid`):**
  - `vendor_code`:  Kod dostawcy (jeśli został zdekodowany).
  - `product_type`: Typ produktu (jeśli został zdekodowany).
  - `cell_chemistry`: Chemia ogniwa (jeśli została zdekodowana).
  - `production_date`:  Data produkcji (jeśli została zdekodowana).
  - `code_type`: Typ kodu (QR lub DataMatrix)
  - `reason`:  (tylko `cell_code_invalid`) Przyczyna niepowodzenia dekodowania (np. "validation_failed").
- **Inne:**
  - Liczba unikalnych zeskanowanych kodów

**Dane te są wykorzystywane wyłącznie do celów analitycznych i nie są powiązane z żadnymi danymi osobowymi.**

### Wsparcie i dodatkowe funkcje:
- **Wesprzyj mnie:** Przycisk przekierowuje do strony wsparcia: [Suppi](https://suppi.pl/gpietrzak). ❤️
- **Repozytorium:** Link w prawym górnym rogu otwiera stronę repozytorium na GitHub: [CellScanner on GitHub](https://github.com/gpietrzak-pl/CellScanner).
- **Strona twórcy:** [gpietrzak.pl](https://gpietrzak.pl/).  
- Aplikacja jest otwartoźródłowa – możesz sprawdzić jej kod i dowiedzieć się, jak działa.

## Wymagania
- **System operacyjny:** Android 5.0 (Lollipop) lub nowszy.
- **Biblioteki:**
  - CameraX (z interop Camera2)
  - ML Kit (Barcode Scanning)
  - Firebase Analytics
- **Uprawnienia:**  
  Aplikacja wymaga uprawnienia do korzystania z kamery.

## Uprawnienia
- **Kamera:**  
  Aplikacja prosi o uprawnienie do korzystania z kamery, aby móc skanować kody QR i DataMatrix.

## Wsparcie
Możesz wesprzeć rozwój aplikacji poprzez:
- [Suppi (Wesprzyj mnie)](https://suppi.pl/gpietrzak)
- [PayPal](https://paypal.me/gpietrzak)
- [Revolut](https://revolut.me/niecodzienny)

Subskrybuj również mój kanał na YouTube: [NieCodzienny Majsterkuje](https://www.youtube.com/@NieCodziennyMajsterkuje)

## Wersja Aplikacji
Obecna wersja: **1.0.1**

## Jak uruchomić aplikację

Aby zainstalować aplikację, przejdź do [zakładki Releases](https://github.com/gpietrzak-pl/CellScanner/releases) w repozytorium i pobierz plik APK dla wersji **1.0.0**, np. [CellScanner.apk](https://github.com/gpietrzak-pl/CellScanner/releases/download/v1.0.0/CellScanner.apk).

**Kroki instalacji:**
1. Upewnij się, że Twoje urządzenie pozwala na instalację aplikacji z nieznanych źródeł.
2. Prześlij plik APK na urządzenie (np. poprzez kabel USB lub pobierz go bezpośrednio).
3. Otwórz plik APK, aby rozpocząć instalację.

## Zadania do dalszego dopracowania

* Zadanie 2: Udoskonalić funkcjonalność przełączania obiektywu.
Obecnie przełączanie na kamerę frontową działa, ale teleobiektyw (wybór tylnej kamery o najwyższej ogniskowej) wymaga dopracowania.

* Zadanie 3: Rozszerzyć obsługę kodów DataMatrix.
Jeśli struktura kodów DataMatrix różni się od tej opisanej, dekodowanie w BatteryQrDecoder należy rozszerzyć o dedykowaną logikę.

* Zadanie 4: Wyrównać obszar skanowania (analizowany przez ML Kit) z ramką wyświetlaną w interfejsie.

© 2025 gpietrzak

Skopiuj powyższe pliki do swojego projektu. Jeśli będziesz miał dodatkowe pytania lub potrzebował kolejnych zmian, daj znać!
