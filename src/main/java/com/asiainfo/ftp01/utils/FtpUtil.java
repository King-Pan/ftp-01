package com.asiainfo.ftp01.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author king-pan
 * @date 2019/1/28
 * @Description ${DESCRIPTION}
 */
@Slf4j
public class FtpUtil {

    private static  String LOCAL_CHARSET = "UTF-8";
    /**
     * FTP协议里面，规定文件名编码为iso-8859-1
     */
    private static  String SERVER_CHARSET = "ISO-8859-1";

    /**
     * 连接ftp服务器
     *
     * @param ftpServerIp ftp服务器IP
     * @param ftpPort     端口
     * @param ftpUsername 用户名
     * @param ftpPassword 密码
     * @return
     * @throws SocketException
     * @throws IOException
     */
    public FTPClient connect(String ftpServerIp, String ftpPort, String ftpUsername, String ftpPassword) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        try {
            log.info("开始连接ftp服务器" + ftpServerIp + "，端口：" + ftpPort);
            // 连接至服务器，端口默认为21时，可直接通过URL连接
            ftpClient.connect(ftpServerIp, Integer.parseInt(ftpPort));
            // 登录服务器
            ftpClient.login(ftpUsername, ftpPassword);
            // 判断返回码是否合法
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                log.info("不合法连接:" + ftpClient.getReplyCode());
                // 不合法时断开连接
                ftpClient.disconnect();
                // 结束程序
                return null;
            }
            // 开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
                log.info("ftp服务器支持UTF-8");
                LOCAL_CHARSET = "UTF-8";
            } else {
                log.error("ftp服务器不支持UTF-8");
            }
            ftpClient.setControlEncoding(LOCAL_CHARSET);

            // 设置文件传输模式
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            // 被动模式
            ftpClient.enterLocalPassiveMode();
            // 设置文件类型，二进制
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            // 设置缓冲区大小
            ftpClient.setBufferSize(3072);

        } catch (IOException e) {
            log.error("连接ftp服务器异常", e);
            throw new RuntimeException("连接ftp服务器异常", e);
        }
        log.info("连接ftp服务器" + ftpServerIp + "，端口：" + ftpPort + "成功");
        return ftpClient;
    }

    /**
     * 登出服务器并断开连接
     *
     * @param ftpClient
     * @return
     * @throws IOException
     */
    public boolean disconnect(FTPClient ftpClient) throws IOException {
        boolean result = false;
        if (null != ftpClient && ftpClient.isConnected()) {
            try {
                // 登出服务器
                result = ftpClient.logout();
            } catch (IOException e) {
                log.error("退出ftp服务器异常" + e.getMessage());
                throw new RuntimeException("退出ftp服务器异常", e);
            } finally {
                // 判断连接是否存在
                if (ftpClient.isConnected()) {
                    try {
                        // 断开连接
                        ftpClient.disconnect();
                    } catch (IOException e) {
                        log.error("关闭ftp服务器异常" + e.getMessage());
                        throw new RuntimeException("关闭ftp服务器异常", e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 上传文件至ftp服务器
     *
     * @param ftpClient      ftp连接
     * @param ftpBasePath    ftp服务器默认目录
     * @param localPath      本地文件路径
     * @param localFileName  本地文件名称
     * @param remotePath     上传文件存储路径
     * @param remoteFileName 上传文件存储名称
     * @return
     */
    public boolean upload(FTPClient ftpClient, String ftpBasePath, String localPath, String localFileName, String remotePath, String remoteFileName) {
        if (StringUtils.isNotBlank(remotePath)) {
            if (remotePath.startsWith(File.separator)) {
                remotePath = ftpBasePath + remotePath;
            } else {
                remotePath = ftpBasePath + File.separator + remotePath;
            }
        }
        boolean result = false;
        InputStream fileInputStream = null;
        try {
            if (StringUtils.isNotBlank(remotePath)) {
                result = ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            } else if (StringUtils.isBlank(remotePath)) {
                result = true;
            }

            // 判断进入操作目录是否成功
            if (!result) {
                log.info("不存在路径[" + remotePath + "]");
                // 首先切换到根目录
                ftpClient.changeWorkingDirectory("/");
                // 创建目录
                String pattern = Pattern.quote(File.separator);
                String[] ftpPaths = remotePath.split(pattern);
                for (int i = 0; i < ftpPaths.length; i++) {
                    String path = ftpPaths[i];
                    if (StringUtils.isNotBlank(path)) {
                        // 目录中文字符集
                        result = ftpClient.changeWorkingDirectory(new String(path.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
                        if (result) {
                            log.info("进入操作目录[" + path + "]成功");
                        } else {
                            log.info("不存在路径[" + path + "]");
                            result = ftpClient.makeDirectory(new String(path.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
                            if (result) {
                                log.info("创建目录[" + path + "]成功");
                            } else {
                                log.error("创建目录[" + path + "]失败");
                            }
                            result = ftpClient.changeWorkingDirectory(new String(path.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
                            if (result) {
                                log.info("进入操作目录[" + path + "]成功");
                            } else {
                                log.error("进入操作目录[" + path + "]失败");
                            }
                        }
                    }
                }
                result = true;
            }
            if (result) {
                // 上传文件
                fileInputStream = new FileInputStream(new File(localPath + File.separator + localFileName));
                // 当前为目标目录，为相对路径
                result = ftpClient.storeFile(new String(remoteFileName.getBytes(LOCAL_CHARSET), SERVER_CHARSET), fileInputStream);
                if (result) {
                    log.info("将：[" + localPath + File.separator + localFileName + "]上传到：[" + remotePath + File.separator + remoteFileName + "]成功");
                } else {
                    log.info("将：[" + localPath + File.separator + localFileName + "]上传到：[" + remotePath + File.separator + remoteFileName + "]失败");
                }
            }
        } catch (IOException e) {
            log.info("将：[" + localPath + File.separator + localFileName + "]上传到：[" + remotePath + File.separator + remoteFileName + "]失败");
            e.printStackTrace();
        } finally {
            // 判断输入流是否存在
            if (null != fileInputStream) {
                try {
                    // 关闭输入流
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("上传文件至FTP异常" + e.getMessage());
                    throw new RuntimeException("上传文件至FTP异常", e);
                }
            }
        }
        return result;
    }

    /**
     * 从ftp服务器下载文件
     *
     * @param ftpClient      ftp连接
     * @param ftpBasePath    ftp服务器默认目录
     * @param remotePath     ftp服务器上的相对路径
     * @param remoteFileName 要下载的文件名
     * @param localPath      下载后保存到本地的路径
     * @param localFileName  下载后保存到本地的文件名
     * @return
     */
    public boolean download(FTPClient ftpClient, String ftpBasePath, String remotePath, String remoteFileName, String localPath, String localFileName) {
        if (StringUtils.isNotBlank(remotePath)) {
            if (remotePath.startsWith(File.separator)) {
                remotePath = ftpBasePath + remotePath;
            } else {
                remotePath = ftpBasePath + File.separator + remotePath;
            }
        }
        boolean result = false;
        File file = new File(localPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            if (StringUtils.isNotBlank(remotePath)) {
                result = ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            } else if (StringUtils.isBlank(remotePath)) {
                result = true;
            }
            if (result) {
                log.info("进入目录[" + remotePath + "]");
            } else {
                log.info("不存在路径[" + remotePath + "]");
            }
            FTPFile[] ftpFiles = ftpClient.listFiles(new String((remoteFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            if (ftpFiles.length == 1) {
                File localFile = new File(localPath + File.separator + localFileName);
                // 输出流
                OutputStream os = new FileOutputStream(localFile);
                if (!ftpClient.retrieveFile(new String((remoteFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET), os)) {
                    result = false;
                    log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载到：[" + localPath + File.separator + localFileName + "]失败");
                    throw new RuntimeException(remotePath + File.separator + remoteFileName + "文件下载失败");
                } else {
                    result = true;
                    log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载到：[" + localPath + File.separator + localFileName + "]成功");
                }
                os.close();
            } else {
                result = false;
                log.error("远程文件不存在：" + remotePath + File.separator + remoteFileName);
                log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载到：[" + localPath + File.separator + localFileName + "]失败");
            }
        } catch (IOException e) {
            result = false;
            log.error("从ftp服务器下载文件异常", e);
            log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载到：[" + localPath + File.separator + localFileName + "]失败");
        }
        return result;
    }

    /**
     * 下载文件到输出流
     *
     * @param ftpClient      ftp连接
     * @param ftpBasePath    ftp服务器默认目录
     * @param os             输出流
     * @param remotePath     ftp服务器上的相对路径
     * @param remoteFileName 要下载的文件名
     * @return
     * @throws IOException
     */
    public boolean download(FTPClient ftpClient, String ftpBasePath, OutputStream os, String remotePath, String remoteFileName) throws IOException {
        if (StringUtils.isNotBlank(remotePath)) {
            if (remotePath.startsWith(File.separator)) {
                remotePath = ftpBasePath + remotePath;
            } else {
                remotePath = ftpBasePath + File.separator + remotePath;
            }
        }
        boolean result = false;
        try {
            result = ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            if (result) {
                log.info("进入目录[" + remotePath + "]");
            } else {
                log.info("不存在路径[" + remotePath + "]");
            }
            FTPFile[] ftpFiles = ftpClient.listFiles(new String((remoteFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            if (ftpFiles.length == 1) {
                if (!ftpClient.retrieveFile(new String((remoteFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET), os)) {
                    result = false;
                    log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载失败");
                    throw new RuntimeException(remotePath + File.separator + remoteFileName + "文件下载失败");
                } else {
                    result = true;
                    log.info("将：[" + remotePath + File.separator + remoteFileName + "]下载成功");
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
     * 删除ftp服务器文件
     *
     * @param ftpClient      ftp连接
     * @param ftpBasePath    ftp服务器默认目录
     * @param remotePath     文件存储目录
     * @param remoteFileName 文件存储名称
     * @return
     */
    public boolean deleteFile(FTPClient ftpClient, String ftpBasePath, String remotePath, String remoteFileName) {
        if (StringUtils.isNotBlank(remotePath)) {
            if (remotePath.startsWith(File.separator)) {
                remotePath = ftpBasePath + remotePath;
            } else {
                remotePath = ftpBasePath + File.separator + remotePath;
            }
        }
        boolean result = false;
        // 连接至服务器
        try {
            result = ftpClient.changeWorkingDirectory(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
            if (result) {
                // 删除文件
                result = ftpClient.deleteFile(new String(remoteFileName.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
                if (result) {
                    log.info("删除在路径[" + remotePath + "]的文件[" + remoteFileName + "]成功");
                } else {
                    log.error("删除在路径[" + remotePath + "]的文件[" + remoteFileName + "]失败");
                }
            } else {
                log.info("不存在路径[" + remotePath + "]");
            }
        } catch (Exception e) {
            log.error("删除ftp服务器上的 文件异常" + e.getMessage());
        }
        return result;
    }

    /**
     * 获取指定路径下文件列表
     *
     * @param ftpClient   ftp连接
     * @param ftpBasePath ftp服务器默认目录
     * @param remotePath  文件存储路径
     * @return
     * @throws IOException
     */
    public List<String> listFiles(FTPClient ftpClient, String ftpBasePath, String remotePath) throws IOException {
        log.info("传入remotePath:" + remotePath);
        if (StringUtils.isNotBlank(remotePath)) {
            if (remotePath.startsWith(File.separator)) {
                remotePath = ftpBasePath + remotePath;
            } else {
                remotePath = ftpBasePath + File.separator + remotePath;
            }
        }
        log.info("实际remotePath:" + remotePath);
        FTPFile[] ftpFiles = ftpClient.listFiles(new String(remotePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET));
        log.info("文件个数：" + ftpFiles.length);
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < ftpFiles.length; i++) {
            if (ftpFiles[i].getType() == FTPFile.FILE_TYPE) {
                result.add(ftpFiles[i].getName());
            }
        }
        Collections.sort(result);
        Collections.reverse(result);
        return result;
    }

    /**
     * 查询ftp服务器上是否有指定文件
     *
     * @param ftpClient
     * @param ftpBasePath
     * @param path        文件路径
     * @param fileName    文件名
     * @return
     */
    public boolean checkFileExist(FTPClient ftpClient, String ftpBasePath, String path, String fileName) {
        boolean result = false;
        try {
            List<String> fileNames = listFiles(ftpClient, ftpBasePath, path);
            for (String name : fileNames) {
                if (name.equals(fileName)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }
}
