package net.minpro.quiz

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.google.android.gms.ads.AdRequest
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_test.*
import kotlinx.android.synthetic.main.content_test.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class TestActivity : AppCompatActivity(),
        AnswerFragment.OnFragmentInteractionListener,
        FinishFragment.OnFragmentInteractionListener, BillingContract {

    var gradeOfTest: Int = 0

    var numberOfQuestion = INITIAL_NUMBER_OF_QUESTION
    var numberOfLife = INITIAL_LIFE
    var cntQuestion = 0//出題の都度、カウンターがインプリメントされる変数。

    lateinit var realm: Realm
    lateinit var results: RealmResults<QuestionModel>
    lateinit var questionList: ArrayList<QuestionModel>

    lateinit var testStatus: TestStatus

    var strAnswer = ""
    var strExplanation = ""

    lateinit var timer: Timer
    //【重要】ここで初期化しているのは自作クラスの方で、既存クラスのBillingContractではない点を注意。
    lateinit var billingManager: BillingManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        fab_sound.setOnClickListener {
            changeSoundMode(it)
        }

        fab_point_get.setOnClickListener {
            billingManager.startPurchaseFlow(sku_GET_3POINTS)
        }

//【重要】Bundleの受け取り処理、いつもの省略形。
        //val bundle = intent.extras
        //gradeOfTest = bundle.getInt(IntentKey)
        //gradeOfTestはMainActivityにおいてはじめて設定されている。
        //1級画面に進めばgradeOfTestは1。2級に進むボタンを押せば値は2を設定している。
        gradeOfTest = intent.extras.getInt(IntentKey.GRADE_OF_TEST.name)

                //バナー広告表示呼び出し。
        setBannerAd()

        btnBack.setOnClickListener {
            Snackbar.make(it, getString(R.string.finish_quiz), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.finish), View.OnClickListener {
                        finish()
                    }).show()
        }

        //アプリ内課金の設定
        setUpAppBilling()

        testStatus = TestStatus.RUNNING

        realm = Realm.getDefaultInstance()
        //Realmからランダムに問題抽出設定を行う。この処理を実施したのち、「問題表示関数(setQuestion)」を行う。
        setQuestionList(gradeOfTest)

        //選択を選んだ時のこたえ合わせ処理への移動等。
        btnAnswer1.setOnClickListener { answerCheck(it) }
        btnAnswer2.setOnClickListener { answerCheck(it) }
        btnAnswer3.setOnClickListener { answerCheck(it) }
        btnAnswer4.setOnClickListener { answerCheck(it) }

        timer = Timer()

        //setQuestionによる問題内容作成処理後、当関数において、問題表示処置を行うｌ
        setQuestion()


    }



    //答え合わせなどの処理。
    //次の引数viewには、btn1なのかbtn2なのかなど判別できるように、Idも引き渡されている。
    private fun answerCheck(view : View) {
        setButtonEnabled(false)//選択肢ボタンを押せないようにする。
        imageJudge.visibility = View.VISIBLE//〇✖イメージを表示させる。
        numberOfQuestion -= 1//残り問題数を-1にする。
        textQuestion.text = numberOfQuestion.toString()//残り問題数の表示内容を-1する。

        val button: Button = view as Button//【重要】引数のViewをダウンキャストしてtextメソッドを可能にさせる。

        //strAnswer = questionList[cntQuestion].answer→Realmから抽出した回答とあっているのか確認。
        if (button.text == strAnswer){//もしあっていればの処理。
            imageJudge.setImageResource(R.drawable.image_correct)//正解の画面表示。
            if (isSoundOn) soundPool?.play2(soundIdCorrect)//正解の音を鳴らす。(音量OKなら)
        } else {
            numberOfLife -= 1//体力を1減らす。
            textLife.text = numberOfLife.toString()
            imageJudge.setImageResource(R.drawable.image_incorrect)
            if (isSoundOn) soundPool?.play2(soundIdInCorrect)
        }

        //上の処理の1秒後に以下のFragment表示処理を行う。
        timer.schedule(1000){runOnUiThread {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container_answer_finish, //どのコンテナに→layoutのcontent_testにあるFrameLayoutのcontainer_answer_finishに対し、
                            AnswerFragment.newInstance(strAnswer, strExplanation)).commit()//AnswerFragmentをnewInstanceする。
                                             //その際、引数値としてnewInstanceの引数に入れて持っていく。
        }}

    }

    //FinishFragment.OnFragmentInteractionListener
    override fun onGoNextBtnOnFinishFragmentClicked() {
        backToMainActivity()//MainActivityへ戻る。
    }

    private fun backToMainActivity() {
        finish()

    }

    //AnswerFragment.OnFragmentInteractionListenerの実装。
    override fun onGoNextBtnOnAnswerFragmentClicked() {
        //ただ単に次の問題を表示するのではなく、そもそも先ほどの回答において「合否」が
        //決定されている可能性がある。なので、次の問題表示の前に、他の画面「合格画面」・「不合格画面」に
        //いくかの確認とその行く場合にはその処理も行う。
        goNextStepAfterAnswerCheck()
    }

    private fun goNextStepAfterAnswerCheck() {

        if (numberOfLife <= 0) {
            supportFragmentManager.beginTransaction()//死亡画面表示へ。
                    //ここのｺﾝﾃﾅは回答とその解説画面表示の際、利用したｺﾝﾃﾝと同様。ただし入れるFragmentは違うが。
                    //gradeOfTestの値はMainActivityで、1級ボタンを押していれば値は1.2級なら値は2になっている。
                    .add(R.id.container_answer_finish, FinishFragment.newInstance(gradeOfTest, TestStatus.FINISH_LOSE)).commit()
            return
        }
        if (numberOfQuestion <= 0){
            //合格画面に行く前に、getSharedPreferencesにConstantsで設定した合否Booleanの設定を行う。
            recordPassStatus(gradeOfTest)
            supportFragmentManager.beginTransaction()//合格画面表示へ。
                    .add(R.id.container_answer_finish, FinishFragment.newInstance(gradeOfTest, TestStatus.FINISH_WIN)).commit()
            return
        }
        setQuestion()//問題表示関数呼び出し。
    }

    //ここでは合格した場合、sharedPreferencesに値を登録しなければならない。
    private fun recordPassStatus(gradeOfTest: Int) {
        //【重要】なぜthisなのか、getSharedPreferencesはContextのメソッドだから。
        val prefs = this.getSharedPreferences(net.minpro.quiz.PREF_FILE_NAME, android.content.Context.MODE_PRIVATE)
        //editは編集。editorは編集者の意味。
        val editor = prefs.edit()//preferences[環境設定の意]を編集するメソッドの呼び出し。
        //gradeOfはMainActivityで1級2級の選択の際、値がそれぞれ「1」または「2」を代入された。
        when (gradeOfTest){
            1 -> {//1級ならば。
                isPassGrade1 = true//Constantsでの宣言はfalseなのでここでtrueの合格にする。
                //受け取り先でisPassGradeを受け取る際は、結局のところ中身はBooleanなので、変数で受け止める際に
                //わかりやすい名前で受け取ることを前提にした場合、必然と同名のisPassGradeになる。
                //【受け取りの際のコード】
                    //isPassGrade1 = pref.getBoolean(PrefKey.PASS_GRADE1.name, false)//pref.getBoolean(key,もしデータがなかった場合の戻り値)
                editor.putBoolean(PrefKey.PASS_GRADE1.name, isPaidGrade1).commit()
            }
            2 -> {//2級ならば。
                isPassGrade2 = true
                editor.putBoolean(PrefKey.PASS_GRADE2.name, isPassGrade2).commit()
            }
        }
    }
    //問題表示関数。
    private fun setQuestion() {

        setButtonEnabled(true)//選択肢ボタンを押せる押せないの条件分岐。
        //答え合わせの際は、選択肢ボタンは押せないようにしなければならない。
        // ここでは押せる引数を渡す。受け取り先のメソッドでは、
        //引数に基づき条件分岐される。

        //【重要】解答時の〇画像、✖画像について、通常は見えないように設定しておく。
        //ここで見える状態にしても見えない状態にしても、setImageResourceが振られていないため、画面表示は行われないためむしろ無駄なコードにも感じるが。。
        imageJudge.visibility = View.INVISIBLE
        //問題表示。尚、すでにquestionListはCollection.shuffleされているので、それに出題カウンターをいれればよいということ。
        textQuestion.text = questionList[cntQuestion].question
        //先のコードでまた選択しの並びのCollectionShuffleがあり、シャッフル後の抽出はArray(配列)に基づく[0~3]になるため、
        //どれが回答かわからなくなるので、現段階において正解の入ったアイテムを抽出し特定しておく。おそらくこの後正解処理でif文に
        //よる回答チェックに使用されるのだと思われる。あと、正解の解説も抽出し避難させておく。
        strAnswer = questionList[cntQuestion].answer
        strExplanation = questionList[cntQuestion].explanation

        //選択肢シャッフル用コレクション。
        val choices = ArrayList<String>()
        choices.add(questionList[cntQuestion].answer)
        choices.add(questionList[cntQuestion].choice1)
        choices.add(questionList[cntQuestion].choice2)
        choices.add(questionList[cntQuestion].choice3)
        //この段階において、正解の入ったアイテムの特定がこのコードだけでは不可能になった。ゆえに先のコードで避難しておいたのだ。
        Collections.shuffle(choices)
        btnAnswer1.text = choices[0]
        btnAnswer2.text = choices[1]
        btnAnswer3.text = choices[2]
        btnAnswer4.text = choices[3]

        //出題カウンターのインプリメント
        cntQuestion++


    }

    //回答ボタンを押せる押せない状態にする。
    //【重要】呼び出し元は、setButtonEnabled(true)という関数。
    //引数部分がBoolean型になると同時に名前ローカルプロパティを設定。
    //(注)varなどはクラスの引数でもない限り不要。プライマリコンストラクタではないので。
    //ゆえに、isEnabledプロパティはこの関数内でしか使用不可。
    private fun setButtonEnabled( isEnabled: Boolean) {

        if (!isEnabled){
            btnAnswer1.isEnabled = false
            btnAnswer2.isEnabled = false
            btnAnswer3.isEnabled = false
            btnAnswer4.isEnabled = false
            btnBack.isEnabled = false
            fab_sound.isEnabled = false
            fab_point_get.isEnabled = false
            return
        }
        btnAnswer1.isEnabled = true
        btnAnswer2.isEnabled = true
        btnAnswer3.isEnabled = true
        btnAnswer4.isEnabled = true
        btnBack.isEnabled = true
        fab_sound.isEnabled = true
        fab_point_get.isEnabled = true


    }


    private fun setQuestionList(gradeOfTest: Int) {
        results = realm.where(QuestionModel::class.java).equalTo(QuestionModel::gradeId.name, gradeOfTest.toString()).findAll()
        questionList = ArrayList(results)
        Collections.shuffle(questionList)

    }

    override fun onResume() {
        super.onResume()
        billingManager.queryPurchases()
        //画面の更新処理。「画面の看板部分」、「サウンド」
        updateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }

    //MainActivityより渡された引数による「級」にともなう、ImageResource内容の変更。
    private fun updateUi() {
        when (gradeOfTest){
            1 -> imageHeaderTest.setImageResource(R.drawable.image_header_grade1)
            2 -> imageHeaderTest.setImageResource(R.drawable.image_header_grade2)
        }

        //【重要】トップレベルファイルで宣言されたisSoundOn(Boolean)は、intentを不要とする実用が可能。
        if (isSoundOn){
            fab_sound.setImageResource(R.drawable.ic_volume_up_black_24dp)
        } else {
            fab_sound.setImageResource(R.drawable.ic_volume_off_black_24dp)
        }

        textNumberOfQuestions.text = numberOfQuestion.toString()
        textLife.text = numberOfLife.toString()

    }

    //バナー広告表示。
    private fun setBannerAd() {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest) //【重要】今回の場合では、なぜかバナー広告表示部分が同ﾌﾟﾛﾊﾟﾃｨになっているがこれは悪い例。

    }

    //アプリ内課金の設定
    private fun setUpAppBilling() {
        //目的1：ここの解説はMainActivityに同一の関数処理があるのでそちら参照。
        billingManager = BillingManager(this)
        billingManager.setContract(this)
    }

    override fun onQueryInventoryFinished() {
        updateUi()
    }

    override fun onGetGrade1Purchased() {
        //から実装
    }

    override fun onGet3PointsPurchased() {
        numberOfQuestion = if(numberOfQuestion < 3) 0 else numberOfQuestion - 3
        textNumberOfQuestions.text = numberOfQuestion.toString()
        makeToast(this, getString(R.string.consumed))
        updateUi()
        goNextStepAfterAnswerCheck()
    }


}


