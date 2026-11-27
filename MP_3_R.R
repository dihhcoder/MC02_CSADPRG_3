#********************
# Last names: ALCANTARA, ANGELES, CONSTANTINO, MATIBAG
# Language: R
# Paradigm(s): Procedural, Functional
#********************

# to run the program (requires R 4.0 or newer)
# download and install R from https://cloud.r-project.org
# put bank_transactions.csv in the same folder as MP_3_R.R

# if in VS code and Windows 11,
# Click the Search icon on the taskbar and type "environment variables"
# Select "Edit the system environment variables" (Control panel)
# In System Properties (Advanced tab), click "Environment Variables"
# In the top box "User variables for ___ ", click "Path", then click "Edit..."
# Click "New" and paste: C:\Program Files\R\R-4.6.1\bin
# Click OK on all three windows
# Fully close VS Code (File > Exit) and reopen it
# In the VS Code terminal, run: Rscript --version (it should show version 4.6.1).
# Run the program: Rscript MP_3_R.R

# Alternative: download RStudio Desktop from https://posit.co/download/rstudio-desktop/
# then open MP_3_R.R in RStudio, set the working directory to the file's folder
# (Session > Set Working Directory > To Source File Location) and click "Source"

CSV_FILE <- "bank_transactions.csv"
CSV_HEADER <- "account_name,account_number,date,time,currency,amount,type,current_balance"

ANNUAL_INTEREST_PCT <- 5
DAYS_PER_YEAR <- 365
MAX_PROJECTION_DAYS <- 3650
INTEREST_PROJECTION_DAYS <- 30

MIN_INITIAL_DEPOSIT <- 5000
FIRST_ACCOUNT_NUMBER <- 1000000001
MAX_TXN_AMOUNT <- 1000000000
MAX_RATE <- 1000000
MAX_BALANCE <- 1000000000000

ADB_MINIMUM <- 100000
SILVER_LIMIT <- 500000
GOLD_LIMIT <- 1000000
INTEREST_MIN_BALANCE <- 10000
SUSPICIOUS_WITHDRAWAL <- 500000
SUSPICIOUS_VELOCITY <- 90
GROWTH_THRESHOLD <- 10
PREVIEW_ROWS <- 2

REPORT1_FILE <- "report1_banking_privilege_tiers.csv"
REPORT2_FILE <- "report2_interest_expense_projection.csv"
REPORT3_FILE <- "report3_suspicious_client_behavior.csv"
REPORT4_FILE <- "report4_account_growth.csv"
SUMMARY_FILE <- "summary.json"

CURRENCY_CODES <- c("PHP", "USD", "JPY", "GBP", "EUR", "CNY")
CURRENCY_NAMES <- c("Philippine Peso", "United States Dollar", "Japanese Yen",
                    "British Pound Sterling", "Euro", "Chinese Yuan Renminni")

ERROR_REASONS <- c("wrong number of values", "missing values", "invalid account number",
                   "invalid date", "invalid time", "invalid amount",
                   "invalid transaction type", "invalid balance")



input_con <- if (interactive()) NULL else file("stdin", open = "r")

read_input <- function(prompt) {
  if (interactive()) {
    line <- readline(prompt)
  } else {
    cat(prompt)
    line <- readLines(input_con, n = 1)
    if (length(line) == 0) {
      cat("\n\nInput closed. Exiting program.\n")
      quit(save = "no")
    }
  }
  trimws(line)
}

read_int <- function(prompt, min, max) {
  repeat {
    input <- read_input(prompt)
    if (grepl("^[0-9]{1,9}$", input)) {
      value <- as.integer(input)
      if (value >= min && value <= max) return(value)
    }
    cat(sprintf("Invalid input. Enter a whole number from %d to %d.\n", min, max))
  }
}

