package kz.solosoft.simplefilemanager.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class File {
    private String fileUrl;
    private List<File> files;
    private Date date;
    private Integer deep;
    private Long size;
    private String name;
    private boolean isDirectory;
}
