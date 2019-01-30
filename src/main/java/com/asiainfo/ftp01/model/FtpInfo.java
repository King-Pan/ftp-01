package com.asiainfo.ftp01.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author king-pan
 * @date 2019/1/28
 * @Description ${DESCRIPTION}
 */
@Data
@Component
@ConfigurationProperties(prefix = "ftp.info")
public class FtpInfo {
    private String host;
    private int port = 21;
    private String username;
    private String password;

    /**
     * ftp路径
     */
    private String remotePath;
    /**
     * 本地路径
     */
    private String localPath;
    /**
     * 解压路径
     */
    private String compressPath;
}
