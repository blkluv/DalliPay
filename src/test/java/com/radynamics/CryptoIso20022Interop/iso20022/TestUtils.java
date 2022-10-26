package com.radynamics.CryptoIso20022Interop.iso20022;

import com.radynamics.CryptoIso20022Interop.cryptoledger.Ledger;
import com.radynamics.CryptoIso20022Interop.exchange.Currency;

public final class TestUtils {
    public static Currency createIssuedCcy(Ledger ledger, String ccyCode) {
        return createIssuedCcy(ledger, ccyCode, ccyCode + "_issuer");
    }

    public static Currency createIssuedCcy(Ledger ledger, String ccyCode, String issuer) {
        return new Currency(ccyCode, ledger.createWallet(issuer, ""));
    }
}
