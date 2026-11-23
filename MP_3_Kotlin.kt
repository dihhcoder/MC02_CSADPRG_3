/* 
********************
Last names: ALCANTARA, ANGELES, CONSTANTINO, MATIBAG
Language: Kotlin
Paradigm(s): Functional, Object-Oriented
********************
*/

// To compile and run Kotlin you need to have a Kotlin compiler 
// and Java Development Kit (JDK) installed and set up
// or use IntelliJ IDEA IDE

// Use this to compile and run the program:
// kotlinc MP_3_Kotlin.kt -include-runtime -d MP_3_Kotlin.jar && java -jar MP_3_Kotlin.jar

import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeParseException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

const val CSV_FILE_NAME = "bank_transactions.csv"
const val CSV_HEADER = "account_name,account_number,date,time,currency,amount,type,current_balance"

// Data class representing a single bank account
data class Account(
    val accountNumber: String,
    val name: String,
    var balance: Double,
    val currency: String = "PHP"
)

// Data class representing a cleaned, validated transaction row
data class Transaction(
    val accountName: String,
    val accountNumber: String,
    val date: LocalDate,
    val time: LocalTime,
    val currency: String,
    val amount: Double,
    val type: String,
    val currentBalance: Double
)

// Data class representing a single row in Report 1
data class PrivilegeTierReportRow(
    val accountName: String,
    val accountAge: Long,
    val currentBalance: Double,
    val averageDailyBalance: Double,
    val privilegeTier: String
)

// Data class representing a single row in Report 2
data class InterestProjectionReportRow(
    val accountName: String,
    val currentBalance: Double,
    val expectedInterest: Double
)

// Data class representing a single row in Report 3
data class SuspiciousBehaviorReportRow(
    val accountName: String,
    val date: LocalDate,
    val totalWithdrawalAmounts: Double,
    val numberOfDeposits: Int,
    val totalDepositAmounts: Double,
    val turnoverVelocity: Double,
    val riskFlagLevel: String
)

// Data class representing a single row in Report 4
data class HighestWithdrawalReportRow(
    val accountName: String,
    val accountNumber: String,
    val withdrawalCount: Int,
    val totalWithdrawalAmount: Double,
    val highestSingleWithdrawal: Double,
    val highestWithdrawalDate: String
)

// Ensure CSV file exists and has the header row
fun ensureCsvFileExists() {
    val file = File(CSV_FILE_NAME)
    if (!file.exists() || file.length() == 0L) {
        file.writeText("$CSV_HEADER\n")
    }
}

// Helper to parse dates into LocalDate (supports dd/MM/yyyy and yyyy-MM-dd)
fun parseDateUniformly(rawDate: String): LocalDate? {
    val formatters = listOf(
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ISO_LOCAL_DATE
    )
    for (formatter in formatters) {
        try {
            return LocalDate.parse(rawDate, formatter)
        } catch (_: DateTimeParseException) {
            // Try next format
        }
    }
    return null
}

// Helper to parse times into LocalTime (supports HH:mm:ss and HH:mm)
fun parseTimeUniformly(rawTime: String): LocalTime? {
    val formatters = listOf(
        DateTimeFormatter.ofPattern("H:mm:ss"),
        DateTimeFormatter.ofPattern("H:mm")
    )
    for (formatter in formatters) {
        try {
            return LocalTime.parse(rawTime, formatter)
        } catch (_: DateTimeParseException) {
            // Try next format
        }
    }
    return null
}

// Generate the next 10-digit account number after the highest existing account number in the file
fun getNextAccountNumber(): String {
    ensureCsvFileExists()
    val file = File(CSV_FILE_NAME)
    var maxAccountNum = 1000000000L

    file.readLines().drop(1).forEach { line ->
        if (line.isNotBlank()) {
            val parts = line.split(",")
            // Index 1 is account_number
            val accNum = parts.getOrNull(1)?.trim()?.toLongOrNull()
            if (accNum != null && accNum > maxAccountNum) {
                maxAccountNum = accNum
            }
        }
    }

    return String.format(Locale.US, "%010d", maxAccountNum + 1)
}

// Load existing accounts and their latest balances from bank_transactions.csv
fun loadAccountsFromCsv(): MutableList<Account> {
    ensureCsvFileExists()
    val file = File(CSV_FILE_NAME)
    val accountMap = linkedMapOf<String, Account>()

    file.readLines().drop(1).forEach { line ->
        if (line.isNotBlank()) {
            val parts = line.split(",")
            if (parts.size >= 8) {
                val accName = parts[0].trim()
                val accNum = parts[1].trim()
                val currency = parts[4].trim().ifEmpty { "PHP" }
                val latestBalance = parts[7].trim().toDoubleOrNull() ?: 0.00

                accountMap[accNum] = Account(
                    accountNumber = accNum,
                    name = accName,
                    balance = latestBalance,
                    currency = currency
                )
            }
        }
    }

    return accountMap.values.toMutableList()
}

