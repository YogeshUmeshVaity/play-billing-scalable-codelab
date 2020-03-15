package com.example.playbilling.trivialdrive.kotlin.billingrepo

import com.android.billingclient.api.Purchase

class BillingWebservice {
    fun getPurchases(): Any {
        return Any()//TODO("not implemented")
    }
    fun updateServer(purchases: Set<Purchase>) {
        //TODO("not implemented")
    }
    fun onComsumeResponse(purchaseToken: String?, responseCode: Int) {
        //TODO("not implemented")
    }
    companion object {
        fun create(): BillingWebservice {
            //TODO("not implemented")
            return BillingWebservice()
        }
    }
}
