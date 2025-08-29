package org.example.aiedabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.aiedabackend.service.FileService;
import org.example.aiedabackend.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件管理", description = "文件上传、下载、预览相关接口")
public class FileController {

    @Autowired
    private FileService fileService;

    @Operation(summary = "文件上传", description = "上传文件到大模型服务")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<FileVO> uploadFile(
            @Parameter(description = "文件内容", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "用户ID", required = true)
            @RequestParam("uid") String uid,
            @Parameter(description = "会话ID", required = true)
            @RequestParam("sid") String sid,
            @Parameter(description = "元数据（JSON字符串）")
            @RequestParam(value = "metadata", required = false) String metadata) {
        
        try {
            if (file == null || file.isEmpty()) {
                return Response.buildFailure("未找到上传的文件", "400");
            }
            
            if (uid == null || uid.trim().isEmpty()) {
                return Response.buildFailure("用户ID不能为空", "400");
            }
            
            if (sid == null || sid.trim().isEmpty()) {
                return Response.buildFailure("会话ID不能为空", "400");
            }
            
            FileVO fileVO = fileService.uploadFile(file, uid, sid, metadata);
            
            return Response.buildSuccess(fileVO);
            
        } catch (Exception e) {
            return Response.buildFailure("文件上传失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "获取文件下载URL", description = "获取文件在大模型服务中的下载地址")
    @GetMapping("/download-url/{fid}")
    public Response<String> getDownloadUrl(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        System.out.println("FileController.getDownloadUrl 被调用，fid: " + fid);
        
        try {
            String downloadUrl = fileService.getDownloadUrl(fid);
            System.out.println("返回下载URL: " + downloadUrl);
            return Response.buildSuccess(downloadUrl);
                    
        } catch (Exception e) {
            System.out.println("获取下载URL失败: " + e.getMessage());
            e.printStackTrace();
            return Response.buildFailure("获取下载URL失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "文件下载", description = "从大模型服务下载指定文件")
    @GetMapping("/download/{fid}")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        System.out.println("FileController.downloadFile 被调用，fid: " + fid);
        
        try {
            Resource resource = fileService.downloadFile(fid);
            FileVO fileInfo = fileService.getFileInfo(fid);
            
            System.out.println("文件下载成功，准备返回: " + fileInfo.getOriginalName());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileInfo.getOriginalName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            System.out.println("文件下载失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "获取文件预览URL", description = "获取文件在大模型服务中的预览地址")
    @GetMapping("/preview-url/{fid}")
    public Response<String> getPreviewUrl(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        try {
            String previewUrl = fileService.getPreviewUrl(fid);
            return Response.buildSuccess(previewUrl);
            
        } catch (Exception e) {
            return Response.buildFailure("获取预览URL失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "文件预览", description = "预览文件内容（从大模型服务获取二进制流）")
    @GetMapping("/preview/{fid}")
    public ResponseEntity<Resource> previewFile(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        System.out.println("FileController.previewFile 被调用，fid: " + fid);
        
        try {
            Resource resource = fileService.downloadFile(fid);
            FileVO fileInfo = fileService.getFileInfo(fid);
            
            System.out.println("文件预览成功，准备返回: " + fileInfo.getOriginalName());
            
            // 根据文件类型设置Content-Type
            String contentType = determineContentType(fileInfo.getOriginalName(), fileInfo.getFileType());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + fileInfo.getOriginalName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            System.out.println("文件预览失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "文件预览（兼容接口）", description = "预览文件内容（返回JSON格式，兼容旧版本）")
    @GetMapping("/preview-legacy/{fid}")
    public Response<FilePreviewVO> previewFileLegacy(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        try {
            FilePreviewVO previewVO = fileService.previewFile(fid);
            return Response.buildSuccess(previewVO);
            
        } catch (Exception e) {
            return Response.buildFailure(e.getMessage(), "500");
        }
    }

    @Operation(summary = "文件列表查询", description = "获取用户上传的文件列表")
    @GetMapping("/list")
    public Response<FileListResponseVO> getUserFiles(
            @Parameter(description = "用户ID", required = true)
            @RequestParam("uid") String uid,
            @Parameter(description = "会话ID，可选（如果提供则只返回该会话的文件）")
            @RequestParam(value = "sid", required = false) String sid) {
        
        try {
            if (uid == null || uid.trim().isEmpty()) {
                return Response.buildFailure("需要提供uid参数", "400");
            }
            
            FileListResponseVO responseVO = fileService.getUserFiles(uid, sid);
            return Response.buildSuccess(responseVO);
            
        } catch (Exception e) {
            return Response.buildFailure("获取文件列表失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "未关联文件列表查询", description = "获取用户在指定会话中未关联记录的文件列表")
    @GetMapping("/list/unlinked")
    public Response<FileListResponseVO> getUnlinkedFiles(
            @Parameter(description = "用户ID", required = true)
            @RequestParam("uid") String uid,
            @Parameter(description = "会话ID", required = true)
            @RequestParam("sid") String sid) {
        
        try {
            if (uid == null || uid.trim().isEmpty()) {
                return Response.buildFailure("需要提供uid参数", "400");
            }
            
            if (sid == null || sid.trim().isEmpty()) {
                return Response.buildFailure("需要提供sid参数", "400");
            }
            
            FileListResponseVO responseVO = fileService.getUnlinkedFiles(uid, sid);
            return Response.buildSuccess(responseVO);
            
        } catch (Exception e) {
            return Response.buildFailure("获取未关联文件列表失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "根据记录ID获取关联文件", description = "获取指定记录ID关联的文件列表")
    @GetMapping("/list/by-record/{rid}")
    public Response<FileListResponseVO> getFilesByRecordId(
            @Parameter(description = "记录ID", required = true)
            @PathVariable("rid") Integer rid) {
        
        try {
            if (rid == null) {
                return Response.buildFailure("需要提供rid参数", "400");
            }
            
            FileListResponseVO responseVO = fileService.getFilesByRecordId(rid);
            return Response.buildSuccess(responseVO);
            
        } catch (Exception e) {
            return Response.buildFailure("获取记录关联文件失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "删除文件", description = "删除指定文件")
    @DeleteMapping("/{fid}")
    public Response<Boolean> deleteFile(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        try {
            boolean result = fileService.deleteFile(fid);
            if (result) {
                return Response.buildSuccess(true);
            } else {
                return Response.buildFailure("文件不存在或删除失败", "404");
            }
            
        } catch (Exception e) {
            return Response.buildFailure("删除文件失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "获取文件信息", description = "获取指定文件的元数据信息")
    @GetMapping("/info/{fid}")
    public Response<FileVO> getFileInfo(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String fid) {
        
        try {
            FileVO fileVO = fileService.getFileInfo(fid);
            return Response.buildSuccess(fileVO);
            
        } catch (Exception e) {
            return Response.buildFailure("获取文件信息失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "获取文件结构（平铺格式，已废弃）", description = "从大模型服务获取当前会话的文件结构（平铺格式，建议使用 /structure-tree）")
    @GetMapping("/structure")
    public Response<FileListResponseVO> getFileStructure(
            @Parameter(description = "用户ID", required = true)
            @RequestParam("uid") String uid,
            @Parameter(description = "会话ID", required = true)
            @RequestParam("sid") String sid) {
        
        try {
            if (uid == null || uid.trim().isEmpty()) {
                return Response.buildFailure("用户ID不能为空", "400");
            }
            
            if (sid == null || sid.trim().isEmpty()) {
                return Response.buildFailure("会话ID不能为空", "400");
            }
            
            FileListResponseVO responseVO = fileService.getFileStructureFromLLM(uid, sid);
            return Response.buildSuccess(responseVO);
            
        } catch (Exception e) {
            return Response.buildFailure("获取文件结构失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "获取层次化文件结构", description = "从大模型服务获取当前会话的层次化文件结构（直接返回LLM结构）")
    @GetMapping("/structure-tree")
    public Response<Map<String, Object>> getHierarchicalFileStructure(
            @Parameter(description = "用户ID", required = true)
            @RequestParam("uid") String uid,
            @Parameter(description = "会话ID", required = true)
            @RequestParam("sid") String sid) {
        
        try {
            if (uid == null || uid.trim().isEmpty()) {
                return Response.buildFailure("用户ID不能为空", "400");
            }
            
            if (sid == null || sid.trim().isEmpty()) {
                return Response.buildFailure("会话ID不能为空", "400");
            }
            
            Map<String, Object> fileStructure = fileService.getHierarchicalFileStructure(uid, sid);
            return Response.buildSuccess(fileStructure);
            
        } catch (Exception e) {
            return Response.buildFailure("获取层次化文件结构失败: " + e.getMessage(), "500");
        }
    }

    @Operation(summary = "本地文件访问", description = "通过本地路径访问文件内容")
    @GetMapping("/local/{uid}/{sid}/{filename}")
    public ResponseEntity<Resource> getLocalFile(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String uid,
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sid,
            @Parameter(description = "文件名", required = true)
            @PathVariable String filename) {
        
        System.out.println("FileController.getLocalFile 被调用，uid: " + uid + ", sid: " + sid + ", filename: " + filename);
        
        try {
            Resource resource = fileService.getFileByLocalPath(uid, sid, filename);
            
            System.out.println("本地文件访问成功，准备返回: " + filename);
            
            // 根据文件扩展名设置适当的Content-Type
            String contentType = "application/octet-stream";
            String lowerFilename = filename.toLowerCase();
            
            if (lowerFilename.endsWith(".txt")) {
                contentType = "text/plain; charset=utf-8";
            } else if (lowerFilename.endsWith(".md")) {
                contentType = "text/markdown; charset=utf-8";
            } else if (lowerFilename.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lowerFilename.endsWith(".json")) {
                contentType = "application/json; charset=utf-8";
            } else if (lowerFilename.endsWith(".xml")) {
                contentType = "application/xml; charset=utf-8";
            } else if (lowerFilename.endsWith(".csv")) {
                contentType = "text/csv; charset=utf-8";
            } else if (lowerFilename.endsWith(".html")) {
                contentType = "text/html; charset=utf-8";
            } else if (lowerFilename.endsWith(".css")) {
                contentType = "text/css; charset=utf-8";
            } else if (lowerFilename.endsWith(".js")) {
                contentType = "application/javascript; charset=utf-8";
            } else if (lowerFilename.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                contentType = "image/" + lowerFilename.substring(lowerFilename.lastIndexOf('.') + 1);
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            System.out.println("本地文件访问失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 根据文件名和MIME类型确定Content-Type
     */
    private String determineContentType(String filename, String mimeType) {
        if (filename == null || filename.trim().isEmpty()) {
            return mimeType != null ? mimeType : "application/octet-stream";
        }
        
        String lowerFilename = filename.toLowerCase();
        
        // 文本类型
        if (lowerFilename.endsWith(".txt")) {
            return "text/plain; charset=utf-8";
        } else if (lowerFilename.endsWith(".md") || lowerFilename.endsWith(".markdown")) {
            return "text/markdown; charset=utf-8";
        } else if (lowerFilename.endsWith(".json")) {
            return "application/json; charset=utf-8";
        } else if (lowerFilename.endsWith(".xml")) {
            return "application/xml; charset=utf-8";
        } else if (lowerFilename.endsWith(".csv")) {
            return "text/csv; charset=utf-8";
        } else if (lowerFilename.endsWith(".html") || lowerFilename.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        } else if (lowerFilename.endsWith(".css")) {
            return "text/css; charset=utf-8";
        } else if (lowerFilename.endsWith(".js") || lowerFilename.endsWith(".javascript")) {
            return "application/javascript; charset=utf-8";
        }
        
        // 图片类型
        else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        }
        
        // 文档类型
        else if (lowerFilename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFilename.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerFilename.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerFilename.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerFilename.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerFilename.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerFilename.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (lowerFilename.endsWith(".rtf")) {
            return "application/rtf";
        }
        
        // 回退到MIME类型或默认值
        return mimeType != null && !mimeType.trim().isEmpty() ? mimeType : "application/octet-stream";
    }
}
