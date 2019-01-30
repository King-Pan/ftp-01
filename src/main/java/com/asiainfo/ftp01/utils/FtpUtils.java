package com.asiainfo.ftp01.utils;

import com.asiainfo.ftp01.model.FileInfo;
import com.asiainfo.ftp01.model.FtpInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author king-pan
 * @date 2019/1/28
 * @Description ${DESCRIPTION}
 */
@Slf4j
@Component
public class FtpUtils {

    public FTPClient ftpClient = null;

    @Autowired
    private FtpInfo ftpInfo;

    private static String LOCAL_CHARSET = "UTF-8";
    /**
     * FTP协议里面，规定文件名编码为iso-8859-1
     */
    private static String SERVER_CHARSET = "ISO-8859-1";


    public FTPClient getFTPClient() {
        return getFTPClient(ftpInfo.getHost(), ftpInfo.getPort(), ftpInfo.getUsername(), ftpInfo.getPassword());
    }

    public FTPClient getFTPClient(String ftpHost, int ftpPort, String ftpUserName, String ftpPassword) {
        try {
            ftpClient = new FTPClient();
            // 连接FTP服务器
            log.info("开始连接ftp服务器" + ftpHost + "，端口：" + ftpPort);
            ftpClient.connect(ftpHost, ftpPort);
            // 登陆FTP服务器
            ftpClient.login(ftpUserName, ftpPassword);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                log.info("未连接到FTP，用户名或密码错误。");
                ftpClient.disconnect();
            } else {
                log.info("FTP连接成功。");
            }
        } catch (SocketException e) {
            log.error("FTP的IP地址可能错误，请正确配置。", e);
        } catch (IOException e) {
            log.error("FTP的端口错误,请正确配置。", e);
        }
        return ftpClient;
    }

    /**
     * 获取指定路径下文件列表
     *
     * @param remotePath 文件存储路径
     * @return
     * @throws IOException
     */
    public List<FileInfo> listFiles(String remotePath) throws IOException {
        log.info("传入remotePath:" + remotePath);
        if (StringUtils.isBlank(remotePath)) {
            throw new RuntimeException("传入的ftp路径为空");
        }

        FTPFile[] ftpFiles = ftpClient.listFiles(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
        log.info("文件个数：" + ftpFiles.length);
        List<FileInfo> result = new ArrayList<>();
        for (int i = 0; i < ftpFiles.length; i++) {
            if (ftpFiles[i].getType() == FTPFile.FILE_TYPE) {
                result.add(new FileInfo(remotePath, ftpFiles[i].getName()));
            }
            if (ftpFiles[i].getType() == FTPFile.DIRECTORY_TYPE) {
                result.addAll(listFiles(remotePath + "/" + ftpFiles[i].getName()));
            }
            log.info("文件名称:{},文件类型:{}", ftpFiles[i].getName(), ftpFiles[i].getType());
        }
        return result;
    }

    /**
     * 下载文件到输出流
     *
     * @param remotePath     ftp服务器上的相对路径
     * @param remoteFileName 要下载的文件名
     * @param localPath      本地目录
     * @return
     * @throws IOException
     */
    public boolean download(String remotePath, String remoteFileName, String localPath) throws IOException {
        log.info("ftp目录:{},ftp文件名：{}，本地目录:{}", remotePath, remoteFileName, localPath);
        boolean result = false;
        try {
            result = ftpClient.changeWorkingDirectory("~");
            if(result){
                log.info("切换到根目录成功:{}" ,ftpClient.printWorkingDirectory() );
            }else{
                log.info("切换到相对目录失败:{}" ,ftpClient.printWorkingDirectory() );
            }
            result = ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            if (result) {
                log.info("进入目录[" + remotePath + "]");
            } else {
                log.error("不存在路径[" + remotePath + "]");
            }
            FTPFile[] ftpFiles = ftpClient.listFiles(new String((remoteFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            if (ftpFiles.length == 1) {
                File localFile = new File(localPath + File.separator + remoteFileName);
                // 输出流
                OutputStream os = new FileOutputStream(localFile);
                if (!ftpClient.retrieveFile(new String((remoteFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET), os)) {
                    log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载到：[" + localPath + File.separator + remoteFileName + "]失败");
                    result = false;
                    throw new RuntimeException(remotePath + File.separator + remoteFileName + "文件下载失败");
                } else {
                    result = true;
                    log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载到：[" + localPath + File.separator + remoteFileName + "]成功");
                }
                os.close();
            } else {
                result = false;
                log.error("远程文件不存在：" + remotePath + File.separator + remoteFileName);
                log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载失败");
                throw new RuntimeException("远程文件不存在：" + remotePath + File.separator + remoteFileName);
            }
        } catch (IOException e) {
            result = false;
            log.error("从ftp服务器下载文件异常", e);
            log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载失败");
        }
        return result;
    }

    /**
     * 登出服务器并断开连接
     *
     * @return
     * @throws IOException
     */
    public void disconnect() throws IOException {
        if (null != ftpClient && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
            } catch (IOException e) {
                log.error("退出ftp服务器异常" + e.getMessage(), e);
                throw new RuntimeException("退出ftp服务器异常", e);
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        log.error("关闭ftp服务器异常" + e.getMessage(), e);
                        throw new RuntimeException("关闭ftp服务器异常", e);
                    }
                }
            }
        }
    }

}
