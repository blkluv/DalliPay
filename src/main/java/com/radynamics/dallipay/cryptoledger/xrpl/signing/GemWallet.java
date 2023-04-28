package com.radynamics.dallipay.cryptoledger.xrpl.signing;

import com.radynamics.dallipay.browserwalletbridge.BridgeException;
import com.radynamics.dallipay.browserwalletbridge.BrowserApi;
import com.radynamics.dallipay.browserwalletbridge.gemwallet.PayloadConverter;
import com.radynamics.dallipay.browserwalletbridge.httpserver.BridgeEventListener;
import com.radynamics.dallipay.browserwalletbridge.httpserver.EmbeddedServer;
import com.radynamics.dallipay.cryptoledger.Ledger;
import com.radynamics.dallipay.cryptoledger.Transaction;
import com.radynamics.dallipay.cryptoledger.signing.*;
import com.radynamics.dallipay.cryptoledger.transaction.TransmissionState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class GemWallet implements TransactionSubmitter {
    private final static Logger log = LogManager.getLogger(GemWallet.class);

    private final BrowserApi api = new com.radynamics.dallipay.browserwalletbridge.gemwallet.GemWallet();
    private final EmbeddedServer server = new EmbeddedServer(api);
    private final TransactionSubmitterInfo info;
    private final ArrayList<TransactionStateListener> stateListener = new ArrayList<>();
    private final Ledger ledger;

    public final static String Id = "gemWallet";

    private final ResourceBundle res = ResourceBundle.getBundle("i18n.TransactionSubmitter");

    public GemWallet(Ledger ledger) {
        if (ledger == null) throw new IllegalArgumentException("Parameter 'ledger' cannot be null");
        this.ledger = ledger;

        info = new TransactionSubmitterInfo();
        info.setTitle(res.getString("gemwallet.title"));
        info.setDescription(res.getString("gemwallet.desc"));
        info.setDetailUri(URI.create("https://gemwallet.app"));

        server.addBridgeEventListener(new BridgeEventListener() {
            @Override
            public void onPayloadSent(com.radynamics.dallipay.browserwalletbridge.httpserver.Transaction t, String txHash) {
                var xrplTx = ((TransactionDto) t).getTransaction();
                xrplTx.setId(txHash);
                xrplTx.setBooked(ZonedDateTime.now());
                xrplTx.refreshTransmission();
                raiseSuccess(xrplTx);
            }

            @Override
            public void onError(com.radynamics.dallipay.browserwalletbridge.httpserver.Transaction t, String key, String message) {
                var xrplTx = ((TransactionDto) t).getTransaction();
                if ("user_rejected".equals(key)) {
                    xrplTx.refreshTransmission(new BridgeException(res.getString("gemwallet.rejected")));
                } else {
                    xrplTx.refreshTransmission(new BridgeException("Error, key: %s, message: %s".formatted(key, message)));
                }
                raiseFailure(xrplTx);
            }
        });
    }

    @Override
    public String getId() {
        return Id;
    }

    @Override
    public Ledger getLedger() {
        return ledger;
    }

    @Override
    public void submit(Transaction[] transactions) {
        try {
            server.start();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return;
        }

        for (var trx : transactions) {
            var t = (com.radynamics.dallipay.cryptoledger.xrpl.Transaction) trx;
            t.refreshTransmissionState(TransmissionState.Waiting);
            raiseProgressChanged(t);
        }

        for (var trx : transactions) {
            var t = (com.radynamics.dallipay.cryptoledger.xrpl.Transaction) trx;
            try {
                var future = server.sendPayment(new TransactionDto(t), PayloadConverter.toJson(t));
                future.join();
            } catch (Exception e) {
                t.refreshTransmission(e);
                raiseFailure(t);
            }
        }

        server.stopHttpServer();
    }

    @Override
    public PrivateKeyProvider getPrivateKeyProvider() {
        return new EmptyPrivateKeyProvider();
    }

    @Override
    public TransactionSubmitterInfo getInfo() {
        return info;
    }

    @Override
    public void addStateListener(TransactionStateListener l) {
        stateListener.add(l);
    }

    private void raiseProgressChanged(com.radynamics.dallipay.cryptoledger.xrpl.Transaction t) {
        for (var l : stateListener) {
            l.onProgressChanged(t);
        }
    }

    private void raiseSuccess(com.radynamics.dallipay.cryptoledger.xrpl.Transaction t) {
        for (var l : stateListener) {
            l.onSuccess(t);
        }
    }

    private void raiseFailure(com.radynamics.dallipay.cryptoledger.xrpl.Transaction t) {
        for (var l : stateListener) {
            l.onFailure(t);
        }
    }

    private static class TransactionDto implements com.radynamics.dallipay.browserwalletbridge.httpserver.Transaction {
        private final com.radynamics.dallipay.cryptoledger.xrpl.Transaction t;

        public TransactionDto(com.radynamics.dallipay.cryptoledger.xrpl.Transaction t) {
            this.t = t;
        }

        @Override
        public double getAmount() {
            return t.getAmount().getNumber().doubleValue();
        }

        @Override
        public String getCcy() {
            return t.getAmount().getCcy().getCode();
        }

        public com.radynamics.dallipay.cryptoledger.xrpl.Transaction getTransaction() {
            return t;
        }
    }
}
