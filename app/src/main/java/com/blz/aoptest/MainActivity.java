package com.blz.aoptest;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testDemo1();
    }

    private void testDemo1() {
//        int i = 1 + 2;
//        String localStr = "测试字符串";
        Uri localUri = Uri.parse("http://blog.hakugyokurou.net/?p=409");
//        Uri localUri1 = TestUriPrint.parse("http://blog.hakugyokurou.net/?p=409");
//        System.out.println("测试" + i);
//        System.out.println("测试str" + localStr);
//        System.out.println("测试uri" + localUri);
    }
}
