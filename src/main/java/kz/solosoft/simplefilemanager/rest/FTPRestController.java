package kz.solosoft.simplefilemanager.rest;

import kz.solosoft.simplefilemanager.model.File;
import kz.solosoft.simplefilemanager.service.FTPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/ftp")
public class FTPRestController {
    private static final Logger LOG = LoggerFactory.getLogger(FTPRestController.class);
    private static final String ROOT = "/";
    private static final Integer ROOT_DEEP = 1;

    @Autowired
    private FTPService ftpService;

    @GetMapping("/dir")
    public List<File> getFolder() {
        return ftpService.files(ROOT, ROOT_DEEP);
    }

    @GetMapping("/search")
    public List<File> getFolder(@RequestParam String filename) {
        return ftpService.search(filename);
    }

    @PostMapping("/createFile")
    public ResponseEntity<String> createFile(@RequestParam MultipartFile file, String filePath){
        if(ftpService.createFile(file,filePath))
            return new ResponseEntity<String>(HttpStatus.OK);
        return new ResponseEntity<String>(HttpStatus.NOT_MODIFIED);
    }

    @PostMapping("/createDir")
    public ResponseEntity<String> createDir(@RequestParam String filePath){
        if(ftpService.createDirectory(filePath))
            return new ResponseEntity<String>(HttpStatus.OK);
        return new ResponseEntity<String>(HttpStatus.NOT_MODIFIED);
    }

}