read_amount <- function(prompt, max) {
  repeat {
    input <- read_input(prompt)
    if (grepl("^[0-9]*\\.?[0-9]+$|^[0-9]+\\.$", input)) {
      value <- as.numeric(input)
      if (value > 0 && value <= max) return(value)
    }
    cat(sprintf("Invalid input. Enter a number greater than 0 (up to %.0f).\n", max))
  }
}

read_yes_no <- function(prompt) {
  repeat {
    input <- toupper(read_input(prompt))
    if (input %in% c("Y", "N")) return(input == "Y")
    cat("Invalid input. Enter Y or N.\n")
  }
}



money <- function(x) formatC(x, format = "f", digits = 2, big.mark = "")

money_comma <- function(x) formatC(x, format = "f", digits = 2, big.mark = ",")

count_comma <- function(x) formatC(x, format = "d", big.mark = ",")

to_cents <- function(x) round(x * 100)

to_php <- function(amount, rate) floor(amount * rate * 100 + 0.5) / 100

daily_interest <- function(cents) {
  divisor <- 100 * DAYS_PER_YEAR
  (cents * ANNUAL_INTEREST_PCT + divisor %/% 2) %/% divisor
}

clean_name <- function(x) gsub("\\s+", " ", trimws(x))



parse_seconds <- function(x) {
  ok <- grepl("^[0-9]{1,2}:[0-9]{2}:[0-9]{2}$", x)
  parts <- matrix(as.integer(unlist(strsplit(ifelse(ok, x, "0:00:00"), ":", fixed = TRUE))),
                  ncol = 3, byrow = TRUE)
  ok <- ok & parts[, 1] <= 23 & parts[, 2] <= 59 & parts[, 3] <= 59
  ifelse(ok, parts[, 1] * 3600 + parts[, 2] * 60 + parts[, 3], NA)
}

empty_transactions <- function() {
  data.frame(row = integer(), name = character(), number = character(),
             date = as.Date(character()), secs = numeric(), amount = numeric(),
             type = character(), balance = numeric(), stringsAsFactors = FALSE)
}

load_dataset <- function() {
  result <- list(found = FALSE, total = 0, php = 0, other = 0,
                 errors = setNames(integer(length(ERROR_REASONS)), ERROR_REASONS),
                 txns = empty_transactions())
  if (!file.exists(CSV_FILE)) return(result)
  result$found <- TRUE

  lines <- readLines(CSV_FILE, warn = FALSE)
  lines <- lines[nzchar(trimws(lines))]
  if (length(lines) > 0 && tolower(trimws(sub(",.*$", "", lines[1]))) == "account_name") {
    lines <- lines[-1]
  }
  total <- length(lines)
  result$total <- total
  if (total == 0) return(result)

  parts <- strsplit(lines, ",", fixed = TRUE)
  shape_ok <- lengths(parts) == 8 & !grepl(",$", lines)
  f <- matrix("", nrow = total, ncol = 8)
  if (any(shape_ok)) f[shape_ok, ] <- do.call(rbind, parts[shape_ok])
  f <- trimws(f)

  is_php <- shape_ok & toupper(f[, 5]) == "PHP"
  result$php <- sum(is_php)
  result$other <- sum(shape_ok & !is_php)

  number_re <- "^[0-9]+(\\.[0-9]+)?$"
  dates <- as.Date(f[, 3], format = "%d/%m/%Y")
  secs <- parse_seconds(f[, 4])
  amounts <- suppressWarnings(as.numeric(f[, 6]))
  balances <- suppressWarnings(as.numeric(f[, 8]))

  checks <- cbind(
    shape_ok,
    rowSums(f == "") == 0,
    grepl("^[0-9]{10}$", f[, 2]),
    grepl("^[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}$", f[, 3]) & !is.na(dates),
    !is.na(secs),
    grepl(number_re, f[, 6]) & !is.na(amounts) & amounts > 0,
    tolower(f[, 7]) %in% c("deposit", "withdrawal"),
    grepl(number_re, f[, 8]) & !is.na(balances)
  )
  valid <- rowSums(!checks) == 0
  bad <- which(!valid & (is_php | !shape_ok))
  if (length(bad) > 0) {
    first_fail <- vapply(bad, function(i) which(!checks[i, ])[1], integer(1))
    result$errors <- setNames(tabulate(first_fail, nbins = length(ERROR_REASONS)), ERROR_REASONS)
  }

  keep <- which(valid & is_php)
  txns <- data.frame(row = keep, name = clean_name(f[keep, 1]), number = f[keep, 2],
                     date = dates[keep], secs = secs[keep], amount = amounts[keep],
                     type = tolower(f[keep, 7]), balance = balances[keep],
                     stringsAsFactors = FALSE)
  result$txns <- txns[order(txns$number, txns$row), ]
  rownames(result$txns) <- NULL
  result
}

