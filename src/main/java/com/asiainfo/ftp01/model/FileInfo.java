package com.asiainfo.ftp01.model;

import lombok.Data;

/**
 * @author king-pan
 * @date 2019/1/29
 * @Description ftp文件信息
 */
@Data
public class FileInfo {

    /**
     * ftp文件路径
     */
    private String path;
    /**
     * ftp文件名称
     */
    private String name;
    public FileInfo(){}
    public FileInfo(String path, String name) {
        this.path = path;
        this.name = name;
    }
}
