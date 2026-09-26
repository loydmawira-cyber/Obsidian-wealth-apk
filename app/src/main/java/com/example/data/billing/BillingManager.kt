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

data class PremiumOfferInfo(
    val summary: String,
    val hasThreeMonthFreeTrial: Boolean
)

class BillingManager(context: Context) : BillingClientStateListener {
    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_annual"
    }

    private val appContext = context.applicationContext
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _offerInfo = MutableStateFlow<PremiumOfferInfo?>(null)
    val offerInfo: StateFlow<PremiumOfferInfo?> = _offerInfo

    private var premiumProduct: ProductDetails? = null
    private var selectedOffer: ProductDetails.SubscriptionOfferDetails? = null
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
        ) { billingResult, queryProductDetailsResult ->
            // PBL 8+ wraps results in QueryProductDetailsResult (fetched + unfetched lists)
            // instead of handing back a raw List<ProductDetails> directly.
            val productDetailsList = queryProductDetailsResult.productDetailsList
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                premiumProduct = productDetailsList.firstOrNull { !it.subscriptionOfferDetails.isNullOrEmpty() }
                    ?: productDetailsList.firstOrNull()
                if (premiumProduct == null) {
                    _message.value = "Premium subscription is not available in Google Play yet."
                    _offerInfo.value = null
                } else {
                    val offers = premiumProduct?.subscriptionOfferDetails.orEmpty()
                    selectedOffer = offers.firstOrNull(::isThreeMonthFreeTrial)
                        ?: offers.firstOrNull { it.offerId == null }
                        ?: offers.firstOrNull()
                    _offerInfo.value = selectedOffer?.let(::toOfferInfo)
                    _message.value = if (selectedOffer == null) {
                        "No Premium subscription offer is available in Google Play."
                    } else {
                        null
                    }
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

        val offer = selectedOffer ?: product.subscriptionOfferDetails?.firstOrNull()
        if (offer == null) {
            _message.value = "No Premium subscription offer is available in Google Play."
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

    private fun isThreeMonthFreeTrial(offer: ProductDetails.SubscriptionOfferDetails): Boolean {
        val phases = offer.pricingPhases.pricingPhaseList
        val trialIndex = phases.indexOfFirst { it.priceAmountMicros == 0L && it.billingPeriod == "P3M" }
        return trialIndex >= 0 && phases.drop(trialIndex + 1).any { it.priceAmountMicros > 0L }
    }

    private fun toOfferInfo(offer: ProductDetails.SubscriptionOfferDetails): PremiumOfferInfo {
        val phases = offer.pricingPhases.pricingPhaseList
        val trialIndex = phases.indexOfFirst { it.priceAmountMicros == 0L && it.billingPeriod == "P3M" }
        val paidPhase = if (trialIndex >= 0) {
            phases.drop(trialIndex + 1).lastOrNull { it.priceAmountMicros > 0L }
        } else {
            phases.lastOrNull { it.priceAmountMicros > 0L }
        }
        val trialAvailable = trialIndex >= 0 && paidPhase != null
        val paidTerms = paidPhase?.let { "${it.formattedPrice} / ${periodLabel(it.billingPeriod)}" }
        val text = when {
            trialAvailable && paidTerms != null -> "3 months free, then $paidTerms. Auto-renews until canceled."
            paidTerms != null -> "$paidTerms. Auto-renews until canceled. A 3-month trial is not available for this account."
            else -> "Premium subscription terms will be shown by Google Play before purchase."
        }
        return PremiumOfferInfo(text, trialAvailable)
    }

    private fun periodLabel(isoPeriod: String): String {
        val parts = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)W)?(?:(\\d+)D)?$")
            .matchEntire(isoPeriod)?.groupValues ?: return "billing period"
        val years = parts[1].toIntOrNull() ?: 0
        val months = parts[2].toIntOrNull() ?: 0
        val weeks = parts[3].toIntOrNull() ?: 0
        val days = parts[4].toIntOrNull() ?: 0
        return when {
            years > 0 && months == 0 -> if (years == 1) "year" else "$years years"
            years > 0 -> "${years * 12 + months} months"
            months > 0 -> if (months == 1) "month" else "$months months"
            weeks > 0 && days == 0 -> if (weeks == 1) "week" else "$weeks weeks"
            weeks > 0 -> "${weeks * 7 + days} days"
            days > 0 -> if (days == 1) "day" else "$days days"
            else -> "billing period"
        }
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
