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
import android.content.Intent;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;

import com.kobakei.ratethisapp.RateThisApp;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.util.HashMap;

import fr.forexperts.chessknight.R;
import fr.forexperts.chessknight.util.PrefUtils;
import fr.forexperts.chessknight.util.inappbilling.IabHelper;
import fr.forexperts.chessknight.util.inappbilling.IabResult;
import fr.forexperts.chessknight.util.inappbilling.Inventory;
import fr.forexperts.chessknight.util.inappbilling.Purchase;

import static fr.forexperts.chessknight.util.LogUtils.LOGD;
import static fr.forexperts.chessknight.util.LogUtils.makeLogTag;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = makeLogTag(MainActivity.class);

    @Bind(R.id.score) TextView mScoreTextView;
    @Bind(R.id.score_value) TextView mCurrentScoreValueTextView;
    @Bind(R.id.best_score) TextView mBestScoreTextView;
    @Bind(R.id.best_score_value) TextView mBestScoreValueTextView;
    @Bind(R.id.game_description) TextView mDescriptionTextView;
    @Bind(R.id.no_ad_button) Button mNoAdButton;
    @Bind(R.id.chessboard) ChessboardView mChessboard;
    @Bind(R.id.adView) AdView mAdView;

    private static HashMap<Integer, Integer> mBestScore;
    private static int mBestScoreValue = 1;
    private static int mCurrentScoreValue = 1;
    private static int mGameNumberCounter = 1;
    private static int mVictoryNumber = 0;

    private static InterstitialAd mInterstitialAd;

    private static GoogleApiClient mGoogleApiClient;

    private static int RC_SIGN_IN = 9001;

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;

    private static int REQUEST_LEADERBOARD = 1;

    private static HashMap<Integer, String> mAchievementID;
    private static HashMap<Integer, String> mLeaderboardID;

    private static IabHelper mHelper;
    static final String ITEM_SKU = "fr.forexperts.chessknight.undo";
    static final String ITEM_SKU_NO_AD = "fr.forexperts.chessknight.no_ad";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Create the Google Api Client with access to the Play Games services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // Fill Google Play Services data array
        fillAchievementIDArray();
        fillLeaderboardIDArray();

        if (PrefUtils.getNoAds(this)) {
            mNoAdButton.setVisibility(View.INVISIBLE);
            mAdView.setVisibility(View.INVISIBLE);
        }

        // Set up Ad banner
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Set up Interstitial
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-7370519346258326/9965638495");
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

        mVictoryNumber = PrefUtils.getVictoryNumber(this);

        // Set up font
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        mScoreTextView.setTypeface(typeface);
        mCurrentScoreValueTextView.setTypeface(typeface);
        mBestScoreTextView.setTypeface(typeface);
        mBestScoreValueTextView.setTypeface(typeface);
        mDescriptionTextView.setTypeface(typeface);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        RateThisApp.Config config = new RateThisApp.Config(3, 3);
        RateThisApp.init(config);
        // Monitor launch times and interval from installation
        RateThisApp.onStart(this);
        // If the criteria is satisfied, "Rate this app" dialog will be shown
        RateThisApp.showRateDialogIfNeeded(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // Already resolving
            return;
        }

        // If the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect
        mGoogleApiClient.connect();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (mHelper != null && !mHelper.handleActivityResult(requestCode, resultCode, intent)) {
            if (requestCode == RC_SIGN_IN) {
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    // Bring up an error dialog to alert the user that sign-in failed.
                    BaseGameUtils.showActivityResultError(this,
                            requestCode, resultCode, R.string.signin_failure);
                }
            }
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    @OnClick(R.id.new_game_button)
    public void newGame() {
        if (!PrefUtils.getNoAds(this) && mInterstitialAd.isLoaded() && mGameNumberCounter % 3 == 0) {
            mInterstitialAd.show();
        } else {
            // Clear the current score
            mCurrentScoreValue = 1;
            mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

            // Clear victory number
            mVictoryNumber = 0;

            // Load the corresponding best score
            mBestScoreValue = mBestScore.get(PrefUtils.getColumnsNumber(this));
            mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));

            // Clear the preferences
            PrefUtils.clearForbiddenSquare(this);
            PrefUtils.clearCurrentScore(this);
            PrefUtils.clearPosition(this);
            PrefUtils.clearVictoryNumber(this);

            // Clear the chessboard
            mChessboard.newGame();

            // Increment game number counter
            mGameNumberCounter++;
        }
    }

    public void newRound() {
        // Increment and save victory number
        mVictoryNumber++;
        PrefUtils.setVictoryNumber(this, mVictoryNumber);

        PrefUtils.clearPosition(this);

        // Clear the chessboard
        mChessboard.startNewRound();
    }

    @OnClick(R.id.undo_button)
    public void undo() {
        if (!PrefUtils.getUndoEnable(this)) {
            if (mHelper == null) {
                String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkwj8ZOZ9" +
                        "MtoIo9LilYjzdHUHrR/wqH6CB4VkDzoyw7VCyRKPyY6rAVWRml3EV1IN3ZELcI8czI4mpxBwtpWd" +
                        "Dx/P8aInrEa8NrLwJjN9EF3fnGcADOUEubts7MijYjnFMMPZzH076MFBD0UztfzuywjymWMgziBl" +
                        "dDdohceqIlS5/CmYklTvPib0BtiJPvGWo2GPr62skLkG4+pqcw+z7mU7rvMFJnF9UtDIMVQy5mJI" +
                        "f+G1AkB0zN1bQkvmPd7z06rN4ysF7mz6U6Hu4Nf2xg5vgLp7vExcCsXiDCpRMsD0TrM+QvD16DOb" +
                        "WBD9rTkeHWUwqA9tsSX0a1VTSBcTQwIDAQAB";

                mHelper = new IabHelper(this, base64EncodedPublicKey);
                mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                    public void onIabSetupFinished(IabResult result) {
                        if (!result.isSuccess()) {
                            LOGD(TAG, "In-app Billing setup failed: " + result);
                        } else {
                            LOGD(TAG, "In-app Billing is set up OK");
                            enableUndo();
                        }
                    }
                });
            } else {
                enableUndo();
            }
        } else {
            mChessboard.undoLastMove();
        }
    }

    @OnClick(R.id.no_ad_button)
    public void noAdButtonClicked() {
       if (mHelper == null) {
           String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkwj8ZOZ9" +
                   "MtoIo9LilYjzdHUHrR/wqH6CB4VkDzoyw7VCyRKPyY6rAVWRml3EV1IN3ZELcI8czI4mpxBwtpWd" +
                   "Dx/P8aInrEa8NrLwJjN9EF3fnGcADOUEubts7MijYjnFMMPZzH076MFBD0UztfzuywjymWMgziBl" +
                   "dDdohceqIlS5/CmYklTvPib0BtiJPvGWo2GPr62skLkG4+pqcw+z7mU7rvMFJnF9UtDIMVQy5mJI" +
                   "f+G1AkB0zN1bQkvmPd7z06rN4ysF7mz6U6Hu4Nf2xg5vgLp7vExcCsXiDCpRMsD0TrM+QvD16DOb" +
                   "WBD9rTkeHWUwqA9tsSX0a1VTSBcTQwIDAQAB";

           mHelper = new IabHelper(this, base64EncodedPublicKey);
           mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
               public void onIabSetupFinished(IabResult result) {
                   if (!result.isSuccess()) {
                       LOGD(TAG, "In-app Billing setup failed: " + result);
                   } else {
                       LOGD(TAG, "In-app Billing is set up OK");
                       disableAd();
                   }
               }
           });
       } else {
           disableAd();
       }
    }

    public void enableUndo() {
        mHelper.launchPurchaseFlow(this, ITEM_SKU, 10001, mPurchaseFinishedListener,
                "enableUndotoken");
    }

    public void disableAd() {
        mHelper.launchPurchaseFlow(this, ITEM_SKU_NO_AD, 10002, mPurchaseFinishedListener,
                "disableAdtoken");
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener =
            new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                // Handle error
                return;
            } else if (purchase.getSku().equals(ITEM_SKU)) {
                consumeItem();
            } else if (purchase.getSku().equals(ITEM_SKU_NO_AD)) {
                consumeNoAdItem();
            }
        }
    };

    public void consumeItem() {
        mHelper.queryInventoryAsync(mReceivedInventoryListener);
    }


    public void consumeNoAdItem() {
        mHelper.queryInventoryAsync(mReceivedNoAdInventoryListener);
    }

    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new
            IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                // Handle failure
            } else {
                mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU), mConsumeFinishedListener);
            }
        }
    };

    IabHelper.QueryInventoryFinishedListener mReceivedNoAdInventoryListener = new
            IabHelper.QueryInventoryFinishedListener() {
                public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                    if (result.isFailure()) {
                        // Handle failure
                    } else {
                        mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU_NO_AD), mConsumeNoAdFinishedListener);
                    }
                }
            };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new
            IabHelper.OnConsumeFinishedListener() {
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    if (result.isSuccess()) {
                        PrefUtils.setUndoEnable(MainActivity.this, true);
                        mChessboard.undoLastMove();
                    }
                }
            };

    IabHelper.OnConsumeFinishedListener mConsumeNoAdFinishedListener = new
            IabHelper.OnConsumeFinishedListener() {
                public void onConsumeFinished(Purchase purchase, IabResult result) {
                    if (result.isSuccess()) {
                        PrefUtils.setNoAds(MainActivity.this, true);
                        mNoAdButton.setVisibility(View.INVISIBLE);
                        mAdView.setVisibility(View.INVISIBLE);
                    }
                }
            };

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHelper != null) {
            mHelper.dispose();
        }
        mHelper = null;
    }

    @OnClick(R.id.leaderboard_button)
    public void getLeaderboard() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(mGoogleApiClient),
                    REQUEST_LEADERBOARD);
        } else {
            showSignInDialog();
        }
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

        final SeekBar chessboardSizeControl =
                (SeekBar) dialog.findViewById(R.id.chessboard_size_control);
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
            }
        });

        Button newGameButton = (Button) dialog.findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefUtils.setColumnsNumber(MainActivity.this,
                        chessboardSizeControl.getProgress() + 4);
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

    private void showSignInDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sign_in);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView pleaseSignInLabel = (TextView) dialog.findViewById(R.id.not_signed_label);
        pleaseSignInLabel.setTypeface(typeface);

        Button signInButton = (Button) dialog.findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleApiClient.connect();
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
        int columnsNumber = PrefUtils.getColumnsNumber(this);
        int scoreFinal = columnsNumber * columnsNumber * (1 + mVictoryNumber) - mVictoryNumber;
        if (mCurrentScoreValue == scoreFinal) {
            showWinDialog();
        } else {
            showGameOverDialog();
        }
    }

    private void showGameOverDialog() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            // Check if an achievement "Play X games in a row" is completed
            if (mGameNumberCounter == 10) {
                Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(1));
            } else if (mGameNumberCounter == 20) {
                Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(2));
            } else if (mGameNumberCounter == 50) {
                Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(3));
            }

            // Check if the best score had been beat
            if (mCurrentScoreValue == mBestScoreValue) {
                int columnsNumber = PrefUtils.getColumnsNumber(this);
                Games.Leaderboards.submitScore(mGoogleApiClient, mLeaderboardID.get(columnsNumber),
                        mBestScoreValue);
            }
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_over);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView gameOverLabel = (TextView) dialog.findViewById(R.id.game_over);
        gameOverLabel.setTypeface(typeface);

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

    private void showWinDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_win);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView winLabel = (TextView) dialog.findViewById(R.id.win);
        winLabel.setTypeface(typeface);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            int columnsNumber = PrefUtils.getColumnsNumber(this);
            Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(columnsNumber));
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

        Button continueButton = (Button) dialog.findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newRound();
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

    private void fillAchievementIDArray() {
        mAchievementID = new HashMap<>();
        mAchievementID.put(1, "CgkIytW_os0bEAIQAw");
        mAchievementID.put(2, "CgkIytW_os0bEAIQBA");
        mAchievementID.put(3, "CgkIytW_os0bEAIQFg");
        mAchievementID.put(4, "CgkIytW_os0bEAIQBQ");
        mAchievementID.put(5, "CgkIytW_os0bEAIQBg");
        mAchievementID.put(6, "CgkIytW_os0bEAIQCg");
        mAchievementID.put(7, "CgkIytW_os0bEAIQCw");
        mAchievementID.put(8, "CgkIytW_os0bEAIQDA");
        mAchievementID.put(9, "CgkIytW_os0bEAIQDQ");
        mAchievementID.put(10, "CgkIytW_os0bEAIQDg");
        mAchievementID.put(11, "CgkIytW_os0bEAIQDw");
        mAchievementID.put(12, "CgkIytW_os0bEAIQEA");
        mAchievementID.put(13, "CgkIytW_os0bEAIQEg");
        mAchievementID.put(14, "CgkIytW_os0bEAIQEw");
        mAchievementID.put(15, "CgkIytW_os0bEAIQFA");
        mAchievementID.put(16, "CgkIytW_os0bEAIQFQ");
    }

    private void fillLeaderboardIDArray() {
        mLeaderboardID = new HashMap<>();
        mLeaderboardID.put(4, "CgkIytW_os0bEAIQFw");
        mLeaderboardID.put(5, "CgkIytW_os0bEAIQGA");
        mLeaderboardID.put(6, "CgkIytW_os0bEAIQGQ");
        mLeaderboardID.put(7, "CgkIytW_os0bEAIQGg");
        mLeaderboardID.put(8, "CgkIytW_os0bEAIQGw");
        mLeaderboardID.put(9, "CgkIytW_os0bEAIQHA");
        mLeaderboardID.put(10, "CgkIytW_os0bEAIQHQ");
        mLeaderboardID.put(11, "CgkIytW_os0bEAIQHg");
        mLeaderboardID.put(12, "CgkIytW_os0bEAIQHw");
        mLeaderboardID.put(13, "CgkIytW_os0bEAIQIA");
        mLeaderboardID.put(14, "CgkIytW_os0bEAIQIQ");
        mLeaderboardID.put(15, "CgkIytW_os0bEAIQIg");
        mLeaderboardID.put(16, "CgkIytW_os0bEAIQIw");
    }
}
