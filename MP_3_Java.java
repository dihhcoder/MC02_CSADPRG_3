/********************
Last names: ALCANTARA, ANGELES, CONSTANTINO, MATIBAG
Language: Java
Paradigm(s): Procedural, Object-Oriented
********************/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

//In order to run the program, you need to have Java installed on your computer. 
//You can download it from the official website: https://www.java.com/en/download/
//Once you have Java installed, you can compile and run the program using the command line or terminal.
//To compile the program, navigate to the directory where the MP_3_Java.java file is located and run the following command:
//javac MP_3_Java.java
//This will create a MP_3_Java.class file in the same directory.
//To run the program, use the following command:
//java MP_3_Java
//Enjoy using the program! If you encounter any issues, please refer to the creators for assistance.

public class MP_3_Java {
    //separator constant string
    public static final String SEPARATOR = "==============================================";
    //constant string for messages
    public static final String MSG00 = "GOODBYE AND THANK YOU FOR USING OUR SYSTEM";
    public static final String MSG01 = "\tWELCOME TO THE BANKING SYSTEM";
    public static final String MSG02 = "Operation Cancelled.";
    public static final String MSG03 = "Type \"Cancel\" to cancel transaction.";
    public static final String MSG04 = "Register Account Name:";
    public static final String MSG05 = "Account for %s Registered Successfully!%n";
    public static final String MSG06 = "Foreign Currency Exchange:";
    public static final String MSG07 = "Source Currency Options:";
    public static final String MSG08 = "Exchanged Currency Options:";
    public static final String MSG09 = "Exchanged Amount: %.2f %s%n";
    public static final String MSG10 = "Record Exchange Rate:";
    public static final String MSG11 = "Exchange Rate for %s Recorded Successfully!%n";
    public static final String MSG12 = "Withdraw Balance:";
    public static final String MSG13 = "Choose Currency For Transaction:";
    public static final String MSG14 = "Current %s Balance: %.2f %n";
    public static final String MSG15 = "Successfully Withdrawn %s %.2f From Your Account!%n";
    public static final String MSG16 = "Updated Account Balance: %s %.2f %n";
    public static final String MSG17 = "Deposit Money To Account:";
    public static final String MSG18 = "Successfully Deposited %s %.2f To Your Account!%n";
    public static final String MSG19 = "Simulate Account Interest Amount:";
    public static final String MSG20 = "Interest Rate: %.2f%% %n";
    public static final String MSG21 = "  Day |  Interest Amount   |  Balance (%s)%n";
    public static final String MSG22 = "%-5d |  %-17.2f |  %.2f %n";
    public static final String MSG23 = "Current Exchange Rate: %.2f %n";
    public static final String MSG24 = "Processing dataset... %n%,d row/s loaded %n%,d filtered rows for PHP currency %n%,d filtered rows with errors %n";
    public static final String MSG25 = "Account registered successfully with the following details:";
    public static final String MSG26 = "Account Name: %s%nAccount Number: %d%nCurrent Balance: %.2f%n";
    public static final String MSG27 = "CSV file not loaded yet. Loading file first...";
    public static final String MSG28 = "Generating reports...\nOutputs saved to individual files...";
    public static final String MSG29 = "\n\nReport 1: Banking Privilege Tiers";
    public static final String MSG30 = "\n\nReport 2: Interest Expense Projection";
    public static final String MSG31 = "\n\nReport 3: Suspicious Client Behavior";
    public static final String MSG32 = "\n\nReport 4: Account Activity";
    public static final String MSG33 = "Full table exported to: %s";
    public static final String MSG34 = "\n\nSummary State (summary.json):";
    public static final String MSG35 = "{total_accounts: %s, total_deposits: %s, total_withdrawals: %s, total_balance: %s, total_interest_expense: %s}%n";
    public static final String MSG36 = "Minimum initial deposit should be at least PHP 5,000.00";
    

    //constant string for menus
    public static final String MNU00 = """
                                    Select your transaction:
                                    [1] Register Account
                                    [2] Deposit Amount
                                    [3] Withdraw Amount
                                    [4] Currency Exchange
                                    [5] Record Exchange Rates
                                    [6] Show Interest Amount
                                    [7] Generate Report
                                    """;
    public static final String MNU01 = """
                                    [1] Philippine Peso (PHP)
                                    [2] United States Dollar (USD)
                                    [3] Japanese Yen (JPY)
                                    [4] British Pound Sterling (GBP)
                                    [5] Euro (EUR)
                                    [6] Chinese Yuan Renminni (CNY)
                                    """;
    public static final String MNU02 = """
                                    [1] United States Dollar (USD)
                                    [2] Japanese Yen (JPY)
                                    [3] British Pound Sterling (GBP)
                                    [4] Euro (EUR)
                                    [5] Chinese Yuan Renminni (CNY)
                                    """;
    public static final String MNU03 = """
                                    Generate Reports:
                                    [1] Load the file
                                    [2] Generate Reports
                                    """;
    
    //constant string for prompt messages
    public static final String PRM00 = "Select a transaction: ";
    public static final String PRM01 = "Would you like to make another transaction? (Y/N): ";
    public static final String PRM02 = "Input Account Name: ";
    public static final String PRM03 = "Choose Source Currency: ";
    public static final String PRM04 = "Input Source Amount: ";
    public static final String PRM05 = "Choose Exchanged Currency: ";
    public static final String PRM06 = "Choose Foreign Currency: ";
    public static final String PRM07 = "Exchange Rate: ";
    public static final String PRM08 = "Select Currency: ";
    public static final String PRM09 = "Input Desired Withdraw Amount: ";
    public static final String PRM10 = "Input Desired Deposit Amount: ";
    public static final String PRM11 = "Input Desired Number of Days: ";
    public static final String PRM12 = "Initial Deposit: ";
    public static final String PRM13 = "Enter your choice: ";

