/*
 * Copyright (c) 2016 中国航天科工集团公司
 *
 * http://www.casic.com.cn/
 */
package demo.com.androidglobalexception;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>名称:CrashHandler<p>
 * <p> 描述:闪退异常处理<p>
 * @author guoweiquan
 * @version 1.0
 * @data 2018/5/18 上午9:22
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler{
    public static final String TAG = "CrashHandler";

    private static CrashHandler instance = new CrashHandler();
    private Context mContext;
    private Application app;

    // 系统默认的 UncaughtException 处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    // 用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String> ();

    // 用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd-HH-mm-ss");


    public UpdataErrorInfoLinster delegete;
    private CrashHandler() {
    }
    public static CrashHandler getInstance() {
        return instance;
    }

    /**
     * @Title: init
     * @Description: 初始化
     * @param context
     * @param app
     *            传入的app
     * @throws
     */
    public void init(Context context, Application app, UpdataErrorInfoLinster l) {
        this.app = app;
        mContext = context;
        this.delegete = l;
        // 获取系统默认的 UncaughtException 处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该 CrashHandler 为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);

    }
    /**
     * 当 UncaughtException 发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            app.onTerminate();
        }
    }

    /**
     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成
     *
     * @param ex
     * @return true：如果处理了该异常信息；否则返回 false
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 收集设备参数信息
        collectDeviceInfo(mContext);
        // 保存日志文件
        saveCrashInfo2File(ex);
        return true;
    }

    /**
     * 收集设备参数信息
     *
     * @param ctx
     */
    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);

            if (pi != null) {
                String versionName = pi.versionName == null ? "null"
                        : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);//版本名称
                infos.put("versionCode", versionCode);//版本号
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息
     *
     * @param ex
     * @return 返回文件名称,便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {
        StringBuffer sb = new StringBuffer ();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter ();
        PrintWriter printWriter = new PrintWriter (writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();

        String result = writer.toString();
        sb.append(result);
        Log.e(TAG,sb.toString());//答应错误信息
        /******************************/
        //上传服务器
        delegete.onUpdataErrorinfo(sb.toString());


        String filePath = "";
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date ());
            String fileName = "error-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                String path = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/crashs";
//                Toast.makeText(mContext, path, Toast.LENGTH_LONG).show();
                File dir = new File (path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                filePath = path + fileName;
                FileOutputStream fos = new FileOutputStream (path + fileName);
                fos.write(sb.toString().getBytes());
                fos.flush();
                fos.close();
            }
            return filePath;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }

        return null;
    }

    public interface UpdataErrorInfoLinster
    {
        void onUpdataErrorinfo(String str);
    }

}
