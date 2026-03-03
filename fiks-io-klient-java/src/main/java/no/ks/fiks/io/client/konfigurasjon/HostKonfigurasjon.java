package no.ks.fiks.io.client.konfigurasjon;

public interface HostKonfigurasjon {

    public String getHost();

    public void setHost(String host);

    public Integer getPort();

    public void setPort(Integer port);

    public String getScheme();

    public void setScheme(String scheme);

    default String getUrl() {
        return String.format("%s://%s:%s", getScheme(), getHost(), getPort());
    }
}