// Append a transaction record to bank_transactions.csv
fun saveTransactionToCsv(
    accountName: String,
    accountNumber: String,
    currency: String,
    amount: Double,
    transactionType: String,
    currentBalance: Double
) {
    ensureCsvFileExists()
    val now = LocalDateTime.now()
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    val currentDate = now.format(dateFormatter)
    val currentTime = now.format(timeFormatter)

    val row = String.format(
        Locale.US,
        "%s,%s,%s,%s,%s,%.2f,%s,%.2f\n",
        accountName,
        accountNumber,
        currentDate,
        currentTime,
        currency,
        amount,
        transactionType,
        currentBalance
    )

    File(CSV_FILE_NAME).appendText(row)
}

// Read, validate, clean, and filter PHP rows from bank_transactions.csv
fun loadDataset(fileName: String = CSV_FILE_NAME): List<Transaction> {
    val file = File(fileName)
    if (!file.exists()) {
        println("Error: File '$fileName' not found!\n")
        return emptyList()
    }

    val cleanedTransactions = mutableListOf<Transaction>()
    var totalRows = 0
    var errorRows = 0
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val nameRegex = Regex("[a-zA-Z.\\-'\\s]+")

    file.useLines { lines ->
        for (line in lines.drop(1)) {
            totalRows++
            val parts = line.split(",")

            if (parts.size != 8 || !parts[0].trim().contains(" ") || !parts[0].trim().matches(nameRegex)) {
                errorRows++
                continue
            }

            try {
                val currency = parts[4].trim().uppercase()
                if (currency != "PHP") continue

                val accountName = parts[0].trim()
                val accountNumber = parts[1].trim()
                val parsedDate = LocalDate.parse(parts[2].trim(), dateFormatter)
                val parsedTime = LocalTime.parse(parts[3].trim(), timeFormatter)
                val amount = parts[5].trim().toDouble()
                val type = parts[6].trim().lowercase()
                val currentBalance = parts[7].trim().toDouble()

                cleanedTransactions.add(
                    Transaction(
                        accountName = accountName,
                        accountNumber = accountNumber,
                        date = parsedDate,
                        time = parsedTime,
                        currency = currency,
                        amount = amount,
                        type = type,
                        currentBalance = currentBalance
                    )
                )
            } catch (_: Exception) {
                errorRows++
            }
        }
    }

    println(
        String.format(
            Locale.US,
            "Processing dataset...\n%,d row/s loaded\n%,d filtered rows for PHP currency\n%,d filtered rows with errors\n",
            totalRows,
            cleanedTransactions.size,
            errorRows
        )
    )

    return cleanedTransactions
}

// currency to pass
fun getCurrencyList(): List<String> {
    return listOf("PHP", "USD", "JPY", "GBP", "EUR", "CNY")
}

// exchange rates 
fun getExchangeRates(): MutableMap<String, Double> {
    return mutableMapOf(
        "PHP" to 1.00,
        "USD" to 62.00,
        "JPY" to 0.40,
        "GBP" to 84.00,
        "EUR" to 72.00,
        "CNY" to 9.00
    )
}

// Y/N menu loop
fun menu(): Boolean {
    var decision: String
    var loop = true
    var verdict = true

    while (loop) {
        print("\nBack to the Main Menu (Y/N): ")
        decision = readln().trim().uppercase()
        print("\n")
        if (decision == "Y") {
            loop = false
            verdict = true
        } else if (decision == "N") {
            loop = false
            verdict = false
            println("Thank you for using the Banking and Currency Exchange Application.")
        } else {
            println("Invalid input! Y or N only.")
        }
    }
    return verdict
}

// Currency choices
fun currencyMenu() {
    println("[1] Philippine Peso (PHP)")
    println("[2] United States Dollar (USD)")
    println("[3] Japanese Yen (JPY)")
    println("[4] British Pound Sterling (GBP)")
    println("[5] Euro (EUR)")
    println("[6] Chinese Yuan Renminni (CNY)")
}

// Helper function to select an account by entering its name
fun selectAccount(accounts: List<Account>): Account? {
    if (accounts.isEmpty()) {
        println("Register First!")
        return null
    }

    print("Enter Account Name: ")
    val inputName = readln().trim().replace("\\s+".toRegex(), " ")

    val matchedAccount = accounts.find { it.name.equals(inputName, ignoreCase = true) }

    if (matchedAccount == null) {
        println("Account not found!")
        return null
    }

    println()
    return matchedAccount
}

