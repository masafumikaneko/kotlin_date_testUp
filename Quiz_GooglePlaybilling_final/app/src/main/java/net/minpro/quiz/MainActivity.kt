package net.minpro.quiz

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import io.realm.Realm
import io.realm.RealmResults
import io.realm.internal.OsResults
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception

class MainActivity : AppCompatActivity(),
        AlertDialogFragment.AlertDialogListerner, BillingContract {

    //全面広告の空間を出すクラス。
    lateinit var interstitialAd: InterstitialAd
    //1級に進むのか2級なのかを変数で持っておく。
    var gradeOfTest: Int = 0

    //【重要】ここで初期化しているのは自作クラスの方で、既存クラスのBillingContractではない点を注意。
    lateinit var billingManager: BillingManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//【超重要】ラムダ式による引数がit:Viewであることに注目する。そして今回、このchangeSoundModeの中身は、Functions.ktファイルでトップライン関数として処理されている。(fabボタンが様々なｱｸﾃｨﾋﾞﾃｨで使用されるため。)
        //【重要】fab_soundはFloatingActionButtonにも拘わらず、itの型がViewになっている。つまり！ラムダ式を用いた際の型は「変化」するので注意。ここでの場合、Functionsの方で関数を書いているが、
        //ラムダ式で引数がView型というかなり上の親になっているので、送った先ではかならずダウンキャストすること。
        //なぜ、floatingActionButtonという型がViewになっているかというと、ラムダ式使用の際、fun setOnClickListener(v: View){~}の引数にfavが入ったため
        fab_sound.setOnClickListener {
            changeSoundMode(it)
        }

        importQuestionsFromCSV()  //CSVを取り込むメソッド。

        //バナー広告メソッドの呼び出し。
        setBannerAd()
        //全画面広告メソッドの呼び出し。
        setInterstitialAd()
        //アプリ内課金の設定
        setUpAppBilling()

        //1級ボタン
        btnGrade1.setOnClickListener {
            if (isPaidGrade1){//課金されているかのチェック。
                gradeOfTest = 1 //1級に進む場合、当ﾌﾟﾛﾊﾟﾃｨの値を1にする。
                goTest(gradeOfTest)
                return@setOnClickListener//【重要】早期リターン。
            }
            billingManager.startPurchaseFlow(sku_GRADE1)


        }
        //2級ボタン
        btnGrade2.setOnClickListener {
            gradeOfTest = 2//1級に進む場合、当ﾌﾟﾛﾊﾟﾃｨの値を2にする。
            goTest(gradeOfTest)//2級に進む際は、条件などないので、当関数において、gradeOfTest変数を引数に、飛ぶ。
        }
    }


    private fun goTest(gradeOfTest: Int) : Unit {
        //【重要】ポイントは、条件分岐をするタイミング。goTestに行く前にチェックを行うのかと考えたが、goTestに飛んでから、つまり、goTestの処理開始前に
        //条件分岐確認をすることが正しい。
        if (!isDataSetFinished) return makeToast(this@MainActivity, getString(R.string.import_fail_notice))
        if (!interstitialAd.isLoaded) return makeToast(this@MainActivity, getString(R.string.import_fail_notice))

        //【重要】ダイアログFragmentをインスタンス。てかFragmentもインスタンスできるのか。
        val dialog = AlertDialogFragment()
        //【重要】見せるFragmentを指定。ここで重要なのは、「supportLibraryでなくandroidXの場合は、supportFragmentManagerではなくなっている可能性が
        // ほんの多少ある。」
        dialog.show(supportFragmentManager, "GoTestAlert")
    }

    //AlertDialogFragment
    override fun onPositiveButtonClicked() {
        interstitialAd.show()//全面広告を表示。
    }
    //AlertDialogFragment
    override fun onNegativeButtonClicked() {
        makeToast(this@MainActivity, getString(R.string.press_cancel))
    }

    private fun setInterstitialAd() {

        //全面広告の空間を出すクラスInterstitialAd(this)をこのクラスに設定。このクラスとInterstitialAdクラスを結ぶ。
        interstitialAd = InterstitialAd(this)
        //InterstitialAdクラスに「広告ID」を設置。
        interstitialAd.adUnitId = getString(R.string.interstitial_ad_unit_id)
        //InterstitialAd(全画面広告管理クラス)に設定した広告をロードするメソッドを呼び出し。
        loadInterstitialAd()


        //interstitialAd.adListener = object : AdListener(){override fun onAdClosed() {処理}
        //上のコードにおいて、なぜ代入が可能だったかという要因について説明を行う。Javaコードにおいて、.adListenerﾌﾟﾛﾊﾟﾃｨは
        //setAdListener() メソッドを意味する(F4)を押せばわかる。しかも引数は限定されており、AdListenerクラスのみしか入れられない仕様と
        //なっている。public void setAdListener （AdListener adListener）
        //そのため、その引数にはAdListenerアブストラクトクラスとメソッドのoverrideが入る。
        //つまり、interstitialAd.setAdListener( 引数 ： object : AdListener(){override fun onAdClosed() {処理})ということになり、
        //引数が1つしかない場合、「 = 」に変更が可能なので、この形態となった。
        //【重要】さらにここでは、object:AdListenerを省略してラムダ式にできそうだが、このAdListenerのもつメソッドは、1つだけではないため、
        //省略不可となり、ラムダ式にはできなかった。
        interstitialAd.adListener = object : AdListener(){
            override fun onAdClosed() {
                //たしか引数が1つしかない時、{}から=という表記方法に変更できた。
                loadInterstitialAd()  //【重要】ここでは広告画面が閉じられた場合の処理が書かれる。
                //つまり広告が閉じられたのだから、「広告のリロード」を行うというシンプルな流れに基づき実行されているが、
                //他のライフサイクルでも賄えている気がするので、本当に必要かは疑問。
                goTestActivity(gradeOfTest)
            }
        }
    }

    //TestActivityへのintentわたし。渡すものは、「級」。
    private fun goTestActivity(gradeOfTest: Int) {
        val intent = Intent(this@MainActivity, TestActivity::class.java).apply {
            putExtra(IntentKey.GRADE_OF_TEST.name, gradeOfTest)
        }
        startActivity(intent)
    }
    //InterstitialAd(全画面広告管理クラス)に設定した広告をロード。
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        interstitialAd.loadAd(adRequest)
    }


    private fun setBannerAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

    }

    override fun onResume() {
        super.onResume()
        updateUi()
        adView?.resume()
        billingManager.queryPurchases()

        //他の画面から戻った際、全画面広告のロードがすんでいない場合、ロードする。
        //尚、f4でわかるが、interstitialAd.isLoadedﾌﾟﾛﾊﾟﾃｨはBoolean型。
        if (!interstitialAd.isLoaded) loadInterstitialAd()
    }



    override fun onPause() {
        super.onPause()
        adView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        adView?.destroy()
        billingManager.destroy()


    }
