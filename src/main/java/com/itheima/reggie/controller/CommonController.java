package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/common")
public class CommonController {

    /**
     * 文件上传
     * @param file
     * @return
     */
    @Value("${reggie.path}")
    private String basePath;

    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        //file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除
       log.info(file.toString());
        String originalFilename = file.getOriginalFilename();//abc.jpg
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));//.jpg

        //使用UUID重新生成文件名，防止文件名称重复造成覆盖
        String fileName = UUID.randomUUID().toString()+suffix;//sdasdasda

                   File dir=new File(basePath);
                  //判断当前目录是否存在
                  if (!dir.exists()){
                      //目录不存在,创建目录
                      dir.mkdirs();//创建多级目录
                  }



        try {
            //将临时文件转存到指定目录,使用原始文件名
            file.transferTo(new File(basePath+fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }


        return R.success(fileName);
    }

    /**
     * 文件下载
     * @param name
     * @param response
     */
     @GetMapping("/download")
    public void download(String name, HttpServletResponse response, HttpServletRequest request ){

         try {
             //输入流，通过输入流读取文件内容
             FileInputStream fileInputStream =new FileInputStream(new File(basePath+name));

             //输出流，通过输出流将服务器上的文件写回浏览器，在浏览器展示图片了
             ServletOutputStream outputStream = response.getOutputStream();

             ServletContext servletContext = request.getServletContext();
             String mimeType = servletContext.getMimeType(name);//xxx.jpj
             //     response.setContentType("image/jpeg");
             response.setContentType(mimeType);

             int len= 0;
             byte[] cache=new byte[1024];

             while ((len=fileInputStream.read(cache))!=-1){
                 outputStream.write(cache,0,len);
                 outputStream.flush();

             }



         } catch (Exception e) {
             e.printStackTrace();
         }




    }


}
