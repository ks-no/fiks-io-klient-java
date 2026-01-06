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
## Versjoner
Versjon 3.x krever Java 17 eller høyere. Har du ikke mulighet for å bruke det må du holde deg på v. 2.x

| Versjon | Java baseline | Spring Boot versjon | Status      |
|---------|---------------|---------------------|-------------|
| 3.x     | Java 17       | 3.X                 | Aktiv       |
| 2.X     | Java 11       | 2.X                 | Vedlikehold |

### Maven
Legg til følgende i POM-filen din:

    <dependencies>
       <dependency>
            <groupId>no.ks.fiks</groupId>
            <artifactId>fiks-io-klient-java</artifactId>
            <version>x.x.x</version>
       </dependency>
    </dependencies>

## Bruk

### Oppsett

```java
final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
                                            // Bygg konfigurasjon - se mer under "Konfigurasjon av klienten"
                                            .build();
final FiksIOKlientFactory fiksIOKlientFactory = new FiksIOKlientFactory(fiksIOKonfigurasjon);
final FiksIOKlient fiksIOKlient = fiksIOKlientFactory.build();
// Lytte på meldinger
fiksIOKlient.newSubscription((motattMelding, svarSender) -> {
                         // Gjør noe med mottatt melding
                     });

// Sende melding
final SendtMelding sendtMelding = fiksIoKonto.map(konto -> fiksIOKlient.send(...)).orElseThrow(() -> new IllegalStateException("Kunne ikke sende til Fiks IO"));
```

##### Oppsett med egen Maskinporten klient
FiksIOKlientFactory bruker [Fiks Maskinporten klient](https://github.com/ks-no/fiks-maskinporten) som standard, men en kan bruke egen klient for generering av AccessToken utstedt fra maskinporten.

NB! Krever skope "ks:fiks"
```java
// sett din egen maskinportenAccessTokenSupplier
fiksIOKlientFactory.setMaskinportenAccessTokenSupplier(maskinportenAccessTokenSupplier)
```

### Konfigurasjon av klienten
FiksIO klienten er default satt opp med 5 tråder for kryptering. Størrelsen kan overstyres ved behov. For å overstyre må en spesifisere FiksIOKonfigurasjon.executor
FiksIO klienten er default satt opp med HttpClient connection pool, med maks 5 requests pr route og 25 totalt. En kan overstyre denne med å konfigurere egen http klient.

NB!! med parallellisering oppnår en ikke raskere prosessering om man kjører med flere enn 5 parallelle tråder pr FiksIO klient, om en ikke samtidig øker størrelse på krypterings og http klient pool


For å konfigurere klienten, trengs en instans av `FiksIOKonfigurasjon`. Den har en tilhørende *builder* som kan benyttes til å angi all konfigurasjon.
Forkortet eksempel:
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
    .fiksIntegrasjonsKonfigurasjon(FiksIntegrasjonKonfigurasjon.builder()
        .integrasjonId("din integrasjonsid")
        .integrasjonPassord("ditt integrasjonspassord")
        .idPortenKonfigurasjon( createMaskinportenKonfigurasjon() ) // Dette er Maskinporten konfigurasjonen! Se eksempel under.
        .build())
    // Resten av konfigurasjonen...
    .build();
```

#### Maskinporten konfigurasjon
IdPortenKonfigurasjon brukes for konfigurasjon av Maskinporten klienten
```java
private static IdPortenKonfigurasjon createMaskinportenPortenKonfigurasjon() {
    return IdPortenKonfigurasjon.builder()
        .accessTokenUri("https://test.maskinporten.no/token")
        .idPortenAudience("https://test.maskinporten.no/")
        .klientId("din klientid")
        .build();
}
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
KontoKonfigurasjon.builder() kan ta inn flere private nøkler. De kan legges til en og en eller som en liste:
```java
KontoKonfigurasjon.builder()
        .kontoId(new KontoId(kontoId))
        .privatNokkel(privateKey1)
        .privatNokkel(privateKey2)
        .build(),
```
```java
KontoKonfigurasjon.builder()
        .kontoId(new KontoId(kontoId))
        .privateNokler(Arrays.asList(privateKey1, privateKey2))
        .privatNokkel(privateKey3)
        .build(),
```

**privateNokler**: `privateNokler` property forventer en eller flere private key i PKCS#8 format. En privat nøkkel som har PKCS#1 format vil føre til en exception. En PKCS#1 nøkkel kan bli konvertert ved hjelp av denne kommandoen:
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

### Bruk av klientKorrelasjonId

Klienten støtter bruk av `klientKorrelasjonId`, en egendefinert header som kan benyttes ved behov. Dette gjør det enklere å identifisere og følge alle meldinger som inngår i en bestemt dialog.
Les mer om dette i [Fiks IO dokumentasjonen](https://developers.fiks.ks.no/tjenester/fiksprotokoll/fiksio/#:~:text=en%20egendefinert%20header.-,KlientKorrelasjonsID,-Dette%20er%20en).



## Dokumentasjon for tjeneste:

 * [FIKS IO](https://ks-no.github.io/fiks-platform/tjenester/fiksio/)
