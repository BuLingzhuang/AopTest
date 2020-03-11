package com.blz.print;

import android.net.Uri;

public class TestUriPrint {

    public static Uri parse(String uri) {
        System.out.println("测试uri" + uri);
        return Uri.parse(uri);
    }
}
