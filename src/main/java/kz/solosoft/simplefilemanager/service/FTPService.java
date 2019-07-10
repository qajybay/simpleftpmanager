package kz.solosoft.simplefilemanager.service;

import kz.solosoft.simplefilemanager.model.File;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class FTPService {
    private static final Logger LOG = LoggerFactory.getLogger(FTPService.class);
    private static final String ROOT = "/";
    private FTPClient ftpClient;
    @Value("${ftp.host}")
    private String ftpHost;

    @Value("${ftp.port}")
    private Integer ftpPort;

    @Value("${ftp.username}")
    private String ftpUsername;

    @Value("${ftp.password}")
    private String ftpPassword;

    @Value("${ftp.dir.deep}")
    private int deep;

    @Value("#{'${upload.file.type}'.split(',')}")
    private List<String> fileTypes;

    public List<File> files(String path, Integer currentDeep) {
        // Рекурсивный вывод иерархии папок и файлов
        List<File> allFiles = new ArrayList<>();
        List<File> files = getSubFile(path, currentDeep);
        List<File> folders = getSubFolder(path, currentDeep);

        if (!CollectionUtils.isEmpty(folders))
            allFiles.addAll(folders);

        if (!CollectionUtils.isEmpty(files))
            allFiles.addAll(files);

        return allFiles;
    }

    private FTPClient ftpClient() {
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(ftpHost, ftpPort);
            ftpClient.login(ftpUsername, ftpPassword);
            return ftpClient;
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    private List<FTPFile> ftpPathFiles(String path) {
        List<FTPFile> ftpList = new ArrayList<>();
        try {
            ftpClient = ftpClient();
            FTPFile[] ftpFiles = ftpClient.listFiles(path);
            ftpList = Arrays.asList(ftpFiles);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return ftpList;
    }

    private String urlResolve(String path, String filename) {
        if (path.equalsIgnoreCase(ROOT)) {
            return path + filename;
        } else {
            return path + "/" + filename;
        }
    }

    private List<File> getSubFile(String path, Integer currentDeep) {

        return ftpPathFiles(path)
                .stream()
                .filter(f -> f.isFile())
                .map((FTPFile f) -> {
                    File file = new File();
                    file.setFileUrl(urlResolve(path, f.getName()));
                    file.setDate(f.getTimestamp().getTime());
                    file.setDeep(currentDeep > 1 ? currentDeep - 1 : currentDeep);
                    file.setDirectory(f.isDirectory());
                    file.setSize(f.getSize());
                    file.setName(f.getName());
                    return file;
                })
                .collect(Collectors.toList());
    }

    private List<File> getSubFolder(String path, Integer currentDeep) {

        return ftpPathFiles(path)
                .stream()
                .filter(f -> f.isDirectory())
                .map(f -> {
                    File file = new File();
                    file.setFileUrl(urlResolve(path, f.getName()));
                    file.setDeep(currentDeep);
                    file.setDate(f.getTimestamp().getTime());
                    file.setDirectory(f.isDirectory());
                    file.setFiles(files(file.getFileUrl(), currentDeep + 1));
                    return file;
                })
                .collect(Collectors.toList());
    }


    public Boolean createFile(MultipartFile file, String filepath) {

        if (checkType(file)) {
            try {
                ftpClient = ftpClient();
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

                return ftpClient.storeFile(filepath, file.getInputStream());

            } catch (IOException io) {
                LOG.error(io.getMessage());
            }
        }
        LOG.error("There is no permission to such file type");
        return false;

    }


    public Boolean createDirectory(String filepath) {
        if (checkDeep(filepath))
            try {
                ftpClient = ftpClient();
                return ftpClient.makeDirectory(filepath);
            } catch (IOException io) {
                LOG.error(io.getMessage());
            }
        LOG.error("You can't create any directory anymore. Max deep is " + deep);
        return false;
    }


    private Boolean checkDeep(String path) {
        String[] parts = path.split("/");
        return parts.length - 1 <= deep;
    }


    private boolean checkType(MultipartFile file) {
        try {
            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());
            LOG.warn("Detected type - " + detectedType);
            if (fileTypes.contains(detectedType))
                return true;
        } catch (IOException io) {
            LOG.error(io.getMessage());
        }

        return false;
    }

    public List<File> search(String filename) {
        List<File> result = new LinkedList<>();
        return find(filename, result, getAllStructure());
    }


    private List<File> getAllStructure(){
        return files(ROOT, 1);
    }


    private List<File> find(String filename, List<File> searchResult, List<File> all){
        List<File> tmp;
        tmp = all
                .stream()
                .filter(f-> !f.isDirectory() && (f.getName().contains(filename.toLowerCase())
                        || f.getName().contains(filename.toUpperCase())) )
                .collect(Collectors.toList());

        if(!CollectionUtils.isEmpty(tmp))
        searchResult.addAll(tmp);

        all
                .stream()
                .filter(f->f.isDirectory())
                .forEach(f->{
                    find(filename, searchResult, f.getFiles());
                });

        return searchResult;
    }

    private List<File> searchFile(String filename, String path, List<File> searchResult) {
    /* не оптимизированный поиск, каждый раз дергает фтп */

        List<File> files;

        List<File> tempFiles;
        List<File> tempFolders;

        files = files(path, 0);

        tempFiles = files
                .stream()
                .filter(f -> !f.isDirectory() && f.getName().contains(filename))
                .collect(Collectors.toList());

        tempFolders = files
                .stream()
                .filter(f -> f.isDirectory())
                .collect(Collectors.toList());


        if (!CollectionUtils.isEmpty(tempFiles))
            searchResult.addAll(tempFiles);

        tempFolders
                .stream()
                .filter(f -> f.isDirectory())
                .forEach(f -> {
                    searchFile(filename, f.getFileUrl(), searchResult);
                });


        return searchResult;
    }

}
