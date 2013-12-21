package com.tomclaw.mandarin.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.11.13
 * Time: 14:10
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String REPORT_URL = "http://tomclaw.com/services/mandarin/scripts/report.php";

    private final DateFormat fileFormatter = new SimpleDateFormat("yy-MM-dd-HH-mm-ss-SSSSSS");
    private String versionName = "0";
    private int versionCode = 0;
    private final File stacktraceDir;
    private final Thread.UncaughtExceptionHandler previousHandler;
    private static final HttpClient httpClient;

    static {
        httpClient = new DefaultHttpClient();
    }

    public ExceptionHandler(Context context, boolean chained) {
        PackageManager mPackManager = context.getPackageManager();
        PackageInfo mPackInfo;
        try {
            mPackInfo = mPackManager.getPackageInfo(context.getPackageName(), 0);
            versionName = mPackInfo.versionName;
            versionCode = mPackInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            // ignore
        }
        if (chained)
            previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        else
            previousHandler = null;
        stacktraceDir = context.getFilesDir();
    }

    public static ExceptionHandler inContext(Context context) {
        return new ExceptionHandler(context, true);
    }

    public static ExceptionHandler reportOnlyHandler(Context context) {
        return new ExceptionHandler(context, false);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        final Date dumpDate = new Date(System.currentTimeMillis());
        File stacktrace = new File(stacktraceDir, String.format("stacktrace-%s.txt", fileFormatter.format(dumpDate)));
        PrintStream printStream = null;
        try {
            printStream = new PrintStream(stacktrace);
            processThrowable(exception, printStream);
        } catch (IOException ignored) {
            // ignore
        } finally {
            if (printStream != null)
                printStream.close();
        }
        if (previousHandler != null)
            previousHandler.uncaughtException(thread, exception);
    }

    private void processThrowable(Throwable exception, PrintStream printStream) {
        if (exception == null)
            return;
        exception.printStackTrace(printStream);
        processThrowable(exception.getCause(), printStream);
    }

    public void releaseReports() {
        Task task = new Task() {

            @Override
            public void executeBackground() throws Throwable {
                Log.d(Settings.LOG_TAG, "Checking for unreleased reports...");
                File[] reports = stacktraceDir.listFiles();
                int total = reports.length;
                int reported = 0;
                for (File report : reports) {
                    Log.d(Settings.LOG_TAG, "Have unreleased crash report: " + report.getName());
                    if (report.exists() && releaseReport(report)) {
                        reported++;
                    }
                }
                Log.d(Settings.LOG_TAG, "Reports sent: " + reported + "/" + total);
            }
        };
        TaskExecutor.getInstance().execute(task);
    }

    private boolean releaseReport(File report) {
        try {
            // Read report file fully.
            FileInputStream inputStream = new FileInputStream(report);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stacktrace = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stacktrace.append(line).append('\n');
            }
            // Creating outgoing HTTP POST parameters.
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("manufacturer", Build.MANUFACTURER));
            params.add(new BasicNameValuePair("model", Build.MODEL));
            params.add(new BasicNameValuePair("brand", Build.BRAND));
            params.add(new BasicNameValuePair("product", Build.PRODUCT));
            params.add(new BasicNameValuePair("device", Build.DEVICE));
            params.add(new BasicNameValuePair("version_name", versionName));
            params.add(new BasicNameValuePair("version_code", String.valueOf(versionCode)));
            params.add(new BasicNameValuePair("type", "stacktrace"));
            params.add(new BasicNameValuePair("stacktrace", stacktrace.toString()));
            // Sending report.
            HttpPost httpPost = new HttpPost(REPORT_URL);
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = httpClient.execute(httpPost);
            String responseString = EntityUtils.toString(response.getEntity());
            Log.d(Settings.LOG_TAG, "report sent response: ".concat(responseString));
            JSONObject json = new JSONObject(responseString);
            if (json.get("status").equals("ok")) {
                report.delete();
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }
}
