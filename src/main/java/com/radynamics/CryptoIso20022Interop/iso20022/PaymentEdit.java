package com.radynamics.CryptoIso20022Interop.iso20022;

import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.Origin;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.TransmissionState;

public class PaymentEdit {
    private final Payment payment;

    private PaymentEdit(Payment payment) {
        if (payment == null) throw new IllegalArgumentException("Parameter 'payment' cannot be null");
        this.payment = payment;
    }

    public static PaymentEdit is(Payment payment) {
        return new PaymentEdit(payment);
    }

    public boolean editable() {
        return payment.getOrigin() != Origin.Ledger && payment.getTransmission() != TransmissionState.Waiting;
    }

    public boolean removable() {
        return editable() && payment.getTransmission() != TransmissionState.Success && payment.getOrigin().isDeletable();
    }
}