summarize_accounts <- function(txns) {
  first <- txns[!duplicated(txns$number), ]
  last <- txns[!duplicated(txns$number, fromLast = TRUE), ]
  data.frame(number = first$number, name = last$name, open_date = first$date,
             start_balance = first$balance, balance = last$balance,
             stringsAsFactors = FALSE)
}

get_accounts <- function() summarize_accounts(load_dataset()$txns)

print_load_summary <- function(data) {
  if (!data$found) {
    cat(sprintf("Processing dataset... could not find %s in this folder.\n", CSV_FILE))
    return(invisible(NULL))
  }
  cat(sprintf("Processing dataset... (%s rows loaded, %s filtered for PHP currency)\n",
              count_comma(data$total), count_comma(data$php)))
  cat(sprintf("Validation: %s rows excluded (not PHP), %s rows removed because of errors.\n",
              count_comma(data$other), count_comma(sum(data$errors))))
  for (reason in names(data$errors)[data$errors > 0]) {
    cat(sprintf("  - %s: %s\n", reason, count_comma(data$errors[[reason]])))
  }
  cat(sprintf("Clean rows kept: %s across %s accounts.\n",
              count_comma(nrow(data$txns)), count_comma(length(unique(data$txns$number)))))
}



file_ends_with_newline <- function() {
  size <- file.size(CSV_FILE)
  bytes <- readBin(CSV_FILE, "raw", n = size)
  bytes[size] == as.raw(10)
}

append_transaction <- function(name, number, type, amount, balance) {
  now <- Sys.time()
  row <- paste(name, number, format(now, "%d/%m/%Y"), format(now, "%H:%M:%S"), "PHP",
               money(amount), type, money(balance), sep = ",")
  tryCatch({
    if (!file.exists(CSV_FILE) || file.size(CSV_FILE) == 0) {
      cat(CSV_HEADER, "\n", file = CSV_FILE, sep = "")
    } else if (!file_ends_with_newline()) {
      cat("\n", file = CSV_FILE, append = TRUE)
    }
    cat(row, "\n", file = CSV_FILE, append = TRUE, sep = "")
    TRUE
  }, error = function(e) {
    cat(sprintf("Could not write to %s. Transaction cancelled.\n", CSV_FILE))
    FALSE
  })
}



print_main_menu <- function() {
  cat("\nSelect Transaction:\n")
  cat("[1] Register Account\n")
  cat("[2] Deposit Amount\n")
  cat("[3] Withdraw Amount\n")
  cat("[4] Currency Exchange\n")
  cat("[5] Record Exchange Rates\n")
  cat("[6] Show Interest Amount\n")
  cat("[7] Generate Reports\n\n")
}

print_currency_menu <- function() {
  cat(sprintf("[%d] %s (%s)\n", seq_along(CURRENCY_CODES), CURRENCY_NAMES, CURRENCY_CODES), sep = "")
  cat("\n")
}

print_account_header <- function(account) {
  cat("Account Name: ", account$name, "\n", sep = "")
  cat("Account Number: ", account$number, "\n", sep = "")
  cat("Current Balance: ", money(account$balance), "\n", sep = "")
  cat("Currency: PHP\n")
}

