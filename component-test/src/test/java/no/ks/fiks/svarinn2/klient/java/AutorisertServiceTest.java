package no.ks.fiks.svarinn2.klient.java;

import no.ks.fiks.commons.authorization.Privilegier;
import no.ks.fiks.commons.authorization.RessursType;
import no.ks.fiks.componenttest.support.autorisasjon.AutorisasjonQueryMock;
import no.ks.fiks.componenttest.support.autorisasjon.AutorisasjonUpdateMock;
import no.ks.fiks.componenttest.support.fullmakt.FullmaktMock;
import no.ks.fiks.componenttest.support.spring.ServiceComponentTest;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

public class AutorisertServiceTest extends ServiceComponentTest {

    public AutorisertServiceTest(String... serviceNames) {
        super(serviceNames);
    }

    @BeforeAll
    public void beforeAll(@Autowired AutorisasjonQueryMock autorisasjonQueryMock, @Autowired AutorisasjonUpdateMock autorisasjonUpdateMock, @Autowired FullmaktMock fullmaktMock) {
        autorisasjonQueryMock.setupAuthorizationQuery(null, Privilegier.ADMIN, null, RessursType.TJENESTE_SVARINN.getId());
        autorisasjonQueryMock.setupAuthorizationSuccess();
        autorisasjonQueryMock.setupRessurs();

        autorisasjonUpdateMock.setupRessursCreation();

        fullmaktMock.setupAuthorizedSuccess();
    }
}
