# FortCode KITS

[![build](https://github.com/tymateuszwierzba-ux/Kity/actions/workflows/build.yml/badge.svg)](https://github.com/tymateuszwierzba-ux/Kity/actions/workflows/build.yml)

Rozbudowany plugin systemu kitów dla serwerów Minecraft (Paper) z kompletnym interfejsem graficznym, statystykami, ekonomią i obsługą rang.

## Funkcje

- **Przeprojektowane GUI** — menu główne kitów, edytor admina, panel ustawień, statystyki, historia, ulubione, ranking TOP i ekrany potwierdzeń
- **Wyszukiwarka, filtry i sortowanie** — szukanie po nazwie, filtrowanie po kategorii i rzadkości, kilka trybów sortowania
- **Rzadkości kitów** — common, rare, epic, legendary z podświetleniem w GUI
- **Ulubione** — gracze przypinają ulubione kity (osobny plik danych)
- **Statystyki i historia** — liczba claimów, ostatnie pobrania, ranking najczęściej pobieranych kitów
- **Ustawienia gracza** — dźwięki, cząsteczki, animacje claima oraz feedback title / bossbar / actionbar
- **Ekonomia (Vault)** — ceny kitów, zakupy, informacja o saldzie w GUI
- **Cooldowny i limity** — czasowy cooldown, kity jednorazowe, limit claimów na gracza
- **Kity powitalne** — automatyczne przyznawanie przy wejściu na serwer
- **Wymagania rang (LuckPerms)** — kit dostępny tylko dla wybranej grupy
- **Typy feedbacku** — chat, title, actionbar, bossbar, wszystkie jednocześnie lub brak
- **Animacje wiadomości** — tagi `<anim:...>` w wiadomościach (config.yml, messages.yml)
- **PlaceholderAPI** — placeholdery do zmiennych serwera i paneli
- **Baza danych** — SQLite (domyślnie) lub MySQL, asynchroniczne zapytania

## Wymagania

- Serwer **Paper 1.18+** (przetestowano na Paper 1.21.11)
- Java **21+** (budowanie i uruchomienie)
- Opcjonalnie: PlaceholderAPI, Vault, LuckPerms

## Budowanie

```bash
mvn clean package
```

Gotowy plik: `target/fortcode-kits-2.0.0.jar`

Każdy push jest też budowany automatycznie w GitHub Actions — gotowy jar znajdziesz w zakładce **Actions → build → Artifacts**.

## Instalacja

1. Skopiuj `fortcode-kits-2.0.0.jar` do folderu `plugins/`
2. Uruchom serwer — plugin wygeneruje pliki konfiguracyjne
3. Skonfiguruj kity w `plugins/fortcode-kits/kits.yml`

## Komendy

| Komenda | Opis |
| --- | --- |
| `/kits` (`/kit`, `/fortkits`) | Otwiera GUI kitów |
| `/adminkit <open\|create\|edit\|delete\|...>` | Panel administracyjny (tworzenie, edycja, statystyki, reload) |

## Uprawnienia

| Uprawnienie | Domyślnie | Opis |
| --- | --- | --- |
| `fortcode.kits.use` | gracz | Otwieranie GUI kitów |
| `fortcode.kits.favorites` | gracz | Ulubione |
| `fortcode.kits.stats` | gracz | Statystyki i historia |
| `fortcode.kits.settings` | gracz | Ustawienia |
| `fortcode.kits.admin` | op | Panel administracyjny |
| `fortcode.kits.give` | op | Ręczne przyznawanie kitów |
| `fortcode.kits.kit.<id>` | op | Dostęp do konkretnego kitu |
| `fortcode.kits.history.all` | op | Historia wszystkich graczy |
| `fortcode.kits.reload` | op | Przeładowanie konfiguracji |

## Pliki konfiguracyjne

| Plik | Zawartość |
| --- | --- |
| `config.yml` | Ustawienia ogólne, baza danych, ekonomia, powitalne kity |
| `kits.yml` | Definicje kitów (ikony, przedmioty, ceny, cooldowny, rangi) |
| `gui.yml` | Wygląd wszystkich interfejsów graficznych |
| `messages.yml` | Wiadomości i animacje |
| `animations.yml`, `animations2.yml` | Szablony animacji wiadomości |
| `favorites.yml`, `stats.yml`, `playersettings.yml` | Dane graczy (generowane automatycznie) |

## Struktura projektu

```
src/main/java/pl/tymek24/fortcodekits/
├── FortCodeKits.java        Główna klasa pluginu
├── command/                 Komendy
├── gui/                     Interfejsy graficzne
├── listener/                Listenery zdarzeń
├── model/                   Model danych (Kit, rzadkości, sortowanie)
├── service/                 Logika biznesowa (cooldowony, statystyki, ulubione)
├── database/                SQLite / MySQL (HikariCP)
├── integration/             PlaceholderAPI, Vault, LuckPerms
├── animation/               Silnik animacji wiadomości
├── security/                Moduł licencji
└── util/                    Narzędzia
src/main/resources/          Konfiguracja pluginu
```
