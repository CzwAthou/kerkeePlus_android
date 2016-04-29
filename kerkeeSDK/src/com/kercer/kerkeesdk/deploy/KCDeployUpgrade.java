package com.kercer.kerkeesdk.deploy;

import com.kercer.kercore.debug.KCLog;
import com.kercer.kercore.task.KCTaskExecutor;
import com.kercer.kerkee.manifest.KCFetchManifest;
import com.kercer.kerkee.manifest.KCManifestObject;
import com.kercer.kerkee.manifest.KCManifestParser;
import com.kercer.kerkeesdk.util.KCUtilVersion;
import com.kercer.kernet.KerNet;
import com.kercer.kernet.download.KCDownloadListener;
import com.kercer.kernet.uri.KCURI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by zihong on 16/3/16.
 */
public class KCDeployUpgrade
{
    protected static String kDefaultManifestName = "cache.manifest";
    private static String kDekFileName = "tmp.dek";

    private String mManifestFileName = kDefaultManifestName;
    private KCDeploy mDeploy;

    public KCDeployUpgrade(KCDeploy aDeploy)
    {
        mDeploy = aDeploy;
    }

    /**
     * POST:
     * {
     * "platform": "android",//platform
     * "buildCode": "5",
     * "list": [//dek list of your local
     * {
     * "id": 0,//dek ID
     * "version": "1.0.2"//dek version
     * }
     * ],
     * "versionName": "2.0.3",//app version
     * "channelId": "umeng"//channel
     * }
     * <p/>
     * <p/>
     * Response:
     * success:
     * {
     * "_token": "UjdEVNiHLsF1UAtmWaLjxCqmh3QeDj8lVBmiDWSQ",
     * "list": [//dek list of can upgrade
     * {
     * "manifestUrl": "http://mob.jz-test.doumi.com/dek/cache.manifest",//dek manifest
     * "ID": 0//dek ID
     * }
     * ],
     * "code": 200//ok
     * }
     * <p/>
     * error
     * {
     * "_token": "rYbiz6goLUBTQ9WH7UFOy38LUk37Q0efgMpLaBln",
     * "name": "params error",
     * "message": "list parse error",//list args error
     * "code": "-500"
     * }
     */

//    public void check()
//    {
//        //test
//        KCStringRequest request = new KCStringRequest(KCHttpRequest.Method.GET, "http://www.linzihong.com/test/update/update", new KCHttpResult.KCHttpResultListener<String>()
//        {
//            @Override
//            public void onHttpResult(KCHttpResponse aResponse, String aResult)
//            {
//                KCLog.i(aResult);
//                try
//                {
//                    JSONObject jsonObject = new JSONObject(aResult);
//                    JSONArray jsonArray = jsonObject.getJSONArray("list");
//                    JSONObject jsWebapp =(JSONObject)jsonArray.get(0);
//                    KCWebApp webApp = new KCWebApp();
//                    webApp.mManifestUrl = jsWebapp.getString("manifestUrl");
//                    webApp.mID = jsWebapp.getInt("ID");
//                    webApp.mRootPath = new File(mDeploy.getResRootPath());
//                    upgradeWebApp(webApp);
//                }
//                catch (JSONException e)
//                {
//                    e.printStackTrace();
//                }
//
//            }
//        }, null)
//        {
//            @Override
//            public byte[] getBody() throws KCAuthFailureError
//            {
//                try
//                {
//                    JSONObject jsonParams = new JSONObject();
//                    try {
//                        jsonParams.put("versionName", "2.2.1");
//                        jsonParams.put("buildCode", "11");
//                        jsonParams.put("platform", "android");
//                        jsonParams.put("channelId", "umeng");
//                        JSONArray jsonArray = new JSONArray();
//                        jsonArray.put(new JSONObject().put("id", 0).put("version","1.0.1"));
//                        jsonParams.put("list", jsonArray);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    String body = jsonParams.toString();
//
//                    //can set charset
//                    return body.getBytes(getParamsEncoding());
//                }
//                catch (Exception e)
//                {
//                }
//
//                return super.getBody();
//            }
//        };
//
//        KerNet.newRequestRunner(null).startAsyn(request);
//
//    }

    public void upgradeWebApps(Collection<KCWebApp> aWebApps)
    {
        Iterator iterator = aWebApps.iterator();
        while (iterator.hasNext())
        {
            KCWebApp webapp = (KCWebApp) iterator.next();
            upgradeWebApp(webapp);
        }
    }


