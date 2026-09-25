package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.models.Category
import com.example.data.models.AccountEntity
import com.example.data.models.CreditCardEntity
import com.example.data.models.GeographicRegion
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.LoanEntity
import com.example.data.models.BudgetEntity
import com.example.data.models.NetWorthSnapshotEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN statementFingerprint TEXT DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN importStatus TEXT NOT NULL DEFAULT 'MANUAL'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN importSource TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN sourceReference TEXT DEFAULT NULL")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sips ADD COLUMN lastDebitedYearMonth TEXT DEFAULT NULL")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `net_worth_snapshots` (" +
                "`dayKey` TEXT NOT NULL, `netWorth` REAL NOT NULL, `assets` REAL NOT NULL, " +
                "`liabilities` REAL NOT NULL, `dateMillis` INTEGER NOT NULL, PRIMARY KEY(`dayKey`))"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budgets` (`category` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, PRIMARY KEY(`category`))"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE net_worth_snapshots ADD COLUMN investments REAL DEFAULT NULL")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Previous versions generated these records from a schedule, not an actual payment.
        // Preserve them for customer confirmation and exclude them from totals until reviewed.
        db.execSQL(
            "UPDATE transactions SET importStatus = 'PENDING_REVIEW' " +
                "WHERE category = 'INVESTMENT_SIP' AND type = 'EXPENSE' AND " +
                "note IN ('Automated SIP investment recorded in Obsidian Wealth', " +
                "'Automated recurring SIP debit recorded by Obsidian Wealth')"
        )
        db.execSQL(
            "UPDATE transactions SET category = 'DEBT_PAYMENT' " +
                "WHERE category = 'OTHER' AND type = 'EXPENSE' AND " +
                "title LIKE 'Payment to %' AND note = 'Card debt reduction'"
        )
        db.execSQL(
            "UPDATE transactions SET category = 'DEBT_PAYMENT' " +
                "WHERE category = 'LOAN_EMI' AND type = 'EXPENSE' AND " +
                "title LIKE 'EMI: %' AND note = 'Manual EMI payment recorded in Obsidian Wealth'"
        )
    }
}

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        HoldingEntity::class,
        SipEntity::class,
        CreditCardEntity::class,
        LoanEntity::class,
        GoalEntity::class,
        NetWorthSnapshotEntity::class,
        BudgetEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "obsidian_wealth_v3.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
            }
        }

        suspend fun reseedDatabaseForRegion(dao: FinanceDao, region: GeographicRegion) {
            dao.clearAllTransactions()
            dao.clearAllAccounts()
            dao.clearAllHoldings()
            dao.clearAllSips()
            dao.clearAllCreditCards()
            dao.clearAllLoans()
            dao.clearAllGoals()
            dao.clearAllSnapshots()
            dao.clearAllBudgets()
            populateDatabaseForRegion(dao, region)
        }

        suspend fun reseedDatabase(dao: FinanceDao) {
            reseedDatabaseForRegion(dao, GeographicRegion.EAST_AFRICA)
        }

        suspend fun populateDatabase(dao: FinanceDao) {
            populateDatabaseForRegion(dao, GeographicRegion.EAST_AFRICA)
        }

        suspend fun populateDatabaseForRegion(dao: FinanceDao, region: GeographicRegion) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            when (region) {
                GeographicRegion.LATIN_AMERICA -> {
                    // Seed Holdings for Latin America (e.g. Mexico / Argentina / LATAM)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "MELI", name = "MercadoLibre Inc (LATAM E-Commerce)", type = HoldingType.STOCK, shares = 120.0, avgBuyPrice = 1420.0, currentPrice = 1680.0, dailyChangePercent = 2.15),
                            HoldingEntity(id = 0, symbol = "AMX", name = "América Móvil S.A.B. de C.V.", type = HoldingType.STOCK, shares = 4500.0, avgBuyPrice = 16.50, currentPrice = 18.20, dailyChangePercent = 0.95),
                            HoldingEntity(id = 0, symbol = "GFNORTE", name = "Grupo Financiero Banorte", type = HoldingType.STOCK, shares = 2800.0, avgBuyPrice = 135.0, currentPrice = 152.0, dailyChangePercent = 1.10),
                            HoldingEntity(id = 0, symbol = "YPF", name = "YPF Sociedad Anónima (Energy)", type = HoldingType.STOCK, shares = 1800.0, avgBuyPrice = 18.50, currentPrice = 22.40, dailyChangePercent = 1.80),
                            HoldingEntity(id = 0, symbol = "EWW", name = "iShares MSCI Mexico ETF", type = HoldingType.ETF, shares = 1200.0, avgBuyPrice = 58.0, currentPrice = 64.5, dailyChangePercent = 0.65),
                            HoldingEntity(id = 0, symbol = "GOLD", name = "Physical Gold Sovereigns & Custody", type = HoldingType.GOLD, shares = 25.0, avgBuyPrice = 2100.0, currentPrice = 2450.0, dailyChangePercent = 0.85),
                            HoldingEntity(id = 0, symbol = "BTC", name = "Bitcoin Treasury Reserve", type = HoldingType.CRYPTO, shares = 0.12, avgBuyPrice = 54000.0, currentPrice = 68000.0, dailyChangePercent = 3.10)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "iShares MSCI Mexico ETF Auto-Invest", category = "LATAM Index Growth", monthlyAmount = 4500.0, debitDayOfMonth = 1, isActive = true, totalInvested = 45000.0, annualizedReturnPercent = 14.2),
                            SipEntity(id = 0, fundName = "MercadoLibre Direct Equity Plan", category = "FinTech & Retail Alpha", monthlyAmount = 3000.0, debitDayOfMonth = 5, isActive = true, totalInvested = 36000.0, annualizedReturnPercent = 18.5)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "BBVA Platinum Card", lastFour = "4421", currentBalance = 28500.0, creditLimit = 150000.0, apr = 24.0, dueDateDays = 5, colorHex = "#1E3A8A"),
                            CreditCardEntity(id = 0, cardName = "Santander Rio Signature", lastFour = "8023", currentBalance = 14200.0, creditLimit = 80000.0, apr = 22.5, dueDateDays = 12, colorHex = "#047857"),
                            CreditCardEntity(id = 0, cardName = "Nu Mexico Digital Card", lastFour = "1940", currentBalance = 8500.0, creditLimit = 45000.0, apr = 18.0, dueDateDays = 20, colorHex = "#B45309")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "Banorte Prime Home Mortgage", lender = "Banorte Bank", totalAmount = 2400000.0, remainingBalance = 1250000.0, emiAmount = 22500.0, interestRate = 11.5, totalMonths = 120, remainingMonths = 54, dueDayOfMonth = 1),
                            LoanEntity(id = 0, loanName = "Santander Auto Finance (Toyota RAV4)", lender = "Santander", totalAmount = 480000.0, remainingBalance = 180000.0, emiAmount = 12500.0, interestRate = 12.8, totalMonths = 48, remainingMonths = 14, dueDayOfMonth = 10)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "LATAM Commercial Property Reserve", category = "Real Estate Capital", targetAmount = 1200000.0, currentAmount = 780000.0, monthlyContribution = 25000.0, targetYear = 2026, colorHex = "#10B981"),
                            GoalEntity(id = 0, title = "Emergency Liquidity Fund", category = "Liquid Safety Net", targetAmount = 350000.0, currentAmount = 280000.0, monthlyContribution = 15000.0, targetYear = 2026, colorHex = "#06B6D4")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Consulting Retainer Income", amount = 85000.0, type = TransactionType.INCOME, category = Category.SALARY, account = "Banorte Checking", dateMillis = now - dayMillis * 2, note = "Monthly corporate retainer deposit", isRecurring = true),
                            TransactionEntity(id = 0, title = "Banorte Dividend Payout", amount = 6200.0, type = TransactionType.INCOME, category = Category.DIVIDENDS, account = "Banorte Brokerage", dateMillis = now - dayMillis * 5, note = "Quarterly equity dividend"),
                            TransactionEntity(id = 0, title = "CDMX Apartment Lease Payment", amount = 22500.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "BBVA Checking", dateMillis = now - dayMillis * 1, note = "Monthly residential rent", isRecurring = true),
                            TransactionEntity(id = 0, title = "Banorte Mortgage EMI", amount = 22500.0, type = TransactionType.EXPENSE, category = Category.LOAN_EMI, account = "Banorte Checking", dateMillis = now - dayMillis * 3, note = "Home loan amortization", isRecurring = true),
                            TransactionEntity(id = 0, title = "Chedraui / Walmart Groceries", amount = 4800.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "BBVA Platinum", dateMillis = now - dayMillis * 4, note = "Household pantry and groceries"),
                            TransactionEntity(id = 0, title = "Telcel Fiber & Mobile Line", amount = 1450.0, type = TransactionType.EXPENSE, category = Category.UTILITIES, account = "Nu Card", dateMillis = now - dayMillis * 7, note = "High-speed internet bill", isRecurring = true)
                        )
                    )
                }

                GeographicRegion.NORTH_AMERICA -> {
                    // Seed Holdings for North America (USD)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "AAPL", name = "Apple Inc.", type = HoldingType.STOCK, shares = 180.0, avgBuyPrice = 172.0, currentPrice = 225.0, dailyChangePercent = 1.45),
                            HoldingEntity(id = 0, symbol = "MSFT", name = "Microsoft Corp", type = HoldingType.STOCK, shares = 95.0, avgBuyPrice = 380.0, currentPrice = 448.0, dailyChangePercent = 1.12),
                            HoldingEntity(id = 0, symbol = "VOO", name = "Vanguard S&P 500 ETF", type = HoldingType.ETF, shares = 320.0, avgBuyPrice = 410.0, currentPrice = 512.0, dailyChangePercent = 0.85),
                            HoldingEntity(id = 0, symbol = "NVDA", name = "NVIDIA Corp", type = HoldingType.STOCK, shares = 210.0, avgBuyPrice = 88.0, currentPrice = 128.0, dailyChangePercent = 2.80),
                            HoldingEntity(id = 0, symbol = "SPAXX", name = "Fidelity Govt Money Market", type = HoldingType.MUTUAL_FUND, shares = 45000.0, avgBuyPrice = 1.0, currentPrice = 1.0, dailyChangePercent = 0.02),
                            HoldingEntity(id = 0, symbol = "GOLD", name = "Physical Gold Bullion Custody", type = HoldingType.GOLD, shares = 15.0, avgBuyPrice = 2150.0, currentPrice = 2520.0, dailyChangePercent = 0.60),
                            HoldingEntity(id = 0, symbol = "BTC", name = "Bitcoin Cold Storage", type = HoldingType.CRYPTO, shares = 0.25, avgBuyPrice = 52000.0, currentPrice = 68000.0, dailyChangePercent = 3.10)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "Vanguard S&P 500 Monthly Auto-Invest", category = "US Equity Index SIP", monthlyAmount = 1500.0, debitDayOfMonth = 1, isActive = true, totalInvested = 18000.0, annualizedReturnPercent = 12.8),
                            SipEntity(id = 0, fundName = "Apple & Tech Alpha Direct Plan", category = "Direct Stock Accumulation", monthlyAmount = 500.0, debitDayOfMonth = 15, isActive = true, totalInvested = 6000.0, annualizedReturnPercent = 16.2)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "Chase Sapphire Reserve", lastFour = "4421", currentBalance = 2850.0, creditLimit = 25000.0, apr = 21.5, dueDateDays = 5, colorHex = "#1E3A8A"),
                            CreditCardEntity(id = 0, cardName = "Amex Platinum Card", lastFour = "8023", currentBalance = 1420.0, creditLimit = 30000.0, apr = 24.0, dueDateDays = 12, colorHex = "#047857")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "Chase Home Mortgage", lender = "Chase Bank", totalAmount = 420000.0, remainingBalance = 265000.0, emiAmount = 2250.0, interestRate = 6.2, totalMonths = 360, remainingMonths = 210, dueDayOfMonth = 1),
                            LoanEntity(id = 0, loanName = "SoFi Auto Finance (Tesla Model Y)", lender = "SoFi", totalAmount = 42000.0, remainingBalance = 14500.0, emiAmount = 680.0, interestRate = 5.8, totalMonths = 60, remainingMonths = 22, dueDayOfMonth = 10)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "Liquid Emergency Reserve Fund", category = "Capital Preservation", targetAmount = 30000.0, currentAmount = 24000.0, monthlyContribution = 1000.0, targetYear = 2026, colorHex = "#10B981"),
                            GoalEntity(id = 0, title = "Index Fund Early Retirement", category = "FIRE Wealth Accumulation", targetAmount = 500000.0, currentAmount = 185000.0, monthlyContribution = 2500.0, targetYear = 2030, colorHex = "#6366F1")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Executive Payroll Direct Deposit", amount = 6800.0, type = TransactionType.INCOME, category = Category.SALARY, account = "Chase Checking", dateMillis = now - dayMillis * 2, note = "Bi-weekly salary deposit", isRecurring = true),
                            TransactionEntity(id = 0, title = "Vanguard S&P 500 Dividend", amount = 380.0, type = TransactionType.INCOME, category = Category.DIVIDENDS, account = "Fidelity Brokerage", dateMillis = now - dayMillis * 5, note = "Quarterly ETF dividend"),
                            TransactionEntity(id = 0, title = "Residential Mortgage Payment", amount = 2250.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "Chase Checking", dateMillis = now - dayMillis * 1, note = "Home mortgage installment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Trader Joe's Supermarket", amount = 185.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "Chase Sapphire", dateMillis = now - dayMillis * 3, note = "Weekly pantry and groceries"),
                            TransactionEntity(id = 0, title = "AT&T Fiber Internet & Wireless", amount = 120.0, type = TransactionType.EXPENSE, category = Category.UTILITIES, account = "Chase Checking", dateMillis = now - dayMillis * 6, note = "Monthly connectivity bill", isRecurring = true)
                        )
                    )
                }

                GeographicRegion.EUROPE -> {
                    // Seed Holdings for Europe (€)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "ASML", name = "ASML Holding NV", type = HoldingType.STOCK, shares = 45.0, avgBuyPrice = 620.0, currentPrice = 840.0, dailyChangePercent = 1.85),
                            HoldingEntity(id = 0, symbol = "MC", name = "LVMH Moët Hennessy", type = HoldingType.STOCK, shares = 35.0, avgBuyPrice = 680.0, currentPrice = 780.0, dailyChangePercent = 0.92),
                            HoldingEntity(id = 0, symbol = "SAP", name = "SAP SE", type = HoldingType.STOCK, shares = 120.0, avgBuyPrice = 140.0, currentPrice = 195.0, dailyChangePercent = 1.15),
                            HoldingEntity(id = 0, symbol = "IEUR", name = "iShares Core MSCI Europe UCITS ETF", type = HoldingType.ETF, shares = 850.0, avgBuyPrice = 52.0, currentPrice = 68.0, dailyChangePercent = 0.70),
                            HoldingEntity(id = 0, symbol = "BNP-CASH", name = "BNP Paribas Euro Money Fund", type = HoldingType.MUTUAL_FUND, shares = 28000.0, avgBuyPrice = 1.0, currentPrice = 1.0, dailyChangePercent = 0.03)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "iShares MSCI Europe Savings Plan", category = "Euro Core Equity SIP", monthlyAmount = 600.0, debitDayOfMonth = 1, isActive = true, totalInvested = 7200.0, annualizedReturnPercent = 11.5)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "Deutsche Bank Gold Mastercard", lastFour = "4421", currentBalance = 1850.0, creditLimit = 10000.0, apr = 18.5, dueDateDays = 5, colorHex = "#1E3A8A"),
                            CreditCardEntity(id = 0, cardName = "Revolut Metal Card", lastFour = "8023", currentBalance = 650.0, creditLimit = 5000.0, apr = 16.0, dueDateDays = 12, colorHex = "#047857")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "BNP Paribas Property Loan", lender = "BNP Paribas", totalAmount = 280000.0, remainingBalance = 165000.0, emiAmount = 1250.0, interestRate = 3.8, totalMonths = 240, remainingMonths = 130, dueDayOfMonth = 1)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "European Wealth Reserve", category = "Capital Preservation", targetAmount = 25000.0, currentAmount = 19500.0, monthlyContribution = 800.0, targetYear = 2026, colorHex = "#10B981")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Corporate Salary Retainer", amount = 5200.0, type = TransactionType.INCOME, category = Category.SALARY, account = "ING Checking", dateMillis = now - dayMillis * 2, note = "Monthly salary deposit", isRecurring = true),
                            TransactionEntity(id = 0, title = "Apartment Lease Payment", amount = 1250.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "ING Checking", dateMillis = now - dayMillis * 1, note = "Monthly rent payment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Carrefour Supermarket Groceries", amount = 165.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "Deutsche Bank Card", dateMillis = now - dayMillis * 4, note = "Weekly groceries")
                        )
                    )
                }

                GeographicRegion.UK -> {
                    // Seed Holdings for UK (£)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "AZN", name = "AstraZeneca PLC", type = HoldingType.STOCK, shares = 140.0, avgBuyPrice = 98.0, currentPrice = 122.0, dailyChangePercent = 1.25),
                            HoldingEntity(id = 0, symbol = "HSBA", name = "HSBC Holdings PLC", type = HoldingType.STOCK, shares = 1800.0, avgBuyPrice = 5.80, currentPrice = 6.90, dailyChangePercent = 0.85),
                            HoldingEntity(id = 0, symbol = "SHEL", name = "Shell PLC", type = HoldingType.STOCK, shares = 950.0, avgBuyPrice = 22.0, currentPrice = 27.5, dailyChangePercent = 1.10),
                            HoldingEntity(id = 0, symbol = "VUKE", name = "Vanguard FTSE 100 UCITS ETF", type = HoldingType.ETF, shares = 1100.0, avgBuyPrice = 31.0, currentPrice = 36.8, dailyChangePercent = 0.65)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "Vanguard FTSE 100 ISA Savings Plan", category = "UK Equity ISA", monthlyAmount = 500.0, debitDayOfMonth = 1, isActive = true, totalInvested = 6000.0, annualizedReturnPercent = 10.8)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "Barclays Avios Barclaycard", lastFour = "4421", currentBalance = 1450.0, creditLimit = 9000.0, apr = 22.0, dueDateDays = 5, colorHex = "#1E3A8A")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "HSBC Residential Mortgage", lender = "HSBC UK", totalAmount = 320000.0, remainingBalance = 195000.0, emiAmount = 1420.0, interestRate = 4.8, totalMonths = 300, remainingMonths = 180, dueDayOfMonth = 1)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "Stocks & Shares ISA Reserve", category = "Tax-Free ISA Wealth", targetAmount = 20000.0, currentAmount = 16500.0, monthlyContribution = 850.0, targetYear = 2026, colorHex = "#10B981")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Monthly UK Salary Credit", amount = 4800.0, type = TransactionType.INCOME, category = Category.SALARY, account = "Barclays Checking", dateMillis = now - dayMillis * 2, note = "Monthly corporate payroll", isRecurring = true),
                            TransactionEntity(id = 0, title = "London Flat Rent / Mortgage", amount = 1420.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "Barclays Checking", dateMillis = now - dayMillis * 1, note = "Monthly property payment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Sainsbury's Supermarket", amount = 145.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "Barclaycard", dateMillis = now - dayMillis * 3, note = "Weekly food shopping")
                        )
                    )
                }

                GeographicRegion.INDIA -> {
                    // Seed Holdings for India (₹)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "RELIANCE", name = "Reliance Industries Ltd", type = HoldingType.STOCK, shares = 450.0, avgBuyPrice = 2400.0, currentPrice = 2980.0, dailyChangePercent = 1.55),
                            HoldingEntity(id = 0, symbol = "TCS", name = "Tata Consultancy Services", type = HoldingType.STOCK, shares = 220.0, avgBuyPrice = 3200.0, currentPrice = 4150.0, dailyChangePercent = 1.20),
                            HoldingEntity(id = 0, symbol = "HDFCBANK", name = "HDFC Bank Ltd", type = HoldingType.STOCK, shares = 850.0, avgBuyPrice = 1450.0, currentPrice = 1680.0, dailyChangePercent = 0.90),
                            HoldingEntity(id = 0, symbol = "SGB", name = "Sovereign Gold Bond 2026", type = HoldingType.GOLD, shares = 80.0, avgBuyPrice = 5800.0, currentPrice = 7250.0, dailyChangePercent = 0.75),
                            HoldingEntity(id = 0, symbol = "ICICI-LIQUID", name = "ICICI Prudential Liquid Fund", type = HoldingType.MUTUAL_FUND, shares = 450000.0, avgBuyPrice = 1.0, currentPrice = 1.0, dailyChangePercent = 0.02)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "HDFC Top 100 Nifty Equity SIP", category = "Large Cap Nifty Index", monthlyAmount = 25000.0, debitDayOfMonth = 1, isActive = true, totalInvested = 300000.0, annualizedReturnPercent = 15.2)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "HDFC Regalia Gold Card", lastFour = "4421", currentBalance = 85000.0, creditLimit = 400000.0, apr = 22.0, dueDateDays = 5, colorHex = "#1E3A8A")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "SBI Prime Home Loan", lender = "State Bank of India", totalAmount = 6500000.0, remainingBalance = 3800000.0, emiAmount = 58000.0, interestRate = 8.5, totalMonths = 240, remainingMonths = 120, dueDayOfMonth = 1)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "Emergency Liquidity Reserve", category = "Liquid Security", targetAmount = 1200000.0, currentAmount = 950000.0, monthlyContribution = 30000.0, targetYear = 2026, colorHex = "#10B981")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Monthly Corporate Salary Credit", amount = 280000.0, type = TransactionType.INCOME, category = Category.SALARY, account = "HDFC Bank", dateMillis = now - dayMillis * 2, note = "Monthly tech payroll", isRecurring = true),
                            TransactionEntity(id = 0, title = "SBI Home Loan EMI", amount = 58000.0, type = TransactionType.EXPENSE, category = Category.LOAN_EMI, account = "HDFC Bank", dateMillis = now - dayMillis * 1, note = "Home loan monthly deduction", isRecurring = true),
                            TransactionEntity(id = 0, title = "DMart Supermarket Groceries via UPI", amount = 12500.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "HDFC Bank UPI", dateMillis = now - dayMillis * 4, note = "Monthly provisions")
                        )
                    )
                }

                GeographicRegion.EAST_AFRICA -> {
                    // Seed Holdings for East Africa (KES)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "SCOM", name = "Safaricom PLC (NSE)", type = HoldingType.STOCK, shares = 85000.0, avgBuyPrice = 16.50, currentPrice = 19.80, dailyChangePercent = 1.54),
                            HoldingEntity(id = 0, symbol = "EQTY", name = "Equity Group Holdings PLC", type = HoldingType.STOCK, shares = 28000.0, avgBuyPrice = 38.00, currentPrice = 44.50, dailyChangePercent = 0.80),
                            HoldingEntity(id = 0, symbol = "EABL", name = "East African Breweries PLC", type = HoldingType.STOCK, shares = 6500.0, avgBuyPrice = 142.00, currentPrice = 168.00, dailyChangePercent = 0.60),
                            HoldingEntity(id = 0, symbol = "KCB", name = "KCB Group PLC", type = HoldingType.STOCK, shares = 15000.0, avgBuyPrice = 28.50, currentPrice = 36.20, dailyChangePercent = 1.10),
                            HoldingEntity(id = 0, symbol = "IFB", name = "CBK Infrastructure Bond (Tax-Free)", type = HoldingType.MUTUAL_FUND, shares = 25000.0, avgBuyPrice = 100.00, currentPrice = 100.00, dailyChangePercent = 0.05),
                            HoldingEntity(id = 0, symbol = "CIC-MMF", name = "CIC Money Market Fund", type = HoldingType.MUTUAL_FUND, shares = 980000.0, avgBuyPrice = 1.00, currentPrice = 1.00, dailyChangePercent = 0.04),
                            HoldingEntity(id = 0, symbol = "GOLD", name = "Physical Gold Bullion", type = HoldingType.GOLD, shares = 35.0, avgBuyPrice = 8200.0, currentPrice = 9450.0, dailyChangePercent = 0.85)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "CIC Money Market Fund Standing Order", category = "Liquid Yield & Preservation", monthlyAmount = 50000.0, debitDayOfMonth = 1, isActive = true, totalInvested = 450000.0, annualizedReturnPercent = 13.2),
                            SipEntity(id = 0, fundName = "Safaricom PLC NSE Share Accumulation", category = "Nairobi Equity Alpha", monthlyAmount = 25000.0, debitDayOfMonth = 5, isActive = true, totalInvested = 320000.0, annualizedReturnPercent = 18.5)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "Standard Chartered Infinite Card", lastFour = "4421", currentBalance = 85000.0, creditLimit = 500000.0, apr = 24.0, dueDateDays = 5, colorHex = "#1E3A8A"),
                            CreditCardEntity(id = 0, cardName = "KCB Platinum Card", lastFour = "8023", currentBalance = 34000.0, creditLimit = 250000.0, apr = 22.5, dueDateDays = 12, colorHex = "#047857")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "Stanbic Prime Home Loan", lender = "Stanbic Bank Kenya", totalAmount = 6500000.0, remainingBalance = 3200000.0, emiAmount = 58000.0, interestRate = 14.5, totalMonths = 120, remainingMonths = 54, dueDayOfMonth = 1),
                            LoanEntity(id = 0, loanName = "NCBA Vehicle Asset Finance", lender = "NCBA Bank Kenya", totalAmount = 2400000.0, remainingBalance = 620000.0, emiAmount = 46000.0, interestRate = 13.8, totalMonths = 48, remainingMonths = 12, dueDayOfMonth = 10)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "Commercial Real Estate Land", category = "Real Estate Acquisition", targetAmount = 4500000.0, currentAmount = 2800000.0, monthlyContribution = 65000.0, targetYear = 2026, colorHex = "#10B981"),
                            GoalEntity(id = 0, title = "CBK Infrastructure Bond Reserve", category = "Tax-Free Fixed Income", targetAmount = 2000000.0, currentAmount = 1450000.0, monthlyContribution = 40000.0, targetYear = 2026, colorHex = "#6366F1")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Executive Tech Salary Inflow", amount = 450000.0, type = TransactionType.INCOME, category = Category.SALARY, account = "NCBA Bank", dateMillis = now - dayMillis * 2, note = "Monthly salary deposit", isRecurring = true),
                            TransactionEntity(id = 0, title = "Safaricom Interim Dividend", amount = 35000.0, type = TransactionType.INCOME, category = Category.DIVIDENDS, account = "CDS Account", dateMillis = now - dayMillis * 8, note = "NSE dividend payout"),
                            TransactionEntity(id = 0, title = "Stanbic Home Loan Amortization", amount = 58000.0, type = TransactionType.EXPENSE, category = Category.LOAN_EMI, account = "Stanbic Checking", dateMillis = now - dayMillis * 1, note = "Home mortgage installment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Carrefour Pantry Groceries", amount = 18400.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "StanChart Card", dateMillis = now - dayMillis * 7, note = "Weekly groceries"),
                            TransactionEntity(id = 0, title = "KPLC Prepaid Electricity Tokens", amount = 8500.0, type = TransactionType.EXPENSE, category = Category.UTILITIES, account = "M-PESA", dateMillis = now - dayMillis * 10, note = "Electricity tokens", isRecurring = true)
                        )
                    )
                }

                GeographicRegion.AUSTRALIA -> {
                    // Seed Holdings for Australia (AUD)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "BHP", name = "BHP Group Ltd", type = HoldingType.STOCK, shares = 450.0, avgBuyPrice = 38.0, currentPrice = 44.5, dailyChangePercent = 1.25),
                            HoldingEntity(id = 0, symbol = "CBA", name = "Commonwealth Bank of Australia", type = HoldingType.STOCK, shares = 210.0, avgBuyPrice = 98.0, currentPrice = 124.0, dailyChangePercent = 0.95),
                            HoldingEntity(id = 0, symbol = "CSL", name = "CSL Limited", type = HoldingType.STOCK, shares = 95.0, avgBuyPrice = 260.0, currentPrice = 295.0, dailyChangePercent = 1.40),
                            HoldingEntity(id = 0, symbol = "VAS", name = "Vanguard Australian Shares ETF", type = HoldingType.ETF, shares = 650.0, avgBuyPrice = 82.0, currentPrice = 94.0, dailyChangePercent = 0.80)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "Vanguard Australian Shares Auto-Invest", category = "ASX Index SIP", monthlyAmount = 800.0, debitDayOfMonth = 1, isActive = true, totalInvested = 9600.0, annualizedReturnPercent = 11.2)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "ANZ Frequent Flyer Black", lastFour = "4421", currentBalance = 2450.0, creditLimit = 15000.0, apr = 20.0, dueDateDays = 5, colorHex = "#1E3A8A")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "Westpac Home Loan", lender = "Westpac Bank", totalAmount = 450000.0, remainingBalance = 280000.0, emiAmount = 2450.0, interestRate = 6.1, totalMonths = 360, remainingMonths = 220, dueDayOfMonth = 1)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "Superannuation & Property Reserve", category = "Australian Wealth", targetAmount = 40000.0, currentAmount = 28000.0, monthlyContribution = 1200.0, targetYear = 2026, colorHex = "#10B981")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Monthly Corporate Salary Credit", amount = 7200.0, type = TransactionType.INCOME, category = Category.SALARY, account = "ANZ Checking", dateMillis = now - dayMillis * 2, note = "Monthly salary deposit", isRecurring = true),
                            TransactionEntity(id = 0, title = "Westpac Mortgage EMI", amount = 2450.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "ANZ Checking", dateMillis = now - dayMillis * 1, note = "Home loan payment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Woolworths Supermarket", amount = 210.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "ANZ Card", dateMillis = now - dayMillis * 3, note = "Weekly food groceries")
                        )
                    )
                }

                GeographicRegion.MIDDLE_EAST -> {
                    // Seed Holdings for Middle East (AED)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "EMIRATESNBD", name = "Emirates NBD Bank PJSC", type = HoldingType.STOCK, shares = 3500.0, avgBuyPrice = 14.2, currentPrice = 17.8, dailyChangePercent = 1.40),
                            HoldingEntity(id = 0, symbol = "EMAAR", name = "Emaar Properties PJSC", type = HoldingType.STOCK, shares = 8500.0, avgBuyPrice = 6.5, currentPrice = 8.2, dailyChangePercent = 1.85),
                            HoldingEntity(id = 0, symbol = "IHC", name = "International Holding Company", type = HoldingType.STOCK, shares = 420.0, avgBuyPrice = 380.0, currentPrice = 410.0, dailyChangePercent = 0.90),
                            HoldingEntity(id = 0, symbol = "SUKUK", name = "UAE Sovereign Dollar Sukuk", type = HoldingType.MUTUAL_FUND, shares = 45000.0, avgBuyPrice = 1.0, currentPrice = 1.0, dailyChangePercent = 0.02)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "Dubai & GCC Equity Accumulation", category = "GCC Growth Portfolio", monthlyAmount = 3500.0, debitDayOfMonth = 1, isActive = true, totalInvested = 42000.0, annualizedReturnPercent = 13.5)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "Emirates NBD Skywards Infinite", lastFour = "4421", currentBalance = 8500.0, creditLimit = 50000.0, apr = 21.0, dueDateDays = 5, colorHex = "#1E3A8A")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "FAB Prime Villa Mortgage", lender = "First Abu Dhabi Bank", totalAmount = 1800000.0, remainingBalance = 1150000.0, emiAmount = 9800.0, interestRate = 5.2, totalMonths = 240, remainingMonths = 140, dueDayOfMonth = 1)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "GCC Property & Equity Reserve", category = "Capital Accumulation", targetAmount = 150000.0, currentAmount = 98000.0, monthlyContribution = 5000.0, targetYear = 2026, colorHex = "#10B981")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Executive Salary Transfer", amount = 28000.0, type = TransactionType.INCOME, category = Category.SALARY, account = "Emirates NBD", dateMillis = now - dayMillis * 2, note = "Monthly corporate payroll", isRecurring = true),
                            TransactionEntity(id = 0, title = "FAB Villa Mortgage Payment", amount = 9800.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "Emirates NBD", dateMillis = now - dayMillis * 1, note = "Property loan payment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Carrefour Dubai Mall Groceries", amount = 850.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "Skywards Card", dateMillis = now - dayMillis * 3, note = "Weekly groceries")
                        )
                    )
                }

                GeographicRegion.SOUTH_AFRICA -> {
                    // Seed Holdings for South Africa (ZAR)
                    dao.insertHoldings(
                        listOf(
                            HoldingEntity(id = 0, symbol = "NPN", name = "Naspers Ltd", type = HoldingType.STOCK, shares = 120.0, avgBuyPrice = 2800.0, currentPrice = 3450.0, dailyChangePercent = 1.65),
                            HoldingEntity(id = 0, symbol = "FSR", name = "FirstRand Ltd", type = HoldingType.STOCK, shares = 2800.0, avgBuyPrice = 58.0, currentPrice = 69.5, dailyChangePercent = 0.90),
                            HoldingEntity(id = 0, symbol = "CPI", name = "Capitec Bank Holdings", type = HoldingType.STOCK, shares = 85.0, avgBuyPrice = 1850.0, currentPrice = 2250.0, dailyChangePercent = 1.40),
                            HoldingEntity(id = 0, symbol = "STX40", name = "Satrix 40 ETF", type = HoldingType.ETF, shares = 1400.0, avgBuyPrice = 68.0, currentPrice = 78.5, dailyChangePercent = 0.75)
                        )
                    )
                    dao.insertSips(
                        listOf(
                            SipEntity(id = 0, fundName = "Satrix 40 Monthly Equity Plan", category = "JSE Top 40 SIP", monthlyAmount = 3500.0, debitDayOfMonth = 1, isActive = true, totalInvested = 42000.0, annualizedReturnPercent = 12.5)
                        )
                    )
                    dao.insertCreditCards(
                        listOf(
                            CreditCardEntity(id = 0, cardName = "Discovery Bank Purple Card", lastFour = "4421", currentBalance = 18500.0, creditLimit = 120000.0, apr = 22.0, dueDateDays = 5, colorHex = "#1E3A8A")
                        )
                    )
                    dao.insertLoans(
                        listOf(
                            LoanEntity(id = 0, loanName = "Standard Bank Home Loan", lender = "Standard Bank SA", totalAmount = 2200000.0, remainingBalance = 1350000.0, emiAmount = 18500.0, interestRate = 11.2, totalMonths = 240, remainingMonths = 130, dueDayOfMonth = 1)
                        )
                    )
                    dao.insertGoals(
                        listOf(
                            GoalEntity(id = 0, title = "JSE Investment & Retirement Fund", category = "South Africa Wealth", targetAmount = 250000.0, currentAmount = 180000.0, monthlyContribution = 6500.0, targetYear = 2026, colorHex = "#10B981")
                        )
                    )
                    dao.insertTransactions(
                        listOf(
                            TransactionEntity(id = 0, title = "Monthly Corporate Salary Credit", amount = 65000.0, type = TransactionType.INCOME, category = Category.SALARY, account = "Standard Bank", dateMillis = now - dayMillis * 2, note = "Monthly salary deposit", isRecurring = true),
                            TransactionEntity(id = 0, title = "Standard Bank Home Loan EMI", amount = 18500.0, type = TransactionType.EXPENSE, category = Category.HOUSING, account = "Standard Bank", dateMillis = now - dayMillis * 1, note = "Mortgage payment", isRecurring = true),
                            TransactionEntity(id = 0, title = "Woolworths Food Groceries", amount = 2800.0, type = TransactionType.EXPENSE, category = Category.FOOD_DINING, account = "Discovery Card", dateMillis = now - dayMillis * 3, note = "Weekly groceries")
                        )
                    )
                }
            }

            // Region seed data is only used by the designated demo/reset flows. Give every
            // seeded amount explicit metadata so it is not mistaken for unresolved user data.
            val currencyCode = region.defaultCurrency.code
            val seededTransactions = dao.getTransactionsSnapshot()
            val accountIds = mutableMapOf<String, Long>()
            seededTransactions.map { it.account }.distinct().forEach { accountName ->
                accountIds[accountName] = dao.insertAccount(
                    AccountEntity(
                        name = accountName,
                        accountType = "DEMO",
                        currencyCode = currencyCode,
                        openingBalance = 0.0,
                        openingBalanceMillis = 0L,
                        openingBalanceConfirmed = true
                    )
                )
            }
            dao.insertTransactions(
                seededTransactions.map { transaction ->
                    transaction.copy(
                        accountId = accountIds[transaction.account],
                        currencyCode = currencyCode
                    )
                }
            )
            dao.insertHoldings(dao.getHoldingsSnapshot().map { it.copy(currencyCode = currencyCode) })
            dao.insertSips(dao.getSipsSnapshot().map { it.copy(currencyCode = currencyCode) })
            dao.insertCreditCards(dao.getCreditCardsSnapshot().map { it.copy(currencyCode = currencyCode) })
            dao.insertLoans(dao.getLoansSnapshot().map { it.copy(currencyCode = currencyCode) })
            dao.insertGoals(dao.getGoalsSnapshot().map { it.copy(currencyCode = currencyCode) })
        }
    }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN transactionKind TEXT NOT NULL DEFAULT 'STANDARD'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN transferGroupId TEXT DEFAULT NULL")
        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `accountType` TEXT NOT NULL, `currencyCode` TEXT, `openingBalance` REAL NOT NULL, `openingBalanceMillis` INTEGER NOT NULL, `statementBalance` REAL, `lastReconciledMillis` INTEGER, `isActive` INTEGER NOT NULL, `openingBalanceConfirmed` INTEGER NOT NULL)")
        db.execSQL("ALTER TABLE holdings ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE sips ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE credit_cards ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE loans ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE goals ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE budgets ADD COLUMN currencyCode TEXT DEFAULT NULL")
        db.execSQL("UPDATE transactions SET category = 'INVESTMENT_SALE', transactionKind = 'ASSET_CONVERSION', importStatus = 'PENDING_REVIEW' WHERE type = 'INCOME' AND category = 'INVESTMENT_SIP' AND sourceReference LIKE 'holdingsale:%'")
        db.execSQL("UPDATE transactions SET importStatus = 'PENDING_REVIEW' WHERE category = 'GOAL_SAVINGS' AND sourceReference LIKE 'goal:%'")
        db.execSQL("UPDATE transactions SET importStatus = 'PENDING_REVIEW' WHERE transactionKind = 'DEBT_SETTLEMENT'")
        db.execSQL("INSERT INTO accounts (name, accountType, currencyCode, openingBalance, openingBalanceMillis, statementBalance, lastReconciledMillis, isActive, openingBalanceConfirmed) SELECT DISTINCT account, 'LEGACY', NULL, 0, 0, NULL, NULL, 1, 0 FROM transactions WHERE TRIM(account) != '' AND transactionKind = 'STANDARD' AND sourceReference IS NULL")
        db.execSQL("UPDATE transactions SET accountId = (SELECT accounts.id FROM accounts WHERE accounts.name = transactions.account LIMIT 1) WHERE accountId IS NULL")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN creditCardId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN loanId INTEGER DEFAULT NULL")
    }
}
