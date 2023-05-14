package com.dominity.ing.transactions.service;

import com.dominity.ing.utils.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

public class TransactionsService {
    private static class AccountDetails {
        private int debit;
        private int credit;
        private double balance;

        public AccountDetails(int debit, int credit, double balance) {
            this.debit = debit;
            this.credit = credit;
            this.balance = balance;
        }

        AccountDetails debit(double amount) {
            this.debit ++;
            this.balance -= amount;
            return this;
        }

        AccountDetails credit(double amount) {
            this.credit ++;
            this.balance += amount;
            return this;
        }

        public int getDebit() {
            return debit;
        }

        public int getCredit() {
            return credit;
        }

        public double getBalance() {
            return balance;
        }
    }

    public void prepareTotal(InputStream inputStream, OutputStream outputStream) throws IOException {
        JsonParser.scrollStreamTill('[', inputStream);
        var accounts = parseTransactions(inputStream);
        prepareResponse(accounts, outputStream);
    }

    private Map<String, AccountDetails> parseTransactions(InputStream inputStream) throws IOException {
        var accounts = new TreeMap<String, AccountDetails>();
        byte ch;
        while ((ch = (byte) inputStream.read()) != -1) {
            if (ch != '{') {
                continue;
            }
            addOrUpdateAccountDetails(inputStream, accounts);
        }
        return accounts;
    }

    private void addOrUpdateAccountDetails(InputStream inputStream, Map<String, AccountDetails> accounts) throws IOException {
        var resultMap = new EnumMap<>(FieldName.class);
        parseJsonField(resultMap, inputStream);
        parseJsonField(resultMap, inputStream);
        parseJsonField(resultMap, inputStream);

        accounts.compute((String) resultMap.get(FieldName.DEBIT_ACCOUNT), (k, v) -> v == null
                ? new AccountDetails(1, 0, -(double) resultMap.get(FieldName.AMOUNT)) : v.debit((double) resultMap.get(FieldName.AMOUNT)));
        accounts.compute((String) resultMap.get(FieldName.CREDIT_ACCOUNT), (k, v) -> v == null
                ? new AccountDetails(0, 1, (double) resultMap.get(FieldName.AMOUNT)) : v.credit((double) resultMap.get(FieldName.AMOUNT)));
    }

    private void parseJsonField(Map<FieldName, Object> resultMap, InputStream inputStream) throws IOException {
        JsonParser.scrollStreamTill('"', inputStream);
        var fieldName = parseFieldName(inputStream);
        if (FieldName.DEBIT_ACCOUNT == fieldName) {
            JsonParser.scrollStreamTill('"', inputStream);
            resultMap.put(FieldName.DEBIT_ACCOUNT, JsonParser.parseString(inputStream));
        } else if (FieldName.CREDIT_ACCOUNT == fieldName) {
            JsonParser.scrollStreamTill('"', inputStream);
            resultMap.put(FieldName.CREDIT_ACCOUNT, JsonParser.parseString(inputStream));
        } else if (FieldName.AMOUNT == fieldName) {
            JsonParser.scrollStreamTill(':', inputStream);
            resultMap.put(FieldName.AMOUNT, JsonParser.parseDouble(inputStream));
        }
    }

    private FieldName parseFieldName(InputStream inputStream) throws IOException {
        return JsonParser.parseFieldName(inputStream, new FieldName[FieldName.values().length], FieldName.values());
    }

    private void prepareResponse(Map<String, AccountDetails> accounts, OutputStream outputStream) throws IOException {
        if (accounts.isEmpty()) {
            outputStream.write('[');
            outputStream.write(']');
            return;
        }
        outputStream.write('[');
        var iterator = accounts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            writeAccountDetails(outputStream, entry.getKey(), entry.getValue());
            if (iterator.hasNext()) {
                outputStream.write(',');
            }
        }
        outputStream.write(']');
    }

    private void writeAccountDetails(OutputStream outputStream, String accountNo, AccountDetails accountDetails) throws IOException {
        outputStream.write('{');
        JsonParser.writeFieldName("account", outputStream);
        outputStream.write(':');
        JsonParser.writeString(accountNo, outputStream);
        outputStream.write(',');
        JsonParser.writeFieldName("debitCount", outputStream);
        outputStream.write(':');
        JsonParser.writeInt(accountDetails.getDebit(), outputStream);
        outputStream.write(',');
        JsonParser.writeFieldName("creditCount", outputStream);
        outputStream.write(':');
        JsonParser.writeInt(accountDetails.getCredit(), outputStream);
        outputStream.write(',');
        JsonParser.writeFieldName("balance", outputStream);
        outputStream.write(':');
        writeDouble(accountDetails.getBalance(), outputStream);
        outputStream.write('}');
    }

    private void writeDouble(double value, OutputStream outputStream) throws IOException {
        var valueStr = Double.toString(value);
        for (int i = 0; i < valueStr.length(); i ++) {
            outputStream.write(valueStr.charAt(i));
        }
    }

    private enum FieldName implements JsonParser.Named {
        DEBIT_ACCOUNT("debitAccount"),
        CREDIT_ACCOUNT("creditAccount"),
        AMOUNT("amount");

        private final String name;

        FieldName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