// Option 1
fun register(accounts: MutableList<Account>) {
    var name: String
    var choiceRegister: String
    var balance: Double
    var loop: Boolean
    var anotherLoop = true
    var promptLoop: Boolean
    var amountLoop: Boolean

    do {
        loop = true
        println("Register Account Name")
        do {
            print("Account Name (First Name Last Name): ")
            name = readln().trim().replace("\\s+".toRegex(), " ")

            if (name.isBlank()) {
                println("Account name cannot be empty!")
            } else if (name.contains(",")) {
                println("Account name cannot contain commas!")
            } else if (name.split(" ").size < 2) {
                println("Please enter both First Name and Last Name!")
            } else if (accounts.any { it.name.equals(name, ignoreCase = true) }) {
                println("Account name already exists! Please enter a different name.")
            } else {
                amountLoop = true
                do {
                    print("Initial Deposit (Minimum PHP 5,000.00): ")
                    balance = readln().toDoubleOrNull() ?: -1.00
                    if (balance >= 5000.00) {
                        val generatedAccountNumber = getNextAccountNumber()
                        val newAccount = Account(
                            accountNumber = generatedAccountNumber,
                            name = name,
                            balance = balance,
                            currency = "PHP"
                        )
                        accounts.add(newAccount)

                        // Save registration and initial deposit to bank_transactions.csv
                        saveTransactionToCsv(
                            accountName = newAccount.name,
                            accountNumber = newAccount.accountNumber,
                            currency = newAccount.currency,
                            amount = balance,
                            transactionType = "deposit",
                            currentBalance = newAccount.balance
                        )

                        println("\nAccount created successfully!")
                        println("Account Number: ${newAccount.accountNumber}")
                        println("Account Name: ${newAccount.name}")
                        println("Initial Deposit: %.2f ${newAccount.currency}".format(newAccount.balance))

                        loop = false
                        amountLoop = false
                    } else {
                        println("Amount less than 5000.00 is invalid!")
                    }
                } while (amountLoop)
            }
        } while (loop)

        promptLoop = true
        do {
            print("\nRegister another account? (Y/N): ")
            choiceRegister = readln().trim().uppercase()
            if (choiceRegister == "Y") {
                print("\n")
                promptLoop = false
            } else if (choiceRegister == "N") {
                anotherLoop = false
                promptLoop = false
            } else {
                println("Invalid input! Y or N only.")
            }
        } while (promptLoop)
    } while (anotherLoop)
}

// Option 2
fun deposit(
    account: Account,
    currencyList: List<String>,
    rates: MutableMap<String, Double>
) {
    var choice: Int
    var transactionCurrency: String
    var amount: Double
    var conversionRate: Double
    var convertedAmount: Double
    var loop = true
    var promptLoop: Boolean
    var choiceDeposit: String

    do {
        println("Deposit Amount")
        println("Account Number: ${account.accountNumber}")
        println("Account Name: ${account.name}")
        println("Current Balance: %.2f".format(account.balance))
        println("Currency: ${account.currency}")
        println("\nTransaction Currency Options:")
        currencyMenu()

        do {
            print("\nTransaction Currency: ")
            choice = readln().toIntOrNull() ?: -1
            if (choice !in 1..currencyList.size)
                println("Invalid choice!")
        } while (choice !in 1..currencyList.size)

        transactionCurrency = currencyList[choice - 1]

        do {
            print("\nDeposit Amount in $transactionCurrency: ")
            amount = readln().toDoubleOrNull() ?: -1.00
            if (amount <= 0)
                println("Invalid amount!")
        } while (amount <= 0)

        conversionRate = rates[transactionCurrency] ?: 1.0
        convertedAmount = amount * conversionRate
        account.balance += convertedAmount

        // Save deposit transaction to bank_transactions.csv
        saveTransactionToCsv(
            accountName = account.name,
            accountNumber = account.accountNumber,
            currency = account.currency,
            amount = convertedAmount,
            transactionType = "deposit",
            currentBalance = account.balance
        )

        println("Converted Amount: %.2f ${account.currency} (1.00 $transactionCurrency = %.2f ${account.currency})".format(convertedAmount, conversionRate))
        println("Updated Balance: %.2f".format(account.balance))
        promptLoop = true

        do {
            print("\nDeposit another amount? (Y/N): ")
            choiceDeposit = readln().trim().uppercase()
            if (choiceDeposit == "Y") {
                print("\n")
                promptLoop = false
            } else if (choiceDeposit == "N") {
                loop = false
                promptLoop = false
            } else {
                println("Invalid input! Y or N only.")
            }
        } while (promptLoop)

    } while (loop)
}