select_account <- function(accounts) {
  if (nrow(accounts) == 0) {
    cat("No accounts yet. Please use [1] Register Account first.\n")
    return(NULL)
  }
  repeat {
    input <- clean_name(read_input("Account Number or Name (blank to cancel): "))
    if (!nzchar(input)) return(NULL)
    idx <- if (grepl("^[0-9]+$", input)) {
      match(input, accounts$number)
    } else {
      match(tolower(input), tolower(accounts$name))
    }
    if (!is.na(idx)) return(accounts[idx, ])
    cat("Account not found.\n")
  }
}

choose_php_amount <- function(prompt_title, rates) {
  cat("\nTransaction Currency Options:\n")
  print_currency_menu()
  code <- CURRENCY_CODES[read_int("Transaction Currency: ", 1, length(CURRENCY_CODES))]
  repeat {
    amount <- read_amount(sprintf("%s (%s): ", prompt_title, code), MAX_TXN_AMOUNT)
    php_amount <- to_php(amount, rates[[code]])
    if (php_amount > 0) break
    cat("Amount is too small (less than 0.01 PHP).\n")
  }
  if (code != "PHP") {
    cat(sprintf("Converted Amount: %s PHP (1 %s = %.4f PHP)\n", money(php_amount), code, rates[[code]]))
  }
  php_amount
}

name_error <- function(name, accounts) {
  if (!nzchar(name)) return("Account name cannot be empty.")
  if (!grepl("^[A-Za-z.'-]+( [A-Za-z.'-]+)+$", name)) {
    return("Enter the first name and last name using letters only (e.g. Juan Dela Cruz).")
  }
  if (tolower(name) %in% tolower(accounts$name)) return("An account with that name already exists.")
  NULL
}



register_accounts <- function(rates) {
  repeat {
    cat("\nRegister Account\n")
    accounts <- get_accounts()

    repeat {
      name <- clean_name(read_input("Account Name: "))
      error <- name_error(name, accounts)
      if (is.null(error)) break
      cat(error, "\n", sep = "")
    }

    repeat {
      initial <- choose_php_amount("Initial Deposit", rates)
      if (initial >= MIN_INITIAL_DEPOSIT && initial <= MAX_BALANCE) break
      cat("Initial deposit must be at least PHP 5,000.00.\n")
    }

    number <- if (nrow(accounts) == 0) FIRST_ACCOUNT_NUMBER else max(as.numeric(accounts$number)) + 1
    number <- sprintf("%.0f", number)

    if (append_transaction(name, number, "deposit", initial, initial)) {
      cat("Account created successfully!\n")
      cat("Account Number: ", number, "\n", sep = "")
      cat("Current Balance: ", money(initial), "\n", sep = "")
    }

    if (!read_yes_no("\nRegister another account? (Y/N): ")) break
  }
}

transact <- function(rates, is_deposit) {
  title <- if (is_deposit) "Deposit Amount" else "Withdraw Amount"
  again <- if (is_deposit) "\nDeposit another amount? (Y/N): " else "\nWithdraw another amount? (Y/N): "

  repeat {
    cat("\n", title, "\n", sep = "")
    account <- select_account(get_accounts())
    if (is.null(account)) return(invisible(NULL))
    print_account_header(account)

    if (!is_deposit && account$balance <= 0) {
      cat("Account has no balance.\n")
    } else {
      php_amount <- choose_php_amount(title, rates)
      new_balance <- round(account$balance + if (is_deposit) php_amount else -php_amount, 2)

      if (is_deposit && new_balance > MAX_BALANCE) {
        cat("Deposit would exceed the maximum allowed balance. Transaction cancelled.\n")
      } else if (!is_deposit && new_balance < 0) {
        cat("Insufficient balance. Transaction cancelled.\n")
      } else if (append_transaction(account$name, account$number,
                                    if (is_deposit) "deposit" else "withdrawal",
                                    php_amount, new_balance)) {
        cat("Updated Balance: ", money(new_balance), "\n", sep = "")
      }
    }

    if (!read_yes_no(again)) break
  }
}

