package com.radynamics.dallipay.cryptoledger;

import okhttp3.HttpUrl;

import java.time.ZonedDateTime;

public class NetworkInfo {
    private HttpUrl url;
    private String networkId;
    private String displayName;

    private static final String liveId = "livenet";
    private static final String testnetId = "testnet";

    public static NetworkInfo create(HttpUrl url, String displayName) {
        return create(url, displayName, null);
    }

    public static NetworkInfo createLivenet(HttpUrl url, String displayName) {
        return create(url, displayName, liveId);
    }

    public static NetworkInfo createTestnet(HttpUrl url, String displayName) {
        return create(url, displayName, testnetId);
    }

    public static NetworkInfo create(HttpUrl url, String displayName, String networkId) {
        if (url == null) throw new IllegalArgumentException("Parameter 'url' cannot be null");
        if (displayName == null) throw new IllegalArgumentException("Parameter 'displayName' cannot be null");
        var o = new NetworkInfo();
        o.networkId = networkId;
        o.url = url;
        o.displayName = displayName;
        return o;
    }

    public String getShortText() {
        return getDisplayName();
    }

    public HttpUrl getUrl() {
        return url;
    }

    public String getId() {
        return networkId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean matches(String text) {
        if (isLivenet() && "main".equalsIgnoreCase(text)) {
            return true;
        }
        if (isTestnet() && "testnet".equalsIgnoreCase(text)) {
            return true;
        }
        return false;
    }

    public ZonedDateTime historyAvailableSince() {
        return ZonedDateTime.now().minusDays(40);
    }

    public boolean isLivenet() {
        return liveId.equals(networkId);
    }

    public boolean isTestnet() {
        return testnetId.equals(networkId);
    }

    public boolean sameNet(NetworkInfo network) {
        if (network == null) throw new IllegalArgumentException("Parameter 'network' cannot be null");
        if (url.equals(network.getUrl())) return true;
        if (networkId.equals(network.networkId)) return true;
        return false;
    }
}
