package org.niit_project.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.niit_project.backend.enums.AttachmentType;
import org.niit_project.backend.models.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    Cloudinary cloudinary;

    public String uploadFile(MultipartFile file, AttachmentType type) throws Exception{
        var resource_type = List.of(AttachmentType.image, AttachmentType.video).contains(type)?type.name().toLowerCase() :"raw";
        var params = ObjectUtils.asMap(
                "use_filename", true,
                "resource_type", resource_type,
                "unique_filename", true,
                "overwrite", false
        );
        var urls = cloudinary.uploader().upload(file.getBytes(), params);

        return urls.get("secured_url").toString();
    }

    public String uploadFile(MultipartFile file, Map<String, Object> params) throws Exception{
        var urls = cloudinary.uploader().upload(file.getBytes(), params);

        return urls.get("secured_url").toString();
    }

    public String uploadFile(File file, AttachmentType type) throws Exception{
        var resource_type = List.of(AttachmentType.image, AttachmentType.video).contains(type)?type.name().toLowerCase() :"raw";
        var params = ObjectUtils.asMap(
                "use_filename", true,
                "resource_type", resource_type,
                "unique_filename", true,
                "overwrite", false
        );
        var urls = cloudinary.uploader().upload(file, params);

        return urls.get("secured_url").toString();
    }

    public String uploadFile(File file, Map<String, Object> params) throws Exception{
        var urls = cloudinary.uploader().upload(file, params);

        return urls.get("secured_url").toString();
    }
}
