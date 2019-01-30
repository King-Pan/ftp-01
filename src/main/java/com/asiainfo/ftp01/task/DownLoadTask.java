package com.asiainfo.ftp01.task;

import com.asiainfo.ftp01.model.FileInfo;
import com.asiainfo.ftp01.model.FtpInfo;
import com.asiainfo.ftp01.utils.FtpUtils;
import com.asiainfo.ftp01.utils.GZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author king-pan
 * @date 2019/1/29
 * @Description ${DESCRIPTION}
 */
@Slf4j
@Component
public class DownLoadTask {

    @Autowired
    private FtpInfo ftpInfo;

    @Autowired
    private FtpUtils ftpUtils;

    @Scheduled(cron = "${ftp.quartz.cron}")
    public void download() {
        log.info("ftp路径:{},本地下载路径:{},本地解压路径:{}", ftpInfo.getRemotePath(), ftpInfo.getLocalPath(), ftpInfo.getCompressPath());
        //创建ftp连接
        ftpUtils.getFTPClient();
        try {
            List<FileInfo> fileNameList = ftpUtils.listFiles(ftpInfo.getRemotePath());
            for (FileInfo fileInfo : fileNameList) {
                log.info("ftp文件信息:{}", fileInfo);
                String localFileName = ftpInfo.getLocalPath() + File.separator + fileInfo.getName();
                File localFile = new File(localFileName);
                if (localFile.exists() || localFile.getName().endsWith(".verf")) {
                    log.info("本地已存在该文件:{}，不下载", localFileName);
                } else {
                    ftpUtils.download(fileInfo.getPath(), fileInfo.getName(), ftpInfo.getLocalPath());
                    if (localFile.exists() && localFile.getName().endsWith(".gz")) {
                        log.info("开始解压文件:{},解压到:{}", localFileName, ftpInfo.getCompressPath());
                        GZipUtils.decompress(localFile, ftpInfo.getCompressPath(), false);
                    }
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                ftpUtils.disconnect();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
