package net.minpro.quiz

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import com.google.android.gms.ads.MobileAds
import io.realm.Realm
import io.realm.RealmConfiguration
import java.util.*

/*
    TODO より簡素に実装できる「Google Play Billing Library」+ Coroutineを使った場合
    https://developer.android.com/google/play/billing/billing_library_overview?hl=ja

 */

/**
 * Created by keybowNew on 2017/10/27.
 */
class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        setRealm()
        setSoundPool()
        loadStatus() //簡素Realm。

        MobileAds.initialize(this, getString(R.string.application_id_admob))

    }
//【読込側】1級に挑んでよいかどうかのチェック。intentの受け取り処理と似ている。→ここでは【【読み込み処理】】
    private fun loadStatus() {//getSharedPreferences(引数：ファイル名 , モード) → Realmを使用するまでもないデータ管理。
    //ファイル名：原則、プロジェクト名+〇〇：任意 が望ましい。何のファイルか誰が見てもわかるようにしておくため。
    //モード：基本MODE_PRIVATEを使用してればOK。これの意味は、ここで保存されるファイルが他のAPPでは参照不可能になるという意味らしい。可能にしていても混乱を招くのみ。
    //getSharedPreferences(net.minpro.quiz.PREF_FILE_NAME, android.content.Context.MODE_PRIVATE)  →  設定ファイル 'name'の内容を取得して保持し、SharedPreferencesを返して、その値を取得および変更できます。
    //【重要】なぜthisなのか、getSharedPreferencesはContextのメソッドだから。
        val pref = this.getSharedPreferences(net.minpro.quiz.PREF_FILE_NAME, android.content.Context.MODE_PRIVATE)
        isPassGrade1 = pref.getBoolean(PrefKey.PASS_GRADE1.name, false)//pref.getBoolean(key,もしデータがなかった場合の戻り値)
        isPassGrade2 = pref.getBoolean(PrefKey.PASS_GRADE2.name, false)
//要はここは【【読み込み処理】】。他のしかるべきところで【【書き込み処理された】】true or falseについて参照する。
    }

    private fun setSoundPool() {

        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            SoundPool.Builder().setAudioAttributes(
                                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                                .setMaxStreams(1)
                                .build()

        } else {
            SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        }
//soundPoolをloadしてsoundIdを作成。なお、Constantsクラスでトップライン宣言済。
        soundIdCorrect = soundPool!!.load(this, R.raw.sound_correct, 1)
        soundIdInCorrect = soundPool!!.load(this, R.raw.sound_incorrect, 1)
        soundIdApplause = soundPool!!.load(this, R.raw.sound_applause, 1)
        soundIdTin = soundPool!!.load(this, R.raw.sound_tin, 1)



    }

    private fun setRealm() {
//【超々重要】Realm の全体削除。従来行ってきたのは行単位での削除か全体クローズ。ここでは全体削除が見られる。
//CSVによるデータベースの取り込みをアプリ起動時に毎回行うと、Realmデータベースは不滅なのでどんどんたまる。なので毎回落とす必要がある。
//下のコードの意味。前提として、CSV取り込みは別のコードで行う。ここではすでに作成済のものの削除。
        Realm.init(this)
        val config = RealmConfiguration.Builder().build() //RealmConfiguration.Builder().build()  →  公式「ビルダーパラメータに基づいてRealmConfigurationを作成します。」
        //RealmConfigurationは、Realmの設定。つまり登録内容を指すと思われる。
        // config(設定)。
        Realm.deleteRealm(config)//configの削除。
    }
//Applicationレベルには、onCreateはあるがonResumeもonPauseもない。なぜならApplicationには画面がないため。そのため、SoundPoolでのバイバイ処理はこの、onTerminateで行う。
    override fun onTerminate() {
        soundPool?.release()
        super.onTerminate()
    }



}




