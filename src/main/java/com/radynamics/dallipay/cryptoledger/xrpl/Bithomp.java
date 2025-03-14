package com.radynamics.dallipay.cryptoledger.xrpl;

import com.radynamics.dallipay.cryptoledger.*;
import com.radynamics.dallipay.cryptoledger.Wallet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.net.URI;

public class Bithomp implements WalletLookupProvider, TransactionLookupProvider {
    final static Logger log = LogManager.getLogger(Bithomp.class);
    private final String baseUrl;

    public static final String Id = "bithomp";
    public static final String displayName = "Bithomp";

    public Bithomp(NetworkInfo network) throws LookupProviderException {
        if (network.isLivenet()) {
            this.baseUrl = "https://www.bithomp.com/explorer/";
        } else if (network.isTestnet()) {
            this.baseUrl = "https://test.bithomp.com/explorer/";
        } else {
            throw new LookupProviderException(String.format("%s doesn't support network %s.", displayName, network.getShortText()));
        }
    }

    @Override
    public void open(Wallet wallet) {
        openInBrowser(wallet.getPublicKey());
    }

    @Override
    public void open(String transactionId) {
        openInBrowser(transactionId);
    }

    private void openInBrowser(String value) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(String.format("%s%s", baseUrl, value)));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("No desktop or no browsing supported");
        }
    }
}
