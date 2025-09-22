package com.bound4.image.adapter.in.web;

import com.bound4.image.application.port.in.UploadImageCommand;
import com.bound4.image.domain.ProjectId;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public record ImageUploadRequest(
    Long projectId,
    MultipartFile[] files
) {
    public List<UploadImageCommand> toCommands() throws IOException {
        List<UploadImageCommand> commands = new ArrayList<>();
        ProjectId domainProjectId = ProjectId.of(projectId);
        
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                commands.add(new UploadImageCommand(
                    domainProjectId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
                ));
            }
        }
        
        return commands;
    }
}