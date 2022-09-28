package com.radynamics.CryptoIso20022Interop.ui.paymentTable;

import com.radynamics.CryptoIso20022Interop.cryptoledger.Wallet;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;

public interface WalletChangedListener {
    void onChanged(Payment p, Wallet newWallet);
}
