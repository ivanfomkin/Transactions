import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Bank implements Runnable {
    private ConcurrentHashMap<String, Account> accounts;
    private final Random random = new Random();
    private static Logger log2file = LogManager.getRootLogger();

    public Bank() {
        this.accounts = new ConcurrentHashMap<>();
    }

    public synchronized boolean isFraud(String fromAccountNum, String toAccountNum, long amount)
            throws InterruptedException {
        //Закомментим задержку, а то очень долго ждать, пока программа завершится
//        Thread.sleep(1000);
        return random.nextBoolean();
    }

    /**
     * TODO: реализовать метод. Метод переводит деньги между счетами.
     * Если сумма транзакции > 50000, то после совершения транзакции,
     * она отправляется на проверку Службе Безопасности – вызывается
     * метод isFraud. Если возвращается true, то делается блокировка
     * счетов (как – на ваше усмотрение)
     */
    public void transfer(String fromAccountNum, String toAccountNum, long amount) {
        if (accounts.keySet().contains(fromAccountNum)
                && accounts.keySet().contains(toAccountNum)) {

            Account fromAccount = accounts.get(fromAccountNum);
            Account toAccount = accounts.get(toAccountNum);
            if (fromAccountNum.equals(toAccountNum)) {
                /**
                 * Тут и далее закомментируем подразумеваемый вывод в консоль, чтобы
                 * приложение кушало меньше памяти, иначе мой ноутбук взлетит на воздух
                 */
//                System.out.println("You can't transfer money to this account");
            } else {
                if (!fromAccount.isLocked() && !toAccount.isLocked()) {

                    if (fromAccount.canDebit(amount)) {
                        fromAccount.debit(amount);
                        toAccount.deposit(amount);

                        if (amount > 50_000) {
                            try {
                                if (isFraud(fromAccountNum, toAccountNum, amount)) {
                                    fromAccount.lock();
                                    toAccount.lock();
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
//                        System.out.println("You can't debit this sum");
                    }
                } else {
//                    System.out.println("Wrong operation. One of (or all) accounts was blocked");
                }
            }
        } else {
//            System.out.println("Wrong operation. Check account numbers and try again");
        }
    }

    /**
     * TODO: реализовать метод. Возвращает остаток на счёте.
     */
    public long getBalance(String accountNum) {
        if (accounts.keySet().contains(accountNum)) {
            return accounts.get(accountNum).checkBalance();
        } else {
//            System.out.println("Can't find account with number " + accountNum);
            return 0;
        }
    }

    private int getLockedAccounts() {
        int lockedAccounts = 0;
        for (Account account : accounts.values()) {
            if (account.isLocked()) lockedAccounts++;
        }
        return lockedAccounts;
    }

    protected void addAccount(Account acc) {
        accounts.put(acc.getAccNumber(), acc);
    }

    private long calculateBankBalance() {
        long bankBalance = accounts.values().stream().mapToLong(Account::checkBalance).sum();
        return bankBalance;
    }

    public ConcurrentHashMap<String, Account> getAccounts() {
        return accounts;
    }

    public long getBankBalance() { //Общий баланс банка (баланс всех счетов)
        return calculateBankBalance();
    }

    @Override
    public void run() {
        for (int i = 0; i < 1_000_000; i++) { //Сделаем 1_000_000 транзакций на поток
            //Получаем случайный номер аккаутна
            String randomAccNumber = String.valueOf(0 + (int) (Math.random() * accounts.values().size()));
            //Пробежимся по всем аккаунтам
            accounts.values().forEach(fromAccount -> {
                long amount = 100 + (long) (Math.random() * 80_000);
                transfer(fromAccount.getAccNumber(), randomAccNumber, amount);
            });
            if (i % 10_000 == 0) { //Каждые 10 тысяч транзакций будем выводить логи в файл
                log2file.info("Sum of all balances is " + calculateBankBalance());
                log2file.info("Locked accounts: " + getLockedAccounts());
            }
        }
    }
}
