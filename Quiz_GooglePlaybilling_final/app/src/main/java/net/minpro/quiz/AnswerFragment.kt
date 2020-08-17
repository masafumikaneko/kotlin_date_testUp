package net.minpro.quiz

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_answer.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AnswerFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AnswerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AnswerFragment : Fragment() {
//newInstanceから受け取ったプロパティの初期化。
    private var answer: String? = null
    private var explanation: String? = null

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //newInstanceで受け取りののち、budleにputしたがそれの受け取り。
        if (arguments != null) {
            answer = arguments.getString(ARG_PARAM1)
            explanation = arguments.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_answer, container, false)
    }

    //onCreateViewで画面構成が完了しているので、自作のものを表示するにはここで設定する。
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        textAnswer.text = answer //正解を表示。
        textExplanation.text = explanation //解説を表示。

        //「次の問題に進むボタン」を押した場合、content_test画面に戻り、そこで次の問題表示処理を行う。
        btnGoNextQ.setOnClickListener {
            mListener?.onGoNextBtnOnAnswerFragmentClicked()
            //他のActivity画面に行くので、当Fragmentの削除。
            fragmentManager.beginTransaction().remove(this).commit()
        }
    }


    //ここでのContextはcontent_testである。
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            //次のコードは勝手に出来上がる。
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        fun onGoNextBtnOnAnswerFragmentClicked()
    }
//-----------------------------------------------------------
    companion object {
        // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
        private val ARG_PARAM1 = IntentKey.ANSWER.name
        private val ARG_PARAM2 = IntentKey.EXPLANATION.name

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AnswerFragment.
         */
        //読み解き方は、TodoアプリのEditFragmentを参照。
        fun newInstance(strAnswer: String, strExplanation: String): AnswerFragment {
            val fragment = AnswerFragment()
            val args = Bundle()
            args.putString(ARG_PARAM1, strAnswer)
            args.putString(ARG_PARAM2, strExplanation)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor
