/*
********************
Last names: ALCANTARA, ANGELES, CONSTANTINO, MATIBAG
Language: C
Paradigm(s): Procedural, Structured (imperative)
********************
*/

// Build and run (one command, requires GCC; put bank_transactions.csv in the same folder):
//   gcc -std=c99 -Wall -Wextra -pedantic MCO2_3_C.c -o MCO2_3_C && ./MCO2_3_C
//   (on Windows: gcc -std=c99 MCO2_3_C.c -o MCO2_3_C.exe && MCO2_3_C.exe)
// Optional non-interactive run that loads the file and generates every report:
//   ./MCO2_3_C --reports
// Optional: fix the reference date (default is today's date) so every language can be
// compared on the same day:   ./MCO2_3_C --reports --reference 27/11/2026

#include <limits.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <time.h>

/* ----------------------------- constants ------------------------------ */

#define CSV_FILE              "bank_transactions.csv"
#define CSV_HEADER            "account_name,account_number,date,time,currency,amount,type,current_balance"
#define FIRST_ACCOUNT_NUMBER  1000000001LL     /* used only when the file has no accounts */

#define CURRENCY_COUNT        6
#define NAME_LEN              100
#define LINE_LEN              256
#define CSV_LINE_LEN          1024

#define ANNUAL_INTEREST_PCT   5                /* 5% per annum                  */
#define DAYS_PER_YEAR         365
#define MAX_PROJECTION_DAYS   3650             /* sanity cap for option [6]     */

/* All money is kept as whole centavos in a long long (exact, no float drift). */
#define MIN_INITIAL_DEPOSIT   500000LL         /* 5,000.00 PHP  (REQ-0001)      */
#define MAX_TXN_CENTAVOS      100000000000LL   /* 1 billion PHP per transaction */
#define MAX_BALANCE_CENTAVOS  100000000000000LL/* 1 trillion PHP balance cap    */
#define MAX_RATE              1000000.0        /* PHP per 1 unit of currency    */
#define MAX_EXCHANGE_AMOUNT   1000000000.0

/* Report rules (all in centavos) */
#define ADB_MIN_CENTAVOS      10000000LL       /* Report 1 filter: ADB >= 100,000.00   */
#define SILVER_LIMIT          50000000LL       /* Silver  < 500,000.00                 */
#define GOLD_LIMIT            100000000LL      /* Gold    < 1,000,000.00               */
#define INTEREST_MIN_BALANCE  1000000LL        /* Report 2 filter: balance >= 10,000.00 */
#define INTEREST_DAYS         30               /* Report 2 horizon                     */
#define SUSPICIOUS_WITHDRAWAL 50000000LL       /* Report 3: withdrawals >= 500,000.00  */
#define SUSPICIOUS_VELOCITY   90               /* Report 3: turnover velocity >= 90 %  */
#define FLAG_WITHDRAWAL_ONLY_DAYS 0            /* 1 = a day with withdrawals but no deposits
                                                  (velocity undefined) is flagged Suspicious */
#define PREVIEW_ROWS          2

/* ------------------------------- types -------------------------------- */

typedef enum { PHP = 0, USD, JPY, GBP, EUR, CNY } CurrencyId;

typedef struct {
    const char *code;
    const char *name;
    double      rate;        /* PHP per 1 unit of this currency */
} Currency;

typedef struct {
    long long number;        /* 10-digit account number                    */
    char      name[NAME_LEN];
    long long balance;       /* latest balance, centavos                   */
    long long lastStamp;     /* seconds since 1970-01-01 of latest txn     */
    int       openDay;       /* day number of the first transaction        */
} Account;

typedef struct {
    int       acct;          /* index into Bank.accts                      */
    int       day;           /* days since 1970-01-01                      */
    int       sec;           /* seconds since midnight                     */
    int       isDeposit;
    long long amount;        /* centavos                                   */
    long long balance;       /* balance after the transaction, centavos    */
} Txn;

typedef enum {
    BAD_LONG, BAD_FIELDS, BAD_ACCOUNT, BAD_DATE, BAD_TIME,
    BAD_AMOUNT, BAD_TYPE, BAD_BALANCE, BAD_COUNT
} BadKind;

static const char *badNames[BAD_COUNT] = {
    "line too long", "missing/extra values", "invalid account number", "invalid date",
    "invalid time", "invalid amount", "invalid transaction type", "invalid balance"
};

typedef struct {
    Account  *accts;  int nAccts;  int capAccts;
    Txn      *txns;   int nTxns;   int capTxns;
    int      *hash;   int hashCap;               /* open addressing: acct index + 1 */

    bool      fileFound;
    long long rowsRead;                          /* data rows in the file          */
    long long rowsPhp;                           /* rows whose currency is PHP     */
    long long rowsOther;                         /* rows excluded: not PHP         */
    long long bad[BAD_COUNT];                    /* PHP rows removed, by reason    */
    long long balanceMismatch;                   /* chain check warnings           */
    char      issues[5][96];                     /* first few problems, for the log*/
    int       nIssues;
} Bank;

typedef struct { int *ord; int *start; } Index;  /* txns sorted by (account, time) */

typedef struct {
    int year, month, days;
    int startDay, endDay;
    int today;               /* the reference date (day number) */
} MonthInfo;

typedef struct { FILE *fp; int shown; } Out;

/* ------------------------------ utilities ------------------------------ */

static void *xrealloc(void *p, size_t size)
{
    void *q = realloc(p, size > 0 ? size : 1);
    if (q == NULL) {
        fprintf(stderr, "Out of memory.\n");
        exit(1);
    }
    return q;
}

static void *xcalloc(size_t n, size_t size)
{
    void *p = calloc(n > 0 ? n : 1, size);
    if (p == NULL) {
        fprintf(stderr, "Out of memory.\n");
        exit(1);
    }
    return p;
}

static void endOfInput(void)
{
    printf("\n\nInput closed. Exiting program.\n");
    exit(0);
}

static void trim(char *s)
{
    size_t len = strlen(s);
    size_t start = 0;

    while (len > 0 && isspace((unsigned char)s[len - 1])) {
        s[--len] = '\0';
    }
    while (s[start] != '\0' && isspace((unsigned char)s[start])) {
        start++;
    }
    if (start > 0) {
        memmove(s, s + start, strlen(s + start) + 1);
    }
}

static bool ciEqual(const char *a, const char *b)
{
    while (*a != '\0' && *b != '\0') {
        if (tolower((unsigned char)*a) != tolower((unsigned char)*b)) {
            return false;
        }
        a++;
        b++;
    }
    return *a == *b;
}

/* "1234567" -> "1,234,567" (handles negatives) */
static void formatGrouped(char *out, size_t size, long long n)
{
    char digits[32];
    char tmp[48];
    int len = snprintf(digits, sizeof digits, "%lld", n < 0 ? -n : n);
    int pos = 0;

    if (n < 0) {
        tmp[pos++] = '-';
    }
    for (int i = 0; i < len; i++) {
        if (i > 0 && (len - i) % 3 == 0) {
            tmp[pos++] = ',';
        }
        tmp[pos++] = digits[i];
    }
    tmp[pos] = '\0';
    snprintf(out, size, "%s", tmp);
}

/* plain "12345.67" (used in the CSV file and in summary.json) */
static void formatCentavos(char *out, size_t size, long long centavos)
{
    long long a = centavos < 0 ? -centavos : centavos;
    snprintf(out, size, "%s%lld.%02lld", centavos < 0 ? "-" : "", a / 100, a % 100);
}

