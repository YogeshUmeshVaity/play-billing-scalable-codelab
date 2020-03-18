package com.example.playbilling.trivialdrive.kotlin.billingrepo

import android.app.Application
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.playbilling.trivialdrive.kotlin.billingrepo.BillingRepository.GameSku.CONSUMABLE_SKUS
import com.example.playbilling.trivialdrive.kotlin.billingrepo.BillingRepository.GameSku.INAPP_SKUS
import com.example.playbilling.trivialdrive.kotlin.billingrepo.BillingRepository.GameSku.SUBS_SKUS
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class BillingRepository private constructor(private val application: Application) :
        PurchasesUpdatedListener, BillingClientStateListener,
        ConsumeResponseListener, SkuDetailsResponseListener {

    companion object {
        private const val LOG_TAG = "BillingRepository"

        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(application: Application): BillingRepository =
                INSTANCE ?: synchronized(this) {
                    INSTANCE
                            ?: BillingRepository(application)
                                    .also { INSTANCE = it }
                }
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBillingServiceDisconnected() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBillingSetupFinished(responseCode: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConsumeResponse(responseCode: Int, purchaseToken: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSkuDetailsResponse(responseCode: Int, skuDetailsList: MutableList<SkuDetails>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * This private object class shows an example retry policies. You may choose to replace it with
     * your own policies.
     */
    private object RetryPolicies {
        private val maxRetry = 5
        private var retryCounter = AtomicInteger(1)
        private val baseDelayMillis = 500
        private val taskDelay = 2000L

        fun resetConnectionRetryPolicyCounter() {
            retryCounter.set(1)
        }

        /**
         * This works because it actually only makes one call. Then it waits for success or failure.
         * onSuccess it makes no more calls and resets the retryCounter to 1. onFailure another
         * call is made, until too many failures cause retryCounter to reach maxRetry and the
         * policy stops trying. This is a safe algorithm: the initial calls to
         * connectToPlayBillingService from instantiateAndConnectToPlayBillingService is always
         * independent of the RetryPolicies. And so the Retry Policy exists only to help and never
         * to hurt.
         */

        fun connectionRetryPolicy(block: () -> Unit) {
            Log.d(LOG_TAG, "connectionRetryPolicy")
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                val counter = retryCounter.getAndIncrement()
                if (counter < maxRetry) {
                    val waitTime: Long = (2f.pow(counter) * baseDelayMillis).toLong()
                    delay(waitTime)
                    block()
                }
            }
        }

        /**
         * All this is doing is check that billingClient is connected and if it's
         * not, request connection, wait x number of seconds and then proceed with
         * the actual task.
         */
        fun taskExecutionRetryPolicy(billingClient: BillingClient, listener: BillingRepository, task: () -> Unit) {
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                if (!billingClient.isReady) {
                    Log.d(LOG_TAG, "taskExecutionRetryPolicy billing not ready")
                    billingClient.startConnection(listener)
                    delay(taskDelay)
                }
                task()
            }
        }
    }

    /**
     * This is the throttling valve. It is used to modulate how often calls are
     * made to the secure server in order to save money.
     */
    private object Throttle {
        private val DEAD_BAND = 7200000//2*60*60*1000: two hours wait
        private val PREFS_NAME = "BillingRepository.Throttle"
        private val KEY = "lastInvocationTime"

        fun isLastInvocationTimeStale(context: Context): Boolean {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastInvocationTime = sharedPrefs.getLong(KEY, 0)
            return lastInvocationTime + DEAD_BAND < Date().time
        }

        fun refreshLastInvocationTime(context: Context) {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putLong(KEY, Date().time)
                apply()
            }
        }
    }

    /**
     * [INAPP_SKUS], [SUBS_SKUS], [CONSUMABLE_SKUS]:
     *
     * Where you define these lists is quite truly up to you. If you don't need
     * customization, then it makes since to define and hardcode them here, as
     * I am doing. Keep simple things simple. But there are use cases where you may
     * need customization:
     *
     * - If you don't want to update your APK (or Bundle) each time you change your
     *   SKUs, then you may want to load these lists from your secure server.
     *
     * - If your design is such that users can buy different items from different
     *   Activities or Fragments, then you may want to define a list for each of
     *   those subsets. I only have two subsets: INAPP_SKUS and SUBS_SKUS
     */

    private object GameSku {
        val GAS = "gas"
        val PREMIUM_CAR = "premium_car"
        val GOLD_MONTHLY = "gold_monthly"
        val GOLD_YEARLY = "gold_yearly"

        val INAPP_SKUS = listOf(GAS, PREMIUM_CAR)
        val SUBS_SKUS = listOf(GOLD_MONTHLY, GOLD_YEARLY)
        val CONSUMABLE_SKUS = listOf(GAS)
        //coincidence that there only gold_status is a sub
        val GOLD_STATUS_SKUS = SUBS_SKUS
    }
}
