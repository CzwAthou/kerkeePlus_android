package com.kercer.kerkeesdk.deploy;

import com.kercer.kerkee.manifest.KCManifestObject;
import com.kercer.kerkee.manifest.KCManifestParser;
import com.kercer.kernet.uri.KCURI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Created by zihong on 16/4/6.
 */
public class KCDek
{
    //contain manifest dir path
    protected KCManifestObject mManifestObject;
    //if net,is a url; if local, is a full path
    //it is dir path
    protected KCURI mManifestUri;
    //dek root path
    protected File mRootPath;
    //the dek belongs to a webapp
    protected KCWebApp mWebApp;

    protected static String kDefaultManifestName = "cache.manifest";
    private String mManifestFileName = KCDek.kDefaultManifestName;
    protected void setManifestFileName(String aManifestFileName)
    {
        mManifestFileName = aManifestFileName;
    }


    protected KCDek(File aRootPath)
    {
        mRootPath = aRootPath;
    }

    public KCManifestObject getManifestObject()
    {
        return mManifestObject;
    }
    public KCURI getManifestUri()
    {
        return mManifestUri;
    }
    public File getRootPath()
    {
        return mRootPath;
    }
    public KCWebApp getWebApp()
    {
        return mWebApp;
    }

    protected String getLocalDekVersion()
    {
        String deployManifest = mRootPath + File.separator + mManifestFileName;
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

}
