package com.leyou.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadService {

    private static final List<String> CONTENT_TYPES = Arrays.asList("image/jpeg", "application/x-jpg");

    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file){

        String originalFilename = file.getOriginalFilename();
        String ext = StringUtils.substringAfterLast(originalFilename, ".");
        // 校验文件类型是不是图片
        String contentType = file.getContentType();
        if (!CONTENT_TYPES.contains(contentType)){
            System.out.println(originalFilename + "：文件类型不合法！");
            // 如果该文件类型不在白名单中，返回null
            return null;
        }

        try {
            // 校验文件的内容
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (bufferedImage == null) {
                System.out.println(originalFilename + "：文件内容不合法！");
                return null;
            }

            // 保存到服务器
            //file.transferTo(new File("C:\\yun7\\img-upload\\" + originalFilename));
            StorePath storePath = this.storageClient.uploadFile(file.getInputStream(), file.getSize(), ext, null);

            // 生成url
            return "http://image.leyou.com/" + storePath.getFullPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