    //constant string for error messages
    public static final String ERR00 = "ERROR: Input cannot be left blank!";
    public static final String ERR01 = "ERROR: Input is not valid!";
    public static final String ERR02 = "ERROR: Input should be between %d and %d!%n";
    public static final String ERR03 = "ERROR: Input should be between %.2f and %.2f!%n";
    public static final String ERR04 = "ERROR: Input should be at least %.2f!%n";
    public static final String ERR05 = "ERROR: Input should be 'Y' or 'N'!";
    public static final String ERR06 = "ERROR: Account already exists!";
    public static final String ERR07 = "ERROR: Currency currently not available!";
    public static final String ERR08 = "ERROR: Account doesn't exist!";
    public static final String ERR09 = "ERROR: Insufficient Balance!";
    public static final String ERR10 = "ERROR: Updating Transaction failed due to file write error!";
    public static final String ERR11 = "ERROR: Cannot read from file! Please ensure the file exists and is accessible.";
    public static final String ERR12 = "ERROR: Account name must contain at least a first and last name, and cannot contain commas!";
    public static final String ERR13 = "ERROR: Cannot make %s%n%n";
    public static final String ERR14 = "ERROR: Insufficient data for record %d. Record %d will not be created.%n%n";

    //constant values
    public static final double INTEREST_RATE = 0.05; //Interest Rate
    public static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("d/M/yyyy"); //date format
    public static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("H:m:ss"); //time format
    public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("d/M/yyyy H:mm:ss"); //date and time format
    public static final String DEF_CURRENCY = "PHP"; //default currency
    public static final String CSV_FILE = "bank_transactions.csv"; //file name for storing transactions
    public static final String UPDATE_FORMAT = "%s,%d,%s,%s,%s,%.2f,%s,%.2f"; //format for updating csv file
    public static final String DEPOSIT = "deposit"; //transaction type for deposit
    public static final String WITHDRAWAL = "withdrawal"; //transaction type for withdrawal
    public static final String REPORT1_FILE = "report1_banking_privilege_tiers.csv"; //file name for report 1
    public static final String REPORT2_FILE = "report2_interest_expense_projection.csv"; //file name for report 2
    public static final String REPORT3_FILE = "report3_suspicious_client_behavior.csv"; //file name for report 3
    public static final String REPORT4_FILE = "report4_account_activity.csv"; //file name for report 4
    public static final String SUMMARY_JSON_FILE = "summary.json"; //file name for summary json
    public static final String MONEY_FORMAT = "%,.2f"; //format for money values
    public static final String NAME_HEADER = "AccountName"; //header for account name in reports
    public static final String AGE_HEADER = "AccountAge"; //header for account age in reports
    public static final String AVG_HEADER = "Average Daily Balance"; //header for average daily balance in reports
    public static final String BALANCE_HEADER = "CurrentBalance"; //header for current balance in reports
    public static final String INTEREST_HEADER = "ExpectedInterest"; //header for expected interest in reports
    public static final String DATE_HEADER = "Date"; //header for date in reports
    public static final String WITHDRAWAL_HEADER = "TotalWithdrawalAmounts"; // header for total withdrawal in reports
    public static final String NUM_DEPOSIT_HEADER = "NumberOfDeposits"; //header for number of deposits in reports
    public static final String DEPOSIT_HEADER = "TotalDepositAmounts"; //header for total deposit in reports
    public static final String VELOCITY_HEADER = "TurnoverVelocity (%)"; //header for turnover velocity in reports
    public static final String RISK_HEADER = "RiskFlagLevel"; //header for risk in reports
    public static final String TIER_HEADER = "PrivilegeTier"; //header for tier in reports
    public static final String DAYS_HEADER = "DaysInactive"; //header for days inactive in reports
    public static final String LAST_HEADER = "LastTransaction"; //header for last transaction date in reports
    public static final String STATUS_HEADER = "Status"; //header for status in reports
    public static final String RECORD_1_FORMAT = "\"%s\",%s,\"%s\",\"%s\",%s%n"; //format for record 1 update
    public static final String TABLE_1_FORMAT_HEADER = "%-20s | %-10s | %-16s | %-21s | %-13s%n"; //format for record 1 header
    public static final String TABLE_1_FORMAT = "%-20s | %-10s | %-16s | %-21s | %-13s%n"; //format for record 1 table
    public static final String RECORD_2_FORMAT = "\"%s\",\"%s\",\"%s\"%n"; //format for record 2 update
    public static final String TABLE_2_FORMAT_HEADER = "%-20s | %-16s | %-16s%n"; //format for record 2 table header
    public static final String TABLE_2_FORMAT = "%-20s | %-16s | %-16s%n"; //format for record 2 table
    public static final String RECORD_3_FORMAT = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n"; //format for record 3 update
    public static final String TABLE_3_FORMAT_HEADER = "%-20s | %-10s | %-22s | %-16s | %-19s | %-20s | %-13s%n";
    public static final String TABLE_3_FORMAT        = "%-20s | %-10s | %-22s | %-16s | %-19s | %-20s | %-13s%n";
    public static final String RECORD_4_FORMAT = "\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n"; //format for record 4 update
    public static final String TABLE_4_FORMAT_HEADER = "%-20s | %-16s | %-12s | %-15s | %-10s%n"; //format for record 4 table header
    public static final String TABLE_4_FORMAT = "%-20s | %-16s | %-12s | %-15s | %-10s%n"; //format for record 3 table

    public static void main(String[] args){
        System.out.println(SEPARATOR);
        System.out.println(MSG01);
        System.out.println(SEPARATOR);
        try(Scanner keyboard = new Scanner(System.in)){
            ArrayList<Account> accs = new ArrayList<Account>();
            ArrayList<Transaction> transactions = new ArrayList<Transaction>();
            int choice = -1;
            boolean backToMenu;
            Map<String, Currency> exchangeRates = new LinkedHashMap<String, Currency>();    
            setUp(exchangeRates);
            while(choice == -1){
                readCSV(true, accs, transactions);
                backToMenu = false;
                while(!backToMenu){
                    try{
                        if(choice == -1){
                            choice = printMenu(keyboard);
                        }
                        caller(choice, accs, keyboard, exchangeRates, transactions);
                        if(choice != 0){
                            backToMenu = promptBack(keyboard, false);
                            if(backToMenu){
                                choice = -1;
                            } else {
                                exit();
                            }
                        } 
                    } catch(CancelledException _){
                        System.out.println(MSG02);
                        backToMenu = promptBack(keyboard, false);
                        if(!backToMenu){
                            exit();
                        } else {
                            choice = -1;
                        }
                    }
                }
            }
        } 
    }