// Option 3
fun withdraw(
    account: Account,
    currencyList: List<String>,
    rates: MutableMap<String, Double>
) {
    var choice: Int
    var transactionCurrency: String
    var amount: Double
    var conversionRate: Double
    var convertedAmount: Double
    var loop = true
    var promptLoop: Boolean
    var choiceWithdraw: String

    do {
        if (account.balance <= 0.00) {
            println("Account has no remaining balance!")
            break
        }

        println("Withdraw Amount")
        println("Account Number: ${account.accountNumber}")
        println("Account Name: ${account.name}")
        println("Current Balance: %.2f".format(account.balance))
        println("Currency: ${account.currency}")
        println("\nTransaction Currency Options:")
        currencyMenu()

        do {
            print("\nTransaction Currency: ")
            choice = readln().toIntOrNull() ?: -1
            if (choice !in 1..currencyList.size)
                println("Invalid choice!")
        } while (choice !in 1..currencyList.size)

        transactionCurrency = currencyList[choice - 1]
        conversionRate = rates[transactionCurrency] ?: 1.0

        do {
            print("\nWithdraw Amount in $transactionCurrency: ")
            amount = readln().toDoubleOrNull() ?: -1.00
            convertedAmount = amount * conversionRate
            if (amount <= 0 || convertedAmount > account.balance)
                println("Invalid amount!")
        } while (amount <= 0 || convertedAmount > account.balance)

        account.balance -= convertedAmount

        // Save withdrawal transaction to bank_transactions.csv
        saveTransactionToCsv(
            accountName = account.name,
            accountNumber = account.accountNumber,
            currency = account.currency,
            amount = convertedAmount,
            transactionType = "withdrawal",
            currentBalance = account.balance
        )

        println("Converted Amount: %.2f ${account.currency} (1.00 $transactionCurrency = %.2f ${account.currency})".format(convertedAmount, conversionRate))
        println("Updated Balance: %.2f".format(account.balance))
        promptLoop = true

        do {
            print("\nWithdraw another amount? (Y/N): ")
            choiceWithdraw = readln().trim().uppercase()
            if (choiceWithdraw == "Y") {
                print("\n")
                promptLoop = false
            } else if (choiceWithdraw == "N") {
                loop = false
                promptLoop = false
            } else {
                println("Invalid input! Y or N only.")
            }
        } while (promptLoop)

    } while (loop)
}

// Option 4
fun exchange(currencyList: List<String>, rates: MutableMap<String, Double>) {
    var currencyLoop = true
    var choiceLoop: Boolean
    var choiceCurrency: String
    var choice: Int
    var sourceCurrency: String
    var sourceRate: Double
    var sourceAmount: Double
    var exchangeCurrency: String
    var exchangeRate: Double
    var exchangeAmount: Double

    while (currencyLoop) {
        choiceLoop = true
        println("Foreign Currency Exchange")
        println("Source Currency Option:")
        currencyMenu()

        do {
            print("\nSource Currency: ")
            choice = readln().toIntOrNull() ?: -1
            if (choice !in 1..currencyList.size)
                println("Invalid choice!")
        } while (choice !in 1..currencyList.size)

        sourceCurrency = currencyList[choice - 1]
        sourceRate = rates[sourceCurrency] ?: 1.0

        do {
            print("Source Amount: ")
            sourceAmount = readln().toDoubleOrNull() ?: 0.00
        } while (sourceAmount <= 0)

        println("\nExchange Currency Option:")
        currencyMenu()

        do {
            print("\nExchange Currency: ")
            choice = readln().toIntOrNull() ?: -1
            if (choice !in 1..currencyList.size)
                println("Invalid choice!")
        } while (choice !in 1..currencyList.size)

        exchangeCurrency = currencyList[choice - 1]
        exchangeRate = rates[exchangeCurrency] ?: 1.0

        exchangeAmount = sourceAmount * (sourceRate / exchangeRate)
        print("Exchange Amount: %.2f\n".format(exchangeAmount))

        // Convert again loop
        do {
            print("\nConvert another currency (Y/N) ... ")
            choiceCurrency = readln().trim().uppercase()
            if (choiceCurrency == "Y") {
                print("\n")
                choiceLoop = false
            } else if (choiceCurrency == "N") {
                currencyLoop = false
                choiceLoop = false
            } else {
                println("Invalid input! Y or N only.")
            }
        } while (choiceLoop)
    }
}

// Option 5
fun record(currencyList: List<String>, oldRates: MutableMap<String, Double>) {
    var choice: Int
    val exchangeCurrency: String
    var exchangeRate: Double

    val rates = oldRates

    println("Record Exchange Rates\n")
    currencyMenu()

    do {
        print("\nSelect Currency: ")
        choice = readln().toIntOrNull() ?: -1
        if (choice !in 1..currencyList.size)
            println("Invalid choice!")
    } while (choice !in 1..currencyList.size)

    do {
        print("Exchange Rate: ")
        exchangeRate = readln().toDoubleOrNull() ?: 0.00
    } while (exchangeRate <= 0)

    exchangeCurrency = currencyList[choice - 1]
    rates[exchangeCurrency] = exchangeRate
}

