/**
 * Copyright 2015 Baptiste Robert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.forexperts.chessknight.ui;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.util.HashMap;

import fr.forexperts.chessknight.R;
import fr.forexperts.chessknight.util.PrefUtils;

import static fr.forexperts.chessknight.util.LogUtils.makeLogTag;

public class MainActivity extends Activity {
    private static final String TAG = makeLogTag(MainActivity.class);

    @Bind(R.id.score) TextView mScoreTextView;
    @Bind(R.id.score_value) TextView mCurrentScoreValueTextView;
    @Bind(R.id.best_score) TextView mBestScoreTextView;
    @Bind(R.id.best_score_value) TextView mBestScoreValueTextView;
    @Bind(R.id.game_description) TextView mDescriptionTextView;
    @Bind(R.id.chessboard) ChessboardView mChessboard;

    private static HashMap<Integer, Integer> mBestScore;
    private static int mBestScoreValue = 1;
    private static int mCurrentScoreValue = 1;
    private static int mGameNumberCounter = 1;

    private static InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Set up Ad banner
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Set up Interstitial
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-7370519346258326/7481993699");
        requestNewInterstitial();

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                newGame();
            }
        });

        // Set up scores
        mBestScore = PrefUtils.getBestScore(this);
        mBestScoreValue = mBestScore.get(PrefUtils.getColumnsNumber(this));
        mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));
        mCurrentScoreValue = PrefUtils.getCurrentScore(this);
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        // Set up font
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        mScoreTextView.setTypeface(typeface);
        mCurrentScoreValueTextView.setTypeface(typeface);
        mBestScoreTextView.setTypeface(typeface);
        mBestScoreValueTextView.setTypeface(typeface);
        mDescriptionTextView.setTypeface(typeface);
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    @OnClick(R.id.new_game_button)
    public void newGame() {
        if (mInterstitialAd.isLoaded() && mGameNumberCounter % 3 == 0) {
            mInterstitialAd.show();
        } else {
            // Clear the current score
            mCurrentScoreValue = 1;
            mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

            // Load the corresponding best score
            mBestScoreValue = mBestScore.get(PrefUtils.getColumnsNumber(this));
            mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));

            // Clear the preferences
            PrefUtils.clearCurrentScore(this);
            PrefUtils.clearPosition(this);

            // Clear the chessboard
            mChessboard.newGame();

            // Increment game number counter
            mGameNumberCounter++;
        }
    }

    @OnClick(R.id.undo_button)
    public void undo() {
        mChessboard.undoLastMove();
    }

    @OnClick(R.id.change_size_button)
    public void changeChessBoardSize() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_size);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        final TextView chessboardSize = (TextView) dialog.findViewById(R.id.chessboard_size);
        chessboardSize.setTypeface(typeface);
        int columnsNumber = PrefUtils.getColumnsNumber(this);
        chessboardSize.setText(columnsNumber + "x" + columnsNumber);

        SeekBar chessboardSizeControl = (SeekBar) dialog.findViewById(R.id.chessboard_size_control);
        chessboardSizeControl.setProgress(columnsNumber - 4);
        chessboardSizeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + 4;
                chessboardSize.setText(progress + "x" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PrefUtils.setColumnsNumber(MainActivity.this, seekBar.getProgress() + 4);
            }
        });

        Button newGameButton = (Button) dialog.findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    public void updateScore(boolean undo) {
        if (!undo) {
            mCurrentScoreValue++;
        } else {
            mCurrentScoreValue--;
        }
        PrefUtils.setCurrentScore(this, mCurrentScoreValue);
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        // Check if the current score is the best score
        if (mCurrentScoreValue > mBestScoreValue) {
            mBestScoreValue = mCurrentScoreValue;
            mBestScore.put(PrefUtils.getColumnsNumber(this), mBestScoreValue);
            PrefUtils.saveBestScore(this, mBestScore);
            mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));
        }
    }

    public void endGame() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_over);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView gameOverLabel = (TextView) dialog.findViewById(R.id.game_over);
        gameOverLabel.setTypeface(typeface);
        int columnsNumber = PrefUtils.getColumnsNumber(this);
        if (mCurrentScoreValue == columnsNumber * columnsNumber) {
            gameOverLabel.setText(getString(R.string.win));
        }

        TextView scoreValue = (TextView) dialog.findViewById(R.id.score_value);
        scoreValue.setText(Integer.toString(mCurrentScoreValue));
        scoreValue.setTypeface(typeface);

        Button newGameButton = (Button) dialog.findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }
}
