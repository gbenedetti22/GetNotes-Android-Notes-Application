package com.unipi.sam.getnotes.utility;

import android.app.Activity;
import android.graphics.Color;

import com.unipi.sam.getnotes.R;

import org.aviran.cookiebar2.CookieBar;

public class PopupMessage {
    public static void show(Activity activity, String message) {
        CookieBar.build(activity)
                .setMessage(message)
                .setSwipeToDismiss(true)
                .setEnableAutoDismiss(true)
                .setBackgroundColor(R.color.materialGrey)
                .setCookiePosition(CookieBar.BOTTOM)
                .setAnimationIn(org.aviran.cookiebar2.R.anim.abc_slide_in_bottom, org.aviran.cookiebar2.R.anim.abc_slide_in_bottom)
                .setAnimationOut(org.aviran.cookiebar2.R.anim.abc_slide_out_bottom, org.aviran.cookiebar2.R.anim.abc_slide_out_bottom)
                .show();
    }

    public static void showError(Activity activity, String message) {
        CookieBar.build(activity)
                .setMessage(message)
                .setMessageColor(R.color.white)
                .setSwipeToDismiss(true)
                .setEnableAutoDismiss(true)
                .setBackgroundColor(R.color.red)
                .setCookiePosition(CookieBar.BOTTOM)
                .setAnimationIn(org.aviran.cookiebar2.R.anim.abc_slide_in_bottom, org.aviran.cookiebar2.R.anim.abc_slide_in_bottom)
                .setAnimationOut(org.aviran.cookiebar2.R.anim.abc_slide_out_bottom, org.aviran.cookiebar2.R.anim.abc_slide_out_bottom)
                .show();
    }
}