// Option 6
fun interest(name: String, balance: Double, currency: String, interest: Double, daily: Double, years: Double) {
    var days: Int
    var newDaily = daily
    var newBalance = balance
    var rawDaily: Double
    var roundedBalance: Double

    println("Show Interest Amount")
    println("Account Name: $name")
    println("Current Balance: %.2f".format(newBalance))
    println("Currency: $currency")
    println("Interest Rate: %.2f %%".format(interest * 100))

    do {
        print("\nTotal Number of Days: ")
        days = readln().toIntOrNull() ?: -1
        if (days < 1 || days > 30)
            println("Invalid number of days!")
    } while (days < 1 || days > 30)

    for (i in 1..days) {
        rawDaily = newBalance * (interest / years)
        newDaily = ceil(rawDaily * 100.0) / 100.0
        newBalance += newDaily
        roundedBalance = ceil(newBalance * 100.0) / 100.0
        println("$i \t| ${"%.2f".format(newDaily)} \t| ${"%.2f".format(roundedBalance)} \t|")
    }
}

// Report 1: Banking Privilege Tiers
fun genrep1(transactions: List<Transaction>): List<PrivilegeTierReportRow> {
    val outputFileName = "report1_banking_privilege_tiers.csv"
    val csvHeader = "AccountName,AccountAge,CurrentBalance,AverageDailyBalance,PrivilegeTier"

    if (transactions.isEmpty()) {
        println("Report 1: Banking Privilege Tiers")
        println("No transactions available to generate report.\n")
        File(outputFileName).writeText("$csvHeader\n")
        println("Full table exported to: $outputFileName\n")
        return emptyList()
    }

    // Determine reference date and the last completed month (previous calendar month)
    val latestDate = transactions.maxOf { it.date }
    val latestMonth = YearMonth.from(latestDate)
    val previousMonth = latestMonth.minusMonths(1)
    val targetMonth = if (transactions.any { YearMonth.from(it.date) == previousMonth }) {
        previousMonth
    } else {
        latestMonth
    }

    val firstDayOfMonth = targetMonth.atDay(1)
    val daysInMonth = targetMonth.lengthOfMonth()

    // Group transactions by account number
    val transactionsByAccount = transactions.groupBy { it.accountNumber }
    val reportRows = mutableListOf<PrivilegeTierReportRow>()

    for ((_, accountTxs) in transactionsByAccount) {
        val sortedTxs = accountTxs.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.time })
        val firstTx = sortedTxs.first()
        val lastTx = sortedTxs.last()

        val accountName = lastTx.accountName
        val accountAge = ChronoUnit.DAYS.between(firstTx.date, latestDate)
        val currentBalance = lastTx.currentBalance

        // Starting balance before the target month begins (0.0 if opened during targetMonth)
        var runningBalance = sortedTxs
            .lastOrNull { it.date.isBefore(firstDayOfMonth) }
            ?.currentBalance ?: 0.00

        // Map transactions within the target month by date
        val monthTxsByDate = sortedTxs
            .filter { YearMonth.from(it.date) == targetMonth }
            .groupBy { it.date }

        // Sum End-of-Day balances across every day in the target month
        var sumEodBalances = 0.00
        for (day in 1..daysInMonth) {
            val currentDay = targetMonth.atDay(day)
            val dayTxs = monthTxsByDate[currentDay]
            if (!dayTxs.isNullOrEmpty()) {
                runningBalance = dayTxs.last().currentBalance
            }
            sumEodBalances += runningBalance
        }

        val averageDailyBalance = sumEodBalances / daysInMonth

        // Filter: AverageDailyBalance >= PHP 100,000.00
        if (averageDailyBalance < 100_000.00) {
            continue
        }

        val privilegeTier = when {
            averageDailyBalance < 500_000.00 -> "Silver"
            averageDailyBalance < 1_000_000.00 -> "Gold"
            else -> "Platinum"
        }

        reportRows.add(
            PrivilegeTierReportRow(
                accountName = accountName,
                accountAge = accountAge,
                currentBalance = currentBalance,
                averageDailyBalance = averageDailyBalance,
                privilegeTier = privilegeTier
            )
        )
    }

    reportRows.sortByDescending { it.averageDailyBalance }

    // Write full sorted table to CSV
    val csvContent = buildString {
        appendLine(csvHeader)
        for (row in reportRows) {
            appendLine(
                String.format(
                    Locale.US,
                    "%s,%d,%.2f,%.2f,%s",
                    row.accountName,
                    row.accountAge,
                    row.currentBalance,
                    row.averageDailyBalance,
                    row.privilegeTier
                )
            )
        }
    }
    File(outputFileName).writeText(csvContent)

    // Display console preview (first 2 rows)
    println("Report 1: Banking Privilege Tiers")
    if (reportRows.isEmpty()) {
        println("No accounts met the PHP 100,000.00 Average Daily Balance threshold.")
    } else {
        println(
            String.format(
                "%-20s | %-11s | %-15s | %-21s | %-14s",
                "Account Name", "Account Age", "Current Balance", "Average Daily Balance", "Privilege Tier"
            )
        )
        for (row in reportRows.take(2)) {
            println(
                String.format(
                    Locale.US,
                    "%-20s | %-11d | %,-15.2f | %,-21.2f | %-14s",
                    row.accountName,
                    row.accountAge,
                    row.currentBalance,
                    row.averageDailyBalance,
                    row.privilegeTier
                )
            )
        }
    }
    println("Full table exported to: $outputFileName\n")
    return reportRows
}

