package net.minpro.coroutinesthreadasynctasksample

import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
    Java threads, Android Async Task and Kotlin Coroutines
    http://zdenekskrobak.com/en/blog/java-threads-android-async-task-and-kotlin-coroutines

    Androidのバックグラウンドを使いこなす Thread, Looper, Handler
        ⇒ これでバックグラウンド処理が必要なことの説明（ででんは作らずにコードで説明）
    https://academy.realm.io/jp/posts/android-thread-looper-handler/

    Coroutineはスレッドではない
    https://stackoverflow.com/questions/43021816/difference-between-thread-and-coroutine-in-kotlin

    公式Coroutine basics
    https://kotlinlang.org/docs/reference/coroutines/basics.html
    Essentially, coroutines are light-weight threads.

 */

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Thread（別のベルトコンベアはバックグラウンド）
        buttonThread.setOnClickListener {
            textTitle.text = "Thread"
            //MySubThread()を自作innerClass。
            val mySubThread = MySubThread()
            mySubThread.start() //≠run
        }

        //AsyncTask（別のベルトコンベアはバックグラウンドだが
        buttonAsync.setOnClickListener {
            textTitle.text = "AsyncTask"
            //AsyncTaskは使い捨てなので実行のたびにインスタンス生成必要
            val myAsyncTask = MyAsyncTask()
            myAsyncTask.execute()
        }

        //Coroutine（メインスレッド上でも別のベルトコンベアを走らせることができる）
        // 依存関係の設定必要=> Javaにdecompileしたらとんでもない大きさになってASがめっちゃ重くなる（無限ループ）
        //https://qiita.com/wm3/items/48b5b5c878561ff4761a
        buttonCoroutine.setOnClickListener {
            textTitle.text = "Coroutine"
            //coroutineのScope(範囲)は、Main(Uiスレッド)でDispatcher(発行)し、それをlaunch(発射)する。
            CoroutineScope(Dispatchers.Main).launch {   //Dispachers.IOだとバックグラウンドになり「Thread」「AsyncTask」と同じような処理が必要になる。
                delay(1000)
                for (i in 1..10){
                    textView.text = i.toString()
                    delay(1000)
                }
                textView.text = "終了"
            }
        }
    }





    //Thread
    inner class MySubThread: Thread(){
        //【簡易認識】Handlerとはサブスレッドをメインスレッドに伝達させるクラス。
        val handler = Handler(Looper.getMainLooper())
        override fun run() {
            for (i in 1..10){
                //runOnUiThread { textView.text = i.toString() }
                //メインスレッドに処理を投げる動作を自力で実装しないといけない
                handler.post { textView.text = i.toString() }
                //1秒間handlerを止めるという意味。
                sleep(1000)//なんでﾚｼｰﾊﾞｰ無しにメソッドが使用可能か疑問だったが、
                //このinner classがsleepMethodのクラスを継承しているから。
            }
        }
    }

    //AsyncTask → Threadをより簡易的なコードで実現できる処理。
    //一つのクラスでUIとバックグラウンド処理の両方を簡単に扱える
    inner class MyAsyncTask: AsyncTask<Void, Int, Void>(){

        //実装必須：バックグラウンドスレッドでやる処理
        override fun doInBackground(vararg params: Void?): Void? {
            for (i in 1..10){
                //バックグラウンドで動いている途中でメインスレッドに結果を渡せる
                //パターン3のコールバックメソッドListener。
                // overrideは、onProgressUpdate(forがあれば)とonPostExecute(forがなくなれば)
                //まあ、引数がthisじゃないので、interface側でﾃﾞﾍﾞﾛｯﾊﾟｰを考慮した特殊な処理のせいだろう。
                publishProgress(i)
                Thread.sleep(1000)
            }
            return null
        }


        //メインスレッドの処理を投げる動作はメソッドとして用意してくれている（publishProgressの直後に呼び出される）
        override fun onProgressUpdate(vararg values: Int?) {
            textView.text = values[0].toString()
        }

        //バックグラウンドのタスクが完了した時にメインスレッドいじる
        override fun onPostExecute(result: Void?) {
            textView.text = "終了"
        }

    }

}
