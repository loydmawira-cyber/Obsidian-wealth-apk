package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.models.Category
import com.example.data.models.CreditCardEntity
import com.example.data.models.GoalEntity
import com.example.data.models.HoldingEntity
import com.example.data.models.HoldingType
import com.example.data.models.LoanEntity
import com.example.data.models.SipEntity
import com.example.data.models.TransactionEntity
import com.example.data.models.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        HoldingEntity::class,
        SipEntity::class,
        CreditCardEntity::class,
        LoanEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = false
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
                ).fallbackToDestructiveMigration()
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
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.financeDao())
                    }
                }
            }
        }

        suspend fun reseedDatabase(dao: FinanceDao) {
            dao.clearAllTransactions()
            dao.clearAllHoldings()
            dao.clearAllSips()
            dao.clearAllCreditCards()
            dao.clearAllLoans()
            dao.clearAllGoals()
            populateDatabase(dao)
        }

        suspend fun populateDatabase(dao: FinanceDao) {
            // Seed Holdings (KSh 9,074,750 aggregate investment portfolio)
            dao.insertHoldings(
                listOf(
                    HoldingEntity(
                        symbol = "SCOM",
                        name = "Safaricom PLC (NSE)",
                        type = HoldingType.STOCK,
                        shares = 85000.0,
                        avgBuyPrice = 16.50,
                        currentPrice = 19.80,
                        dailyChangePercent = 1.54
                    ),
                    HoldingEntity(
                        symbol = "EQTY",
                        name = "Equity Group Holdings PLC",
                        type = HoldingType.STOCK,
                        shares = 28000.0,
                        avgBuyPrice = 38.00,
                        currentPrice = 44.50,
                        dailyChangePercent = 0.80
                    ),
                    HoldingEntity(
                        symbol = "EABL",
                        name = "East African Breweries PLC",
                        type = HoldingType.STOCK,
                        shares = 6500.0,
                        avgBuyPrice = 142.00,
                        currentPrice = 168.00,
                        dailyChangePercent = 0.60
                    ),
                    HoldingEntity(
                        symbol = "KCB",
                        name = "KCB Group PLC",
                        type = HoldingType.STOCK,
                        shares = 15000.0,
                        avgBuyPrice = 28.50,
                        currentPrice = 36.20,
                        dailyChangePercent = 1.10
                    ),
                    HoldingEntity(
                        symbol = "IFB",
                        name = "CBK Infrastructure Bond (17.9% Tax-Free)",
                        type = HoldingType.MUTUAL_FUND,
                        shares = 25000.0,
                        avgBuyPrice = 100.00,
                        currentPrice = 100.00,
                        dailyChangePercent = 0.05
                    ),
                    HoldingEntity(
                        symbol = "CIC-MMF",
                        name = "CIC Money Market Fund (Daily Compounding)",
                        type = HoldingType.MUTUAL_FUND,
                        shares = 980000.0,
                        avgBuyPrice = 1.00,
                        currentPrice = 1.00,
                        dailyChangePercent = 0.04
                    ),
                    HoldingEntity(
                        symbol = "GOLD",
                        name = "Physical Gold Sovereigns & Bullion (Custody)",
                        type = HoldingType.GOLD,
                        shares = 35.0,
                        avgBuyPrice = 8200.0,
                        currentPrice = 9450.0,
                        dailyChangePercent = 0.85
                    ),
                    HoldingEntity(
                        symbol = "BTC",
                        name = "Bitcoin Treasury Cold Reserve",
                        type = HoldingType.CRYPTO,
                        shares = 0.08,
                        avgBuyPrice = 6800000.0,
                        currentPrice = 8750000.0,
                        dailyChangePercent = 3.10
                    )
                )
            )

            // Seed SIPs (Standing Orders & Automated Mandates)
            dao.insertSips(
                listOf(
                    SipEntity(
                        fundName = "CIC Money Market Fund (Daily Compounding)",
                        category = "Liquid Yield & Capital Preservation",
                        monthlyAmount = 50000.0,
                        debitDayOfMonth = 1,
                        isActive = true,
                        totalInvested = 450000.0,
                        annualizedReturnPercent = 13.2
                    ),
                    SipEntity(
                        fundName = "Safaricom PLC NSE Share Accumulation",
                        category = "Nairobi Securities Exchange Dividend Alpha",
                        monthlyAmount = 25000.0,
                        debitDayOfMonth = 5,
                        isActive = true,
                        totalInvested = 320000.0,
                        annualizedReturnPercent = 18.5
                    ),
                    SipEntity(
                        fundName = "Britam Wealth & Unit Trust Fund",
                        category = "Balanced East Africa Growth",
                        monthlyAmount = 20000.0,
                        debitDayOfMonth = 15,
                        isActive = true,
                        totalInvested = 210000.0,
                        annualizedReturnPercent = 14.8
                    )
                )
            )

            // Seed Credit Cards
            dao.insertCreditCards(
                listOf(
                    CreditCardEntity(
                        cardName = "Standard Chartered Infinite Card",
                        lastFour = "4421",
                        currentBalance = 85000.0,
                        creditLimit = 500000.0,
                        apr = 24.0,
                        dueDateDays = 5,
                        colorHex = "#1E3A8A"
                    ),
                    CreditCardEntity(
                        cardName = "KCB Platinum Card",
                        lastFour = "8023",
                        currentBalance = 34000.0,
                        creditLimit = 250000.0,
                        apr = 22.5,
                        dueDateDays = 12,
                        colorHex = "#047857"
                    ),
                    CreditCardEntity(
                        cardName = "NCBA Loop Digital Credit",
                        lastFour = "1940",
                        currentBalance = 18500.0,
                        creditLimit = 150000.0,
                        apr = 18.0,
                        dueDateDays = 20,
                        colorHex = "#B45309"
                    )
                )
            )

            // Seed Loans
            dao.insertLoans(
                listOf(
                    LoanEntity(
                        loanName = "Stanbic Prime Home Loan",
                        lender = "Stanbic Bank Kenya",
                        totalAmount = 6500000.0,
                        remainingBalance = 3200000.0,
                        emiAmount = 58000.0,
                        interestRate = 14.5,
                        totalMonths = 120,
                        remainingMonths = 54,
                        dueDayOfMonth = 1
                    ),
                    LoanEntity(
                        loanName = "NCBA Vehicle Asset Finance (Harrier Hybrid)",
                        lender = "NCBA Bank Kenya",
                        totalAmount = 2400000.0,
                        remainingBalance = 620000.0,
                        emiAmount = 46000.0,
                        interestRate = 13.8,
                        totalMonths = 48,
                        remainingMonths = 12,
                        dueDayOfMonth = 10
                    )
                )
            )

            // Seed Goals
            dao.insertGoals(
                listOf(
                    GoalEntity(
                        title = "1-Acre Commercial Plot in Nanyuki",
                        category = "Land & Real Estate Acquisition",
                        targetAmount = 4500000.0,
                        currentAmount = 2800000.0,
                        monthlyContribution = 65000.0,
                        targetYear = 2026,
                        colorHex = "#10B981"
                    ),
                    GoalEntity(
                        title = "CBK Infrastructure Bond Primary Issue",
                        category = "Tax-Free Fixed Income Reserve",
                        targetAmount = 2000000.0,
                        currentAmount = 1450000.0,
                        monthlyContribution = 40000.0,
                        targetYear = 2026,
                        colorHex = "#6366F1"
                    ),
                    GoalEntity(
                        title = "Family Health & Emergency Reserve (MMF)",
                        category = "Liquid Safety Net (6-Mo)",
                        targetAmount = 1500000.0,
                        currentAmount = 1200000.0,
                        monthlyContribution = 30000.0,
                        targetYear = 2026,
                        colorHex = "#06B6D4"
                    ),
                    GoalEntity(
                        title = "Diani Holiday Villa Vacation",
                        category = "Leisure & Coastal Travel",
                        targetAmount = 350000.0,
                        currentAmount = 240000.0,
                        monthlyContribution = 25000.0,
                        targetYear = 2025,
                        colorHex = "#EC4899"
                    )
                )
            )

            // Seed Transactions
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L
            dao.insertTransactions(
                listOf(
                    TransactionEntity(
                        title = "Executive Tech Salary Inflow",
                        amount = 450000.0,
                        type = TransactionType.INCOME,
                        category = Category.SALARY,
                        account = "NCBA Bank Nairobi",
                        dateMillis = now - (dayMillis * 2),
                        note = "Monthly corporate salary deposit via RTGS",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "FinTech Consulting Retainer (EAC Advisory)",
                        amount = 85000.0,
                        type = TransactionType.INCOME,
                        category = Category.FREELANCE,
                        account = "M-PESA Business Till",
                        dateMillis = now - (dayMillis * 5),
                        note = "Q3 advisory project payout"
                    ),
                    TransactionEntity(
                        title = "NSE & Safaricom Interim Dividend",
                        amount = 35000.0,
                        type = TransactionType.INCOME,
                        category = Category.DIVIDENDS,
                        account = "Standard Investment Bank CDS",
                        dateMillis = now - (dayMillis * 8),
                        note = "SCOM interim dividend payout to CDS account"
                    ),
                    TransactionEntity(
                        title = "Stanbic Home Loan Monthly Amortization",
                        amount = 58000.0,
                        type = TransactionType.EXPENSE,
                        category = Category.LOAN_EMI,
                        account = "Stanbic Checking",
                        dateMillis = now - (dayMillis * 1),
                        note = "Principal KSh 49,200 + Interest KSh 8,800",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "NCBA Car Loan Deduction",
                        amount = 46000.0,
                        type = TransactionType.EXPENSE,
                        category = Category.LOAN_EMI,
                        account = "NCBA Bank",
                        dateMillis = now - (dayMillis * 3),
                        note = "Vehicle asset finance standing order",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "CIC Money Market Fund Standing Order",
                        amount = 50000.0,
                        type = TransactionType.EXPENSE,
                        category = Category.INVESTMENT_SIP,
                        account = "NCBA Bank",
                        dateMillis = now - (dayMillis * 4),
                        note = "Recurring automated MMF wealth accumulation",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "Safaricom Share Accumulation SIP",
                        amount = 25000.0,
                        type = TransactionType.EXPENSE,
                        category = Category.INVESTMENT_SIP,
                        account = "M-PESA",
                        dateMillis = now - (dayMillis * 6),
                        note = "Automated NSE equity purchase via Dyer & Blair",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "Carrefour Two Rivers Pantry Groceries",
                        amount = 18400.0,
                        type = TransactionType.EXPENSE,
                        category = Category.FOOD_DINING,
                        account = "Standard Chartered Card",
                        dateMillis = now - (dayMillis * 7),
                        note = "Bi-weekly household groceries and pantry"
                    ),
                    TransactionEntity(
                        title = "Java House & Artcaffe Client Meetings",
                        amount = 6800.0,
                        type = TransactionType.EXPENSE,
                        category = Category.FOOD_DINING,
                        account = "NCBA Loop",
                        dateMillis = now - (dayMillis * 9),
                        note = "Advisory lunch & coffee meetings"
                    ),
                    TransactionEntity(
                        title = "KPLC Prepaid Electricity Tokens",
                        amount = 8500.0,
                        type = TransactionType.EXPENSE,
                        category = Category.UTILITIES,
                        account = "M-PESA",
                        dateMillis = now - (dayMillis * 10),
                        note = "Stima monthly token purchase via Paybill 888880",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "Safaricom Home Fiber 50Mbps",
                        amount = 5000.0,
                        type = TransactionType.EXPENSE,
                        category = Category.UTILITIES,
                        account = "M-PESA",
                        dateMillis = now - (dayMillis * 12),
                        note = "High speed home internet monthly subscription",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "Rubis Energy V-Power Fuel",
                        amount = 12500.0,
                        type = TransactionType.EXPENSE,
                        category = Category.TRANSPORT,
                        account = "KCB Platinum Card",
                        dateMillis = now - (dayMillis * 13),
                        note = "Full tank fuel at Rubis Westlands",
                        isRecurring = true
                    ),
                    TransactionEntity(
                        title = "Westlands Health & Rackets Club Gym",
                        amount = 9500.0,
                        type = TransactionType.EXPENSE,
                        category = Category.HEALTHCARE,
                        account = "Standard Chartered Card",
                        dateMillis = now - (dayMillis * 14),
                        note = "Monthly fitness and recovery membership",
                        isRecurring = true
                    )
                )
            )
        }
    }
}
