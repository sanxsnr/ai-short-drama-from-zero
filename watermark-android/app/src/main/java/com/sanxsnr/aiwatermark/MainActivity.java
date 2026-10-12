package com.sanxsnr.aiwatermark;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.RectF;
import android.widget.*;
import android.view.*;
import android.text.InputType;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 101;
    private Uri imageUri;
    private ImageView preview;
    private EditText textA, textB;
    private SeekBar opacity, font, density;
    private TextView status;
    private Bitmap previewBitmap;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(20,20,24));
        getWindow().setNavigationBarColor(Color.rgb(20,20,24));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(20,20,24));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(30),dp(18),dp(20));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = label("AI生成 · 图片水印", 24, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(label("纯本地处理 · 不会AI重绘图片", 12, 0xffaaaaaa));

        Button choose = button("① 选择图片");
        root.addView(choose);
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, PICK_IMAGE);
        });

        preview = new ImageView(this);
        preview.setBackgroundColor(0xff303037);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(310));
        ip.topMargin = dp(12);
        root.addView(preview,ip);

        textA = field("AI生成图像");
        textB = field("AI原创虚拟角色");
        root.addView(label("水印文字（两组交替排列）", 14, Color.WHITE));
        root.addView(textA);
        root.addView(textB);

        opacity = slider(root,"透明度", 46);
        font = slider(root,"文字大小", 50);
        density = slider(root,"铺满密度", 64);

        View.OnClickListener changed = v -> refreshPreview();
        textA.addTextChangedListener(watcher());
        textB.addTextChangedListener(watcher());
        for (SeekBar bar : new SeekBar[]{opacity,font,density}) {
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser) { if(fromUser) refreshPreview(); }
                @Override public void onStartTrackingTouch(SeekBar s) {}
                @Override public void onStopTrackingTouch(SeekBar s) {}
            });
        }

        Button save = button("② 保存带水印图片到相册");
        root.addView(save);
        save.setOnClickListener(v -> exportImage());
        status = label("请选择图片开始", 13,0xffcccccc);
        root.addView(status);
    }

    private android.text.TextWatcher watcher() {
        return new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){refreshPreview();}
            public void afterTextChanged(android.text.Editable e){}
        };
    }

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String s,int sp,int color) {
        TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);
        v.setPadding(0,dp(8),0,dp(7));return v;
    }
    private Button button(String s){
        Button v=new Button(this);v.setText(s);v.setAllCaps(false);
        v.setTextColor(Color.WHITE);v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffb74040));return v;
    }
    private EditText field(String val){
        EditText e=new EditText(this);e.setSingleLine(true);e.setText(val);
        e.setTextColor(Color.WHITE);e.setHintTextColor(0xff999999);e.setTextSize(16);
        e.setInputType(InputType.TYPE_CLASS_TEXT);return e;
    }
    private SeekBar slider(LinearLayout root,String name,int value){
        root.addView(label(name,14,Color.WHITE));
        SeekBar s=new SeekBar(this);s.setMax(100);s.setProgress(value);root.addView(s);return s;
    }

    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=PICK_IMAGE || result!=RESULT_OK || data==null || data.getData()==null)return;
        imageUri=data.getData();
        try { getContentResolver().takePersistableUriPermission(imageUri,Intent.FLAG_GRANT_READ_URI_PERMISSION); }catch(Exception ignored){}
        try {
            BitmapFactory.Options opt=new BitmapFactory.Options();
            opt.inJustDecodeBounds=true;
            try(InputStream in=getContentResolver().openInputStream(imageUri)){BitmapFactory.decodeStream(in,null,opt);}
            int factor=1;
            while(opt.outWidth/factor>1500 || opt.outHeight/factor>1500)factor*=2;
            opt.inJustDecodeBounds=false;opt.inSampleSize=factor;
            try(InputStream in=getContentResolver().openInputStream(imageUri)) {
                if(previewBitmap!=null)previewBitmap.recycle();
                previewBitmap=BitmapFactory.decodeStream(in,null,opt);
            }
            if(previewBitmap==null)throw new Exception("无法解码图片");
            refreshPreview();status.setText("图片已载入");
        }catch(Exception e){status.setText("打开失败："+e.getMessage());}
    }

    private void refreshPreview(){
        if(previewBitmap==null)return;
        try {
            Bitmap output=previewBitmap.copy(Bitmap.Config.ARGB_8888,true);
            paintWatermarks(output,textA.getText().toString(),textB.getText().toString(),opacity.getProgress(),font.getProgress(),density.getProgress());
            preview.setImageBitmap(output);
        }catch(OutOfMemoryError e){status.setText("预览内存不足，请选择更小的图片");}
    }

    private static void paintWatermarks(Bitmap bitmap,String first,String second,int opacity,int size,int density){
        if(first.trim().isEmpty() && second.trim().isEmpty())return;
        Canvas canvas=new Canvas(bitmap);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(Color.rgb(181,37,37));
        p.setAlpha(Math.max(15,Math.min(235,(int)(opacity*2.15f))));
        p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        float fs=Math.max(14f,w*(0.026f+size*0.00065f));
        p.setTextSize(fs);
        float maxWidth=Math.max(p.measureText(first),p.measureText(second));
        float xStep=Math.max(maxWidth+fs*0.35f, w*(0.56f-density*0.0033f));
        float yStep=Math.max(fs*1.65f, h*(0.40f-density*0.0028f));
        int row=0;
        for(float y=fs;y<h+fs;y+=yStep){
            float start=(row%2==0)?fs*0.25f:-xStep*0.45f;
            int col=0;
            for(float x=start;x<w+maxWidth;x+=xStep){
                String content=((row+col)%2==0)?first:second;
                if(content.trim().isEmpty())content=first.trim().isEmpty()?second:first;
                canvas.drawText(content,x,y,p);
                col++;
            }
            row++;
        }
    }

    private void exportImage(){
        if(imageUri==null){status.setText("请先选图");return;}
        final Uri source=imageUri;
        final String a=textA.getText().toString(), b=textB.getText().toString();
        final int alpha=opacity.getProgress(),sz=font.getProgress(),den=density.getProgress();
        status.setText("正在导出，请稍候…");
        new Thread(()->{
            Uri dest=null;
            try {
                Bitmap bitmap;
                try(InputStream in=getContentResolver().openInputStream(source)){
                    bitmap=BitmapFactory.decodeStream(in);
                }
                if(bitmap==null)throw new Exception("读取原图失败");
                Bitmap target=bitmap.copy(Bitmap.Config.ARGB_8888,true);
                if(target!=bitmap)bitmap.recycle();
                paintWatermarks(target,a,b,alpha,sz,den);
                ContentValues v=new ContentValues();
                v.put(MediaStore.Images.Media.DISPLAY_NAME,"AI水印_"+System.currentTimeMillis()+".png");
                v.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
                v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/AI水印");
                v.put(MediaStore.Images.Media.IS_PENDING,1);
                dest=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
                if(dest==null)throw new Exception("相册写入失败");
                try(OutputStream out=getContentResolver().openOutputStream(dest)){
                    if(out==null||!target.compress(Bitmap.CompressFormat.PNG,100,out))throw new Exception("写入失败");
                }
                target.recycle();
                v.clear();v.put(MediaStore.Images.Media.IS_PENDING,0);
                getContentResolver().update(dest,v,null,null);
                runOnUiThread(()->status.setText("已保存到相册：Pictures/AI水印"));
            }catch(Throwable ex){
                if(dest!=null)try{getContentResolver().delete(dest,null,null);}catch(Exception ignored){}
                String msg=ex instanceof OutOfMemoryError?"原图过大，手机内存不足":String.valueOf(ex.getMessage());
                runOnUiThread(()->status.setText("导出失败："+msg));
            }
        }).start();
    }
}
