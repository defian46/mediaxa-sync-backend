package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.entity.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DatabasePerformanceTest {

    private fun generateTransactions(count: Int): List<Transaction> {
        val list = ArrayList<Transaction>(count)
        val now = System.currentTimeMillis()
        for (i in 0 until count) {
            list.add(
                Transaction(
                    localId = i.toLong() + 1,
                    uuid = UUID.randomUUID().toString(),
                    storeId = 1L,
                    deviceId = "DEV-TEST",
                    transactionNumber = "TRX-%06d".format(i),
                    timestamp = now - (i * 60_000L), // spread out in the past
                    cashierUuid = "cashier-1",
                    cashierName = "Kasir Test",
                    discount = if (i % 5 == 0) 5000.0 else 0.0,
                    subtotal = 50000.0,
                    total = if (i % 5 == 0) 45000.0 else 50000.0,
                    transactionHpp = 20000.0,
                    grossProfit = if (i % 5 == 0) 25000.0 else 30000.0,
                    paymentMethod = "CASH",
                    amountReceived = 50000.0,
                    changeAmount = if (i % 5 == 0) 5000.0 else 0.0,
                    status = "COMPLETED",
                    updatedAt = now
                )
            )
        }
        return list
    }

    private fun runBenchmark(count: Int) {
        println("=== BENCHMARK FOR $count TRANSACTIONS ===")

        // 1. Generation & Mock Insertion Time
        val startGen = System.currentTimeMillis()
        val transactions = generateTransactions(count)
        val endGen = System.currentTimeMillis()
        println("Generated & indexed $count items in ${endGen - startGen} ms.")

        // 2. Index Lookup Time (simulate Map/Cache by UUID)
        val startMap = System.currentTimeMillis()
        val uuidMap = transactions.associateBy { it.uuid }
        val endMap = System.currentTimeMillis()
        println("Mapped by UUID in ${endMap - startMap} ms.")

        // Pick a random UUID and lookup
        val targetUuid = transactions[count / 2].uuid
        val startLookup = System.nanoTime()
        val found = uuidMap[targetUuid]
        val endLookup = System.nanoTime()
        println("Lookup by UUID took ${endLookup - startLookup} ns.")
        assertTrue(found != null)

        // 3. Analytics Calculation Time (simulate Dashboard load)
        val startAnalytics = System.currentTimeMillis()
        var totalRevenue = 0.0
        var totalDiscount = 0.0
        var totalHpp = 0.0
        var totalGrossProfit = 0.0

        for (tx in transactions) {
            totalRevenue += tx.total
            totalDiscount += tx.discount
            totalHpp += tx.transactionHpp
            totalGrossProfit += tx.grossProfit
        }
        val endAnalytics = System.currentTimeMillis()
        println("Dashboard analytics calculation took ${endAnalytics - startAnalytics} ms.")
        
        // Assertions to verify correctness
        assertTrue(totalRevenue > 0)
        println("Total Revenue: IDR $totalRevenue | Total HPP: IDR $totalHpp\n")
    }

    @Test
    fun benchmark1kTransactions() {
        runBenchmark(1000)
    }

    @Test
    fun benchmark10kTransactions() {
        runBenchmark(10000)
    }

    @Test
    fun benchmark100kTransactions() {
        runBenchmark(100000)
    }
}
