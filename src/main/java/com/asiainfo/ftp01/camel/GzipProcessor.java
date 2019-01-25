package com.asiainfo.ftp01.camel;

import com.asiainfo.ftp01.utils.GZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.net.ftp.FTPFile;
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
public class GzipProcessor implements Processor {

    @Value("${ftp.local.data.dir}")
    private String localDir;


    @Value("${ftp.local.data.compress}")
    private String compressPath;

    @Override
    public void process(Exchange exchange) throws Exception {
        log.info("开始解压");
        Message message = exchange.getIn();
        GenericFile<?> gf = (GenericFile<?>) message.getBody();
        FTPFile ftpFile = (FTPFile) gf.getFile();
        String localFilePath = localDir + "/" + gf.getFileName();
        File gZipFile = new File(localFilePath);
        log.info("下载文件名:{},解压路径:{}", localFilePath, compressPath);
        GZipUtils.decompress(gZipFile, compressPath, false);
        log.info("解压完成");
    }
}
