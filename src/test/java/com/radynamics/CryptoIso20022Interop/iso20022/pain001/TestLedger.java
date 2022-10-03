package com.radynamics.CryptoIso20022Interop.iso20022.pain001;

import com.radynamics.CryptoIso20022Interop.DateTimeRange;
import com.radynamics.CryptoIso20022Interop.cryptoledger.*;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.ValidationResult;
import com.radynamics.CryptoIso20022Interop.exchange.Currency;
import com.radynamics.CryptoIso20022Interop.exchange.Money;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.time.ZonedDateTime;

public class TestLedger implements Ledger {
    private final static int FACTOR = 1000;
    private WalletInfoProvider[] walletInfoProvider = new WalletInfoProvider[0];
    private NetworkInfo network;

    private static final String nativeCcySymbol = "TEST";

    @Override
    public LedgerId getId() {
        return LedgerId.Xrpl;
    }

    @Override
    public String getNativeCcySymbol() {
        return nativeCcySymbol;
    }

    @Override
    public Transaction createTransaction() {
        return new TestTransaction(this, 0d, getNativeCcySymbol());
    }

    @Override
    public void send(Transaction[] transactions) throws Exception {
        throw new NotImplementedException();
    }

    static Money convertToNativeCcyAmount(long amountSmallestUnit) {
        return Money.of(amountSmallestUnit / FACTOR, new Currency(nativeCcySymbol));
    }

    @Override
    public FeeSuggestion getFeeSuggestion() {
        return new FeeSuggestion(convertToNativeCcyAmount(5), convertToNativeCcyAmount(10), convertToNativeCcyAmount(15));
    }

    @Override
    public Wallet createWallet(String publicKey, String secret) {
        return new Wallet() {
            @Override
            public String getPublicKey() {
                return publicKey;
            }

            @Override
            public String getSecret() {
                return secret;
            }

            @Override
            public void setSecret(String secret) {
                // do nothing
            }

            @Override
            public MoneyBag getBalances() {
                return new MoneyBag();
            }

            @Override
            public LedgerId getLedgerId() {
                return getId();
            }
        };
    }

    @Override
    public void refreshBalance(Wallet wallet) {
        // do nothing;
    }

    @Override
    public TransactionResult listPaymentsSent(Wallet wallet, ZonedDateTime since, int limit) throws Exception {
        return new TransactionResult();
    }

    @Override
    public TransactionResult listPaymentsReceived(Wallet wallet, DateTimeRange period) throws Exception {
        return new TransactionResult();
    }

    @Override
    public boolean exists(Wallet wallet) {
        return true;
    }

    @Override
    public ValidationResult[] validateReceiver(Wallet wallet) {
        return new ValidationResult[0];
    }

    @Override
    public NetworkInfo getNetwork() {
        throw new NotImplementedException();
    }

    @Override
    public void setNetwork(NetworkInfo network) {
        this.network = network;
    }

    @Override
    public PaymentHistoryProvider getPaymentHistoryProvider() {
        return null;
    }

    @Override
    public WalletInfoProvider[] getInfoProvider() {
        return walletInfoProvider;
    }

    @Override
    public void setInfoProvider(WalletInfoProvider[] walletInfoProvider) {
        this.walletInfoProvider = walletInfoProvider;
    }

    @Override
    public boolean isValidPublicKey(String publicKey) {
        return true;
    }

    @Override
    public boolean isSecretValid(Wallet wallet) {
        return !StringUtils.isEmpty(wallet.getSecret());
    }

    @Override
    public NetworkInfo[] getDefaultNetworkInfo() {
        return new NetworkInfo[0];
    }
}
