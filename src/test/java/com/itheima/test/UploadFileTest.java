package com.itheima.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


public class UploadFileTest {

    @Test
    public void test1(){

        String fileName="erereww.jpg";
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
    }
}
