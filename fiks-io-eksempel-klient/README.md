# Fiks IO Java Eksempel Applikasjon

Eksempel applikasjon som viser hvordan man bruker Fiks IO Java-klienten til å sende og motta meldinger.

## Om eksempel applikasjonen

Eksempel applikasjonen er en Java-konsollapplikasjon som demonstrerer følgende funksjoner:
- Opprettelse av en Fiks IO-klient
- Abonnering på meldinger
- Sending av meldinger
- Svar på meldinger
- Henting av kontoinformasjon

Applikasjonen kjører som en interaktiv konsolloop som lytter etter brukerinput for å sende meldinger og hente kontoinformasjon. Når applikasjonen mottar en PING-melding, svarer den automatisk med en PONG-melding.

## Forutsetninger

For å kunne bruke Fiks IO må du ha:
- En aktiv Fiks IO-konto med en tilknyttet integrasjon
- Virksomhetssertifikat
- Privat nøkkel for Fiks IO-kontoen
- Integrasjons-ID og passord

Disse kan settes opp for din organisasjon på:
- [FIKS-Konfigurasjon (test)](https://forvaltning.fiks.test.ks.no/fiks-konfigurasjon/)
- [FIKS-Konfigurasjon (produksjon)](https://forvaltning.fiks.ks.no/fiks-konfigurasjon/)

## Installasjon og konfigurering

### Konfigurer egenskaper

Applikasjonen trenger flere konfigurasjonsfiler. Kopier eksempelfiler og fyller inn dine verdier:

```bash
cp src/main/resources/config.properties.example.test src/main/resources/config.properties
cp src/main/resources/maskinporten.properties.example.test src/main/resources/maskinporten.properties
cp src/main/resources/fiks-api.properties.example.test src/main/resources/fiks-api.properties
cp src/main/resources/amqp.properties.example.test src/main/resources/amqp.properties
```

Se eksempelfiler (`fiks-io-eksempel-klient/src/main/resources/*.example.test`) for test-miljøkonfigurering.

Viktige egenskaper:
- **config.properties**: Inneholder sertifikat, nøkkel, konto-ID og integrasjonsdetaljer
- **maskinporten.properties**: Maskinporten-konfigurering for autentisering
- **fiks-api.properties**: API-verts (test: `api.fiks.test.ks.no`, prod: `api.fiks.ks.no`)
- **amqp.properties**: RabbitMQ-konfigurering (test: `io.fiks.test.ks.no`, prod: `io.fiks.ks.no`)

### Sertifikater og nøkler

Plassér ditt virksomhetssertifikat og privat nøkkel i `src/main/resources/`:

```bash
cp /path/to/virksomhetssertifikat.p12 src/main/resources/

cp /path/to/private.key src/main/resources/
```

## Kjøring av applikasjonen

Bygg prosjektet:
```bash
mvn clean install
```

Kjør applikasjonen:
```bash
mvn exec:java -Dexec.mainClass="no.ks.fiks.io.client.eksempel.EksempelApp" -f fiks-io-eksempel-klient/pom.xml
```

Kommandoene må kjøres fra `fiks-io-klient-java/` mappen for å fungere.

## Bruk

Etter oppstart av applikasjonen vil du se følgende meny:

```
╔════════════════════════════════════════════════════════════════╗
║             TILGJENGELIGE KOMMANDOER                           ║
╠════════════════════════════════════════════════════════════════╣
║  MELDINGER:                                                    ║
║    P - Send PING melding                                       ║
║    G - Send PONG melding                                       ║
║                                                                ║
║  KONTO OG TILGANG:                                             ║
║    K - Hent konto informasjon og status                        ║
║    M - Hent maskinporten token                                 ║
║                                                                ║
║  FIKS ARKIV KONTO:                                             ║
║    N - Opprett Fiks Arkiv konto som arkiv                      ║
║    T - Send tilgangsforespørsel til opprettet konto            ║
║    S - Se alle tilgangforspørslene                             ║
║    A - Godkjenn alle tilgangforspørslene                       ║
║                                                                ║
║  SYSTEM:                                                       ║
║    Q - Avslutter applikasjonen                                 ║
╚════════════════════════════════════════════════════════════════╝
```

### Kommandoer

#### Meldinger
- **P** - Send en PING melding til mottaker
- **G** - Send en PONG melding til mottaker

#### Konto og Tilgang
- **K** - Henter og viser kontoinformasjon inkludert konto-ID, navn, Fiks-org navn og status
- **M** - Henter og viser gjeldende Maskinporten access token

#### Fiks Arkiv Konto
Disse kommandoene brukes for å opprette en Fiks Arkiv-konto og håndtere tilgangsforespørsler:
- **N** - Opprett en ny Fiks Arkiv-konto som arkiv i organisasjonen
- **T** - Send tilgangsforespørsel til den opprettede Fiks Arkiv-kontoen
- **S** - Se alle mottatte tilgangsforespørsler
- **A** - Godkjenn alle mottatte tilgangsforespørsler

#### System
- **Q** - Avslutter applikasjonen

### Fiks Arkiv Konto - Opprettelsesflyt

For å opprette en Fiks Arkiv-konto, følg disse stegene:

1. **Opprett Fiks Arkiv konto (N)**: Opprett en ny Fiks Arkiv-konto på system id angitt i properties fil.
2. **Send tilgangsforespørsel (T)**: Send en tilgangsforespørsel til den opprettede kontoen
3. **Se tilgangsforespørsler (S)**: Vis alle mottatte tilgangsforespørsler
4. **Godkjenn tilgangsforespørsler (A)**: Godkjenn alle tilgangsforespørslene

Hver konto kan kun opprettes én gang. Hvis du prøver å opprette den igjen, vil applikasjonen informere deg om at den allerede er opprettet.

### Maskinporten Token og API-kall

Fiks IO API bruker Maskinporten for autentisering. Velg alternativ **M** i menyen for å hente en Maskinporten access token. Tokenet er en JWT (JSON Web Token) som genereres basert på din organisasjons virksomhetssertifikat og privatnøkkel.

**Viktig:** Maskinporten token har en begrenset levetid på typisk 2 minutt og må fornyes ved behov. Hvis det er behov for å gjøre mange API-kall manuelt så bør det settes opp en maskinporten klient med lengre levetid.

#### Eksempel API-kall med curl

Her er et eksempel på hvordan du kan hente konto-informasjon ved å bruke et Maskinporten token:

```bash
curl -X 'GET' \
  'https://api.fiks.test.ks.no/fiks-protokoll/katalog/api/v1/kontoer/<kontoid>' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <maskinporten-token>' \
  -H 'IntegrasjonId: <integrasjonid>' \
  -H 'IntegrasjonPassord: <integrasjonpassord>'
```

**Parametre som må byttes:**
- `<kontoid>`: Din Fiks IO konto-ID
- `<maskinporten-token>`: Maskinporten access token fra kommando M
- `<integrasjonid>`: Din integrasjons-ID
- `<integrasjonpassord>`: Dit integrasjons-passord


## Ressurser

- [Fiks IO dokumentasjon](https://ks-no.github.io/fiks-plattform/tjenester/fiksprotokoll/fiksio/)
- [Maskinporten dokumentasjon](https://docs.digdir.no/docs/idporten/maskinporten/)
