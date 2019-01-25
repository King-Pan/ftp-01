package com.asiainfo.ftp01.camel;

import com.asiainfo.ftp01.utils.GZipUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author king-pan
 * @date 2019/1/24
 * @Description ${DESCRIPTION}
 */
@Slf4j
@Component
public class FtpDownloadRoute extends RouteBuilder {
    @Value("${ftp.server.uri}")
    private String ftpUri;


    @Value("${ftp.local.data.dir}")
    private String localDir;


    @Autowired
    private GzipProcessor gZipProcessor;

    @Override
    public void configure() throws Exception {

        from(ftpUri).to("file:" + localDir).process(gZipProcessor)
                .log(LoggingLevel.INFO, log, "download file ${file:name} complete.");
    }
}
