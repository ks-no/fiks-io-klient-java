# Fiks IO java klient
[![MIT Licens](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/ks-no/fiks-io-klient-java/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/no.ks.fiks/fiks-io-klient-java.svg)](https://search.maven.org/search?q=g:no.ks.fiks%20a:fiks-io-klient-java)
[![DepShield Badge](https://depshield.sonatype.org/badges/ks-no/fiks-io-klient-java/depshield.svg)](https://depshield.github.io)
![GitHub last commit](https://img.shields.io/github/last-commit/ks-no/fiks-io-klient-java.svg)
![GitHub Release Date](https://img.shields.io/github/release-date/ks-no/fiks-io-klient-java.svg)

Klient for å bruke Fiks IO i et JVM miljø
## Getting Started
Legg til maven dependency.

### Prerequisites

  - Java 1.8 or higher

##### Maven
Add dependency no.ks.fiks.svarut:svarut-rest-klient in your POM.

    <dependencies>
       <dependency>
            <groupId>no.ks.fiks</groupId>
            <artifactId>fiks-io-klient-java</artifactId>
            <version>1.2.5</version>
       </dependency>
    </dependencies>


## Usage

```java
final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.builder()
                                            // sett konfig
                                            .build();
final FiksIOKlient fiksIOKlient = FiksIOKlientFactory.build(fiksIOKonfigurasjon);
// Lytte på meldinger
fiksIOKlient.newSubscription((motattMelding, svarSender) -> {
                         // Gjør noe med mottatt melding
                     });

// Slå opp konto
final Optional<Konto> fiksIoKonto = fiksIOKlient.lookup(...);


// Sende melding
final SendtMelding sendtMelding = fiksIoKonto.map(k -> k.send(...)).orElseThrow(() -> new IllegalStateException("Kunne ikke sende til Fiks IO"));
```


## Dokumentasjon for tjeneste:
 
 * [FIKS IO](https://ks-no.github.io/fiks-platform/tjenester_under_utvikling/fiksio/)
