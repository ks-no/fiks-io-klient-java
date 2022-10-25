# Fiks IO java klient
[![MIT Licens](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/ks-no/fiks-io-klient-java/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/no.ks.fiks/fiks-io-klient-java.svg)](https://search.maven.org/search?q=g:no.ks.fiks%20a:fiks-io-klient-java)
[![DepShield Badge](https://depshield.sonatype.org/badges/ks-no/fiks-io-klient-java/depshield.svg)](https://depshield.github.io)
![GitHub last commit](https://img.shields.io/github/last-commit/ks-no/fiks-io-klient-java.svg)
![GitHub Release Date](https://img.shields.io/github/release-date/ks-no/fiks-io-klient-java.svg)

Dette er en klient for å bruke Fiks IO i et JVM miljø


Fiks IO er et meldingssystem for offentlig sektor i Norge. [Om Fiks IO](https://ks-no.github.io/fiks-plattform/tjenester/fiksprotokoll/fiksio/)

Fiks IO er også det underliggende meldingssystemet for Fiks Protokoll meldinger. Les mer om Fiks Protokoll [her](https://ks-no.github.io/fiks-plattform/tjenester/fiksprotokoll/)


### Forenkler bruk av Fiks-IO
Denne klienten og tilsvarende klienter for andre språk utgitt av KS forenkler autentisering, kryptering og kommunikasjon med meldinger via Fiks-IO.
Fiks-IO forlanger f.eks. noen spesifikke [headere](https://ks-no.github.io/fiks-plattform/tjenester/fiksprotokoll/fiksio/#headere) i meldingene.
Ved å bruke denne klienten blir disse detaljene skjult og forenkler sending og mottak av meldinger gjennom Fiks-IO. Du kan lese mer om Fiks-IO headerene [her](https://ks-no.github.io/fiks-plattform/tjenester/fiksprotokoll/fiksio/#headere).


## Ta i bruk
### Forutsetninger

  - Java 11 eller høyere

### Maven
Legg til følgende i POM-filen din:

    <dependencies>
       <dependency>
            <groupId>no.ks.fiks</groupId>
            <artifactId>fiks-io-klient-java</artifactId>
            <version>2.0.0</version>
       </dependency>
    </dependencies>

## Bruk

```java
final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
                                            // sett konfig
                                            .build();
final FiksIOKlientFactory fiksIOKlientFactory = new FiksIOKlientFactory(fiksIOKonfigurasjon);
final FiksIOKlient fiksIOKlient = fiksIOKlientFactory.build();
// Lytte på meldinger
fiksIOKlient.newSubscription((motattMelding, svarSender) -> {
                         // Gjør noe med mottatt melding
                     });

// Slå opp konto
final Optional<Konto> fiksIoKonto = fiksIOKlient.lookup(...);


// Sende melding
final SendtMelding sendtMelding = fiksIoKonto.map(konto -> fiksIOKlient.send(...)).orElseThrow(() -> new IllegalStateException("Kunne ikke sende til Fiks IO"));
```

### Konfigurasjon av klienten

For å konfigurere klienten, trengs en instans av `FiksIOKonfigurasjon`. Den har en tilhørende *builder* som kan benyttes til å angi all konfigurasjon. Eksempel:
```java
FiksIoKonfigurasjon konfigurasjon = FiksIOKonfigurasjon.builder()
    .fiksApiKonfigurasjon(FiksApiKonfigurasjon.builder()
        .host("api.fiks.ks.no")
        .port(443)
        .scheme("https")
        .build())
    .amqpKonfigurasjon(AmqpKonfigurasjon.builder()
        .host("io.fiks.ks.no")
        .port(5671)
        .build())
    // Resten av konfigurasjonen...
    .build();
```

For å gjøre det enklere å sette opp standard konfigurasjon for *test* og *prod* miljøene, finnes det i tillegg to funksjoner `defaultProdConfiguration` og `defaultTestConfiguration` for å lage default konfigurasjon. Påkrevde felter angis som argument, slik:
```java
final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.defaultProdConfiguration(
    clientId,
    integrationId,
    integrationPassword,
    KontoKonfigurasjon.builder()
        .kontoId(new KontoId(kontoId))
        .privatNokkel(privateKey)
        .build(),
    VirksomhetssertifikatKonfigurasjon.builder()
        .keyAlias(keyAlias)
        .keyPassword(keyPassword)
        .keyStore(keyStore)
        .keyStorePassword(keyStorePassword)
        .build());
```


**privatNokkel**: `privatNokkel` property forventer en private key i PKCS#8 format. En privat nøkkel som har PKCS#1 format vil føre til en exception. En PKCS#1 nøkkel kan bli konvertert ved hjelp av denne kommandoen:
```powershell
openssl pkcs8 -topk8 -nocrypt -in <pkcs#1 key file> -out <pkcs#8 key file>
```


### Konfigurasjon av Offentlig Nøkkel for mottaker

Ved oppsett av Fiks IO klienten kan man velge om man vil sette opp en egen metode for å hente offentlig nøkkel til mottaker av meldinger.
Man må da lage en egen implementasjon av `PublicKeyProvider` med metoden `X509Certificate getPublicKey(final KontoId kontoId)`. Altså gitt en `KontoId` skal
den returnere en nøkkel som skal benyttes ved sending av meldinger til den kontoen. En instans av denne implementasjonen angis som argument til `FiksIOKlientFactory` slik:
`public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon, @NonNull PublicKeyProvider publicKeyProvider)`

Om man ikke angir en egen implemtasjon av `PublicKeyProvider`, vil klienten bli satt opp med en instans av `KatalogPublicKeyProvider` som
vil hente offentlig nøkkel fra katalogtjenesten. Man kaller da konstruktøren til FiksIOKlientFactory med kun 1 argument slik:
`public FiksIOKlientFactory(@NonNull FiksIOKonfigurasjon fiksIOKonfigurasjon)`

## Dokumentasjon for tjeneste:

 * [FIKS IO](https://ks-no.github.io/fiks-platform/tjenester/fiksio/)