// Report 2: Interest Expense Projection
fun genrep2(transactions: List<Transaction>): List<InterestProjectionReportRow> {
    val outputFileName = "report2_interest_expense_projection.csv"
    val csvHeader = "AccountName,CurrentBalance,ExpectedInterest"
    val annualInterestRate = 0.05
    val daysInYear = 365.0
    val projectionDays = 30

    if (transactions.isEmpty()) {
        println("Report 2: Interest Expense Projection")
        println("No transactions available to generate report.\n")
        File(outputFileName).writeText("$csvHeader\n")
        println("Full table exported to: $outputFileName\n")
        return emptyList()
    }

    val latestTxComparator = compareBy<Transaction> { it.date }.thenBy { it.time }

    val reportRows = transactions
        .groupBy { it.accountNumber }
        .values
        .mapNotNull { accountTxs ->
            val lastTx = accountTxs.maxWithOrNull(latestTxComparator) ?: return@mapNotNull null
            if (lastTx.currentBalance < 10_000.00) return@mapNotNull null

            val expectedInterest = lastTx.currentBalance * (annualInterestRate / daysInYear) * projectionDays

            InterestProjectionReportRow(
                accountName = lastTx.accountName,
                currentBalance = lastTx.currentBalance,
                expectedInterest = expectedInterest
            )
        }
        .sortedByDescending { it.currentBalance }

    // Write full sorted table to CSV
    val csvContent = buildString {
        appendLine(csvHeader)
        for (row in reportRows) {
            appendLine(
                String.format(
                    Locale.US,
                    "%s,%.2f,%.2f",
                    row.accountName,
                    row.currentBalance,
                    row.expectedInterest
                )
            )
        }
    }
    File(outputFileName).writeText(csvContent)

    // Display console preview (first 2 rows)
    println("Report 2: Interest Expense Projection")
    if (reportRows.isEmpty()) {
        println("No accounts met the PHP 10,000.00 Current Balance threshold.")
    } else {
        println(
            String.format(
                "%-20s | %-15s | %-17s",
                "Account Name", "Current Balance", "Expected Interest"
            )
        )
        for (row in reportRows.take(2)) {
            println(
                String.format(
                    Locale.US,
                    "%-20s | %,-15.2f | %,-17.2f",
                    row.accountName,
                    row.currentBalance,
                    row.expectedInterest
                )
            )
        }
    }
    println("Full table exported to: $outputFileName\n")
    return reportRows
}

// Report 3: Suspicious Client Behavior
fun genrep3(transactions: List<Transaction>): List<SuspiciousBehaviorReportRow> {
    val outputFileName = "report3_suspicious_client_behavior.csv"
    val csvHeader = "AccountName,Date,TotalWithdrawalAmounts,NumberOfDeposits,TotalDepositAmounts,TurnoverVelocity,RiskFlagLevel"
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    if (transactions.isEmpty()) {
        println("Report 3: Suspicious Client Behavior")
        println("No transactions available to generate report.\n")
        File(outputFileName).writeText("$csvHeader\n")
        println("Full table exported to: $outputFileName\n")
        return emptyList()
    }

    // Group transactions by accountNumber and date
    val dailyGroups = transactions.groupBy { Pair(it.accountNumber, it.date) }
    val reportRows = mutableListOf<SuspiciousBehaviorReportRow>()

    for ((key, dayTxs) in dailyGroups) {
        val date = key.second
        val accountName = dayTxs.last().accountName

        var totalWithdrawalAmounts = 0.00
        var numberOfDeposits = 0
        var totalDepositAmounts = 0.00

        for (tx in dayTxs) {
            if (tx.type == "withdrawal") {
                totalWithdrawalAmounts += tx.amount
            } else if (tx.type == "deposit") {
                numberOfDeposits++
                totalDepositAmounts += tx.amount
            }
        }

        val turnoverVelocity = if (totalDepositAmounts > 0.00) {
            (totalWithdrawalAmounts / totalDepositAmounts) * 100.0
        } else {
            0.00
        }

        val riskFlagLevel = if (totalWithdrawalAmounts >= 500_000.00 || turnoverVelocity >= 90.0) {
            "Suspicious"
        } else {
            "Normal"
        }

        reportRows.add(
            SuspiciousBehaviorReportRow(
                accountName = accountName,
                date = date,
                totalWithdrawalAmounts = totalWithdrawalAmounts,
                numberOfDeposits = numberOfDeposits,
                totalDepositAmounts = totalDepositAmounts,
                turnoverVelocity = turnoverVelocity,
                riskFlagLevel = riskFlagLevel
            )
        )
    }

    // Sort ascending by Date, then descending by RiskFlagLevel ("Suspicious" before "Normal")
    reportRows.sortWith(
        compareBy<SuspiciousBehaviorReportRow> { it.date }
            .thenByDescending { it.riskFlagLevel }
    )

    // Write full sorted table to CSV
    val csvContent = buildString {
        appendLine(csvHeader)
        for (row in reportRows) {
            appendLine(
                String.format(
                    Locale.US,
                    "%s,%s,%.2f,%d,%.2f,%.2f,%s",
                    row.accountName,
                    row.date.format(dateFormatter),
                    row.totalWithdrawalAmounts,
                    row.numberOfDeposits,
                    row.totalDepositAmounts,
                    row.turnoverVelocity,
                    row.riskFlagLevel
                )
            )
        }
    }
    File(outputFileName).writeText(csvContent)

    // Display console preview (first 2 rows)
    println("Report 3: Suspicious Client Behavior")
    println(
        String.format(
            "%-20s | %-10s | %-17s | %-10s | %-15s | %-12s | %-10s",
            "Account Name", "Date", "Total Withdrawals", "Deposits", "Total Deposits", "Velocity (%)", "Risk Flag"
        )
    )
    for (row in reportRows.take(2)) {
        println(
            String.format(
                Locale.US,
                "%-20s | %-10s | %,-17.2f | %-10d | %,-15.2f | %,-12.2f | %-10s",
                row.accountName,
                row.date.format(dateFormatter),
                row.totalWithdrawalAmounts,
                row.numberOfDeposits,
                row.totalDepositAmounts,
                row.turnoverVelocity,
                row.riskFlagLevel
            )
        )
    }
    println("Full table exported to: $outputFileName\n")
    return reportRows
}

// Report 4: Highest Withdrawals (September to December)
fun genrep4(transactions: List<Transaction>): List<HighestWithdrawalReportRow> {
    val outputFileName = "report4_highest_withdrawals.csv"
    val csvHeader = "AccountName,AccountNumber,WithdrawalCount,TotalWithdrawalAmount,HighestSingleWithdrawal,HighestWithdrawalDate"
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    if (transactions.isEmpty()) {
        println("Report 4: Highest Withdrawals (September to December)")
        println("No transactions available to generate report.\n")
        File(outputFileName).writeText("$csvHeader\n")
        println("Full table exported to: $outputFileName\n")
        return emptyList()
    }

    // Filter for withdrawal transactions from September (9) to December (12)
    val sepToDecWithdrawals = transactions.filter { tx ->
        tx.type == "withdrawal" && tx.date.monthValue in 9..12
    }

    val withdrawalsByAccount = sepToDecWithdrawals.groupBy { it.accountNumber }
    val reportRows = mutableListOf<HighestWithdrawalReportRow>()

    for ((accountNumber, accountWithdrawals) in withdrawalsByAccount) {
        val sortedWithdrawals = accountWithdrawals.sortedWith(
            compareBy<Transaction> { it.date }.thenBy { it.time }
        )
        val maxWithdrawalTx = sortedWithdrawals.maxByOrNull { it.amount } ?: continue

        val accountName = sortedWithdrawals.last().accountName
        val withdrawalCount = sortedWithdrawals.size
        val totalWithdrawalAmount = sortedWithdrawals.sumOf { it.amount }
        val highestSingleWithdrawal = maxWithdrawalTx.amount
        val highestWithdrawalDate = "${maxWithdrawalTx.date.format(dateFormatter)} ${maxWithdrawalTx.time.format(timeFormatter)}"

        reportRows.add(
            HighestWithdrawalReportRow(
                accountName = accountName,
                accountNumber = accountNumber,
                withdrawalCount = withdrawalCount,
                totalWithdrawalAmount = totalWithdrawalAmount,
                highestSingleWithdrawal = highestSingleWithdrawal,
                highestWithdrawalDate = highestWithdrawalDate
            )
        )
    }

    // Sort descending by TotalWithdrawalAmount (highest withdrawals first)
    reportRows.sortByDescending { it.totalWithdrawalAmount }

    // Write full sorted table to CSV
    val csvContent = buildString {
        appendLine(csvHeader)
        for (row in reportRows) {
            appendLine(
                String.format(
                    Locale.US,
                    "%s,%s,%d,%.2f,%.2f,%s",
                    row.accountName,
                    row.accountNumber,
                    row.withdrawalCount,
                    row.totalWithdrawalAmount,
                    row.highestSingleWithdrawal,
                    row.highestWithdrawalDate
                )
            )
        }
    }
    File(outputFileName).writeText(csvContent)

    // Display console preview (first 2 rows)
    println("Report 4: Highest Withdrawals (September to December)")
    if (reportRows.isEmpty()) {
        println("No withdrawal transactions found from September to December.")
    } else {
        println(
            String.format(
                "%-20s | %-14s | %-11s | %-17s | %-16s | %-23s",
                "Account Name", "Account Number", "Withdrawals", "Total Withdrawn", "Highest Single", "Highest Withdrawal Date"
            )
        )
        for (row in reportRows.take(2)) {
            println(
                String.format(
                    Locale.US,
                    "%-20s | %-14s | %-11d | %,-17.2f | %,-16.2f | %-23s",
                    row.accountName,
                    row.accountNumber,
                    row.withdrawalCount,
                    row.totalWithdrawalAmount,
                    row.highestSingleWithdrawal,
                    row.highestWithdrawalDate
                )
            )
        }
    }
    println("Full table exported to: $outputFileName\n")
    return reportRows
}

