# CellScanner

**CellScanner** to aplikacja mobilna oparta na technologii Android oraz CameraX, która umożliwia skanowanie kodów QR i DataMatrix przy użyciu kamery urządzenia. Aplikacja wykorzystuje ML Kit firmy Google do detekcji i dekodowania kodów, a następnie prezentuje odczytane informacje oraz umożliwia ich dalsze przetwarzanie (np. otwieranie dedykowanej strony internetowej).

## Funkcje

- **Skanowanie kodów QR i DataMatrix:**  
  Wykrywanie kodów przy użyciu kamery oraz automatyczne filtrowanie kodów znajdujących się w określonym obszarze (ramce).

- **Dekodowanie danych:**  
  Po zeskanowaniu kodu, aplikacja weryfikuje jego poprawność i dekoduje informacje (np. dane producenta, datę produkcji, numer seryjny).  
  (W przyszłości planowane jest rozszerzenie dekodera o dodatkowe typy kodów.)

- **Przełączanie obiektywów:**  
  Umożliwia zmianę kamery (tylna, teleobiektyw, przednia) – choć funkcjonalność teleobiektywu wymaga jeszcze dopracowania.

- **Łatwy dostęp do informacji:**  
  Wynik dekodowania wyświetlany jest na ekranie z ciemnym tłem, aby zapewnić czytelność.

- **Wsparcie:**  
  Aplikacja zawiera przycisk "Wesprzyj mnie", który przekierowuje do strony wsparcia, umożliwiając pomoc twórcy.

## Wymagania

- **System operacyjny:** Android 5.0 (Lollipop) lub nowszy.
- **Biblioteki:**
    - CameraX (wraz z interop Camera2)
    - ML Kit (Barcode Scanning)
- **Uprawnienia:**  
  Aplikacja wymaga uprawnienia do korzystania z kamery.

## Uprawnienia

- **Kamera:** Aplikacja prosi o uprawnienie do korzystania z kamery, aby móc skanować kody QR i DataMatrix.

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
   git clone https://github.com/twoje_konto/CellScanner.git
