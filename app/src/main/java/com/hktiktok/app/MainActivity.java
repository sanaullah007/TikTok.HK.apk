package com.hktiktok.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MainActivity extends Activity {
    private static final int PICK_VIDEOS = 101;
    private final ArrayList<Uri> videos = new ArrayList<>();
    private final HashSet<String> likes = new HashSet<>();
    private final HashMap<String, ArrayList<String>> comments = new HashMap<>();
    private int index = 0;
    private FrameLayout root;
    private VideoView video;
    private TextView heading, caption, likeButton, commentButton;
    private float touchStartY;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        drawScreen();
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        return t;
    }
    private FrameLayout.LayoutParams place(int w, int h, int gravity, int margin) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w,h,gravity);
        p.setMargins(margin,margin,margin,margin);
        return p;
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private void drawScreen() {
        root.removeAllViews();
        video = new VideoView(this);
        root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        video.setOnCompletionListener(mp -> { if (!videos.isEmpty()) { index=(index+1)%videos.size();play(); } });
        video.setOnErrorListener((mp,what,extra) -> {
            caption.setText("Unable to play this video. Try another format.");
            return true;
        });

        if (videos.isEmpty()) {
            TextView empty = text("TikTok.HK\n\nYour video feed is empty\nTap + to import videos",22,true);
            root.addView(empty,place(-1,dp(230),Gravity.CENTER,dp(16)));
        }

        heading = text("TikTok.HK     For You",20,true);
        root.addView(heading,place(-1,dp(64),Gravity.TOP,dp(12)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        likeButton = text("♡\nLike",18,true);
        commentButton = text("☏\nComment",16,true);
        TextView share = text("↗\nShare",17,true);
        actions.addView(likeButton,new LinearLayout.LayoutParams(dp(86),dp(78)));
        actions.addView(commentButton,new LinearLayout.LayoutParams(dp(86),dp(78)));
        actions.addView(share,new LinearLayout.LayoutParams(dp(86),dp(78)));
        root.addView(actions,place(dp(90),dp(240),Gravity.RIGHT | Gravity.CENTER_VERTICAL,dp(6)));
        likeButton.setOnClickListener(v -> toggleLike());
        commentButton.setOnClickListener(v -> comment());
        share.setOnClickListener(v -> shareVideo());

        caption = text("",15,false);
        caption.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams cp = place(-1,dp(66),Gravity.BOTTOM,dp(12));
        cp.bottomMargin=dp(82);
        cp.rightMargin=dp(100);
        root.addView(caption,cp);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(Color.rgb(18,18,18));
        TextView home = text("Home",17,true);
        TextView add = text("+",32,true);
        TextView profile = text("Profile",17,true);
        nav.addView(home,new LinearLayout.LayoutParams(0,dp(65),1));
        nav.addView(add,new LinearLayout.LayoutParams(0,dp(65),1));
        nav.addView(profile,new LinearLayout.LayoutParams(0,dp(65),1));
        root.addView(nav,place(-1,dp(65),Gravity.BOTTOM,0));
        home.setOnClickListener(v -> { if (!videos.isEmpty()) play(); });
        add.setOnClickListener(v -> importVideos());
        profile.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("TikTok.HK Profile")
            .setMessage("Local demo profile\nImported videos: "+videos.size()+"\nLiked videos: "+likes.size()+"\n\nOnline accounts are not available in this prototype.")
            .setPositiveButton("OK",null).show());
        updateLabels();
        if (!videos.isEmpty()) play();
    }
    private String key() { return videos.isEmpty() ? "" : videos.get(index).toString(); }
    private void updateLabels() {
        if (videos.isEmpty()) {
            caption.setText("Import a video to get started");
            likeButton.setText("♡\nLike");
            commentButton.setText("☏\nComment");
            return;
        }
        caption.setText("Video " + (index+1) + " of " + videos.size() + "\nSwipe up for next video");
        likeButton.setText((likes.contains(key()) ? "♥" : "♡") + "\nLike");
        int count=comments.containsKey(key()) ? comments.get(key()).size() : 0;
        commentButton.setText("☏\n"+count+" Comments");
    }
    private void play() {
        if(videos.isEmpty()) return;
        video.setVideoURI(videos.get(index));
        video.setOnPreparedListener(mp -> { mp.setLooping(false); video.start(); });
        updateLabels();
    }
    private void toggleLike() {
        if(videos.isEmpty()) return;
        String k=key();
        if(!likes.add(k)) likes.remove(k);
        updateLabels();
    }
    private void importVideos() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("video/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(i,PICK_VIDEOS);
    }
    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(req!=PICK_VIDEOS || result!=RESULT_OK || data==null) return;
        int count = 0;
        ClipData clips=data.getClipData();
        if(clips!=null) {
            for(int n=0;n<clips.getItemCount();n++) {
                addUri(clips.getItemAt(n).getUri(),data.getFlags());
                count++;
            }
        } else if(data.getData()!=null) {
            addUri(data.getData(),data.getFlags());
            count++;
        }
        if(count>0) { index=videos.size()-count; drawScreen(); }
    }
    private void addUri(Uri uri,int flags) {
        if (!videos.contains(uri)) videos.add(uri);
        try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch(SecurityException ignored) {}
    }
    private void comment() {
        if(videos.isEmpty()) return;
        final String k=key();
        ArrayList<String> list=comments.get(k);
        if(list==null) { list=new ArrayList<>(); comments.put(k,list); }
        final ArrayList<String> existing=list;
        EditText input=new EditText(this);
        input.setHint("Write a comment");
        String history = existing.isEmpty() ? "No comments yet" : android.text.TextUtils.join("\n",existing);
        new AlertDialog.Builder(this).setTitle("Comments")
            .setMessage(history).setView(input)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Post",(d,w) -> {
                String s=input.getText().toString().trim();
                if(!s.isEmpty()) existing.add(s);
                updateLabels();
            }).show();
    }
    private void shareVideo() {
        if(videos.isEmpty()) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("video/*");
        i.putExtra(Intent.EXTRA_STREAM,videos.get(index));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i,"Share video"));
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_DOWN) touchStartY=e.getY();
        if(e.getAction()==MotionEvent.ACTION_UP && !videos.isEmpty()) {
            float delta=e.getY()-touchStartY;
            if(Math.abs(delta)>dp(100)) {
                index=(index+(delta<0?1:videos.size()-1))%videos.size();
                play();
                return true;
            }
        }
        return super.dispatchTouchEvent(e);
    }
    @Override protected void onPause() { super.onPause(); if(video!=null) video.pause(); }
    @Override protected void onResume() { super.onResume(); if(video!=null && !videos.isEmpty()) video.start(); }
}
