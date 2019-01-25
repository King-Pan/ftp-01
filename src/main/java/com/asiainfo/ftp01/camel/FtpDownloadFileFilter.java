package com.asiainfo.ftp01.camel;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author king-pan
 * @date 2019/1/24
 * @Description ${DESCRIPTION}
 */
@Slf4j
@Component
public class FtpDownloadFileFilter implements GenericFileFilter<Object> {

    @Value("${ftp.local.data.dir}")
    private String localDir;

    public static final String EXT = ".gz";

    /**
     * 过滤下载文件
     *
     * @author sunk
     */
    @Override
    public boolean accept(GenericFile<Object> file) {
        boolean accept = false;

        System.out.println(file.getFile());
        System.out.println(file.getFile().getClass().getName());
        String localFilePath = localDir + File.separator + file.getFileName();
        log.info("文件名称:{},文件类型:{},目录:{}", file.getFileName(), file.getFile().getClass().getName(), file.isDirectory() ? "是" : "否");

        if (file.isDirectory()) {
            accept = true;
        } else {
            File localFile = new File(localFilePath);
            //本地如果存在准备下载的文件，则不下载
            if (localFile.exists()) {
                accept = false;
            }else if(file.getFileName().endsWith(EXT)){
                accept = true;
            }
        }
        return accept;
    }
}
