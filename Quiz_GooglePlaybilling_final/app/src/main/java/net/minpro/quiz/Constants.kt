package net.minpro.quiz

import android.media.SoundPool

/**
 * Created by keybowNew on 2017/10/27.
 */
//SoundPoolの初期化。
var soundPool: SoundPool? = null
//soundIdのトップライン宣言。
var soundIdCorrect = 0
var soundIdInCorrect = 0
var soundIdApplause = 0
var soundIdTin = 0

////getSharedPreferencesのファイル名として設定。
val PREF_FILE_NAME = "net.minpro.quiz.status"

//1級入場条件のステータス。
//2級に合格しているかのチェック。
//【重要】今回合否に関しての記述がConstantsにて多数あるが、intentKeyや、getSharedPreferencesKey
//によるもので、プロパティの合否設定は、次のものだけである。
var isPassGrade1 = false
var isPassGrade2 = false
//課金したかどうかのチェック。
var isPaidGrade1 = false

enum class PrefKey{
//SharedPreference用インテントキー
    PASS_GRADE1,
    PASS_GRADE2
}
//サウンドを流す流さないの確認変数。
var isSoundOn = false
//CSVデータが読み込めたかをチェックするﾌﾟﾛﾊﾟﾃｨ。
var isDataSetFinished = false

enum class IntentKey {
    //この内部のプロパティたちは、特に関連性はなく、ただ単にIntentKeyの寄せ集め。
    GRADE_OF_TEST,
    ANSWER,
    EXPLANATION,
    GRADE_FINISH,
    TEST_STATUS
}

enum class TestStatus {
    //現在の状況について
    RUNNING,
    FINISH_WIN,
    FINISH_LOSE
}

//残りの問題数の初期化。
val INITIAL_NUMBER_OF_QUESTION = 10
//残りのライフの初期化。
val INITIAL_LIFE = 3

const val sku_GRADE1 = "net.minpro.quiz.grade1"
const val sku_GET_3POINTS = "net.minpro.quiz.get3points"
val skuListAll: List<String> = listOf(sku_GRADE1, sku_GET_3POINTS)


