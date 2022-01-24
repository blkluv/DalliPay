package com.radynamics.CryptoIso20022Interop.iso20022.pain001;

import com.radynamics.CryptoIso20022Interop.exchange.CurrencyConverter;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRate;
import com.radynamics.CryptoIso20022Interop.iso20022.Address;
import com.radynamics.CryptoIso20022Interop.iso20022.IbanAccount;
import com.radynamics.CryptoIso20022Interop.iso20022.OtherAccount;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;
import com.radynamics.CryptoIso20022Interop.iso20022.creditorreference.ReferenceType;
import com.radynamics.CryptoIso20022Interop.transformation.AccountMapping;
import com.radynamics.CryptoIso20022Interop.transformation.TransactionTranslator;
import com.radynamics.CryptoIso20022Interop.transformation.TransformInstruction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class Pain001ReaderTest {
    @Test
    public void readExampleZA1() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new OtherAccount("010832052"), "receiver_010832052"));
        ti.add(new AccountMapping(new OtherAccount("010391391"), "receiver_010391391"));
        ti.add(new AccountMapping(new OtherAccount("010649858"), "receiver_010649858"));
        ti.add(new AccountMapping(new OtherAccount("032233441"), "receiver_032233441"));
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
                new ExchangeRate("EUR", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001ExampleZA1.xml")));

        assertNotNull(transactions);
        assertEquals(4, transactions.length);

        assertTransaction(transactions[0], "010832052", "receiver_010832052", 459000, ReferenceType.Isr, "000000000000060029920346303");
        assertAddress(transactions[0].getReceiverAddress(), new Address("Settelen AG"));
        assertTransaction(transactions[1], "010391391", "receiver_010391391", 3949750, ReferenceType.Isr, "210000000003139471430009017");
        assertAddress(transactions[1].getReceiverAddress(), new Address("Destination AG") {{
            setStreet("Zielstrasse 13");
            setZip("3000");
            setCity("Bern");
            setCountryShort("CH");
        }});
        assertTransaction(transactions[2], "010649858", "receiver_010649858", 2838640, ReferenceType.Isr, "030015972590806420080020801");
        assertAddress(transactions[2].getReceiverAddress(), new Address("Swisscom (Schweiz) AG") {{
            setStreet("Alte Tiefenaustrasse 6");
            setCity("3050 Bern");
        }});
        assertTransaction(transactions[3], "032233441", "receiver_032233441", 1727530, ReferenceType.Isr, "332015900002760103813712236");
        assertAddress(transactions[3].getReceiverAddress(), new Address("Ingram Micro GmbH") {{
            setCity("6330 Cham");
        }});
    }

    @Test
    public void readExampleZA6Scor() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new IbanAccount("GB96MIDL40271522859882"), "receiver_GB96MIDL40271522859882"));
        ti.add(new AccountMapping(new OtherAccount("40271522859882"), "receiver_40271522859882"));
        ti.add(new AccountMapping(new IbanAccount("GB96MIDL40271522859882"), "receiver_GB96MIDL40271522859882"));
        ti.add(new AccountMapping(new IbanAccount("GB96MIDL40271522859882"), "receiver_GB96MIDL40271522859882"));
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
                new ExchangeRate("GBP", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001ExampleZA6Scor.xml")));

        assertNotNull(transactions);
        assertEquals(4, transactions.length);

        assertTransaction(transactions[0], "GB96MIDL40271522859882", "receiver_GB96MIDL40271522859882", 5000000, ReferenceType.Scor, "RF712348231");
        assertTransaction(transactions[1], "40271522859882", "receiver_40271522859882", 6000000);
        assertTransaction(transactions[2], "GB96MIDL40271522859882", "receiver_GB96MIDL40271522859882", 7000000);
        assertTransaction(transactions[3], "GB96MIDL40271522859882", "receiver_GB96MIDL40271522859882", 8000000);
    }

    @ParameterizedTest
    @CsvSource({"TEST", "USE"})
    public void readNoExchangeRate(String targetCcy) throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(targetCcy);
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new IbanAccount("GB96MIDL40271522859882"), "receiver_GB96MIDL40271522859882"));
        ExchangeRate[] rates = new ExchangeRate[0];
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001ExampleZA6Scor.xml")));

        assertNotNull(transactions);
        assertEquals(4, transactions.length);

        assertEquals("GBP", transactions[0].getFiatCcy());
        Assertions.assertEquals(5000, (double) transactions[0].getAmount());
        var expectedLedgerAmount = ledger.getNativeCcySymbol().equals(targetCcy) ? 5000000 : 0;
        assertTransaction(transactions[0], "GB96MIDL40271522859882", "receiver_GB96MIDL40271522859882", expectedLedgerAmount, ReferenceType.Scor, "RF712348231");
    }

    @Test
    public void readSwissQrBillWithQrReference() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH4431999123000889012"), "receiver_CH4431999123000889012"));
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001SwissQrBillWithQrReference.xml")));

        assertNotNull(transactions);
        assertEquals(1, transactions.length);

        assertTransaction(transactions[0], "CH4431999123000889012", "receiver_CH4431999123000889012", 1949750, ReferenceType.SwissQrBill, "210000000003139471430009017");
    }

    @Test
    public void readSwissQrBillWithScorReference() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5800791123000889012"), "receiver_CH4431999123000889012"));
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001SwissQrBillWithScorReference.xml")));

        assertNotNull(transactions);
        assertEquals(1, transactions.length);

        assertTransaction(transactions[0], "CH5800791123000889012", "receiver_CH4431999123000889012", 199950, ReferenceType.Scor, "RF18539007547034");
    }

    @Test
    public void readSwissQrBillWithoutReference() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5800791123000889012"), "receiver_CH4431999123000889012"));
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001SwissQrBillWithoutReference.xml")));

        assertNotNull(transactions);
        assertEquals(1, transactions.length);

        assertTransaction(transactions[0], "CH5800791123000889012", "receiver_CH4431999123000889012", 4444000);
    }

    @Test
    public void readNoAccountMapping() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001SwissQrBillWithoutReference.xml")));

        assertNotNull(transactions);
        assertEquals(1, transactions.length);
        assertNotNull(transactions[0].getSenderAccount());
        assertEquals("CH5481230000001998736", transactions[0].getSenderAccount().getUnformatted());
        assertTransaction(transactions[0], null, "CH5800791123000889012", null, 4444000, null, null);
    }

    @Test
    public void readRmtInfUstrd() throws Exception {
        var ledger = new TestLedger();
        var ti = new TransformInstruction(ledger);
        ti.setTargetCcy(ledger.getNativeCcySymbol());
        // DbtrAcct
        ti.add(new AccountMapping(new IbanAccount("CH5481230000001998736"), "sender_CH5481230000001998736"));
        // CdtrAcct
        ti.add(new AccountMapping(new OtherAccount("25-9034-2"), "receiver_25-9034-2"));
        ExchangeRate[] rates = {
                new ExchangeRate("CHF", ledger.getNativeCcySymbol(), 1, LocalDateTime.now()),
        };
        var ccyConverter = new CurrencyConverter(rates);
        var r = new Pain001Reader(ledger, ti, ccyConverter);

        var tt = new TransactionTranslator(ti, ccyConverter);
        var transactions = tt.apply(r.read(getClass().getClassLoader().getResourceAsStream("pain001/Six/pain001RmtInfUstrd.xml")));

        assertNotNull(transactions);
        assertEquals(1, transactions.length);

        var t = transactions[0];
        assertNotNull(t.getReceiverAccount());
        assertEquals("25-9034-2", t.getReceiverAccount().getUnformatted());
        assertNotNull(t.getReceiverWallet());
        assertEquals("receiver_25-9034-2", t.getReceiverWallet().getPublicKey());
        assertNotNull(t.getMessages());
        assertEquals(1, t.getMessages().length);
        assertEquals("Rechnung Nr. 408", t.getMessages()[0]);
        assertNotNull(t.getStructuredReferences());
        assertEquals(0, t.getStructuredReferences().length);
    }

    private void assertAddress(Address actual, Address expected) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getStreet(), actual.getStreet());
        assertEquals(expected.getZip(), actual.getZip());
        assertEquals(expected.getCity(), actual.getCity());
    }

    private void assertTransaction(Payment t, String receiverAccount, String receiverWallet, double amount) {
        assertTransaction(t, receiverAccount, receiverWallet, amount, null, null);
    }

    private void assertTransaction(Payment t, String receiverAccount, String receiverWallet, double amount, ReferenceType type, String referenceUnformatted) {
        assertTransaction(t, "sender_CH5481230000001998736", receiverAccount, receiverWallet, amount, type, referenceUnformatted);
    }

    private void assertTransaction(Payment t, String senderWallet, String receiverAccount, String receiverWallet, double amount, ReferenceType type, String referenceUnformatted) {
        if (senderWallet == null) {
            assertNull(t.getSenderWallet());
        } else {
            assertNotNull(t.getSenderWallet());
            assertEquals(senderWallet, t.getSenderWallet().getPublicKey());
        }
        assertEquals(amount, t.getLedgerAmountSmallestUnit(), 0);
        assertEquals("TEST", t.getLedgerCcy());
        assertNotNull(t.getReceiverAccount());
        assertEquals(receiverAccount, t.getReceiverAccount().getUnformatted());
        if (receiverWallet == null) {
            assertNull(t.getReceiverWallet());
        } else {
            assertNotNull(t.getReceiverWallet());
            assertEquals(receiverWallet, t.getReceiverWallet().getPublicKey());
        }
        assertNull(t.getId());
        assertNull(t.getInvoiceId());
        assertNotNull(t.getMessages());
        assertEquals(0, t.getMessages().length);
        assertNotNull(t.getStructuredReferences());
        if (referenceUnformatted == null) {
            assertEquals(0, t.getStructuredReferences().length);
        } else {
            assertEquals(1, t.getStructuredReferences().length);
            assertEquals(type, t.getStructuredReferences()[0].getType());
            assertEquals(referenceUnformatted, t.getStructuredReferences()[0].getUnformatted());
        }
    }
}
