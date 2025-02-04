# CellScanner

**CellScanner** to otwartoźródłowa aplikacja mobilna na system Android służąca do skanowania kodów QR i DataMatrix umieszczonych na ogniwach LFP (i nie tylko). Kody te są grawerowane lub nadrukowywane przez producentów ogniw i zawierają szczegółowe informacje, które pozwalają na weryfikację autentyczności, śledzenie produkcji oraz identyfikację poszczególnych ogniw.

## Co to za aplikacja?

Aplikacja została stworzona, aby:
- Skanować kody QR i DataMatrix znajdujące się na ogniwach, pakietach lub modułach baterii,
- Dekodować zawarte w nich informacje, takie jak:
  - **Vendor Code** – kod dostawcy,
  - **Product Type** – typ produktu (np. ogniwo, pakiet, moduł),
  - **Cell Chemistry** – chemia ogniwa (np. LiFePO4),
  - **Specification Code** – kod specyfikacji (np. model lub pojemność),
  - **Traceability Code** – kod umożliwiający pełną identyfikację produkcji,
  - **Factory Location** – lokalizacja fabryki,
  - **Production Date** – data produkcji,
  - **Cell Serial Number** – numer seryjny ogniwa.
- Umożliwić szybką weryfikację i dokumentację ogniw wykorzystywanych m.in. w systemach magazynowania energii.

## Jak działa aplikacja?

1. **Skanowanie:**  
   Aplikacja wykorzystuje bibliotekę CameraX oraz ML Kit (Barcode Scanning) do wykrywania kodów QR i DataMatrix.  
   Obszar skanowania (analizowany przez ML Kit) jest wyrównany z ramką wyświetlaną w interfejsie użytkownika.

2. **Dekodowanie:**  
   Po wykryciu kodu aplikacja weryfikuje jego poprawność i dekoduje zawarte informacje.  
   Dekodowane dane wyświetlane są w czytelnym formacie na ciemnym tle, co umożliwia łatwą analizę.

3. **Przełączanie obiektywów:**  
   Aplikacja umożliwia przełączanie między kamerą tylną, teleobiektywem (funkcjonalność wymaga dalszych poprawek) oraz kamerą przednią.

4. **Wsparcie i dodatkowe funkcje:**  
   W interfejsie znajduje się przycisk „Wesprzyj mnie”, który przekierowuje do strony wsparcia, a także link do repozytorium aplikacji.

## Wymagania

- **System operacyjny:** Android 5.0 (Lollipop) lub nowszy.
- **Biblioteki:**
  - CameraX (z interop Camera2)
  - ML Kit (Barcode Scanning)
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

Subskrybuj również mój kanał na YouTube:  
[NieCodzienny Majsterkuje](https://www.youtube.com/@NieCodziennyMajsterkuje)

## Jak uruchomić aplikację

1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/gpietrzak-pl/CellScanner.git
2. Otwórz projekt w Android Studio.
3. Zbuduj i uruchom aplikację na urządzeniu lub emulatorze.
4. Upewnij się, że aplikacja ma przyznane uprawnienie do korzystania z kamery.

## Zadania do dalszego dopracowania

* Zadanie 2: Udoskonalić funkcjonalność przełączania obiektywu.
Obecnie przełączanie na kamerę frontową działa, ale teleobiektyw (wybór tylnej kamery o najwyższej ogniskowej) wymaga dopracowania.

* Zadanie 3: Rozszerzyć obsługę kodów DataMatrix.
Jeśli struktura kodów DataMatrix różni się od tej opisanej, dekodowanie w BatteryQrDecoder należy rozszerzyć o dedykowaną logikę.

* Zadanie 4: Wyrównać obszar skanowania (analizowany przez ML Kit) z ramką wyświetlaną w interfejsie.

© 2025 gpietrzak

Skopiuj powyższe pliki do swojego projektu. Jeśli będziesz miał dodatkowe pytania lub potrzebował kolejnych zmian, daj znać!