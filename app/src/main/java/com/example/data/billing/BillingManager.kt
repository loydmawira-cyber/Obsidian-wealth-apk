package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager(context: Context) : BillingClientStateListener {
    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_annual"
    }

    private val appContext = context.applicationContext
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var premiumProduct: ProductDetails? = null
    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.orEmpty().forEach { processPurchase(it) }
            } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                _message.value = "Google Play billing error: ${billingResult.debugMessage}"
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        try {
            billingClient.startConnection(this)
        } catch (e: Exception) {
            _message.value = "Google Play Billing unavailable: ${e.localizedMessage}"
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _message.value = "Google Play Billing is unavailable: ${billingResult.debugMessage}"
            return
        }
        queryPremiumProduct()
        refreshPurchases()
    }

    override fun onBillingServiceDisconnected() {
        _message.value = "Google Play Billing disconnected. Try again later."
    }

    private fun queryPremiumProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                premiumProduct = productDetailsList.firstOrNull()
                if (premiumProduct == null) {
                    _message.value = "Premium subscription is not available in Google Play yet."
                }
            } else {
                _message.value = "Could not load Premium: ${billingResult.debugMessage}"
            }
        }
    }

    private fun refreshPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isPremium.value = purchases.any { purchase ->
                    purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                purchases.forEach { processPurchase(it) }
            }
        }
    }

    fun launchPremiumPurchase(activity: Activity) {
        val product = premiumProduct
        if (product == null) {
            _message.value = "Premium is still loading. Please try again in a moment."
            queryPremiumProduct()
            return
        }

        val offer = product.subscriptionOfferDetails?.firstOrNull()
        if (offer == null) {
            _message.value = "No annual Premium offer is available in Google Play."
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offer.offerToken)
            .build()

        val billingParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        billingClient.launchBillingFlow(activity, billingParams)
    }

    private fun processPurchase(purchase: Purchase) {
        if (!purchase.products.contains(PREMIUM_PRODUCT_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        _isPremium.value = true
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _message.value = "Premium purchase needs acknowledgment: ${result.debugMessage}"
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