currency_exchange <- function(rates) {
  repeat {
    cat("\nForeign Currency Exchange\n")
    cat("Source Currency Option:\n")
    print_currency_menu()
    src <- CURRENCY_CODES[read_int("Source Currency: ", 1, length(CURRENCY_CODES))]
    amount <- read_amount("Source Amount: ", MAX_TXN_AMOUNT)

    cat("\nExchanged Currency Options:\n")
    print_currency_menu()
    dst <- CURRENCY_CODES[read_int("Exchange Currency: ", 1, length(CURRENCY_CODES))]

    if (src == dst) {
      cat(sprintf("Source and exchange currency are the same (%s). Nothing to convert.\n", src))
    } else {
      cat("Exchange Amount: ", money(amount * rates[[src]] / rates[[dst]]), "\n", sep = "")
    }

    if (!read_yes_no("\nConvert another currency (Y/N)? ")) break
  }
}

record_exchange_rate <- function(rates) {
  cat("\nRecord Exchange Rate\n\n")
  print_currency_menu()
  code <- CURRENCY_CODES[read_int("Select Foreign Currency: ", 1, length(CURRENCY_CODES))]

  if (code == "PHP") {
    cat("PHP is the base currency; its rate is fixed at 1.00 and cannot be changed.\n")
    return(rates)
  }

  cat(sprintf("Enter how many PHP equal 1 %s.\n", code))
  rates[[code]] <- read_amount("Exchange Rate: ", MAX_RATE)
  cat(sprintf("Recorded: 1 %s = %.4f PHP\n", code, rates[[code]]))
  rates
}

show_interest <- function() {
  cat("\nShow Interest Amount\n")
  account <- select_account(get_accounts())
  if (is.null(account)) return(invisible(NULL))
  print_account_header(account)
  cat("Interest Rate: ", ANNUAL_INTEREST_PCT, "%\n\n", sep = "")

  days <- read_int("Total Number of Days: ", 1, MAX_PROJECTION_DAYS)

  balance <- to_cents(account$balance)
  cat("\nDay  | Interest | Balance |\n")
  for (day in 1:days) {
    interest <- daily_interest(balance)
    balance <- balance + interest
    cat(sprintf("%-4d | %-8s | %-7s |\n", day, money(interest / 100), money(balance / 100)))
  }
}



csv_escape <- function(x) {
  ifelse(grepl("[,\"]", x), paste0("\"", gsub("\"", "\"\"", x), "\""), x)
}

write_report <- function(df, file_name, title) {
  cells <- vapply(df, as.character, character(nrow(df)))
  if (nrow(df) == 1) cells <- matrix(cells, nrow = 1)
  rows <- if (nrow(df) > 0) apply(cells, 1, function(r) paste(csv_escape(r), collapse = ",")) else character()
  writeLines(c(paste(csv_escape(names(df)), collapse = ","), rows), file_name)

  cat("\n", title, "\n", sep = "")
  if (nrow(df) == 0) {
    cat("(no rows)\n")
  } else {
    print(head(df, PREVIEW_ROWS), row.names = FALSE, right = FALSE)
  }
  cat(sprintf("(Full table exported to %s)\n", file_name))
}

report_month <- function() {
  current_month <- as.Date(format(Sys.Date(), "%Y-%m-01"))
  start <- seq(current_month, by = "-1 month", length.out = 2)[2]
  end <- current_month - 1
  list(start = start, end = end, days = as.integer(end - start) + 1)
}

