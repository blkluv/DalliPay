package com.radynamics.dallipay.ui.options.XrplPriceOracleEdit;

import com.radynamics.dallipay.cryptoledger.xrpl.IssuedCurrency;
import org.apache.commons.lang3.StringUtils;

public class Record {
    public String first;
    public String second;
    public String issuer;
    public String receiver;

    public Record() {
    }

    public Record(IssuedCurrency o) {
        this();
        first = o.getPair().getFirstCode();
        second = o.getPair().getSecondCode();
        issuer = o.getIssuer().getPublicKey();
        receiver = o.getReceiver().getPublicKey();
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(first) && StringUtils.isEmpty(second) && StringUtils.isEmpty(issuer) && StringUtils.isEmpty(receiver);
    }

    @Override
    public String toString() {
        return String.format("%s/%s, %s, %s", first, second, issuer, receiver);
    }
}