    //function definition for updating csv file with new transaction
    public static void updateList(Account acc, String transactionType, double amount){
        String fileName = CSV_FILE;
        LocalDateTime now = LocalDateTime.now();
        String newLine = String.format(UPDATE_FORMAT, acc.getName(), acc.getNumber(), now.format(DF), 
                                       now.format(TF), DEF_CURRENCY, amount, transactionType, acc.getBalance());
        try(FileWriter fw = new FileWriter(fileName, true); 
            PrintWriter pw = new PrintWriter(fw)){
            pw.println(newLine);
        } catch (IOException e) {
            System.out.println(ERR10);
        }
    }

    //function definition for reading csv file and storing data into arraylists
    public static void readCSV(boolean startUp, ArrayList<Account> accs, ArrayList<Transaction> transactions){
        int recordCount = 0;
        int errorCount = 0;
        int phpCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE))) {
            accs.clear();
            transactions.clear();
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                recordCount++;
                String[] values = line.split(",");
                if (values.length != 8 || !values[0].trim().contains(" ") || !values[0].trim().matches("[a-zA-Z.\\-'\\s]+")) {
                    errorCount++;
                    continue;
                }
                try {
                    if (!values[4].trim().equalsIgnoreCase("PHP")) {
                        continue;
                    }
                    phpCount++;
                    String accName = values[0].trim();
                    long accNumber = Long.parseLong(values[1].trim());
                    LocalDateTime readDateTime = LocalDateTime.parse(values[2].trim() + " " + values[3].trim(), DTF);
                    double balance = Double.parseDouble(values[7].trim());
                    double amount = Double.parseDouble(values[5].trim());
                    String type = values[6].trim();
                    boolean onList = false;
                    transactions.add(new Transaction(accName, accNumber, readDateTime, amount, type, balance));
                    for (Account acc : accs) {
                        if (acc.getNumber() == accNumber) {
                            if (readDateTime.isBefore(acc.getOpenDateTime())) {
                                acc.setOpenDateTime(readDateTime);
                            }
                            if (!readDateTime.isBefore(acc.getLastTransactionDateTime())) {
                                acc.setBalance(balance);
                                acc.setLastTransactionDateTime(readDateTime);
                            }
                            onList = true;
                            break;
                        }
                    }
                    if (!onList) {
                        accs.add(new Account(accName, accNumber, balance, INTEREST_RATE, readDateTime, readDateTime));
                    }
                } catch (Exception _) {
                    errorCount++;
                }
            }
            if(!startUp){
                System.out.println(SEPARATOR);
                System.out.printf(MSG24, recordCount, phpCount, errorCount);
                System.out.println(SEPARATOR);
            }
        } catch (IOException _) {
            System.out.println(ERR11);
        }
    }

    //sets up default exchange rates
    public static void setUp(Map<String, Currency> exchangeRates){
        exchangeRates.put("PHP", new Currency("Philippine Peso", 1.0));
        exchangeRates.put("USD", new Currency("United States Dollar", 52.00));
        exchangeRates.put("JPY", new Currency("Japanese Yen", 0.40));
        exchangeRates.put("GBP", new Currency("British Pound", 84.00));
        exchangeRates.put("EUR", new Currency("Euro", 72.00));
        exchangeRates.put("CNY", new Currency("Chinese Yuan Reminni", 9.00));
    }

    //calls functions based on menu input
    public static void caller(int choice, ArrayList<Account> accs, Scanner keyboard, Map<String, Currency> exchangeRates, ArrayList<Transaction> transactions){
        switch(choice){
            case 1:
                regAcc(accs, keyboard, exchangeRates);
                break;
            case 2:
                depositAmount(accs, keyboard, exchangeRates);
                break;
            case 3:
                withdrawAmount(accs, keyboard, exchangeRates);
                break;
            case 4:
                currEx(keyboard, exchangeRates, accs);
                break;
            case 5:
                recordExRate(keyboard, exchangeRates);
                break;
            case 6:
                showInterestAmount(accs, keyboard, exchangeRates);
                break;
            case 7:
                generateReports(accs, transactions, keyboard);
                break;
            default:
                System.out.println(ERR01);
        }
    }

    //prints menu and takes transaction choice
    public static int printMenu(Scanner keyboard){
        int choice = -1;
        while(choice == -1){
            System.out.println(MNU00);
            choice = getInt(PRM00, keyboard, 1, 7, true, true);
            System.out.println(SEPARATOR);
        }
        return choice;
    }

    //prints prompt to go back to menu and takes boolean input
    public static boolean promptBack(Scanner keyboard, boolean isCancelAllowed){
        System.out.println(SEPARATOR);
        String input = getString(PRM01, keyboard, true, isCancelAllowed);
        if(input.equalsIgnoreCase("Y")){
            System.out.println(SEPARATOR);
            return true;
        } else if (input.equalsIgnoreCase("N")){
            return false;
        } else {
            System.out.println(ERR05);
            return promptBack(keyboard, isCancelAllowed);
        }
    }

    //function definition for registering account name
    public static void regAcc(ArrayList<Account> list, Scanner keyboard, Map<String, Currency> exrate){
        System.out.println(MSG04);
        String name = getString(PRM02, keyboard, false, true);
        name = name.trim();
        if(!name.contains(" ") || !name.matches("[a-zA-Z\\s]+")){
            System.out.println(ERR12);
            return;
        }
        boolean exists = false;
        for(Account acc: list){
            if(name.equalsIgnoreCase(acc.getName())){
                exists = true;
                break;
            }
        }
        if(exists){
            System.out.println(ERR06);
        } else {
            long max = 999999999L;
            for(int i = 0; i < list.size(); i++){
                if(list.get(i).getNumber() > max){
                    max = list.get(i).getNumber();
                }
            }
            long newAccountNumber = max + 1;
            System.out.println(MNU01);
            int currency = getInt(PRM08, keyboard, 1, 6, false, true);
            System.out.println(MSG36);
            double minimum = convertCurrency(5000.0, currency, 1.0);
            double initialDeposit = getDouble(PRM12, keyboard, minimum, Double.POSITIVE_INFINITY, false, true);
            double toAdd = convertCurrency(initialDeposit, exrate.get(getCurrencyCode(currency)).getValue(), 1.0);
            LocalDateTime now = LocalDateTime.now();
            Account newAccount = new Account(name, newAccountNumber, toAdd, INTEREST_RATE, now, now);
            System.out.println(MSG25);
            System.out.printf(MSG26, name, newAccountNumber, toAdd);
            updateList(newAccount, DEPOSIT, toAdd);
        }
    }

    //function definition for currency exchange feature
    public static void currEx(Scanner keyboard, Map<String, Currency> exRate, ArrayList<Account> list){
        System.out.println(MSG06);
        System.out.println(MSG07);
        System.out.println(MNU01);
        int sourceCurrency = getInt(PRM03, keyboard, 1, 6, false, true);
        System.out.println(SEPARATOR);
        double sourceAmount = getDouble(PRM04, keyboard, 0.0, Double.POSITIVE_INFINITY, false, true);
        System.out.println(SEPARATOR);
        System.out.println(MSG08);
        System.out.println(MNU01);
        int exchangedCurrency = getInt(PRM05, keyboard, 1, 6, false, true);
        System.out.println(SEPARATOR);
        double exchangedAmount = convertCurrency(sourceAmount, exRate.get(getCurrencyCode(sourceCurrency)).getValue(), exRate.get(getCurrencyCode(exchangedCurrency)).getValue());
        System.out.printf(MSG09, exchangedAmount, getCurrencyCode(exchangedCurrency));
    }

    //function definition for recording/modifying exchange rates
    public static void recordExRate(Scanner keyboard, Map<String, Currency> exRate){
        System.out.println(MSG10);
        System.out.println(MNU02);
        int foreignCurrency = getInt(PRM06, keyboard, 0, 5, false, true);
        foreignCurrency++;
        System.out.println(SEPARATOR);
        System.out.printf(MSG23, exRate.get(getCurrencyCode(foreignCurrency)).getValue());
        double exchangeRate = getDouble(PRM07, keyboard, 0, Double.POSITIVE_INFINITY, false, true);
        System.out.println(SEPARATOR);
        if(exRate.containsKey(getCurrencyCode(foreignCurrency))){
            exRate.get(getCurrencyCode(foreignCurrency)).setValue(exchangeRate);
            System.out.printf(MSG11, getCurrencyCode(foreignCurrency));
            displayExRates(exRate);
        } else {
            System.out.println(ERR07);
        }
    }

    //helper function for getting currency code(e.g. "PHP", "USD", "JPY")
    public static String getCurrencyCode(int choice){
        switch(choice){
            case 1:
                return "PHP";
            case 2:
                return "USD";
            case 3:
                return "JPY";
            case 4:
                return "GBP";
            case 5:
                return "EUR";
            case 6:
                return "CNY";
            default:
                return "";
        }
    }

    //function definition for displaying current exchange rates set
    public static void displayExRates(Map<String, Currency> exchangeRates){
        System.out.println("Current Exchange Rates:");
        for(Map.Entry<String, Currency> entry : exchangeRates.entrySet()){
           System.out.printf("%-30s: %.2f\n", entry.getValue().getName() + " (" + entry.getKey() + ")", entry.getValue().getValue());
        }
    }

    //function definition for simulating interest amount and total for given day value
    public static void showInterestAmount(ArrayList<Account> list, Scanner keyboard, Map<String, Currency> exchangeRates){
        int accKey = -1;
        System.out.println(MSG19);
        String nameKey = getString(PRM02, keyboard, false, true);
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).getName().equalsIgnoreCase(nameKey)){
                accKey = i;
                break;
            }
        }
        if(accKey != -1){
            System.out.println(MSG13);
            System.out.println(MNU01);
            int currency = getInt(PRM08, keyboard, 1, 6, false, true);
            System.out.printf(MSG14, getCurrencyCode(currency), list.get(accKey).getBalance());
            System.out.printf(MSG20, list.get(accKey).getInterestRate() * 100);
            System.out.println(SEPARATOR);
            int days = getInt(PRM11, keyboard, 1, Integer.MAX_VALUE, false, true);
            System.out.println(SEPARATOR);
            System.out.printf(MSG21, getCurrencyCode(currency));
            System.out.println(SEPARATOR);
            double displayBalance = list.get(accKey).getBalance() * exchangeRates.get(getCurrencyCode(currency)).getValue();
            for(int i = 0; i < days; i++){
                //Daily Interest = (End-of-Day Balance) x (Annual Interest Rate / 365)
                double dailyInterest = displayBalance * (list.get(accKey).getInterestRate() / 365);
                dailyInterest = Math.round(dailyInterest * 100.0) / 100.0;
                displayBalance += dailyInterest;
                System.out.printf(MSG22, i + 1, dailyInterest, displayBalance);
            }
        } else {
            System.out.println(ERR08);
        }

    }

    //function definition for depositing php amounts through various currencies
    public static void depositAmount(ArrayList<Account> list, Scanner keyboard, Map<String, Currency> exRate){
        int accKey = -1;
        System.out.println(MSG17);
        String nameKey = getString(PRM02, keyboard, false, true);
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).getName().equalsIgnoreCase(nameKey)){
                accKey = i;
                break;
            }
        }
        if(accKey != -1){
            System.out.println(MSG13);
            System.out.println(MNU01);
            int currency = getInt(PRM08, keyboard, 1, 6, false, true);
            double depositAmount = getDouble(PRM10, keyboard, 0.01, Double.POSITIVE_INFINITY, false, true);
            double toAdd = convertCurrency(depositAmount, exRate.get(getCurrencyCode(currency)).getValue(), 1.0);
            double newBalance = list.get(accKey).getBalance() + toAdd;
            list.get(accKey).setBalance(newBalance);
            updateList(list.get(accKey), DEPOSIT, toAdd);
            System.out.println(SEPARATOR);
            System.out.printf(MSG18, getCurrencyCode(currency), depositAmount);
            System.out.printf(MSG16, getCurrencyCode(1), newBalance);
        } else {
            System.out.println(ERR08);
        }
    }

    //function definition for withdrawing php amounts through various currencies
    public static void withdrawAmount(ArrayList<Account> list, Scanner keyboard, Map<String, Currency> exRate){
        int accKey = -1;
        System.out.println(MSG12);
        String nameKey = getString(PRM02, keyboard, false, true);
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).getName().equalsIgnoreCase(nameKey)){
                accKey = i;
                break;
            }
        }
        if(accKey != -1){
            System.out.println(MSG13);
            System.out.println(MNU01);
            int currency = getInt(PRM08, keyboard, 1, 6, false, true);
            double currBal = convertCurrency(list.get(accKey).getBalance(), 1.0, exRate.get(getCurrencyCode(currency)).getValue());
            System.out.printf(MSG14, getCurrencyCode(currency), currBal);
            if(currBal <= 0){
                System.out.println(ERR09);
                return;
            }
            double withdrawAmount = getDouble(PRM09, keyboard, 0.01, currBal, false, true);
            double withdrawAmountInPHP = convertCurrency(withdrawAmount, exRate.get(getCurrencyCode(currency)).getValue(), 1.0);
            double newBalance = list.get(accKey).getBalance() - withdrawAmountInPHP;
            if(newBalance < 0){
                newBalance = 0;
            }
            list.get(accKey).setBalance(newBalance);
            updateList(list.get(accKey), WITHDRAWAL, withdrawAmountInPHP);
            System.out.println(SEPARATOR);
            System.out.printf(MSG15, getCurrencyCode(currency), withdrawAmount);
            System.out.printf(MSG16, getCurrencyCode(1), newBalance);
    } else {
            System.out.println(ERR08);
        }
    }
    
    //helper function for converting amount values given source and target currency rates
    public static double convertCurrency(double amount, double sourceRate, double targetRate){
        return (amount * sourceRate) / targetRate;
    }

    //helper function handling string input capture
    public static String getString(String prompt, Scanner keyboard, boolean isMenu, boolean isCancelAllowed){
        String input = "";
        boolean isValid = false;
        while(!isValid){
            if(isCancelAllowed){
                System.out.println(MSG03);
            }
            System.out.print(prompt);
            input = keyboard.nextLine();
            if(input.trim().isEmpty()){
                System.out.println(ERR00);
                continue;
            }
            if(isCancelAllowed && input.trim().equalsIgnoreCase("cancel")){
                if(!isMenu){
                    throw new CancelledException();
                } else {
                    exit();
                }
                
            } 
            isValid = true;
        }
        return input.trim();
    }

    //helper function handling integer input capture
    public static int getInt(String prompt, Scanner keyboard, int min, int max, boolean isMenu, boolean isCancelAllowed){
        String input = "";
        int choice = -1;
        boolean isValid = false;
        while(!isValid){
            try{
                input = getString(prompt, keyboard, isMenu, isCancelAllowed);
                choice = Integer.parseInt(input);
                if(choice >= min && choice <= max){
                    isValid = true;
                } else {
                    System.out.printf(ERR02, min, max);
                }
            } catch(NumberFormatException _){
                System.out.println(ERR01);
            }   
        }
        return choice;
    }

    //helper function handling double input capture
    public static double getDouble(String prompt, Scanner keyboard, double min, double max, boolean isMenu, boolean isCancelAllowed){
        double input = -1.0;
        String scanned = "";
        boolean isValid = false;
        while(!isValid){    
            try{
                scanned = getString(prompt, keyboard, isMenu, isCancelAllowed);
                input = Double.parseDouble(scanned);
                if(input >= min && input <= max){
                    isValid = true;
                } else {
                    if(max == Double.POSITIVE_INFINITY){
                        System.out.printf(ERR04, min);
                    } else {
                        System.out.printf(ERR03, min, max);
                    }   
                }
            } catch(NumberFormatException _){
                System.out.println(ERR01);
            }  
        }
        return input;
    }

    //function definition for generating reports based on the loaded CSV file
    public static void generateReports(ArrayList<Account> accs, ArrayList<Transaction> transactions, Scanner keyboard){
        boolean inReportMenu = true;
        while (inReportMenu) {
            System.out.println(MNU03);
            int choice = getInt(PRM13, keyboard, 1, 2, false, true);
            if (choice == 1) {
                System.out.println(SEPARATOR);
                readCSV(false, accs, transactions);
            } else if (choice == 2) {
                if (transactions.isEmpty()) {
                    System.out.println(MSG27);
                    readCSV(false, accs, transactions);
                }
                System.out.println(SEPARATOR);
                System.out.println(MSG28);
                generateFirstReport(accs, transactions);
                generateSecondReport(accs);
                generateThirdReport(accs, transactions);
                generateFourthReport(accs, transactions);
                generateSummaryJson(accs, transactions);
                inReportMenu = false;
            }
        }
    }

    //function definition for generating the first report (Banking Privilege Tiers)
    public static void generateFirstReport(ArrayList<Account> accs, ArrayList<Transaction> transactions){
        if(accs.isEmpty() || transactions.isEmpty()){
            System.out.printf(ERR14, 1, 1);
            return;
        }
        Map<Long, List<Transaction>> accTrans = new HashMap<>();
        for (Transaction t : transactions) {
            accTrans.computeIfAbsent(t.getAccountNumber(), _ -> new ArrayList<>()).add(t);
        }
        for (List<Transaction> list : accTrans.values()) {
            list.sort(Comparator.comparing(Transaction::getDateTime));
        }
        LocalDate latest = transactions.get(0).getDate();
        LocalDate earliest = latest;
        for (Transaction t : transactions) {
            if (t.getDate().isAfter(latest)) {
                latest = t.getDate();
            } 
            if (t.getDate().isBefore(earliest)) {
                earliest = t.getDate();
            } 
        }
        YearMonth targetMonth = YearMonth.from(latest).minusMonths(1);
        if (targetMonth.isBefore(YearMonth.from(earliest))) {
            targetMonth = YearMonth.from(latest);
        }
        LocalDate monthStart = targetMonth.atDay(1);
        int daysInMonth = targetMonth.lengthOfMonth();
        ArrayList<Privilege> privileges = new ArrayList<>();
        for (Account acc : accs) {
            List<Transaction> transact = accTrans.get(acc.getNumber());
            double endBalance = 0.0;
            Map<LocalDate, Double> monthBal = new HashMap<>();
            for (Transaction t : transact) {
                LocalDate d = t.getDate();
                if (d.isBefore(monthStart)) {
                    endBalance = t.getBalanceAfter();
                } else if (YearMonth.from(d).equals(targetMonth)) {
                    monthBal.put(d, t.getBalanceAfter());
                }
            }
            double sumBal = 0.0;
            for (int i = 1; i <= daysInMonth; i++) {
                LocalDate currDay = targetMonth.atDay(i);
                if (monthBal.containsKey(currDay)) {
                    endBalance = monthBal.get(currDay);
                }
                sumBal += endBalance;
            }
            double avg = sumBal / daysInMonth;
            String tier = null;
            if (avg >= 100000.00) {
                if (avg < 500000.00) {
                    tier = "Silver";
                } else if (avg < 1000000.00) {
                    tier = "Gold";
                } else {
                    tier = "Platinum";
                }
                long accountAge = ChronoUnit.DAYS.between(acc.getOpenDateTime().toLocalDate(), latest);
                if (accountAge < 0) {
                    accountAge = 0;
                }
                privileges.add(new Privilege(acc.getName(), accountAge, acc.getBalance(), avg, tier));
            }
        }
        privileges.sort(Comparator.comparingDouble(Privilege::getAvg).reversed());
        System.out.println(MSG29);
        System.out.printf(TABLE_1_FORMAT_HEADER, NAME_HEADER, AGE_HEADER, BALANCE_HEADER, AVG_HEADER, TIER_HEADER);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(REPORT1_FILE)))) {
            pw.println(NAME_HEADER + "," + AGE_HEADER + "," + BALANCE_HEADER + "," + AVG_HEADER + "," + TIER_HEADER);
            for (int i = 0; i < privileges.size(); i++) {
                Privilege r = privileges.get(i);
                String name = r.getName();
                String age = String.format(Locale.US, "%,d", r.getAge());
                String bal = String.format(Locale.US, MONEY_FORMAT, r.getBal());
                String ave = String.format(Locale.US, MONEY_FORMAT, r.getAvg());
                String prvtr = r.getTier();
                pw.printf(RECORD_1_FORMAT, name, age, bal, ave, prvtr);
                if (i < 2) {
                    System.out.printf(TABLE_1_FORMAT, name, age, bal, ave, prvtr);
                }
            }
            System.out.printf(MSG33, REPORT1_FILE);
        } catch (IOException e) {
            System.out.printf(ERR13, REPORT1_FILE);
        }
    }

    //function definition for generating the second report (Interest Expense Projection)
    public static void generateSecondReport(ArrayList<Account> accs){
        List<Account> eligible = new ArrayList<>();
        for (Account acc : accs) {
            if (acc.getBalance() >= 10000.00) {
                eligible.add(acc);
            }
        }
        if(eligible.isEmpty()){
            System.out.printf(ERR14, 2, 2);
            return;
        }
        eligible.sort(Comparator.comparingDouble(Account::getBalance).reversed());
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(REPORT2_FILE)))) {
            pw.println(NAME_HEADER + "," + BALANCE_HEADER + "," + INTEREST_HEADER);
            System.out.println(MSG30);
            System.out.printf(TABLE_2_FORMAT_HEADER, NAME_HEADER, BALANCE_HEADER, INTEREST_HEADER);
            for (int i = 0; i < eligible.size(); i++) {
                Account acc = eligible.get(i);
                String name = acc.getName();
                double balance = acc.getBalance();
                double expectedInterest = balance * (0.05 / 365.0) * 30.0;
                String formattedBalance = String.format(Locale.US, MONEY_FORMAT, balance);
                String formattedInterest = String.format(Locale.US, MONEY_FORMAT, expectedInterest);
                pw.printf(RECORD_2_FORMAT, name, formattedBalance, formattedInterest);
                if(i < 2){
                    System.out.printf(TABLE_2_FORMAT, name, formattedBalance, formattedInterest);
                }
            }
            System.out.printf(MSG33, REPORT2_FILE);
        } catch (IOException e) {
            System.out.printf(ERR13, REPORT2_FILE);
        }
    }

    //function definition for generating the third report (Suspicious Client Behavior)
    public static void generateThirdReport(ArrayList<Account> accs, ArrayList<Transaction> transactions){
        if(transactions.isEmpty()){
            System.out.printf(ERR14, 3, 3);
            return;
        }
        Map<String, DailyBehavior> dailyMap = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            String dailyKey = t.getAccountNumber() + "-" + t.getDate().toString();
            DailyBehavior dailyRecord = dailyMap.computeIfAbsent(dailyKey, _ -> new DailyBehavior(t.getAccountName(), t.getAccountNumber(), t.getDate()));
            String type = t.getType();
            if (type.equalsIgnoreCase(WITHDRAWAL)) {
                dailyRecord.addTotalW(t.getAmount());
            } else if (type.equalsIgnoreCase(DEPOSIT)) {
                dailyRecord.incNumDeposits();
                dailyRecord.addTotalD(t.getAmount());
            }
        }
        ArrayList<DailyBehavior> records = new ArrayList<>(dailyMap.values());
        records.sort(Comparator.comparing(DailyBehavior::getDate).thenComparingInt(record -> switch(record.getRiskFlagLevel()){
            case "Suspicious" -> 1;
            case "Normal" -> 2;
            default -> 3;
        }));
        System.out.println(MSG31);
        System.out.printf(TABLE_3_FORMAT_HEADER, NAME_HEADER, DATE_HEADER, WITHDRAWAL_HEADER, NUM_DEPOSIT_HEADER, DEPOSIT_HEADER, VELOCITY_HEADER, RISK_HEADER);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(REPORT3_FILE)))) {
            pw.println(NAME_HEADER + "," + DATE_HEADER + "," + WITHDRAWAL_HEADER + "," + NUM_DEPOSIT_HEADER + "," + DEPOSIT_HEADER + "," + VELOCITY_HEADER + "," + RISK_HEADER);
            for (int i = 0; i < records.size(); i++) {
                DailyBehavior r = records.get(i);
                String dateStr = r.date.format(DF);
                String risk = r.getRiskFlagLevel();
                String withdrawformatted = String.format(Locale.US, MONEY_FORMAT, r.totalWithdrawals);
                String depositformatted = String.format(Locale.US, MONEY_FORMAT, r.totalDeposits);
                String velocityformatted = r.getFormattedVelocity();
                pw.printf(RECORD_3_FORMAT, r.accName, dateStr, withdrawformatted, r.numberOfDeposits, depositformatted, velocityformatted, risk);
                if (i < 2) {
                    System.out.printf(TABLE_3_FORMAT, r.accName, dateStr, withdrawformatted, r.numberOfDeposits, depositformatted, velocityformatted, risk);
                }
            }
            System.out.printf(MSG33, REPORT3_FILE);
        } catch (IOException _) {
            System.out.printf(ERR13, REPORT3_FILE);
        }
    }

    //function definition for generating the fourth report (Account Activity)
    public static void generateFourthReport(ArrayList<Account> accs, ArrayList<Transaction> transactions) {
        if (accs.isEmpty() || transactions.isEmpty()) {
            System.out.printf(ERR14, 4, 4);
            return;
        }
        LocalDate reference = transactions.get(0).getDate();
        for (Transaction t : transactions) {
            if (t.getDate().isAfter(reference)) {
                reference = t.getDate();
            }
        }
        ArrayList<Activity> acts = new ArrayList<>();
        for (Account acc : accs) {
            LocalDate last = acc.getLastTransactionDateTime().toLocalDate();
            long daysInactive = ChronoUnit.DAYS.between(last, reference);
            if (daysInactive < 0) {
                daysInactive = 0;
            }
            Activity a = new Activity(acc.getName(), acc.getNumber(), acc.getBalance(), 0, null, null);
            a.setDays(daysInactive);
            if (daysInactive <= 30) {
                a.setStatus("Active");
            } else if (daysInactive <= 90) {
                a.setStatus("At-Risk");
            } else {
                a.setStatus("Dormant");
            }
            a.setLast(last);
            acts.add(a);
        }
        acts.sort(Comparator.comparingInt((Activity record) -> switch(record.getStatus()) {
            case "Dormant" -> 1;
            case "At-Risk" -> 2;
            case "Active" -> 3;
            default -> 4;
        }).thenComparing(Activity::getDays, Comparator.reverseOrder()));
        System.out.println(MSG32);
        System.out.printf(TABLE_4_FORMAT_HEADER, NAME_HEADER, BALANCE_HEADER, DAYS_HEADER, LAST_HEADER, STATUS_HEADER);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(REPORT4_FILE)))) {
            pw.println(NAME_HEADER + "," + BALANCE_HEADER + "," + DAYS_HEADER + "," + LAST_HEADER + "," + STATUS_HEADER);
            for (int i = 0; i < acts.size(); i++) {
                Activity a = acts.get(i);
                String name = a.getName();
                String bal = String.format(Locale.US, MONEY_FORMAT, a.getBalance());
                String day = String.format("%,d", a.getDays());
                String lastformatted = a.getLast().format(DF);
                String active = a.getStatus();
                pw.printf(RECORD_4_FORMAT, name, bal, day, lastformatted, active);
                if (i < 2) {
                    System.out.printf(TABLE_4_FORMAT,name, bal, day, lastformatted, active);
                }
            }
            System.out.printf(MSG33, REPORT4_FILE);
        } catch (IOException _) {
            System.out.printf(ERR13, REPORT4_FILE);
        }
    }

    //function definition for creation of json file
    public static void generateSummaryJson(ArrayList<Account> accs, ArrayList<Transaction> transactions) {
        int totalAccounts = accs.size();
        double totalDeposits = 0.0;
        double totalWithdrawals = 0.0;
        double totalBalance = 0.0;
        double totalInterestExpense = 0.0;
        for (Transaction t : transactions) {
            String type = t.getType();
            if (type.equalsIgnoreCase(DEPOSIT)) {
                totalDeposits += t.getAmount();
            } else if (type.equalsIgnoreCase(WITHDRAWAL)) {
                totalWithdrawals += t.getAmount();
            }
        }
        for (Account acc : accs) {
            totalBalance += acc.getBalance();
            if (acc.getBalance() >= 10000.00) {
                double interest = Math.round((acc.getBalance() * (0.05 / 365.0) * 30.0) * 100.0) / 100.0;
                totalInterestExpense += interest;
            }
        }
        String totalAccsFormatted = String.format(Locale.US, "%,d", totalAccounts);
        String totalDepositFormatted = String.format(Locale.US, MONEY_FORMAT, totalDeposits);
        String totalWithdrawFormatted = String.format(Locale.US, MONEY_FORMAT, totalWithdrawals);
        String totalBalanceFormatted = String.format(Locale.US, MONEY_FORMAT, totalBalance);
        String totalInterestFormatted = String.format(Locale.US, MONEY_FORMAT, totalInterestExpense);
        String jsonContent = """
                {
                    "total_accounts": "%s",
                    "total_deposits": "%s",
                    "total_withdrawals": "%s",
                    "total_balance": "%s",
                    "total_interest_expense": "%s"
                }
                """.formatted(totalAccsFormatted, totalDepositFormatted, totalWithdrawFormatted, totalBalanceFormatted, totalInterestFormatted);
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(SUMMARY_JSON_FILE)))) {
            pw.println(jsonContent);
            System.out.println(MSG34);
            System.out.printf(MSG35, totalAccsFormatted, totalDepositFormatted, totalWithdrawFormatted, totalBalanceFormatted, totalInterestFormatted);
        } catch (IOException _) {
            System.out.printf(ERR13, SUMMARY_JSON_FILE);
        } 
    }

    //helper function for exiting the program
    public static void exit(){
        System.out.println(SEPARATOR);
        System.out.println(MSG00);
        System.out.println(SEPARATOR);
        System.exit(0);
    }

    //nested class for account object
    public static class Account {
        private String name;
        private long number;
        private double balance;
        private double interestRate;
        private LocalDateTime openDateTime;
        private LocalDateTime lastTransactionDateTime;

        public Account(String name, long number, double balance, double interestRate, LocalDateTime openDateTime, LocalDateTime lastTransactionDateTime) {
            this.name = name;
            this.number = number;
            this.balance = balance;
            this.interestRate = interestRate;
            this.openDateTime = openDateTime;
            this.lastTransactionDateTime = lastTransactionDateTime;
        }

        public String getName() {
            return this.name;
        }

        public long getNumber() {
            return this.number;
        }

        public double getBalance() {
            return this.balance;
        }

        public double getInterestRate() {
            return this.interestRate;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }

        public LocalDateTime getOpenDateTime() {
            return this.openDateTime;
        }

        public LocalDateTime getLastTransactionDateTime() {
            return this.lastTransactionDateTime;
        }

        public void setLastTransactionDateTime(LocalDateTime lastTransactionDateTime) {
            this.lastTransactionDateTime = lastTransactionDateTime;
        }

        public void setOpenDateTime(LocalDateTime openDateTime) {
            this.openDateTime = openDateTime;
        }

        public void recordTransaction(double newBalance, LocalDateTime readDateTime) {
            if (readDateTime.isBefore(this.openDateTime)) {
                this.openDateTime = readDateTime;
            }
            if (!readDateTime.isBefore(this.lastTransactionDateTime)) {
                this.balance = newBalance;
                this.lastTransactionDateTime = readDateTime;
            }
        }
    }

    //nested class for transaction object
    public static class Transaction {
        private String accountName;
        private long accountNumber;
        private LocalDateTime dateTime;
        private double amount;
        private String type;
        private double balanceAfter;

        public Transaction(String accountName, long accountNumber, LocalDateTime dateTime, double amount, String type, double balanceAfter) {
            this.accountName = accountName;
            this.accountNumber = accountNumber;
            this.dateTime = dateTime;
            this.amount = amount;
            this.type = type;
            this.balanceAfter = balanceAfter;
        }

        public String getAccountName() {
            return accountName; 
        }

        public long getAccountNumber() { 
            return accountNumber; 
        }

        public LocalDateTime getDateTime() { 
            return dateTime; 
        }

        public LocalDate getDate() { 
            return dateTime.toLocalDate(); 
        }

        public double getAmount() { 
            return amount; 
        }

        public String getType() { 
            return type; 
        }

        public double getBalanceAfter() { 
            return balanceAfter; 
        }
    }

    //nested class for currency object
    public static class Currency{
        private String name;
        private double value;

        public Currency(String name, double value){
            this.name = name;
            this.value = value;
        }

        public String getName(){
            return this.name;
        }

        public double getValue(){
            return this.value;
        }

        public void setValue(double value){
            this.value = value;
        }
    }

    //nested class for daily behavior object
    public static class DailyBehavior {
        String accName;
        long accNumber;
        LocalDate date;
        double totalWithdrawals = 0.0;
        int numberOfDeposits = 0;
        double totalDeposits = 0.0;

        public DailyBehavior(String accName, long accNumber, LocalDate date) {
            this.accName = accName;
            this.accNumber = accNumber;
            this.date = date;
        }

        public String getName() {
            return accName;
        }

        public long getNumber() {
            return accNumber;
        }

        public LocalDate getDate(){
            return date;
        }

        public double getTotalWithdrawals() {
            return totalWithdrawals;
        }

        public int getNumberOfDeposits() {
            return numberOfDeposits;
        }

        public double getTotalDeposits() {
            return totalDeposits;
        }

        public void incNumDeposits(){
            this.numberOfDeposits++;
        }

        public void addTotalW(double toAdd){
            this.totalWithdrawals += toAdd;
        }

        public void addTotalD(double toAdd){
            this.totalDeposits += toAdd;
        }

        public Double getTurnoverVelocity() {
            if (totalDeposits == 0.0) {
                return null;
            }
            return (totalWithdrawals / totalDeposits) * 100.0;
        }

        public String getFormattedVelocity() {
            Double velocity = getTurnoverVelocity();
            if (velocity == null) {
                return "N/A";
            }
            return String.format(Locale.US, MONEY_FORMAT, velocity);
        }

        public String getRiskFlagLevel() {
            Double velocity = getTurnoverVelocity();
            boolean highVelocity = (velocity != null && velocity >= 90.0);
            if (totalWithdrawals >= 500000.0 || highVelocity) {
                return "Suspicious";
            }
            return "Normal";
        }
    }

    //nested class for privilege object
    public static class Privilege {
        String accName;
        long accAge;
        double currBal;
        double avgDailyBal;
        String tier;

        public Privilege(String accName, long accAge, double currBal, double avgDailyBal, String tier) {
            this.accName = accName;
            this.accAge = accAge;
            this.currBal = currBal;
            this.avgDailyBal = avgDailyBal;
            this.tier = tier;
        }

        public String getName() {
            return this.accName;
        }

        public long getAge() {
            return this.accAge;
        }

        public double getBal() {
            return this.currBal;
        }

        public double getAvg() {
            return this.avgDailyBal;
        }

        public String getTier() {
            return this.tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }
    }

    //nested class for activity object
    public static class Activity {
            String accName;
            long accNum;
            double currBal;
            long daysInactive;
            LocalDate lastTransaction;
            String status;

            public Activity(String accName, long accNum, double currBal, long daysInactive, LocalDate lastTransaction, String status) {
                this.accName = accName;
                this.accNum = accNum;
                this.currBal = currBal;
                this.daysInactive = daysInactive;
                this.lastTransaction = lastTransaction;
                this.status = status;
            }

            public String getName() {
                return this.accName;
            }

            public long getNum(){
                return this.accNum;
            }

            public double getBalance() {
                return this.currBal;
            }

            public long getDays() {
                return this.daysInactive;
            }

            public LocalDate getLast() {
                return this.lastTransaction;
            }

            public String getStatus() {
                return this.status;
            }

            public void setLast(LocalDate last) {
                this.lastTransaction = last;
            }

            public void setDays(long days) {
                this.daysInactive = days;
            }

            public void setStatus(String status){
                this.status = status;
            }
        }

    //nested class for operation cancelling
    public static class CancelledException extends RuntimeException {}
}