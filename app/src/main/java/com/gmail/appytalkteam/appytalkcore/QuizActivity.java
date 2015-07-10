package com.gmail.appytalkteam.appytalkcore;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Stays Alive through the quiz, feeds Word[]s to QuestionFragment
 * Created by Lewis on 02/07/15.
 */
public class QuizActivity extends AppCompatActivity implements QuestionFragment.OnFragmentInteractionListener {
// Application variables defined in appconfig.xml and set with initializeVariables()
    int totalQuestions;
    int nChoices;
    int numberOfSoundEffects;

    // Soundloader for our quiz
    // This can be upgraded to Soundpool.Builder in a few months
    @SuppressWarnings("deprecation")
    SoundPool quizSounds = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
    HashMap<String, Integer> soundMap = new HashMap<>();
    int numberOfSoundsLoaded =0;

    // Variable for managing the quiz
    Quiz quiz;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeVariables();
        setContentView(R.layout.quiz_activity);
        disableOrientation(); // Because it crashes the sound-loading progress stars
        LoadQuizTask loadquizasynchronously = new LoadQuizTask(this);
        loadquizasynchronously.execute();

        quizSounds.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool quizSounds, int currentSound, int status) {
                displayProgress(0, (int)Math.floor(numberOfSoundsLoaded));
                numberOfSoundsLoaded++;
                if (numberOfSoundsLoaded ==totalQuestions+numberOfSoundEffects){
                    moveProgressBarToTop();
                    //displayQuestion();
                }

            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        keepProgressBarAtTop();
        displayProgress(quiz.getQuestionNumber(), totalQuestions);
        QuestionFragment nextQuestion = QuestionFragment.newInstance(quiz.getCurrentQuestion());
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.question_frame, nextQuestion);
        fragmentTransaction.commit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("Answers", quiz.getCurrentQuestion().getWords());
        outState.putInt("CorrectAnswer", quiz.getCurrentQuestion().getAnswer());
    }

    private class LoadQuizTask extends AsyncTask<Void, Void, Vocabulary> {
        private Context myCtx;

        public LoadQuizTask(Context ctx){
            this.myCtx = ctx;
        }

        @Override
        protected Vocabulary doInBackground(Void ... nope) {
            Vocabulary vocab = new Vocabulary(myCtx);
        return vocab;
        }

        @Override
        protected void onPostExecute(Vocabulary vocab) {
            quiz = new Quiz(myCtx,vocab);
            ArrayList<Word> AllAnswers = quiz.getAllAnswers();

            LoadSoundsTask loadSoundsAsynchronously = new LoadSoundsTask(myCtx);
            loadSoundsAsynchronously.execute(AllAnswers);


        }

    }

    private class LoadSoundsTask extends AsyncTask<ArrayList<Word>, Void, HashMap>{

        private Context myCtx;

        public LoadSoundsTask(Context ctx){
            // set context
            this.myCtx = ctx;
        }
        @Override
        protected HashMap doInBackground(ArrayList<Word>... arrayList) {
            ArrayList<Word> AllAnswers =  arrayList[0];
            HashMap<String, Integer> asyncSoundMap = new HashMap<>();
            // load sound files for current vocabulary into the quizSounds pool
            // hashmap matches each word to the soundId
            for(int i=0; i<(AllAnswers.size()); i++){
                int currentSound = AllAnswers.get(i).audioRes(myCtx);
                String wordText = AllAnswers.get(i).getWordText();
                asyncSoundMap.put(wordText, quizSounds.load(myCtx, currentSound, 1));

            }
            asyncSoundMap.put("correctSound", quizSounds.load(myCtx, R.raw.yay, 1));
            asyncSoundMap.put("incorrectSound", quizSounds.load(myCtx, R.raw.click, 1));
            return asyncSoundMap;
        }


        @Override
        protected void onPostExecute(HashMap resultingHashmap) {
            super.onPostExecute(resultingHashmap);
            soundMap = resultingHashmap;

        }
    }


    @Override
    public void replayPromptSound(View view){
        Random rn = new Random();
        float soundSpeed = (rn.nextInt(3)+9) / 10.0f;
        Word answer = quiz.getCurrentQuestion().getWords().get(quiz.getCurrentQuestion().getAnswer());
        quizSounds.play(soundMap.get(answer.getWordText()), 1.0f, 1.0f, 1, 0, soundSpeed);
    }



    @Override
    public void correctAnswerSelected(View view) {
        Question currentQ = quiz.getCurrentQuestion();
        //disable orientation change while 'success' screen shows. is re-enabled by next question
        disableOrientation();
        // get all siblings, and disable clickiness
        // nothing more to click in this question
        LinearLayout cont = (LinearLayout) view.getParent();
        int count = cont.getChildCount();
        int skip;
        if(count == nChoices){ // horizontal layout
            skip = 0;
        } else { //vertical layout
            Button prompt = (Button) cont.getChildAt(0);
            prompt.setEnabled(false);
            skip = 1;
        }
        for(int i = skip; i<count; i++){
            ImageButton b = (ImageButton) cont.getChildAt(i);
            b.setEnabled(false);
        }
        for(int i = 0; i<nChoices; i++) {
//            tried[i] = true;
            currentQ.setGuessed(i,true);
        } // until the end of this function
        Random rn = new Random();
        float soundSpeed = (rn.nextInt(2)+8)/10.0f;
        quizSounds.play(soundMap.get("correctSound"), 1.0f, 1.0f, 1, 0, soundSpeed);
        getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.successcolor));

        displayProgress(quiz.getQuestionNumber() + 1, totalQuestions);
        Handler handler = new Handler(); // TODO: this delay is temporary to stop sounds overlapping
        handler.postDelayed(new Runnable() {
            public void run() {
                newQuestion();
            }
        }, 2000);
    }



    @Override
    public void wrongAnswerSelected(View view) {
        Question currentQ = quiz.getCurrentQuestion();
        currentQ.setGuessed(Integer.parseInt((String) view.getTag()), true); //[Integer.parseInt((String) view.getTag())] = true;
        view.setClickable(false);
        view.setAlpha(0.3f);
        quizSounds.play(soundMap.get("incorrectSound"), 1.0f, 1.0f, 1, 0, 1.0f);
    }

    public void newQuestion(){

        if (quiz.incrementQuestionNumber()){
            displayQuestion();
            int correctAnswer = quiz.getCurrentQuestion().getAnswer();
            Word Answer = quiz.getCurrentQuestion().getWords().get(correctAnswer);
            try {
                quizSounds.play(soundMap.get(Answer.getWordText()), 1.0f, 1.0f, 1, 0, 1.0f);
            } catch (NullPointerException e) {
                // do nothing, this should be the end of the quiz
            }
        }
        else {
            quizSounds.release();
            Intent intent = new Intent(this, LanguageSelectorActivity.class);
            startActivity(intent);
            this.finish();
        }
    }
    public void displayQuestion(){
        enableOrientation();
        if (quiz.getQuestionNumber() < totalQuestions){
            int tester = quiz.getQuestionNumber();
            QuestionFragment nextQuestion = QuestionFragment.newInstance(quiz.getCurrentQuestion());
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.question_frame, nextQuestion);
            fragmentTransaction.commit();}
        else{
            quizSounds.release();
            Intent intent = new Intent(this, LanguageSelectorActivity.class);
            startActivity(intent);
            this.finish();
        }
    }
    private void displayProgress(int full, int empty){
        // when loading, call like displayProgress(0,n), when playing displayProgress(n,total)
        // This will display a hard maximum number of stars defined by the number of kids in quiz_activity.xml
        // And a soft minimum defined by totalQuestions
        LinearLayout layout = (LinearLayout) findViewById(R.id.progress_frame);
        int count = layout.getChildCount();
        for(int i = count-1; i >= totalQuestions; i--){
            layout.removeViewAt(i);
        }
        ImageView v;
        for(int i = 0; i < totalQuestions; i++){
            try {
                v = (ImageView) layout.getChildAt(i);

                if(i < full) {
                    v.setImageResource(R.drawable.star_full);
                } else if(i < empty) {
                    v.setImageResource(R.drawable.star_empty);
                } else {
                    v.setImageResource(android.R.color.transparent);
                }
            } catch(NullPointerException e) {break;}
        }
    }
    private void moveProgressBarToTop() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.progress_frame);
        ObjectAnimator moveProgressBarToTop = ObjectAnimator.ofFloat(layout, "Y", 0);
        moveProgressBarToTop.setDuration(600);
        moveProgressBarToTop.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                int correctAnswer = quiz.getCurrentQuestion().getAnswer();
                Word Answer = quiz.getCurrentQuestion().getWords().get(correctAnswer);
                try {
                    quizSounds.play(soundMap.get(Answer.getWordText()), 1.0f, 1.0f, 1, 0, 1.0f);
                } catch (NullPointerException e) {
                    // do nothing, this should be the end of the quiz
                }
                displayQuestion();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        moveProgressBarToTop.start();
    }
    void disableOrientation(){
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        switch(rotation) {
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case  Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }

    }
    void enableOrientation(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
    private void keepProgressBarAtTop(){
        LinearLayout layout = (LinearLayout) findViewById(R.id.progress_frame);
        ObjectAnimator moveProgressBarToTop = ObjectAnimator.ofFloat(layout, "Y", 0);
        moveProgressBarToTop.setDuration(600);
        moveProgressBarToTop.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        moveProgressBarToTop.start();
    }
    void initializeVariables(){
        Resources res = getResources();
        totalQuestions = res.getInteger(R.integer.numberOfQuestions);
        nChoices = res.getInteger(R.integer.numberOfChoices);
        // tried = new boolean[nChoices];
        numberOfSoundEffects = res.getInteger(R.integer.numberOfSoundEffects);} // YAY! and *click*
    }