report1_privilege_tiers <- function(txns, accounts) {
  month <- report_month()
  day_numbers <- as.numeric(seq(month$start, month$end, by = "day"))

  eod_sums <- vapply(split(txns, txns$number), function(t) {
    t <- t[order(t$date, t$secs, t$row), ]
    idx <- findInterval(day_numbers, as.numeric(t$date))
    sum(ifelse(idx == 0, 0, to_cents(t$balance[pmax(idx, 1)])))
  }, numeric(1))[accounts$number]

  qualified <- eod_sums >= ADB_MINIMUM * 100 * month$days
  rows <- accounts[qualified, ]
  sums <- eod_sums[qualified]
  adb <- ((2 * sums + month$days) %/% (2 * month$days)) / 100
  tier <- ifelse(sums < SILVER_LIMIT * 100 * month$days, "Silver",
                 ifelse(sums < GOLD_LIMIT * 100 * month$days, "Gold", "Platinum"))
  ord <- order(-sums, rows$name)

  df <- data.frame(
    AccountName = rows$name[ord],
    AccountAge = count_comma(as.integer(Sys.Date() - rows$open_date[ord])),
    CurrentBalance = money_comma(rows$balance[ord]),
    AverageDailyBalance = money_comma(adb[ord]),
    PrivilegeTier = tier[ord],
    check.names = FALSE, stringsAsFactors = FALSE
  )
  cat(sprintf("\nReport month: %s (%d days)", format(month$start, "%B %Y"), month$days))
  write_report(df, REPORT1_FILE, "Report 1: Banking Privilege Tiers")
}

report2_interest_projection <- function(accounts) {
  rows <- accounts[accounts$balance >= INTEREST_MIN_BALANCE, ]
  start <- to_cents(rows$balance)
  balance <- start
  for (day in seq_len(INTEREST_PROJECTION_DAYS)) balance <- balance + daily_interest(balance)
  interest <- balance - start
  ord <- order(-interest, rows$name)

  df <- data.frame(
    AccountName = rows$name[ord],
    CurrentBalance = money_comma(rows$balance[ord]),
    ExpectedInterest = money_comma(interest[ord] / 100),
    check.names = FALSE, stringsAsFactors = FALSE
  )
  write_report(df, REPORT2_FILE, "Report 2: Interest Expense Projection")
  sum(interest) / 100
}

report3_suspicious_behavior <- function(txns, accounts) {
  is_deposit <- txns$type == "deposit"
  cents <- to_cents(txns$amount)
  daily <- aggregate(
    cbind(withdrawals = ifelse(is_deposit, 0, cents),
          deposits = ifelse(is_deposit, cents, 0),
          deposit_count = as.integer(is_deposit)) ~ number + date,
    data = data.frame(number = txns$number, date = txns$date),
    FUN = sum
  )
  daily$name <- accounts$name[match(daily$number, accounts$number)]
  velocity <- ifelse(daily$deposits > 0,
                     ((daily$withdrawals * 20000 + daily$deposits) %/% (2 * daily$deposits)) / 100, NA)
  suspicious <- daily$withdrawals >= SUSPICIOUS_WITHDRAWAL * 100 |
    (daily$deposits > 0 & daily$withdrawals * 100 >= SUSPICIOUS_VELOCITY * daily$deposits)
  ord <- order(daily$date, !suspicious, daily$name)

  df <- data.frame(
    AccountName = daily$name[ord],
    Date = format(daily$date[ord], "%d/%m/%Y"),
    TotalWithdrawalAmounts = money_comma(daily$withdrawals[ord] / 100),
    NumberOfDeposits = count_comma(daily$deposit_count[ord]),
    TotalDepositAmounts = money_comma(daily$deposits[ord] / 100),
    "TurnoverVelocity (%)" = ifelse(is.na(velocity[ord]), "N/A", money_comma(velocity[ord])),
    RiskFlagLevel = ifelse(suspicious[ord], "Suspicious", "Normal"),
    check.names = FALSE, stringsAsFactors = FALSE
  )
  write_report(df, REPORT3_FILE, "Report 3: Suspicious Client Behavior")
}

