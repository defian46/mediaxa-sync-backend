package com.mediaxa.business.suite

import com.mediaxa.business.suite.data.local.entity.Ingredient
import com.mediaxa.business.suite.data.local.entity.MenuRecipe
import org.junit.Assert.assertEquals
import org.junit.Test

class BOMCalculationTest {

    @Test
    fun testIngredientUnitPriceCalculation() {
        val purchasePrice = 25000.0
        val packageSize = 1000.0
        val unitPrice = purchasePrice / packageSize
        
        assertEquals(25.00, unitPrice, 0.001)

        val cupPurchasePrice = 38000.0
        val cupPackageSize = 50.0
        val cupUnitPrice = cupPurchasePrice / cupPackageSize

        assertEquals(760.0, cupUnitPrice, 0.001)
    }

    @Test
    fun testRecipeHppAndMarginCalculations() {
        val mangga = Ingredient(
            name = "Mangga",
            unit = "gram",
            purchasePrice = 25000.0,
            packageSize = 1000.0,
            unitPrice = 25.0
        )
        val cup = Ingredient(
            name = "Cup 16 oz",
            unit = "pcs",
            purchasePrice = 38000.0,
            packageSize = 50.0,
            unitPrice = 760.0
        )

        val recipeMangga = MenuRecipe(
            menuUuid = "jus_mangga_uuid",
            ingredientUuid = mangga.uuid,
            quantityNeeded = 100.0
        )
        val recipeCup = MenuRecipe(
            menuUuid = "jus_mangga_uuid",
            ingredientUuid = cup.uuid,
            quantityNeeded = 1.0
        )

        val costMangga = mangga.unitPrice * recipeMangga.quantityNeeded
        val costCup = cup.unitPrice * recipeCup.quantityNeeded
        val estimatedHpp = costMangga + costCup

        assertEquals(3260.0, estimatedHpp, 0.001)

        val menuPrice = 10000.0
        val margin = ((menuPrice - estimatedHpp) / menuPrice) * 100

        assertEquals(67.4, margin, 0.001)
    }
}