    public void upgradeWebApp(final KCWebApp aWebApp)
    {
        KCTaskExecutor.executeTask(new Runnable()
        {
            public void run()
            {
                Map mapServerManifest = KCFetchManifest.fetchServerManifests(aWebApp.mManifestUrl);

                try
                {
                    if (mapServerManifest.size() > 0)
                    {
                        for (Map.Entry<String, KCManifestObject> entry : (Set<Map.Entry<String, KCManifestObject>>) mapServerManifest.entrySet())
                        {
                            String urlManifest = entry.getKey();
                            KCManifestObject serverManifestObject = entry.getValue();
                            KCDek dek = new KCDek();
                            dek.mManifestObject = serverManifestObject;
                            dek.mManifestUri = KCURI.parse(urlManifest);
                            String relativeDir = serverManifestObject.mRelativePath.substring(0, serverManifestObject.mRelativePath.lastIndexOf(File.separator));
                            dek.mRootPath = new File(aWebApp.mRootPath+File.separator+relativeDir);
                            dek.mWebApp = aWebApp;
                            downloadDEK(dek);
                        }
                    }
                }
                catch (URISyntaxException e)
                {
                    KCLog.e(e);
                }


            }
        });
    }


    private void downloadDEK(final KCDek aDek)
    {
        if (isNeedUpgrade(aDek))
        {
            try
            {
                String downloadDir = aDek.mRootPath.getAbsolutePath();
                downloadDir = downloadDir.substring(0, downloadDir.lastIndexOf(File.separator));
                final File dekFile = new File(downloadDir + File.separator + kDekFileName);

                KCDownloadListener downloadListener = new KCDownloadListener()
                {
                    @Override
                    public void onPrepare()
                    {
                        if(dekFile.exists())
                            dekFile.delete();

//                        KCUtilFile.deleteRecyle(dekFile);
                    }

                    @Override
                    public void onReceiveFileLength(long downloadedBytes, long fileLength)
                    {
                    }

                    @Override
                    public void onProgressUpdate(long downloadedBytes, long fileLength, int speed)
                    {
                    }

                    @Override
                    public void onComplete(long downloadedBytes, long fileLength, int totalTimeInSeconds)
                    {
                        //if succes deploy
                        mDeploy.deploy(dekFile, aDek);
                    }

                    @Override
                    public void onError(long downloadedBytes, Throwable e)
                    {

                    }
                };

                KerNet.defaultDownloadEngine().startDownload(aDek.mManifestObject.getDownloadUrl(),dekFile.getAbsolutePath(), downloadListener, true, false);
            }
            catch (FileNotFoundException e)
            {
                KCLog.e(e);
            }
            catch (URISyntaxException e)
            {
                KCLog.e(e);
            }
        }

    }

    private boolean isNeedUpgrade(KCDek aDek)
    {
        boolean isNeedUpgrade = true;
        String curLocalDekVersion = getLocalDekVersion(aDek);
        if (curLocalDekVersion != null && curLocalDekVersion.length()>0)
        {
            String curAppVersion = mDeploy.getMainBundle().getVersionName();
            int dekCompare = KCUtilVersion.compareVersion(curLocalDekVersion, aDek.mManifestObject.getVersion());
            int apkCompare = KCUtilVersion.compareVersion(curAppVersion, aDek.mManifestObject.getRequiredVersion());
            if (dekCompare < 0 && apkCompare >= 0)
            {
                KCLog.e("KCDeploy", "remote dek need update");
                isNeedUpgrade = true;
            }
            else
            {
                KCLog.e("KCDeploy", "remote dek do not need update");
                isNeedUpgrade = false;
            }
        }

        return isNeedUpgrade;
    }

    private String getLocalDekVersion(KCDek aDek)
    {
        String deployManifest = aDek.mRootPath + File.separator + mManifestFileName;
        KCManifestObject manifestObject = null;
        try
        {
            manifestObject = KCManifestParser.ParserManifest(new FileInputStream(deployManifest));
        }
        catch (FileNotFoundException e)
        {
        }
        if (manifestObject != null) return manifestObject.getVersion();
        return null;
    }

    public void setManifestFileName(String aManifestFileName)
    {
        mManifestFileName = aManifestFileName;
    }

}
