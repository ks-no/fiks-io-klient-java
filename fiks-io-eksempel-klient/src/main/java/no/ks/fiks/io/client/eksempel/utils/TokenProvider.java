package no.ks.fiks.io.client.eksempel.utils;

import no.ks.fiks.maskinporten.AccessTokenRequestBuilder;
import no.ks.fiks.maskinporten.Maskinportenklient;

public class TokenProvider {
    private final Maskinportenklient maskinportenKlient;
    private static final String MASKINPORTEN_KS_SCOPE = "ks:fiks";

    public TokenProvider(Maskinportenklient maskinportenKlient) {
        this.maskinportenKlient = maskinportenKlient;
    }

    public String getMaskinportenToken() {
        return maskinportenKlient.getAccessToken(new AccessTokenRequestBuilder().scope(MASKINPORTEN_KS_SCOPE).build());
    }
}