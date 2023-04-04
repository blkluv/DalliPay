package com.radynamics.dallipay.cryptoledger.xrpl;

import com.moandjiezana.toml.Toml;
import com.radynamics.dallipay.cryptoledger.NetworkInfo;
import com.radynamics.dallipay.cryptoledger.Wallet;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.HashMap;

public class DomainVerifier {
    final static Logger log = LogManager.getLogger(DomainVerifier.class);
    private NetworkInfo network;

    public DomainVerifier(NetworkInfo network) {
        this.network = network;
    }

    public boolean isValid(Wallet wallet, String domain) {
        Toml toml;
        try {
            var scheme = domain.startsWith("https://") ? "" : "https://";
            var url = new URL(String.format("%s%s/.well-known/xrp-ledger.toml", scheme, domain));
            toml = new Toml().read(url.openStream());
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return false;
        }

        var accounts = toml.getList("ACCOUNTS");
        if (accounts == null) {
            return false;
        }

        for (var o : accounts) {
            if (!(o instanceof HashMap)) {
                continue;
            }
            var map = ((HashMap<?, String>) o);
            var networkId = map.get("network");
            var matchesNetwork = StringUtils.isEmpty(networkId) || network.matches(networkId);

            var address = map.get("address");
            if (matchesNetwork && address.equals(wallet.getPublicKey())) {
                return true;
            }
        }

        return false;
    }
}
