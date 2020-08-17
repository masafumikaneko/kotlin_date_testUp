package net.minpro.quiz

interface BillingContract {
    fun onQueryInventoryFinished()
    fun onGetGrade1Purchased()
    fun onGet3PointsPurchased()
}