report4_account_growth <- function(accounts) {
  start <- to_cents(accounts$start_balance)
  latest <- to_cents(accounts$balance)
  change <- latest - start
  rate <- ifelse(start > 0, floor(change / start * 10000 + 0.5) / 100, NA)
  measure <- ifelse(is.na(rate), sign(change) * Inf, rate)
  status <- ifelse(measure > GROWTH_THRESHOLD, "Growing",
                   ifelse(measure < -GROWTH_THRESHOLD, "Declining", "Stable"))
  ord <- order(-measure, accounts$name)

  df <- data.frame(
    AccountName = accounts$name[ord],
    StartingBalance = money_comma(start[ord] / 100),
    LatestBalance = money_comma(latest[ord] / 100),
    NetChange = money_comma(change[ord] / 100),
    "GrowthRate (%)" = ifelse(is.na(rate[ord]), "N/A", money_comma(rate[ord])),
    GrowthStatus = status[ord],
    check.names = FALSE, stringsAsFactors = FALSE
  )
  write_report(df, REPORT4_FILE, "Report 4: Account Growth")
  table(factor(status, levels = c("Growing", "Stable", "Declining")))
}

write_summary <- function(txns, accounts, total_interest, growth_counts) {
  cents <- to_cents(txns$amount)
  values <- c(
    total_accounts = as.character(nrow(accounts)),
    total_deposits = money(sum(cents[txns$type == "deposit"]) / 100),
    total_withdrawals = money(sum(cents[txns$type == "withdrawal"]) / 100),
    total_balance = money(sum(to_cents(accounts$balance)) / 100),
    total_interest_expense = money(total_interest),
    growing_accounts = as.character(growth_counts[["Growing"]]),
    stable_accounts = as.character(growth_counts[["Stable"]]),
    declining_accounts = as.character(growth_counts[["Declining"]])
  )
  json <- paste0("{\n", paste0("  \"", names(values), "\": ", values, collapse = ",\n"), "\n}")
  writeLines(json, SUMMARY_FILE)
  cat("\nSummary State (summary.json):\n", json, "\n", sep = "")
}

generate_reports <- function(data) {
  txns <- data$txns
  if (nrow(txns) == 0) {
    cat(sprintf("No transactions to report on. Make sure %s is in this folder.\n", CSV_FILE))
    return(invisible(NULL))
  }
  accounts <- summarize_accounts(txns)

  cat("Generating reports...\n")
  cat("Outputs saved to individual files...\n")
  report1_privilege_tiers(txns, accounts)
  total_interest <- report2_interest_projection(accounts)
  report3_suspicious_behavior(txns, accounts)
  growth_counts <- report4_account_growth(accounts)
  write_summary(txns, accounts, total_interest, growth_counts)
}

reports_menu <- function() {
  data <- NULL
  repeat {
    cat("\nGenerate Reports\n")
    cat("[1] Load the file\n")
    cat("[2] Generate Reports\n")
    cat("[0] Back to the Main Menu\n\n")
    choice <- read_int("Enter choice: ", 0, 2)

    if (choice == 0) break
    if (choice == 1) {
      data <- load_dataset()
      print_load_summary(data)
    }
    if (choice == 2) {
      if (is.null(data)) {
        cat("File not loaded yet. Loading the file first...\n")
        data <- load_dataset()
        print_load_summary(data)
      }
      generate_reports(data)
    }
  }
}



main <- function() {
  rates <- list(PHP = 1.00, USD = 52.00, JPY = 0.40, GBP = 84.00, EUR = 72.00, CNY = 9.00)

  repeat {
    print_main_menu()
    choice <- read_int("Choice: ", 1, 7)

    if (choice == 1) register_accounts(rates)
    if (choice == 2) transact(rates, TRUE)
    if (choice == 3) transact(rates, FALSE)
    if (choice == 4) currency_exchange(rates)
    if (choice == 5) rates <- record_exchange_rate(rates)
    if (choice == 6) show_interest()
    if (choice == 7) reports_menu()

    if (choice != 4 && !read_yes_no("\nBack to the Main Menu (Y/N): ")) break
  }

  cat("\nThank you for using the Banking and Currency Exchange Application.\n")
}

options(width = 200)
main()
