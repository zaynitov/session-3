package ru.sbt.jschool.session3.problem1;

import java.util.*;

/**
 */
public class AccountServiceImpl implements AccountService {
    protected FraudMonitoring fraudMonitoring;
    Map<Long, Account> accountMap = new HashMap<>();
    Set<Long> setOfTransactions = new HashSet<>();

    public AccountServiceImpl(FraudMonitoring fraudMonitoring) {
        this.fraudMonitoring = fraudMonitoring;
    }

    @Override
    public Result create(long clientID, long accountID, float initialBalance, Currency currency) {

        if (fraudMonitoring.check(clientID)) return Result.FRAUD;


        if (accountMap.containsKey(accountID)) {
            return Result.ALREADY_EXISTS;
        }
        accountMap.put(accountID, new Account(clientID, accountID, currency, initialBalance));
        return Result.OK;

    }

    @Override
    public List<Account> findForClient(long clientID) {


        List<Account> accountList = new ArrayList<>();

        for (Account account : accountMap.values()) {
            if (account.getClientID() == clientID)
                accountList.add(account);
        }
        if (accountList == null) return Collections.EMPTY_LIST;

        return accountList;

    }

    @Override
    public Account find(long accountID) {

        if (accountMap.containsKey(accountID)) return accountMap.get(accountID);

        return null;
    }

    @Override
    public Result doPayment(Payment payment) {
        if (!setOfTransactions.contains(payment.getOperationID())) {
            setOfTransactions.add(payment.getOperationID());
        } else {
            return Result.ALREADY_EXISTS;
        }

        Account accountPayer = find(payment.getPayerAccountID());
        Account accountRecipient = find(payment.getRecipientAccountID());
        if (accountPayer == null || accountPayer.getClientID() != payment.getPayerID()) {
            return Result.PAYER_NOT_FOUND;
        }
        if (accountRecipient == null || accountRecipient.getClientID() != payment.getRecipientID()) {
            return Result.RECIPIENT_NOT_FOUND;
        }
        if (accountPayer.getBalance() < payment.getAmount()) {
            return Result.INSUFFICIENT_FUNDS;
        }
        if (accountPayer.getCurrency().equals(accountRecipient.getCurrency())) {
            accountRecipient.setBalance(accountRecipient.getBalance() + payment.getAmount());
            accountPayer.setBalance(accountPayer.getBalance() - payment.getAmount());
        } else {
            accountPayer.setBalance(accountPayer.getBalance() - payment.getAmount());
            accountRecipient.setBalance(accountRecipient.getBalance() +
                    accountPayer.getCurrency().to(payment.getAmount(), accountRecipient.getCurrency()));
        }

        refreshAccountMap(accountPayer);
        refreshAccountMap(accountRecipient);

        return Result.OK;
    }


    public void refreshAccountMap(Account accountForRefresh) {
        if (accountMap.containsKey(accountForRefresh.getAccountID())) {
            accountMap.get(accountForRefresh.getAccountID()).setBalance(accountForRefresh.getBalance());
        }

    }


}