// Helper to safely escape strings for JSON output
fun escapeJson(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

// Generate summary.json aggregating key statistics across the dataset and all 4 reports
fun generateSummaryJson(
    transactions: List<Transaction>,
    report2Rows: List<InterestProjectionReportRow>
) {
    val outputFileName = "summary.json"
    val transactionsByAccount = transactions.groupBy { it.accountNumber }

    val totalAccounts = transactionsByAccount.size
    val totalDeposits = transactions
        .filter { it.type == "deposit" }
        .sumOf { it.amount }
    val totalWithdrawals = transactions
        .filter { it.type == "withdrawal" }
        .sumOf { it.amount }
    val totalBalance = transactionsByAccount.values.sumOf { accountTxs ->
        accountTxs.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.time })
            .last()
            .currentBalance
    }
    val totalInterestExpense = report2Rows.sumOf { it.expectedInterest }

    val jsonContent = String.format(
        Locale.US,
        "{total_accounts: %,d, total_deposits: %,.2f, total_withdrawals: %,.2f, total_balance: %,.2f, total_interest_expense: %,.2f}",
        totalAccounts,
        totalDeposits,
        totalWithdrawals,
        totalBalance,
        totalInterestExpense
    )

    File(outputFileName).writeText("$jsonContent\n")

    println("Summary State ($outputFileName):")
    println(jsonContent)
    println()
}

// Option 7
fun report() {
    var loop = true
    var choice: Int
    var loadedTransactions: List<Transaction> = emptyList()
    do {
        println("Generate Reports\n")
        println("[1] Load the file")
        println("[2] Generate Reports")
        println("[3] Back to Main Menu")
        print("Enter choice: ")
        choice = readln().toIntOrNull() ?: -1
        print("\n")

        when (choice) {
            1 -> {
                loadedTransactions = loadDataset()
            }
            
            2 -> {
                println("Generating reports...")
                println("Outputs saved to individual files...\n")
                val r1 = genrep1(loadedTransactions)
                print("\n")
                val r2 = genrep2(loadedTransactions)
                print("\n")
                val r3 = genrep3(loadedTransactions)
                print("\n")
                val r4 = genrep4(loadedTransactions)
                print("\n")
                generateSummaryJson(loadedTransactions, r2)

            }
            
            3 -> loop = false

            else -> println("Invalid choice!\n")
        }
    } while (loop)
}

fun main() {
    var loop = true
    var choice: Int
    val interest = 0.05
    val daysYear = 365.00
    val dailyInterest = 0.00

    val accounts = loadAccountsFromCsv()
    val myCurrencies = getCurrencyList()
    val myRates = getExchangeRates()

    while (loop) {
        println("Select Transaction:")
        println("[1] Register Account Name")
        println("[2] Deposit Amount")
        println("[3] Withdraw Amount")
        println("[4] Currency Exchange")
        println("[5] Record Exchange Rates")
        println("[6] Show Interest Amount")
        println("[7] Generate Reports")
        print("\nChoice: ")
        choice = readln().toIntOrNull() ?: -1
        print("\n")

        when (choice) {
            1 -> {
                register(accounts)
                loop = menu()
            }

            2 -> {
                val selectedAccount = selectAccount(accounts)
                if (selectedAccount != null) {
                    deposit(selectedAccount, myCurrencies, myRates)
                }
                loop = menu()
            }

            3 -> {
                val selectedAccount = selectAccount(accounts)
                if (selectedAccount != null) {
                    withdraw(selectedAccount, myCurrencies, myRates)
                }
                loop = menu()
            }

            4 -> {
                exchange(myCurrencies, myRates)
                loop = menu()
            }

            5 -> {
                record(myCurrencies, myRates)
                loop = menu()
            }

            6 -> {
                val selectedAccount = selectAccount(accounts)
                if (selectedAccount != null) {
                    interest(
                        selectedAccount.name,
                        selectedAccount.balance,
                        selectedAccount.currency,
                        interest,
                        dailyInterest,
                        daysYear
                    )
                }
                loop = menu()
            }

            7 -> {
                report()
                loop = menu()
            }

            else -> println("Invalid choice!\n")
        }
    }
}