//1級入場ボタンの条件ごとのバックグラウンドリソース設定。
    private fun updateUi() {
        if (isPassGrade2 && isPaidGrade1){
            btnGrade1.setBackgroundResource(R.drawable.button_grade1)
            btnGrade1.isEnabled = true
        } else if(isPassGrade2 && !isPaidGrade1){//課金はしていない。
            btnGrade1.setBackgroundResource(R.drawable.button_go_grade1)
            btnGrade1.isEnabled = true
        } else {
            btnGrade1.setBackgroundResource(R.drawable.button_vague_grade1)
            btnGrade1.isEnabled = false
        }

    }
//CSVを取り込み、Realmにセットするメソッド。
    private fun importQuestionsFromCSV() {

    //◆CSVを取り込むメソッド。---------------------
        val reader = setCSVReader() //readerに入っているのは、setCSVReaderの戻り値であるCSVReaderになる。
    //-------------------------------------------
    //1. Array<String>は、要素数を変えられない配列を示す。(注)今回のCSVデータの場合でいうと、列(field)は8列と定まっているため変更不可のArrayを使用。
    //2. MutableList<>は、要素数および内容どちらも変更可能な配列を示す。
    //上記1.2.を読み解く場合 → Array<String>(要素数を変えられない配列)が、MutableList<>(今後その配列自体の数がいくつになるかわからない。)として宣言されているということ。
    //また初期値をnullとしているがそれは、Array<String>による以後の要素数変更不可の制約があるためで、もし仮に""を入れてしまった場合、その内容の変更が出来なくなるので、こういう場合は、nullを指定。
    //私自身、実務で分かっていると思うが、データベースとしてのCSVの形態はこれしかないといえるので、ktにおけるcsv取り込みの際の型も自ずとこれしかないといえる。
        var tempList: MutableList<Array<String>>? = null
        try { //CSVのデータ取り込みつまりInputStreamの取り込みは失敗のリスクが比較的あるので、try処理が推奨されている。
            tempList = reader.readAll() //CSVReader.readAll()
    //◆Realmに放り込むメソッド。-------------------
            writeCSVDataToRealm(tempList!!)
    //-------------------------------------------

        } catch (e: IOException){//CSVのデータ(InPutStreamのｴﾗｰ時に表示されるコメント。)
            makeToast(this@MainActivity, getString(R.string.import_fail))//失敗しましたのトースト。
            //ConstantsファイルにてCSVのデータ読み込み処理の成功チェックﾌﾟﾛﾊﾟﾃｨ。
            isDataSetFinished = false//失敗しました。
        } finally {
            reader.close()//CSVReader.close()
        }

    }
    //◆Realmに放り込むメソッド。
    private fun writeCSVDataToRealm(tempList: MutableList<Array<String>>) {
        val realm = Realm.getDefaultInstance()
        //なぜ、.iterator()メソッドが使用可能でどういった経由なのか説明。
        //↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
        //interface MutableList<E> : 「List<E>」, MutableCollection<E>実装
        //interface List<out E> : 「Collection<E>」実装
        //interface Collection<out E> : 「Iterable<E>」実装
        //abstract operator 「fun iterator(): Iterator<T>」実装  →  【超重要】 .iterator()が使用可能になる。
        val iterator = tempList.iterator()  //iterator(): Iterator<T> → 対象のオブジェクトの要素の反復子を返します。
        // 反復子とは、反復子は、コンテナからオブジェクトを1つずつ取り出すはたらきをするオブジェクト。
        //Iteratorクラスのメソッド↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
           //abstract boolean	hasNext()
               //true反復にさらに要素があるかどうかを返します。
           //abstract E	next()
               //反復の次の要素を返します。
        //RealmクラスのメソッドexecuteTransactionAsync(Realm.Transaction transaction, Realm.Transaction.OnSuccess onSuccess, Realm.Transaction.OnError onError)3引数について。
        //http://realm.io.s3-website-us-east-1.amazonaws.com/docs/java/1.2.0/api/io/realm/Realm.html
        //【ポイント】3つとも、関数型インターフェース。つまり、使用者側にとってはoverrideによるコールバックメソッドとなる。今回すべてラムダになっている。
        //Realm.Transaction transaction  →  override fun executeであり、[ 取引内容を書く ]。
        //Realm.Transaction.OnSuccess → override fun onSuccess（） であり、 [ 取引成功時の内容を書く ]。
        //Realm.Transaction.OnError onError → override fun onErrorであり、[ 取引失敗時の内容を書く ]。
        realm.executeTransactionAsync({
            while (iterator.hasNext()){  //冗長:tempList.iterator().hasNext()。→true(次にデータがある状態)であれば、次の処理を行う。
                val record : Array<String>   = iterator.next()  //冗長:tempList.iterator().next()。文字通りレコード（Array<String>）をつくる。
                val questionDB = it.createObject(QuestionModel::class.java)//itを活用し、登録処理の準備をする。
                questionDB.apply {
                    //レコード（Array<String>）単位で処理していく。通例だとコレクションではないので、[0][1]などが、今回の混乱ポイント。
                    id = record[0]//レコードの項目1目idを入れる。
                    gradeId = record[1]//レコードの項目2目gradeIdを入れる。
                    question = record[2]
                    answer = record[3]
                    choice1 = record[4]
                    choice2 = record[5]
                    choice3 = record[6]
                    explanation = record[7]
                }
            }
        }, {
            isDataSetFinished = true
            makeToast(this@MainActivity, getString(R.string.import_success))
        }, {
            isDataSetFinished = false
            makeToast(this@MainActivity, getString(R.string.import_fail))
        })

    }
    //◆CSVを取り込むメソッド。
    private fun setCSVReader(): CSVReader {//戻り値をこれにする。
        //CSVの入っているファイルにアクセスするには、AssetManagerクラスを使用する。
        val assetManager: AssetManager = resources.assets  //CSVデータが入っているのはassetsフォルダであり、assets操作のクラスは、AssetManagerなので、そこにassetsフォルダのリソースをいれる。
        //CSVを読みこむためのInputStream(数字の羅列)を設定。画像同様、数字の羅列を順繰りに読みこむ処理を行うためInputStream。
        val inputStream = assetManager.open("Questions.csv")  //対象CSVを読み込む。assetManager.openの戻り値が、inputStreamなわけだが、CSVのデータ取り込みでの型はinputStreamである点を覚える。
        //↓概要。
        //parserの内部環境設定。
        //http://opencsv.sourceforge.net/apidocs/com/opencsv/CSVParserBuilder.html#CSVParserBuilder--
        //csvを解析する際のseparator(区切り)はなんですか。→カンマ区切り。
        val parser = CSVParserBuilder().withSeparator(',').build()
        //↓概要。
        //CSVReaderBuilder:整理されたCSVを読むクラスとその定数。  InputStreamReader:InputStreamを読むクラス。   withCSVParser:CSVを解析するInputStreamクラスのメソッド。
        return CSVReaderBuilder(InputStreamReader(inputStream)).withCSVParser(parser).build()
    }


    //アプリ内課金の設定
    private fun setUpAppBilling() {
        //目的1：BillingManagerの引数にこのActivityを入れることで、BillingManagerClassでの当クラスの操作が可能になる。
        //目的2：当クラスでのBillingManagerのインスタンス化。これをすることで、次のbillingManagerメソッドを使用可能にしている。
        //billingManagerクラス直下で初期化。
        billingManager = BillingManager(this)
        //またさらに混乱を招いている要因は、今回のこのBillingContractInterfaceがどこかのクラス内に属しているのではなく、interfaceで独立して
        //存在している点。いままではどこかのクラスに属していた。しかし本来、interfaceはクラスと同じ階層にいる。
        billingManager.setContract(this)
        //【パターン3のコールバックListener】
        // 引数となるactivityは、BillingContractの実装部分のみ受け渡しされている。

        //【下の考えは間違い。しかし後学のために残す。】
        //今回のこの2つのthisはいずれも結論としては、BillingManagerクラスでのこのClassの初期化にある。どちらも目的が同じであるならば、
        //なぜわざわざ2種類の別名プロパティーで当クラスのインスタンスが必要だったのか。
    }


    override fun onQueryInventoryFinished() {
        updateUi()
    }

    override fun onGetGrade1Purchased() {
        isPaidGrade1 = true
        updateUi()
    }

    override fun onGet3PointsPurchased() {
        //から実装
    }
}
