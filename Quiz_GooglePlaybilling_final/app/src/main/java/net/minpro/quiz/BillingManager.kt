package net.minpro.quiz

import android.app.Activity
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//数少ない他のactivityをインスタンスさせる処理。
//【メインクラス】
class BillingManager(val activity: Activity) : PurchasesUpdatedListener {


    //【メインクラス2】BillingContract → BillingManagerの処理結果をActivityに通知。
    lateinit var billingContract: BillingContract//自作interface。
    fun setContract(contract: BillingContract){//BillingContractがinterfaceの時点で、コールバックだとわかる。
        //引数となるactivityは、BillingContractの実装部分のみ受け渡しされている。AppCompatActivityは含まない。
        //【重要[推測]】つまり、 billingContract = contract = activityであるため、
        //今後のbillingContract操作は、対象activityの操作となる。
        billingContract = contract
    }

    //BillingClient(顧客を立てる)
    // → 【公式】GooglePlayへの接続を担当し、重いので別のスレッドで動く。-----------------------------------
    //supportFragmentManagerのようなアクセス機能になる。
    val billingClient: BillingClient

    //initは絶対最初にインスタンスするコード。
    init {
        //BillingClient.newBuilder(context).build() → Google Play Billing Library関係のクラスを使用するためのBuild。
        //今回は後ほどactivityを使用するのでContextの子孫であるactivityをいれている。
        //setListener(this) → override fun onPurchasesUpdatedを作成。
        //【確認】setListener(this)は関数型interfaceなのでsetListener{処理}ともできるが
        //この処理が長くなるので今回はコールバックパターン3でいく。
        billingClient = BillingClient.newBuilder(activity).setListener(this).build()

        //自作メソッド：GooglePlayへの接続、接続成功・失敗のコールバックを受け取る。
        connectToPlayBillingService()
    }

    //BillingClientStateListenerは、接続成功・失敗のコールバックを受け取る。
    private fun connectToPlayBillingService() {
        //【読み解き方】
        //ラムダ式型関数複数型override。
        //object:BillingClientStateListenerでAltEnterで、override×2つimplement。
        billingClient.startConnection(object : BillingClientStateListener {
            //接続失敗コールバック。
        override fun onBillingServiceDisconnected() {
                //接続失敗が返ってきた場合、もう1度試みる処理。
                //retrypolicy(引数)に対し、引数に関数を入れている。()省略しているのでわかりにくい。
                //connectToPlayBillingService関数の中に、それを呼び出す関数ができる点におどろき。
                retrypolicy { connectToPlayBillingService() }
            }
            //接続成功コールバック。
        override fun onBillingSetupFinished(responseCode: Int) {
                if (responseCode == BillingClient.BillingResponse.OK){
                    querySkuDetails()
                    queryPurchases()
                }
            }
        })
    }



    private val skuDetails = mutableListOf<SkuDetails>()

    private fun querySkuDetails() {

        fun task(){
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(skuListAll).setType(BillingClient.SkuType.INAPP)
            billingClient.querySkuDetailsAsync(params.build()){ responseCode, skuDetailsList ->  
                if (responseCode == BillingClient.BillingResponse.OK){
                    skuDetails.addAll(skuDetailsList)
                }
            }
        }
        
        retrypolicy { task() }
    }


    fun queryPurchases() {

        fun task(){
            val purchasesResult = ArrayList<Purchase>()
            val result = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
            result?.purchasesList?.apply {
                purchasesResult.addAll(this)
            }

            purchasesResult.forEach { purchase ->
                handlePurchases(purchase)
            }


            billingContract.onQueryInventoryFinished()

        }
        retrypolicy { task() }
    }

    fun handlePurchases(purchase: Purchase) {
        when (purchase.sku){
            sku_GRADE1 -> {
                billingContract.onGetGrade1Purchased()
            }
            sku_GET_3POINTS -> {
                consumePurchase(purchase)
            }
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        if (purchase.sku == sku_GET_3POINTS){
            billingClient.consumeAsync(purchase.purchaseToken) { responseCode, purchaseToken ->
                if (responseCode == BillingClient.BillingResponse.OK){
                    billingContract.onGet3PointsPurchased()
                }

            }
        }

    }

    fun startPurchaseFlow(skuId: String) {
        fun task(){
            val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails.find { it.sku == skuId })
                    .build()
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
        retrypolicy { task() }
    }


    //
    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        when (responseCode){
            BillingClient.BillingResponse.OK -> {
                purchases!!.forEach { purchases ->
                    handlePurchases(purchases)
                }
            }
            BillingClient.BillingResponse.ITEM_ALREADY_OWNED -> {
                makeToast(activity, activity.getString(R.string.item_already_owned))
            }
            BillingClient.BillingResponse.USER_CANCELED -> {
                makeToast(activity, activity.getString(R.string.press_cancel))
            }
            else -> {
                makeToast(activity, activity.getString(R.string.error_occurred))
            }
        }
    }

    //【(task -> Unit)の部分】
    //taskのところの名前はなんでもよい。
    //なぜわざわざ「外だし」したのかというと、GooglePlayとの接続の都度、retrypolicyを行うため。
    //connectToPlayBillingServiceを「task:()」と表現し、それの戻り値が「Unit」ということも再現。
    private fun retrypolicy(task: () -> Unit) {

        CoroutineScope(Dispatchers.Main).launch {
            //概要 ： BillingClientが isReady (接続) されていなければ connectToPlayBillingService() へ接続を試みる。
            //delay(2000L)2秒間試みます。
            //問題なければtask()します。
            if (!billingClient.isReady) {
                //ここの呼び出し関数は、引数とは関係ない。
                connectToPlayBillingService()
                delay(2000L)
            }
            //引数
            task()
        }
    }

    fun destroy() {
        billingClient.endConnection()

    }


}