/* comma-formatted "1,234,567.89" (REQ-0012 report output) */
static void formatMoneyGrouped(char *out, size_t size, long long centavos)
{
    char whole[32];
    long long a = centavos < 0 ? -centavos : centavos;

    formatGrouped(whole, sizeof whole, a / 100);
    snprintf(out, size, "%s%s.%02lld", centavos < 0 ? "-" : "", whole, a % 100);
}

/* ---------------------------- calendar helpers ------------------------- */

static bool isLeap(int y)
{
    return (y % 4 == 0 && y % 100 != 0) || y % 400 == 0;
}

static int daysInMonth(int y, int m)
{
    static const int dm[12] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    return (m == 2 && isLeap(y)) ? 29 : dm[m - 1];
}

/* days since 1970-01-01 (proleptic Gregorian) */
static int daysFromCivil(int y, int m, int d)
{
    int era;
    unsigned yoe, doy, doe;

    y -= (m <= 2);
    era = (y >= 0 ? y : y - 399) / 400;
    yoe = (unsigned)(y - era * 400);
    doy = (unsigned)((153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1);
    doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    return era * 146097 + (int)doe - 719468;
}

static void civilFromDays(int z, int *y, int *m, int *d)
{
    int era;
    unsigned doe, yoe, doy, mp;

    z += 719468;
    era = (z >= 0 ? z : z - 146096) / 146097;
    doe = (unsigned)(z - era * 146097);
    yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
    doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    mp = (5 * doy + 2) / 153;
    *d = (int)(doy - (153 * mp + 2) / 5 + 1);
    *m = (int)(mp < 10 ? mp + 3 : mp - 9);
    *y = (int)yoe + era * 400 + (*m <= 2);
}

/* The reference date is today's date, unless --reference dd/mm/yyyy was given. */
static int g_referenceDay = INT_MIN;

static int referenceDay(void)
{
    time_t now;
    struct tm *lt;

    if (g_referenceDay != INT_MIN) {
        return g_referenceDay;
    }
    now = time(NULL);
    lt = localtime(&now);
    return daysFromCivil(lt->tm_year + 1900, lt->tm_mon + 1, lt->tm_mday);
}

/* ------------------------------ text parsing --------------------------- */

static bool parseUInt(const char **pp, int minDigits, int maxDigits, int *out)
{
    const char *p = *pp;
    int n = 0;
    int v = 0;

    while (isdigit((unsigned char)*p)) {
        if (n >= maxDigits) {
            return false;
        }
        v = v * 10 + (*p - '0');
        p++;
        n++;
    }
    if (n < minDigits) {
        return false;
    }
    *pp = p;
    *out = v;
    return true;
}

/* dd/mm/yyyy (1 or 2 digit day and month, 4 digit year) with real calendar check */
static bool parseDateStr(const char *s, int *day)
{
    const char *p = s;
    int d, m, y;

    if (!parseUInt(&p, 1, 2, &d) || *p++ != '/') {
        return false;
    }
    if (!parseUInt(&p, 1, 2, &m) || *p++ != '/') {
        return false;
    }
    if (!parseUInt(&p, 4, 4, &y) || *p != '\0') {
        return false;
    }
    if (y < 1900 || y > 2100 || m < 1 || m > 12 || d < 1 || d > daysInMonth(y, m)) {
        return false;
    }
    *day = daysFromCivil(y, m, d);
    return true;
}

/* hh:mm:ss, 24-hour (the hour may have 1 or 2 digits, e.g. 8:22:00) */
static bool parseTimeStr(const char *s, int *sec)
{
    const char *p = s;
    int h, m, c;

    if (!parseUInt(&p, 1, 2, &h) || *p++ != ':') {
        return false;
    }
    if (!parseUInt(&p, 2, 2, &m) || *p++ != ':') {
        return false;
    }
    if (!parseUInt(&p, 2, 2, &c) || *p != '\0') {
        return false;
    }
    if (h > 23 || m > 59 || c > 59) {
        return false;
    }
    *sec = h * 3600 + m * 60 + c;
    return true;
}

/* Digits with at most one '.', to whole centavos.  Extra decimals are rounded
 * half up (data cleaning); strict2 rejects more than 2 decimals (user input). */
static bool parseMoney(const char *s, bool strict2, bool allowZero, long long *out)
{
    long long whole = 0;
    int wholeDigits = 0;
    int frac[3] = { 0, 0, 0 };
    int fracDigits = 0;
    bool dot = false;
    long long c;

    for (const char *p = s; *p != '\0'; p++) {
        if (isdigit((unsigned char)*p)) {
            if (!dot) {
                if (++wholeDigits > 13) {
                    return false;
                }
                whole = whole * 10 + (*p - '0');
            } else {
                if (fracDigits < 3) {
                    frac[fracDigits] = *p - '0';
                }
                fracDigits++;
            }
        } else if (*p == '.' && !dot) {
            dot = true;
        } else {
            return false;
        }
    }
    if (wholeDigits == 0 && fracDigits == 0) {
        return false;
    }
    if (strict2 && fracDigits > 2) {
        return false;
    }
    c = whole * 100 + frac[0] * 10 + frac[1];
    if (fracDigits >= 3 && frac[2] >= 5) {
        c++;
    }
    if (!allowZero && c == 0) {
        return false;
    }
    *out = c;
    return true;
}

/* Splits a line on commas in place.  Returns the real field count (may exceed max). */
static int splitFields(char *line, char *fields[], int maxFields)
{
    int n = 0;
    char *p = line;

    for (;;) {
        char *comma = strchr(p, ',');

        if (n < maxFields) {
            fields[n] = p;
        }
        n++;
        if (comma == NULL) {
            break;
        }
        *comma = '\0';
        p = comma + 1;
    }
    for (int i = 0; i < n && i < maxFields; i++) {
        trim(fields[i]);
    }
    return n;
}

/* ------------------------- keyboard input helpers ---------------------- */

static void readLine(const char *prompt, char *buf, size_t size)
{
    size_t len;

    printf("%s", prompt);
    fflush(stdout);

    if (fgets(buf, (int)size, stdin) == NULL) {
        endOfInput();
    }

    len = strlen(buf);
    if (len > 0 && buf[len - 1] == '\n') {
        buf[len - 1] = '\0';
    } else {
        int ch;                       /* line was too long: discard the rest */
        while ((ch = getchar()) != '\n' && ch != EOF) {
            ;
        }
    }
    trim(buf);
}

static bool parseWholeNumber(const char *s, long *out)
{
    char *end;

    if (*s == '\0') {
        return false;
    }
    for (const char *p = s; *p != '\0'; p++) {
        if (!isdigit((unsigned char)*p)) {
            return false;
        }
    }
    if (strlen(s) > 9) {              /* keeps the value safely inside long */
        return false;
    }
    *out = strtol(s, &end, 10);
    return true;
}

/* Accepts only digits with at most one '.', e.g. 500, 500.00, .5, 52. (rates) */
static bool parsePositiveDecimal(const char *s, double *out)
{
    int digits = 0;
    int dots = 0;

    for (const char *p = s; *p != '\0'; p++) {
        if (isdigit((unsigned char)*p)) {
            digits++;
        } else if (*p == '.') {
            dots++;
        } else {
            return false;
        }
    }
    if (digits == 0 || dots > 1) {
        return false;
    }
    *out = strtod(s, NULL);
    return *out > 0.0;
}

static int promptInt(const char *prompt, int min, int max)
{
    char buf[LINE_LEN];
    long value;

    for (;;) {
        readLine(prompt, buf, sizeof buf);
        if (parseWholeNumber(buf, &value) && value >= min && value <= max) {
            return (int)value;
        }
        printf("Invalid input. Enter a whole number from %d to %d.\n", min, max);
    }
}

static double promptAmount(const char *prompt, double max)
{
    char buf[LINE_LEN];
    double value;

    for (;;) {
        readLine(prompt, buf, sizeof buf);
        if (parsePositiveDecimal(buf, &value) && value <= max) {
            return value;
        }
        printf("Invalid input. Enter a number greater than 0 (up to %.0f).\n", max);
    }
}

/* PHP amount with at most 2 decimals, returned in centavos */
static long long promptMoney(const char *prompt, long long minC, long long maxC)
{
    char buf[LINE_LEN];
    char lo[48];
    char hi[48];
    long long value;

    for (;;) {
        readLine(prompt, buf, sizeof buf);
        if (parseMoney(buf, true, false, &value) && value >= minC && value <= maxC) {
            return value;
        }
        formatMoneyGrouped(lo, sizeof lo, minC);
        formatMoneyGrouped(hi, sizeof hi, maxC);
        printf("Invalid input. Enter an amount from %s to %s (at most 2 decimal places).\n", lo, hi);
    }
}

static bool promptYesNo(const char *prompt)
{
    char buf[LINE_LEN];

    for (;;) {
        readLine(prompt, buf, sizeof buf);
        if (strlen(buf) == 1 && (toupper((unsigned char)buf[0]) == 'Y' ||
                                 toupper((unsigned char)buf[0]) == 'N')) {
            return toupper((unsigned char)buf[0]) == 'Y';
        }
        printf("Invalid input. Enter Y or N.\n");
    }
}

/* ------------------------------ interest ------------------------------- */

/* Daily interest in centavos, rounded half up:
 * balance x (ANNUAL_INTEREST_PCT / 100) / DAYS_PER_YEAR, in integer math. */
static long long dailyInterest(long long balanceCentavos)
{
    const long long divisor = 100LL * DAYS_PER_YEAR;
    return (balanceCentavos * ANNUAL_INTEREST_PCT + divisor / 2) / divisor;
}

/* Interest earned over `days` days with daily compounding (same rule as the table in [6]) */
static long long projectInterest(long long balance, int days)
{
    long long b = balance;

    for (int d = 0; d < days; d++) {
        b += dailyInterest(b);
    }
    return b - balance;
}

/* ------------------------------- the bank ------------------------------ */

static void bankClear(Bank *b)
{
    free(b->accts);
    free(b->txns);
    free(b->hash);
    memset(b, 0, sizeof *b);
}

static unsigned long hashNumber(long long n)
{
    unsigned long long x = (unsigned long long)n * 11400714819323198485ULL;
    return (unsigned long)(x >> 32);
}

static void hashInsert(Bank *b, int idx)
{
    unsigned long h = hashNumber(b->accts[idx].number) & (unsigned long)(b->hashCap - 1);

    while (b->hash[h] != 0) {
        h = (h + 1) & (unsigned long)(b->hashCap - 1);
    }
    b->hash[h] = idx + 1;
}

static int findAccount(const Bank *b, long long number)
{
    unsigned long h;

    if (b->hashCap == 0) {
        return -1;
    }
    h = hashNumber(number) & (unsigned long)(b->hashCap - 1);
    while (b->hash[h] != 0) {
        int idx = b->hash[h] - 1;
        if (b->accts[idx].number == number) {
            return idx;
        }
        h = (h + 1) & (unsigned long)(b->hashCap - 1);
    }
    return -1;
}

static int findAccountByName(const Bank *b, const char *name)
{
    for (int i = 0; i < b->nAccts; i++) {
        if (ciEqual(b->accts[i].name, name)) {
            return i;
        }
    }
    return -1;
}

static void rebuildHash(Bank *b, int cap)
{
    free(b->hash);
    b->hashCap = cap;
    b->hash = xcalloc((size_t)cap, sizeof(int));
    for (int i = 0; i < b->nAccts; i++) {
        hashInsert(b, i);
    }
}

static int addAccount(Bank *b, long long number, const char *name)
{
    int idx;

    if (b->nAccts == b->capAccts) {
        b->capAccts = b->capAccts > 0 ? b->capAccts * 2 : 256;
        b->accts = xrealloc(b->accts, (size_t)b->capAccts * sizeof(Account));
    }
    if ((b->nAccts + 1) * 2 > b->hashCap) {
        rebuildHash(b, b->hashCap > 0 ? b->hashCap * 2 : 512);
    }
    idx = b->nAccts++;
    memset(&b->accts[idx], 0, sizeof(Account));
    b->accts[idx].number = number;
    snprintf(b->accts[idx].name, NAME_LEN, "%s", name);
    hashInsert(b, idx);
    return idx;
}

static void addTxn(Bank *b, Txn t)
{
    if (b->nTxns == b->capTxns) {
        b->capTxns = b->capTxns > 0 ? b->capTxns * 2 : 4096;
        b->txns = xrealloc(b->txns, (size_t)b->capTxns * sizeof(Txn));
    }
    b->txns[b->nTxns++] = t;
}

/* ---------------------- ordering: (account, day, time, row) ------------- */

static const Txn  *g_txns;
static const Bank *g_bank;

static int cmpOrder(const void *x, const void *y)
{
    int i = *(const int *)x;
    int j = *(const int *)y;
    const Txn *a = &g_txns[i];
    const Txn *b = &g_txns[j];

    if (a->acct != b->acct) {
        return a->acct < b->acct ? -1 : 1;
    }
    if (a->day != b->day) {
        return a->day < b->day ? -1 : 1;
    }
    if (a->sec != b->sec) {
        return a->sec < b->sec ? -1 : 1;
    }
    return (i > j) - (i < j);         /* same second: keep file order */
}

static Index buildIndex(const Bank *b)
{
    Index ix;

    ix.ord = xcalloc((size_t)b->nTxns, sizeof(int));
    ix.start = xcalloc((size_t)b->nAccts + 1, sizeof(int));
    for (int i = 0; i < b->nTxns; i++) {
        ix.ord[i] = i;
    }
    g_txns = b->txns;
    qsort(ix.ord, (size_t)b->nTxns, sizeof(int), cmpOrder);
    for (int i = 0; i < b->nTxns; i++) {
        ix.start[b->txns[i].acct + 1]++;
    }
    for (int a = 0; a < b->nAccts; a++) {
        ix.start[a + 1] += ix.start[a];
    }
    return ix;
}

static void freeIndex(Index *ix)
{
    free(ix->ord);
    free(ix->start);
    ix->ord = NULL;
    ix->start = NULL;
}

/* latest balance, latest timestamp and opening day of every account */
static void summarizeAccounts(Bank *b)
{
    Index ix = buildIndex(b);

    for (int a = 0; a < b->nAccts; a++) {
        if (ix.start[a] < ix.start[a + 1]) {
            const Txn *first = &b->txns[ix.ord[ix.start[a]]];
            const Txn *last = &b->txns[ix.ord[ix.start[a + 1] - 1]];

            b->accts[a].openDay = first->day;
            b->accts[a].balance = last->balance;
            b->accts[a].lastStamp = (long long)last->day * 86400 + last->sec;
        }
    }
    freeIndex(&ix);
}

/* -------------------- REQ-0003/4/5: reading and cleaning the CSV -------- */

static bool readCsvLine(FILE *fp, char *buf, size_t size, bool *tooLong)
{
    size_t len;

    *tooLong = false;
    if (fgets(buf, (int)size, fp) == NULL) {
        return false;
    }
    len = strlen(buf);
    if (len > 0 && buf[len - 1] == '\n') {
        buf[len - 1] = '\0';
    } else if (!feof(fp)) {
        int ch;                       /* line longer than the buffer: skip the rest */
        *tooLong = true;
        while ((ch = fgetc(fp)) != '\n' && ch != EOF) {
            ;
        }
    }
    return true;
}

static void noteIssue(Bank *b, BadKind kind, long lineNo, const char *detail)
{
    b->bad[kind]++;
    if (b->nIssues < 5) {
        snprintf(b->issues[b->nIssues++], sizeof b->issues[0], "line %ld: %s (%.40s)",
                 lineNo, badNames[kind], detail);
    }
}

static void loadDataset(Bank *b)
{
    FILE *fp;
    char line[CSV_LINE_LEN];
    char raw[CSV_LINE_LEN];
    bool tooLong;
    bool first = true;
    long lineNo = 0;
    long long *prev;

    bankClear(b);
    fp = fopen(CSV_FILE, "r");
    if (fp == NULL) {
        return;
    }
    b->fileFound = true;

    while (readCsvLine(fp, line, sizeof line, &tooLong)) {
        char *f[8];
        int n;
        long long number = 0;
        long long amount = 0;
        long long balance = 0;
        int day = 0;
        int sec = 0;
        int isDeposit = 0;
        int idx;
        Txn t;

        lineNo++;
        trim(line);
        if (line[0] == '\0') {
            continue;
        }
        snprintf(raw, sizeof raw, "%s", line);
        n = splitFields(line, f, 8);

        if (first) {                              /* skip the header row */
            first = false;
            if (ciEqual(f[0], "account_name")) {
                continue;
            }
        }
        b->rowsRead++;

        if (tooLong) {
            noteIssue(b, BAD_LONG, lineNo, "row skipped");
            continue;
        }
        if (n != 8) {
            noteIssue(b, BAD_FIELDS, lineNo, raw);
            continue;
        }
        {
            bool missing = false;
            for (int i = 0; i < 8; i++) {
                if (f[i][0] == '\0') {
                    missing = true;
                }
            }
            if (missing) {
                noteIssue(b, BAD_FIELDS, lineNo, raw);
                continue;
            }
        }

        /* REQ-0003: keep Philippine currency only */
        if (!ciEqual(f[4], "PHP")) {
            b->rowsOther++;
            continue;
        }
        b->rowsPhp++;

        /* REQ-0004/5: validate and convert every field; rows that fail are filtered out */
        if (strlen(f[1]) != 10 || strlen(f[0]) >= NAME_LEN) {
            noteIssue(b, BAD_ACCOUNT, lineNo, f[1]);
            continue;
        }
        {
            bool digitsOnly = true;
            for (const char *p = f[1]; *p != '\0'; p++) {
                if (!isdigit((unsigned char)*p)) {
                    digitsOnly = false;
                }
            }
            if (!digitsOnly) {
                noteIssue(b, BAD_ACCOUNT, lineNo, f[1]);
                continue;
            }
            number = strtoll(f[1], NULL, 10);
        }
        if (!parseDateStr(f[2], &day)) {
            noteIssue(b, BAD_DATE, lineNo, f[2]);
            continue;
        }
        if (!parseTimeStr(f[3], &sec)) {
            noteIssue(b, BAD_TIME, lineNo, f[3]);
            continue;
        }
        if (!parseMoney(f[5], false, false, &amount)) {
            noteIssue(b, BAD_AMOUNT, lineNo, f[5]);
            continue;
        }
        if (ciEqual(f[6], "deposit")) {
            isDeposit = 1;
        } else if (!ciEqual(f[6], "withdrawal")) {
            noteIssue(b, BAD_TYPE, lineNo, f[6]);
            continue;
        }
        if (!parseMoney(f[7], false, true, &balance)) {
            noteIssue(b, BAD_BALANCE, lineNo, f[7]);
            continue;
        }

        idx = findAccount(b, number);
        if (idx < 0) {
            idx = addAccount(b, number, f[0]);
        }
        t.acct = idx;
        t.day = day;
        t.sec = sec;
        t.isDeposit = isDeposit;
        t.amount = amount;
        t.balance = balance;
        addTxn(b, t);
    }
    fclose(fp);

    summarizeAccounts(b);

    /* extra check: every row's balance should equal previous balance +/- amount */
    prev = xcalloc((size_t)b->nAccts, sizeof(long long));
    for (int i = 0; i < b->nTxns; i++) {
        const Txn *t = &b->txns[i];
        long long expected = prev[t->acct] + (t->isDeposit ? t->amount : -t->amount);

        if (expected != t->balance) {
            b->balanceMismatch++;
        }
        prev[t->acct] = t->balance;
    }
    free(prev);
}

static void printLoadSummary(const Bank *b)
{
    char x[48];
    char y[48];
    long long removed = 0;

    for (int k = 0; k < BAD_COUNT; k++) {
        removed += b->bad[k];
    }

    if (!b->fileFound) {
        printf("Processing dataset... could not open %s.\n", CSV_FILE);
        printf("Place the file in the same folder as the program, or register an account to create it.\n");
        return;
    }
    formatGrouped(x, sizeof x, b->rowsRead);
    formatGrouped(y, sizeof y, b->rowsPhp);
    printf("Processing dataset... (%s rows loaded, %s filtered for PHP currency)\n", x, y);

    formatGrouped(x, sizeof x, b->rowsOther);
    printf("Validation: %s rows excluded (not PHP)", x);
    formatGrouped(x, sizeof x, removed);
    printf(", %s rows removed because of errors", x);
    formatGrouped(x, sizeof x, b->nTxns);
    formatGrouped(y, sizeof y, b->nAccts);
    printf(".\nClean rows kept: %s across %s accounts.\n", x, y);

    for (int k = 0; k < BAD_COUNT; k++) {
        if (b->bad[k] > 0) {
            formatGrouped(x, sizeof x, b->bad[k]);
            printf("  - %s: %s\n", badNames[k], x);
        }
    }
    for (int i = 0; i < b->nIssues; i++) {
        printf("  e.g. %s\n", b->issues[i]);
    }
    if (b->balanceMismatch > 0) {
        formatGrouped(x, sizeof x, b->balanceMismatch);
        printf("  warning: %s rows whose balance does not match previous balance +/- amount\n", x);
    }
}

/* -------------------- REQ-0001/0002: saving to the CSV ------------------ */

static bool appendCsvRow(const char *name, long long number, int day, int sec,
                         bool isDeposit, long long amount, long long balance)
{
    FILE *fp;
    bool empty = true;
    bool needNewline = false;
    const char *eol = "\n";                 /* follow the file's own style (LF or CRLF) */
    char amountStr[48];
    char balanceStr[48];
    int y, m, d;

    fp = fopen(CSV_FILE, "rb");
    if (fp != NULL) {
        char head[2048];
        size_t got = fread(head, 1, sizeof head, fp);

        for (size_t i = 0; i < got; i++) {
            if (head[i] == '\n') {
                if (i > 0 && head[i - 1] == '\r') {
                    eol = "\r\n";
                }
                break;
            }
        }
        if (fseek(fp, 0, SEEK_END) == 0 && ftell(fp) > 0) {
            empty = false;
            if (fseek(fp, -1, SEEK_END) == 0) {
                needNewline = (fgetc(fp) != '\n');
            }
        }
        fclose(fp);
    }

    fp = fopen(CSV_FILE, "ab");
    if (fp == NULL) {
        return false;
    }
    if (empty) {
        fprintf(fp, "%s%s", CSV_HEADER, eol);
    } else if (needNewline) {
        fputs(eol, fp);
    }
    civilFromDays(day, &y, &m, &d);
    formatCentavos(amountStr, sizeof amountStr, amount);
    formatCentavos(balanceStr, sizeof balanceStr, balance);
    fprintf(fp, "%s,%lld,%02d/%02d/%04d,%02d:%02d:%02d,PHP,%s,%s,%s%s",
            name, number, d, m, y, sec / 3600, (sec / 60) % 60, sec % 60,
            amountStr, isDeposit ? "deposit" : "withdrawal", balanceStr, eol);
    return fclose(fp) == 0;
}

/* Records one deposit/withdrawal: writes the CSV row first, then updates memory. */
static bool recordTransaction(Bank *b, int acct, bool isDeposit, long long amount)
{
    Account *a = &b->accts[acct];
    long long newBalance = a->balance + (isDeposit ? amount : -amount);
    long long stamp;
    time_t now = time(NULL);
    struct tm *lt = localtime(&now);
    Txn t;

    stamp = (long long)daysFromCivil(lt->tm_year + 1900, lt->tm_mon + 1, lt->tm_mday) * 86400
            + lt->tm_hour * 3600 + lt->tm_min * 60 + lt->tm_sec;
    if (stamp < a->lastStamp) {       /* never place a row before the account's last one */
        stamp = a->lastStamp;
    }
    t.acct = acct;
    t.day = (int)(stamp / 86400);
    t.sec = (int)(stamp % 86400);
    t.isDeposit = isDeposit;
    t.amount = amount;
    t.balance = newBalance;

    if (!appendCsvRow(a->name, a->number, t.day, t.sec, isDeposit, amount, newBalance)) {
        printf("Could not write to %s. Transaction cancelled.\n", CSV_FILE);
        return false;
    }
    addTxn(b, t);
    a->balance = newBalance;
    a->lastStamp = stamp;
    if (a->openDay == 0) {                /* first transaction of a new account */
        a->openDay = t.day;
    }
    return true;
}

/* ---------------------------- menu helpers ----------------------------- */

static void printMainMenu(void)
{
    printf("\nSelect Transaction:\n");
    printf("[1] Register Account\n");
    printf("[2] Deposit Amount\n");
    printf("[3] Withdraw Amount\n");
    printf("[4] Currency Exchange\n");
    printf("[5] Record Exchange Rates\n");
    printf("[6] Show Interest Amount\n");
    printf("[7] Generate Reports\n");
    printf("[0] Exit\n\n");
}

static void printCurrencyMenu(const Currency currencies[])
{
    for (int i = 0; i < CURRENCY_COUNT; i++) {
        printf("[%d] %s (%s)\n", i + 1, currencies[i].name, currencies[i].code);
    }
    printf("\n");
}

static CurrencyId promptCurrency(const char *prompt)
{
    return (CurrencyId)(promptInt(prompt, 1, CURRENCY_COUNT) - 1);
}

static void printAccountHeader(const Account *acc)
{
    char money[48];

    formatMoneyGrouped(money, sizeof money, acc->balance);
    printf("Account Name: %s\n", acc->name);
    printf("Account Number: %lld\n", acc->number);
    printf("Current Balance: %s\n", money);
    printf("Currency: PHP\n");
}

/* Asks for an account number or name; returns its index, or -1 if cancelled. */
static int selectAccount(const Bank *b)
{
    char buf[LINE_LEN];

    if (b->nAccts == 0) {
        printf("No accounts yet. Please use [1] Register Account first.\n");
        return -1;
    }
    for (;;) {
        bool digitsOnly = true;
        int idx;

        readLine("Account Number or Name (blank to cancel): ", buf, sizeof buf);
        if (buf[0] == '\0') {
            return -1;
        }
        for (const char *p = buf; *p != '\0'; p++) {
            if (!isdigit((unsigned char)*p)) {
                digitsOnly = false;
            }
        }
        if (digitsOnly && strlen(buf) <= 18) {
            idx = findAccount(b, strtoll(buf, NULL, 10));
        } else {
            idx = findAccountByName(b, buf);
        }
        if (idx >= 0) {
            return idx;
        }
        printf("Account not found.\n");
    }
}

/* Every deposit/withdrawal can be made in any currency; it is always converted to PHP
 * (using the recorded rate) and the PHP amount is what gets saved. Returns centavos. */
static long long promptPhpAmount(const Currency currencies[], const char *label, long long minC)
{
    char prompt[LINE_LEN];
    char lo[48];
    CurrencyId id;

    printf("\nTransaction Currency Options:\n");
    printCurrencyMenu(currencies);
    id = promptCurrency("Transaction Currency: ");
    snprintf(prompt, sizeof prompt, "%s in %s: ", label, currencies[id].code);

    for (;;) {
        long long centavos;

        if (id == PHP) {
            return promptMoney(prompt, minC, MAX_TXN_CENTAVOS);
        }
        centavos = (long long)(promptAmount(prompt, MAX_EXCHANGE_AMOUNT) * currencies[id].rate * 100.0 + 0.5);
        if (centavos < 1) {
            printf("Amount is too small (less than 0.01 PHP).\n");
            continue;
        }
        if (centavos > MAX_TXN_CENTAVOS) {
            printf("Amount is too large for one transaction.\n");
            continue;
        }
        {
            char money[48];
            formatMoneyGrouped(money, sizeof money, centavos);
            printf("Converted Amount: %s PHP (1 %s = %.4f PHP)\n", money, currencies[id].code,
                   currencies[id].rate);
        }
        if (centavos < minC) {
            formatMoneyGrouped(lo, sizeof lo, minC);
            printf("The amount must be at least %s PHP.\n", lo);
            continue;
        }
        return centavos;
    }
}

/* ------------------------- feature: [1] register ----------------------- */

/* "First name Last name": at least two words, letters and . - ' only, no duplicates */
static bool validName(const Bank *b, char *name)
{
    size_t out = 0;
    int words = 0;
    bool inWord = false;

    for (size_t i = 0; name[i] != '\0'; i++) {      /* collapse repeated spaces */
        if (isspace((unsigned char)name[i])) {
            if (out > 0 && name[out - 1] != ' ') {
                name[out++] = ' ';
            }
        } else {
            name[out++] = name[i];
        }
    }
    name[out] = '\0';
    trim(name);

    if (name[0] == '\0') {
        printf("Account name cannot be empty.\n");
        return false;
    }
    if (strlen(name) >= NAME_LEN) {
        printf("Account name is too long (max %d characters).\n", NAME_LEN - 1);
        return false;
    }
    for (const char *p = name; *p != '\0'; p++) {
        unsigned char c = (unsigned char)*p;
        if (c == ' ') {
            inWord = false;
            continue;
        }
        if (!(isalpha(c) || c >= 128 || c == '.' || c == '-' || c == '\'')) {
            printf("Account name may only contain letters, spaces, '.', '-' and '''.\n");
            return false;
        }
        if (!inWord) {
            words++;
            inWord = true;
        }
    }
    if (words < 2) {
        printf("Please enter the first name and the last name (e.g. Juan Dela Cruz).\n");
        return false;
    }
    if (findAccountByName(b, name) >= 0) {
        printf("An account with that name already exists.\n");
        return false;
    }
    return true;
}

static void registerAccounts(Bank *b, const Currency currencies[])
{
    do {
        char name[LINE_LEN];
        char money[48];
        long long initial;
        long long number = FIRST_ACCOUNT_NUMBER;
        int idx;

        printf("\nRegister Account\n");
        do {
            readLine("Account Name: ", name, sizeof name);
        } while (!validName(b, name));

        initial = promptPhpAmount(currencies, "Initial Deposit", MIN_INITIAL_DEPOSIT);

        for (int i = 0; i < b->nAccts; i++) {       /* next number after the highest one */
            if (b->accts[i].number >= number) {
                number = b->accts[i].number + 1;
            }
        }
        idx = addAccount(b, number, name);
        if (!recordTransaction(b, idx, true, initial)) {
            b->nAccts--;                              /* roll back: the file write failed */
            rebuildHash(b, b->hashCap);
            continue;
        }
        formatMoneyGrouped(money, sizeof money, b->accts[idx].balance);
        printf("Account created successfully!\n");
        printf("Account Number: %lld\n", b->accts[idx].number);
        printf("Current Balance: %s\n", money);
    } while (promptYesNo("\nRegister another account? (Y/N): "));
}

/* ----------------------- feature: [2] deposit, [3] withdraw ------------- */

static void transact(Bank *b, const Currency currencies[], bool isDeposit)
{
    int idx = -1;

    do {
        const char *title = isDeposit ? "Deposit Amount" : "Withdraw Amount";
        char money[48];
        long long amount;
        Account *acc;

        printf("\n%s\n", title);
        idx = selectAccount(b);
        if (idx < 0) {
            return;
        }
        acc = &b->accts[idx];
        printAccountHeader(acc);

        if (!isDeposit && acc->balance == 0) {
            printf("Account has no balance!\n");
            continue;
        }
        amount = promptPhpAmount(currencies, title, 1);

        if (isDeposit && acc->balance + amount > MAX_BALANCE_CENTAVOS) {
            printf("Deposit would exceed the maximum allowed balance. Transaction cancelled.\n");
            continue;
        }
        if (!isDeposit && amount > acc->balance) {
            printf("Insufficient balance. Transaction cancelled.\n");
            continue;
        }
        if (recordTransaction(b, idx, isDeposit, amount)) {
            formatMoneyGrouped(money, sizeof money, acc->balance);
            printf("Updated Balance: %s\n", money);
        }
    } while (promptYesNo(isDeposit ? "\nDeposit another amount? (Y/N): "
                                   : "\nWithdraw another amount? (Y/N): "));
}

/* ----------------------- features from MCO1: [4], [5], [6] -------------- */

static void currencyExchange(const Currency currencies[])
{
    do {
        CurrencyId src;
        CurrencyId dst;
        double amount;
        double result;

        printf("\nForeign Currency Exchange\n");
        printf("Source Currency Option:\n");
        printCurrencyMenu(currencies);
        src = promptCurrency("Source Currency: ");
        amount = promptAmount("Source Amount: ", MAX_EXCHANGE_AMOUNT);

        printf("\nExchanged Currency Options:\n");
        printCurrencyMenu(currencies);
        dst = promptCurrency("Exchange Currency: ");

        if (src == dst) {
            printf("Source and exchange currency are the same (%s). Nothing to convert.\n",
                   currencies[src].code);
        } else {
            result = amount * currencies[src].rate / currencies[dst].rate;
            printf("Exchange Amount: %.2f\n", result);
        }
    } while (promptYesNo("\nConvert another currency (Y/N)? "));
}

static void recordExchangeRates(Currency currencies[])
{
    CurrencyId id;

    printf("\nRecord Exchange Rate\n\n");
    printCurrencyMenu(currencies);
    id = promptCurrency("Select Foreign Currency: ");

    if (id == PHP) {
        printf("PHP is the base currency; its rate is fixed at 1.00 and cannot be changed.\n");
        return;
    }
    printf("Enter how many PHP equal 1 %s.\n", currencies[id].code);
    currencies[id].rate = promptAmount("Exchange Rate: ", MAX_RATE);
    printf("Recorded: 1 %s = %.4f PHP\n", currencies[id].code, currencies[id].rate);
}

/* projection only: the real balance is not modified */
static void showInterest(const Bank *b)
{
    long long balance;
    long long interest;
    int days;
    int idx;
    char interestStr[48];
    char balanceStr[48];

    printf("\nShow Interest Amount\n");
    idx = selectAccount(b);
    if (idx < 0) {
        return;
    }
    printAccountHeader(&b->accts[idx]);
    printf("Interest Rate: %d%%\n\n", ANNUAL_INTEREST_PCT);

    days = promptInt("Total Number of Days: ", 1, MAX_PROJECTION_DAYS);

    printf("\nDay  | Interest | Balance      |\n");
    balance = b->accts[idx].balance;
    for (int day = 1; day <= days; day++) {
        interest = dailyInterest(balance);
        balance += interest;
        formatCentavos(interestStr, sizeof interestStr, interest);
        formatCentavos(balanceStr, sizeof balanceStr, balance);
        printf("%-4d | %-8s | %-12s |\n", day, interestStr, balanceStr);
    }
}

/* ------------------------------ report output -------------------------- */

static void csvWriteField(FILE *fp, const char *s)
{
    if (strpbrk(s, ",\"\n") == NULL) {
        fputs(s, fp);
        return;
    }
    fputc('"', fp);
    for (; *s != '\0'; s++) {
        if (*s == '"') {
            fputc('"', fp);
        }
        fputc(*s, fp);
    }
    fputc('"', fp);
}

static bool outOpen(Out *o, const char *path)
{
    o->shown = 0;
    o->fp = fopen(path, "w");
    if (o->fp == NULL) {
        printf("Could not create %s.\n", path);
        return false;
    }
    return true;
}

/* writes one CSV row and prints the header and the first rows as a console preview */
static void outRow(Out *o, int ncols, const char *cells[], bool header)
{
    for (int i = 0; i < ncols; i++) {
        if (i > 0) {
            fputc(',', o->fp);
        }
        csvWriteField(o->fp, cells[i]);
    }
    fputc('\n', o->fp);

    if (header || o->shown < PREVIEW_ROWS) {
        printf("  ");
        for (int i = 0; i < ncols; i++) {
            printf("%s%s", i > 0 ? " | " : "", cells[i]);
        }
        printf("\n");
        if (!header) {
            o->shown++;
        }
    }
}

static bool outClose(Out *o)
{
    return fclose(o->fp) == 0;
}

static int cmpIdentity(int i, int j)
{
    int c = strcmp(g_bank->accts[i].name, g_bank->accts[j].name);

    if (c != 0) {
        return c;
    }
    return (g_bank->accts[i].number > g_bank->accts[j].number) -
           (g_bank->accts[i].number < g_bank->accts[j].number);
}

/* "last month" = the calendar month before the reference date's month */
static MonthInfo findReportMonth(void)
{
    MonthInfo mi;
    int d;

    mi.today = referenceDay();
    civilFromDays(mi.today, &mi.year, &mi.month, &d);
    mi.month--;
    if (mi.month == 0) {
        mi.month = 12;
        mi.year--;
    }
    mi.days = daysInMonth(mi.year, mi.month);
    mi.startDay = daysFromCivil(mi.year, mi.month, 1);
    mi.endDay = mi.startDay + mi.days - 1;
    return mi;
}

static void dateString(char *out, size_t size, int day)
{
    int y, m, d;

    civilFromDays(day, &y, &m, &d);
    snprintf(out, size, "%02d/%02d/%04d", d, m, y);
}

/* ------------------------------- Report 1 ------------------------------ */

typedef struct { int acct; long long sum; } AdbRow;

static int cmpAdb(const void *x, const void *y)
{
    const AdbRow *a = (const AdbRow *)x;
    const AdbRow *b = (const AdbRow *)y;

    if (a->sum != b->sum) {
        return a->sum > b->sum ? -1 : 1;           /* descending ADB */
    }
    return cmpIdentity(a->acct, b->acct);
}

static void report1(const Bank *b, const Index *ix, const MonthInfo *mi)
{
    static const char *header[5] = { "AccountName", "AccountAge", "CurrentBalance",
                                     "AverageDailyBalance", "PrivilegeTier" };
    AdbRow *rows = xcalloc((size_t)b->nAccts, sizeof(AdbRow));
    int n = 0;
    Out out;

    printf("\nReport 1: Banking Privilege Tiers\n");
    for (int a = 0; a < b->nAccts; a++) {
        long long sum = 0;
        long long bal = 0;
        int p = ix->start[a];
        int end = ix->start[a + 1];

        /* end-of-day balance = balance after the last transaction on or before that day */
        for (int day = mi->startDay; day <= mi->endDay; day++) {
            while (p < end && b->txns[ix->ord[p]].day <= day) {
                bal = b->txns[ix->ord[p]].balance;
                p++;
            }
            sum += bal;
        }
        if (sum >= ADB_MIN_CENTAVOS * mi->days) {
            rows[n].acct = a;
            rows[n].sum = sum;
            n++;
        }
    }
    g_bank = b;
    qsort(rows, (size_t)n, sizeof(AdbRow), cmpAdb);

    if (outOpen(&out, "report1_banking_privilege_tiers.csv")) {
        outRow(&out, 5, header, true);
        for (int i = 0; i < n; i++) {
            const Account *a = &b->accts[rows[i].acct];
            long long days = mi->days;
            long long adb = (2 * rows[i].sum + days) / (2 * days);     /* rounded to the centavo */
            const char *tier = rows[i].sum < SILVER_LIMIT * days ? "Silver"
                             : rows[i].sum < GOLD_LIMIT * days ? "Gold" : "Platinum";
            char age[16];
            char cur[48];
            char avg[48];
            const char *cells[5];

            int ageDays = mi->today - a->openDay;

            snprintf(age, sizeof age, "%d", ageDays > 0 ? ageDays : 0);
            formatMoneyGrouped(cur, sizeof cur, a->balance);
            formatMoneyGrouped(avg, sizeof avg, adb);
            cells[0] = a->name;
            cells[1] = age;
            cells[2] = cur;
            cells[3] = avg;
            cells[4] = tier;
            outRow(&out, 5, cells, false);
        }
        outClose(&out);
        printf("(Full table exported to report1_banking_privilege_tiers.csv)\n");
    }
    free(rows);
}

/* ------------------------------- Report 2 ------------------------------ */

typedef struct { int acct; long long interest; } InterestRow;

static int cmpInterest(const void *x, const void *y)
{
    const InterestRow *a = (const InterestRow *)x;
    const InterestRow *b = (const InterestRow *)y;

    if (a->interest != b->interest) {
        return a->interest > b->interest ? -1 : 1;  /* descending interest */
    }
    return cmpIdentity(a->acct, b->acct);
}

/* returns the total expected interest (used by summary.json) */
static long long report2(const Bank *b)
{
    static const char *header[3] = { "AccountName", "CurrentBalance", "ExpectedInterest" };
    InterestRow *rows = xcalloc((size_t)b->nAccts, sizeof(InterestRow));
    int n = 0;
    long long total = 0;
    Out out;

    printf("\nReport 2: Interest Expense Projection\n");
    for (int a = 0; a < b->nAccts; a++) {
        if (b->accts[a].balance >= INTEREST_MIN_BALANCE) {
            rows[n].acct = a;
            rows[n].interest = projectInterest(b->accts[a].balance, INTEREST_DAYS);
            total += rows[n].interest;
            n++;
        }
    }
    g_bank = b;
    qsort(rows, (size_t)n, sizeof(InterestRow), cmpInterest);

    if (outOpen(&out, "report2_interest_expense_projection.csv")) {
        outRow(&out, 3, header, true);
        for (int i = 0; i < n; i++) {
            const Account *a = &b->accts[rows[i].acct];
            char cur[48];
            char exp[48];
            const char *cells[3];

            formatMoneyGrouped(cur, sizeof cur, a->balance);
            formatMoneyGrouped(exp, sizeof exp, rows[i].interest);
            cells[0] = a->name;
            cells[1] = cur;
            cells[2] = exp;
            outRow(&out, 3, cells, false);
        }
        outClose(&out);
        printf("(Full table exported to report2_interest_expense_projection.csv)\n");
    }
    free(rows);
    return total;
}

/* ------------------------------- Report 3 ------------------------------ */

typedef struct {
    int       acct;
    int       day;
    long long withdrawals;
    long long deposits;
    int       nDeposits;
    bool      suspicious;
} DayRow;

static int cmpDay(const void *x, const void *y)
{
    const DayRow *a = (const DayRow *)x;
    const DayRow *b = (const DayRow *)y;

    if (a->day != b->day) {
        return a->day < b->day ? -1 : 1;             /* ascending date */
    }
    if (a->suspicious != b->suspicious) {
        return a->suspicious ? -1 : 1;               /* Suspicious before Normal */
    }
    return cmpIdentity(a->acct, b->acct);
}

static void report3(const Bank *b, const Index *ix)
{
    static const char *header[7] = { "AccountName", "Date", "TotalWithdrawalAmounts",
                                     "NumberOfDeposits", "TotalDepositAmounts",
                                     "TurnoverVelocity (%)", "RiskFlagLevel" };
    DayRow *rows = xcalloc((size_t)b->nTxns, sizeof(DayRow));
    int n = 0;
    Out out;

    printf("\nReport 3: Suspicious Client Behavior\n");
    for (int a = 0; a < b->nAccts; a++) {
        int p = ix->start[a];
        int end = ix->start[a + 1];

        while (p < end) {
            DayRow *r = &rows[n];
            int day = b->txns[ix->ord[p]].day;

            r->acct = a;
            r->day = day;
            while (p < end && b->txns[ix->ord[p]].day == day) {
                const Txn *t = &b->txns[ix->ord[p]];
                if (t->isDeposit) {
                    r->deposits += t->amount;
                    r->nDeposits++;
                } else {
                    r->withdrawals += t->amount;
                }
                p++;
            }
            if (r->deposits > 0) {
                r->suspicious = r->withdrawals * 100 >= (long long)SUSPICIOUS_VELOCITY * r->deposits;
            } else {
                r->suspicious = FLAG_WITHDRAWAL_ONLY_DAYS && r->withdrawals > 0;
            }
            if (r->withdrawals >= SUSPICIOUS_WITHDRAWAL) {
                r->suspicious = true;
            }
            n++;
        }
    }
    g_bank = b;
    qsort(rows, (size_t)n, sizeof(DayRow), cmpDay);

    if (outOpen(&out, "report3_suspicious_client_behavior.csv")) {
        outRow(&out, 7, header, true);
        for (int i = 0; i < n; i++) {
            const DayRow *r = &rows[i];
            char date[16];
            char wd[48];
            char count[16];
            char dep[48];
            char vel[48];
            const char *cells[7];

            dateString(date, sizeof date, r->day);
            formatMoneyGrouped(wd, sizeof wd, r->withdrawals);
            snprintf(count, sizeof count, "%d", r->nDeposits);
            formatMoneyGrouped(dep, sizeof dep, r->deposits);
            if (r->deposits > 0) {                     /* withdrawals / deposits x 100, to 2 decimals */
                long long v = (r->withdrawals * 20000 + r->deposits) / (2 * r->deposits);
                formatMoneyGrouped(vel, sizeof vel, v);
            } else {
                snprintf(vel, sizeof vel, "N/A");      /* no deposits that day: undefined */
            }
            cells[0] = b->accts[r->acct].name;
            cells[1] = date;
            cells[2] = wd;
            cells[3] = count;
            cells[4] = dep;
            cells[5] = vel;
            cells[6] = r->suspicious ? "Suspicious" : "Normal";
            outRow(&out, 7, cells, false);
        }
        outClose(&out);
        printf("(Full table exported to report3_suspicious_client_behavior.csv)\n");
    }
    free(rows);
}

/* ------------------------ Report 4 (our own report) --------------------- */
/* Peak Transaction Hours: groups every transaction by the hour of the day it was
 * made (using the time column) and shows how many transactions, how much money in
 * and how much money out each hour handled, busiest hour first.
 * Justification: branches and the fraud team can see when staffing and monitoring
 * are needed most, which none of the other reports show. */

static void report4(const Bank *b)
{
    static const char *header[4] = { "Hour", "NumberOfTransactions", "TotalDeposits",
                                     "TotalWithdrawals" };
    long long dep[24] = { 0 };
    long long wd[24] = { 0 };
    int count[24] = { 0 };
    int order[24];
    int n = 0;
    Out out;

    printf("\nReport 4: Peak Transaction Hours\n");
    for (int i = 0; i < b->nTxns; i++) {
        int h = b->txns[i].sec / 3600;

        count[h]++;
        if (b->txns[i].isDeposit) {
            dep[h] += b->txns[i].amount;
        } else {
            wd[h] += b->txns[i].amount;
        }
    }
    for (int h = 0; h < 24; h++) {                  /* only hours that had activity */
        if (count[h] > 0) {
            order[n++] = h;
        }
    }
    for (int i = 1; i < n; i++) {                   /* busiest first, ties by earlier hour */
        int h = order[i];
        int k = i - 1;

        while (k >= 0 && count[order[k]] < count[h]) {
            order[k + 1] = order[k];
            k--;
        }
        order[k + 1] = h;
    }

    if (outOpen(&out, "report4_peak_transaction_hours.csv")) {
        outRow(&out, 4, header, true);
        for (int i = 0; i < n; i++) {
            int h = order[i];
            char label[16];
            char cnt[16];
            char d[48];
            char w[48];
            const char *cells[4];

            snprintf(label, sizeof label, "%02d:00-%02d:59", h, h);
            snprintf(cnt, sizeof cnt, "%d", count[h]);
            formatMoneyGrouped(d, sizeof d, dep[h]);
            formatMoneyGrouped(w, sizeof w, wd[h]);
            cells[0] = label;
            cells[1] = cnt;
            cells[2] = d;
            cells[3] = w;
            outRow(&out, 4, cells, false);
        }
        outClose(&out);
        printf("(Full table exported to report4_peak_transaction_hours.csv)\n");
    }
}

/* ------------------------------- summary.json -------------------------- */

static void writeSummary(const Bank *b, long long totalInterest)
{
    long long deposits = 0;
    long long withdrawals = 0;
    long long balance = 0;
    char dep[48], wd[48], bal[48], intr[48];
    FILE *fp;

    for (int i = 0; i < b->nTxns; i++) {
        if (b->txns[i].isDeposit) {
            deposits += b->txns[i].amount;
        } else {
            withdrawals += b->txns[i].amount;
        }
    }
    for (int a = 0; a < b->nAccts; a++) {
        balance += b->accts[a].balance;
    }
    formatCentavos(dep, sizeof dep, deposits);
    formatCentavos(wd, sizeof wd, withdrawals);
    formatCentavos(bal, sizeof bal, balance);
    formatCentavos(intr, sizeof intr, totalInterest);

    printf("\nSummary State (summary.json):\n");
    printf("{\n  \"total_accounts\": %d,\n  \"total_deposits\": %s,\n  \"total_withdrawals\": %s,\n"
           "  \"total_balance\": %s,\n  \"total_interest_expense\": %s\n}\n",
           b->nAccts, dep, wd, bal, intr);

    fp = fopen("summary.json", "w");
    if (fp == NULL) {
        printf("Could not create summary.json.\n");
        return;
    }
    fprintf(fp, "{\n  \"total_accounts\": %d,\n  \"total_deposits\": %s,\n  \"total_withdrawals\": %s,\n"
                "  \"total_balance\": %s,\n  \"total_interest_expense\": %s\n}\n",
            b->nAccts, dep, wd, bal, intr);
    fclose(fp);
}

/* ---------------------------- generate reports ------------------------- */

static void generateReports(const Bank *b)
{
    Index ix;
    MonthInfo mi;
    long long totalInterest;
    char refDate[16];

    if (b->nTxns == 0) {
        printf("No transactions to report on. Use [1] Load the file first (is %s in this folder?).\n", CSV_FILE);
        return;
    }
    printf("Generating reports...\n");
    printf("Outputs saved to individual files...\n");

    ix = buildIndex(b);
    mi = findReportMonth();
    dateString(refDate, sizeof refDate, mi.today);
    printf("Reference date: %s | Report month (previous month): %02d/%04d (%d days)\n",
           refDate, mi.month, mi.year, mi.days);

    report1(b, &ix, &mi);
    totalInterest = report2(b);
    report3(b, &ix);
    report4(b);
    writeSummary(b, totalInterest);
    freeIndex(&ix);
}

static void reportsMenu(Bank *b)
{
    for (;;) {
        int choice;

        printf("\nGenerate Reports\n");
        printf("[1] Load the file\n");
        printf("[2] Generate Reports\n");
        printf("[0] Back to the Main Menu\n\n");
        choice = promptInt("Enter choice: ", 0, 2);

        if (choice == 0) {
            return;
        }
        if (choice == 1) {
            loadDataset(b);
            printLoadSummary(b);
        } else {
            generateReports(b);
        }
    }
}

/* -------------------------------- main -------------------------------- */

int main(int argc, char *argv[])
{
    Bank bank;
    Currency currencies[CURRENCY_COUNT] = {
        { "PHP", "Philippine Peso",          1.0 },   /* base currency; the rest are default rates */
        { "USD", "United States Dollar",     52.0 },
        { "JPY", "Japanese Yen",             0.40 },
        { "GBP", "British Pound Sterling",   84.0 },
        { "EUR", "Euro",                     72.0 },
        { "CNY", "Chinese Yuan Renminni",    9.0 }
    };
    bool running = true;

    bool reportsOnly = false;

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--reports") == 0) {
            reportsOnly = true;
        } else if (strcmp(argv[i], "--reference") == 0 && i + 1 < argc) {
            int day;

            if (!parseDateStr(argv[++i], &day)) {
                printf("Invalid reference date. Use dd/mm/yyyy.\n");
                return 1;
            }
            g_referenceDay = day;
        } else {
            printf("Usage: %s [--reports] [--reference dd/mm/yyyy]\n", argv[0]);
            return 1;
        }
    }

    memset(&bank, 0, sizeof bank);
    loadDataset(&bank);

    if (reportsOnly) {
        printLoadSummary(&bank);
        generateReports(&bank);
        bankClear(&bank);
        return 0;
    }

    if (bank.fileFound) {
        char x[48];
        char y[48];
        formatGrouped(x, sizeof x, bank.nTxns);
        formatGrouped(y, sizeof y, bank.nAccts);
        printf("Loaded %s transactions for %s accounts from %s.\n", x, y, CSV_FILE);
    } else {
        printf("%s not found; it will be created when the first account is registered.\n", CSV_FILE);
    }

    while (running) {
        printMainMenu();
        switch (promptInt("Choice: ", 0, 7)) {
            case 1: registerAccounts(&bank, currencies);       break;
            case 2: transact(&bank, currencies, true);         break;
            case 3: transact(&bank, currencies, false);        break;
            case 4: currencyExchange(currencies);              break;
            case 5: recordExchangeRates(currencies);           break;
            case 6: showInterest(&bank);                       break;
            case 7: reportsMenu(&bank);                        break;
            default: running = false;                          break;
        }
    }

    printf("\nThank you for using the Banking and Currency Exchange Application.\n");
    bankClear(&bank);
    return 0;
}
