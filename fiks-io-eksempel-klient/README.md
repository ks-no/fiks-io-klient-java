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

## Bruk

Etter oppstart av applikasjonen vil du se følgende meny:

```
Starter interaktiv konsoll:
  P - Send PING melding
  G - Send PONG melding
  K - Hent konto informasjon og status
  Q - Avslutter applikasjonen
```

## Ressurser

- [Fiks IO dokumentasjon](https://ks-no.github.io/fiks-plattform/tjenester/fiksprotokoll/fiksio/)
- [Maskinporten dokumentasjon](https://docs.digdir.no/docs/idporten/maskinporten/)
