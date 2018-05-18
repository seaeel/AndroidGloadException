package demo.com.androidglobalexception;

import android.app.Application;
import android.util.Log;

/**
 * <p>描述:<p>
 *
 * @author guoweiquan
 * @version 1.0
 * @data 2018/5/18 上午9:22
 */


public class MyApp extends Application {
    public static String TAG = "MyApp";
    @Override
    public void onCreate() {
        super.onCreate ();
        Log.e(TAG,"onCreate is running");
        CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext(), this ,new CrashHandler.UpdataErrorInfoLinster()
		{

			@Override
			public void onUpdataErrorinfo(String str) {
				//发送错误信息给服务器
                Log.e("TAG","exception is-->" + str);
			}
		});

		int a = 1/0 ;
    }
}
